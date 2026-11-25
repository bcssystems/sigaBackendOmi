package com.bcsystems.siga.dto;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

import java.math.BigDecimal;

public record MakerFilamentoMovimientoRequest(
        @NotNull Integer filamentoId,
        @NotBlank String tipo,
        @DecimalMin("0.001") BigDecimal cantidad,
        @DecimalMin("0.000") BigDecimal stockObjetivo,
        String referencia,
        String observacion
) {
}
