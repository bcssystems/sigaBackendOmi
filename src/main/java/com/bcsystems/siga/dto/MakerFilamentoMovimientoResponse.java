package com.bcsystems.siga.dto;

import java.math.BigDecimal;
import java.time.LocalDateTime;

public record MakerFilamentoMovimientoResponse(
        Integer id,
        Integer filamentoId,
        String filamento,
        String tipo,
        BigDecimal cantidad,
        BigDecimal stockAnterior,
        BigDecimal stockNuevo,
        String referencia,
        String observacion,
        String usuario,
        LocalDateTime fechaMovimiento
) {
}
