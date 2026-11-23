package com.bcsystems.siga.service;

import com.bcsystems.siga.domain.*;
import com.bcsystems.siga.domain.en.*;
import com.bcsystems.siga.repository.ComisionRepository;
import com.bcsystems.siga.repository.VentaRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import java.math.*;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.*;

@Service
@RequiredArgsConstructor
public class ComisionVentaService {
    private final ComisionRepository comisionRepository;
    private final VentaRepository ventaRepository;
    private final ConfiguracionService configuracionService;

    @Transactional
    public int generarParaCorte(CorteCaja corte) {
        String rolesConfig = configuracionService.getValor("comisionVentaRoles", "");
        Set<Integer> roles = parseRoles(rolesConfig);
        BigDecimal porcentaje = BigDecimal.valueOf(configuracionService.getValorDouble("comisionVentaPorcentaje", 0));
        if (roles.isEmpty() || porcentaje.signum() <= 0) return 0;

        List<Venta> ventas = ventaRepository.findByCajaIdCajaAndEstadoAndFechaBetweenOrderByFechaDesc(
                corte.getCaja().getIdCaja(), EstadoVenta.COMPLETADA,
                corte.getFechaApertura(), corte.getFechaCierre());
        int creadas = 0;
        for (Venta venta : ventas) {
            Persona persona = venta.getUsuario();
            if (persona == null || persona.getRol() == null || !roles.contains(persona.getRol().getIdRol())) continue;
            if (comisionRepository.existsByVentaIdVenta(venta.getIdVenta())) continue;
            BigDecimal base = BigDecimal.valueOf(venta.getTotal()).setScale(2, RoundingMode.HALF_UP);
            BigDecimal monto = base.multiply(porcentaje).divide(BigDecimal.valueOf(100), 2, RoundingMode.HALF_UP);
            comisionRepository.save(Comision.builder()
                    .persona(persona).venta(venta).concepto("Comisión por venta #" + venta.getIdVenta())
                    .tipoComision(TipoComision.VENTA).montoBase(base).porcentajeComision(porcentaje)
                    .montoComision(monto).estadoComision(EstadoComision.PENDIENTE)
                    .fechaComision(corte.getFechaCierre() == null ? LocalDate.now() : corte.getFechaCierre().toLocalDate())
                    .build());
            creadas++;
        }
        return creadas;
    }

    @Transactional
    public void cancelarPorVenta(Integer ventaId) {
        comisionRepository.findByVentaIdVenta(ventaId).forEach(comision -> {
            if (comision.getNomina() == null) {
                comision.setEstadoComision(EstadoComision.CANCELADA);
                comisionRepository.save(comision);
            }
        });
    }

    private Set<Integer> parseRoles(String value) {
        Set<Integer> result = new HashSet<>();
        for (String item : value.split(",")) {
            try { if (!item.isBlank()) result.add(Integer.valueOf(item.trim())); }
            catch (NumberFormatException ignored) { }
        }
        return result;
    }
}
