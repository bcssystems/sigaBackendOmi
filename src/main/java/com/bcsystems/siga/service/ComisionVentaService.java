package com.bcsystems.siga.service;

import com.bcsystems.siga.domain.*;
import com.bcsystems.siga.domain.en.*;
import com.bcsystems.siga.repository.ComisionRepository;
import com.bcsystems.siga.repository.VentaRepository;
import com.bcsystems.siga.repository.VentaDetalleRepository;
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
    private final VentaDetalleRepository ventaDetalleRepository;
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
            BigDecimal base = calcularUtilidad(venta);
            // Una venta sin utilidad no genera comisión.
            if (base.signum() <= 0) continue;
            BigDecimal monto = base.multiply(porcentaje).divide(BigDecimal.valueOf(100), 2, RoundingMode.HALF_UP);
            comisionRepository.save(Comision.builder()
                    .persona(persona).venta(venta).concepto("Comisión por utilidad de venta #" + venta.getIdVenta())
                    .tipoComision(TipoComision.VENTA).montoBase(base).porcentajeComision(porcentaje)
                    .montoComision(monto).estadoComision(EstadoComision.PENDIENTE)
                    .fechaComision(corte.getFechaCierre() == null ? LocalDate.now() : corte.getFechaCierre().toLocalDate())
                    .build());
            creadas++;
        }
        return creadas;
    }

    /**
     * La base de comisión es la utilidad real: total cobrado al cliente menos
     * el costo total de los productos vendidos. Usar venta.total aquí incluye
     * descuentos ya aplicados al cliente.
     */
    private BigDecimal calcularUtilidad(Venta venta) {
        BigDecimal totalCobrado = decimal(venta.getTotal());
        BigDecimal costoTotal = ventaDetalleRepository.findByVentaIdVenta(venta.getIdVenta()).stream()
                .map(this::costoDetalle)
                .reduce(BigDecimal.ZERO, BigDecimal::add);
        return totalCobrado.subtract(costoTotal).setScale(2, RoundingMode.HALF_UP);
    }

    private BigDecimal costoDetalle(VentaDetalle detalle) {
        BigDecimal costoUnitario = detalle.getCostoUnitario();
        // Los detalles históricos todavía no tienen costo_unitario; para ellos
        // se conserva compatibilidad usando el costo actual del producto.
        if (costoUnitario == null && detalle.getProducto() != null) {
            costoUnitario = decimal(detalle.getProducto().getCostoPromedio());
        }
        BigDecimal cantidad = detalle.getCantidad() == null ? BigDecimal.ZERO : detalle.getCantidad();
        return noNegativo(costoUnitario).multiply(noNegativo(cantidad));
    }

    private BigDecimal decimal(Double value) {
        return value == null || !Double.isFinite(value) ? BigDecimal.ZERO : BigDecimal.valueOf(value);
    }

    private BigDecimal noNegativo(BigDecimal value) {
        return value == null || value.signum() < 0 ? BigDecimal.ZERO : value;
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
