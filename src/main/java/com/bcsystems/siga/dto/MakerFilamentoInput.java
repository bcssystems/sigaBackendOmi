package com.bcsystems.siga.dto;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotNull;

import java.math.BigDecimal;

public record MakerFilamentoInput(
        @NotNull Integer filamentoId,
        @NotNull @DecimalMin("0.001") BigDecimal gramos
) {
}
