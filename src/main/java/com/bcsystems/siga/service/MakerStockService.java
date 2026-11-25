package com.bcsystems.siga.service;

import com.bcsystems.siga.domain.MakerFilamento;
import com.bcsystems.siga.domain.MakerFilamentoMovimiento;
import com.bcsystems.siga.domain.en.TipoMovimientoMaker;
import com.bcsystems.siga.dto.MakerFilamentoMovimientoRequest;
import com.bcsystems.siga.dto.MakerFilamentoMovimientoResponse;
import com.bcsystems.siga.exception.InvalidEntryException;
import com.bcsystems.siga.exception.NotFoundException;
import com.bcsystems.siga.repository.MakerFilamentoMovimientoRepository;
import com.bcsystems.siga.repository.MakerFilamentoRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.Locale;

/**
 * Mantiene el stock de filamento Maker y su bitácora en una sola transacción.
 * El stock se expresa siempre en kilogramos con tres decimales.
 */
@Service
@RequiredArgsConstructor
public class MakerStockService {
    private static final int STOCK_SCALE = 3;
    private static final RoundingMode ROUNDING = RoundingMode.HALF_UP;

    private final MakerFilamentoRepository filamentoRepository;
    private final MakerFilamentoMovimientoRepository movimientoRepository;

    @Transactional
    public MakerFilamentoMovimiento registrar(MakerFilamentoMovimientoRequest request, String usuario) {
        TipoMovimientoMaker tipo = parseTipo(request.tipo());
        if (tipo == TipoMovimientoMaker.AJUSTE) {
            if (request.stockObjetivo() == null) {
                throw new InvalidEntryException("Un ajuste requiere el stock objetivo en kilogramos");
            }
            return ajustar(request.filamentoId(), request.stockObjetivo(), request.referencia(),
                    request.observacion(), usuario);
        }
        if (request.cantidad() == null) {
            throw new InvalidEntryException("La cantidad es obligatoria para este movimiento");
        }
        return aplicar(request.filamentoId(), tipo, request.cantidad(), request.referencia(),
                request.observacion(), usuario);
    }

    @Transactional
    public MakerFilamentoMovimiento registrarEntradaInicial(Integer filamentoId, BigDecimal cantidad,
                                                            String referencia, String usuario) {
        if (cantidad == null || cantidad.signum() <= 0) {
            return null;
        }
        return aplicar(filamentoId, TipoMovimientoMaker.ENTRADA, cantidad, referencia,
                "Stock inicial del filamento", usuario);
    }

    @Transactional
    public MakerFilamentoMovimiento consumir(Integer filamentoId, BigDecimal cantidad, String referencia,
                                             String observacion, String usuario) {
        return aplicar(filamentoId, TipoMovimientoMaker.CONSUMO, cantidad, referencia, observacion, usuario);
    }

    @Transactional
    public MakerFilamentoMovimiento ajustar(Integer filamentoId, BigDecimal stockObjetivo, String referencia,
                                            String observacion, String usuario) {
        if (stockObjetivo == null || stockObjetivo.signum() < 0) {
            throw new InvalidEntryException("El stock objetivo no puede ser negativo");
        }

        MakerFilamento filamento = filamentoRepository.findByIdForUpdate(filamentoId)
                .orElseThrow(() -> new NotFoundException("Filamento no encontrado"));
        BigDecimal anterior = stock(filamento);
        BigDecimal nuevo = scale(stockObjetivo);
        BigDecimal diferencia = nuevo.subtract(anterior).abs();
        if (diferencia.signum() == 0) {
            throw new InvalidEntryException("El stock objetivo coincide con el stock actual");
        }
        return guardarMovimiento(filamento, TipoMovimientoMaker.AJUSTE, diferencia, anterior, nuevo,
                referencia, observacion, usuario);
    }

    @Transactional(readOnly = true)
    public Page<MakerFilamentoMovimientoResponse> listar(Integer filamentoId, Pageable pageable) {
        Page<MakerFilamentoMovimiento> movimientos = filamentoId == null
                ? movimientoRepository.findAllByOrderByFechaMovimientoDesc(pageable)
                : movimientoRepository.findByFilamentoIdOrderByFechaMovimientoDesc(filamentoId, pageable);
        return movimientos.map(this::response);
    }

    private MakerFilamentoMovimiento aplicar(Integer filamentoId, TipoMovimientoMaker tipo, BigDecimal cantidad,
                                             String referencia, String observacion, String usuario) {
        if (cantidad == null || cantidad.signum() <= 0) {
            throw new InvalidEntryException("La cantidad debe ser mayor a cero");
        }
        BigDecimal importe = scale(cantidad);
        if (importe.signum() == 0) {
            throw new InvalidEntryException("La cantidad debe tener al menos tres decimales de kilogramo");
        }

        MakerFilamento filamento = filamentoRepository.findByIdForUpdate(filamentoId)
                .orElseThrow(() -> new NotFoundException("Filamento no encontrado"));
        BigDecimal anterior = stock(filamento);
        BigDecimal delta = tipo == TipoMovimientoMaker.ENTRADA ? importe : importe.negate();
        BigDecimal nuevo = scale(anterior.add(delta));
        if (nuevo.signum() < 0) {
            throw new InvalidEntryException("Stock insuficiente de filamento " + filamento.getNombre()
                    + ". Disponible: " + anterior + " kg; solicitado: " + importe + " kg");
        }
        return guardarMovimiento(filamento, tipo, importe, anterior, nuevo, referencia, observacion, usuario);
    }

    private MakerFilamentoMovimiento guardarMovimiento(MakerFilamento filamento, TipoMovimientoMaker tipo,
                                                       BigDecimal cantidad, BigDecimal anterior, BigDecimal nuevo,
                                                       String referencia, String observacion, String usuario) {
        filamento.setStockKg(nuevo);
        filamentoRepository.save(filamento);
        MakerFilamentoMovimiento movimiento = MakerFilamentoMovimiento.builder()
                .filamento(filamento)
                .tipo(tipo)
                .cantidad(scale(cantidad))
                .stockAnterior(scale(anterior))
                .stockNuevo(scale(nuevo))
                .referencia(blankToNull(referencia))
                .observacion(blankToNull(observacion))
                .usuario(blankToNull(usuario))
                .build();
        return movimientoRepository.save(movimiento);
    }

    private TipoMovimientoMaker parseTipo(String value) {
        if (value == null || value.isBlank()) {
            throw new InvalidEntryException("El tipo de movimiento es obligatorio");
        }
        try {
            return TipoMovimientoMaker.valueOf(value.trim().toUpperCase(Locale.ROOT));
        } catch (IllegalArgumentException ex) {
            throw new InvalidEntryException("Tipo de movimiento Maker no válido");
        }
    }

    private BigDecimal stock(MakerFilamento filamento) {
        return scale(filamento.getStockKg() == null ? BigDecimal.ZERO : filamento.getStockKg());
    }

    private BigDecimal scale(BigDecimal value) {
        return value.setScale(STOCK_SCALE, ROUNDING);
    }

    private String blankToNull(String value) {
        return value == null || value.isBlank() ? null : value.trim();
    }

    private MakerFilamentoMovimientoResponse response(MakerFilamentoMovimiento movimiento) {
        MakerFilamento filamento = movimiento.getFilamento();
        return new MakerFilamentoMovimientoResponse(
                movimiento.getId(),
                filamento == null ? null : filamento.getId(),
                filamento == null ? null : filamento.getNombre(),
                movimiento.getTipo().name(),
                movimiento.getCantidad(),
                movimiento.getStockAnterior(),
                movimiento.getStockNuevo(),
                movimiento.getReferencia(),
                movimiento.getObservacion(),
                movimiento.getUsuario(),
                movimiento.getFechaMovimiento());
    }
}
