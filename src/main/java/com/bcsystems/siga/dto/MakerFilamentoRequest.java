package com.bcsystems.siga.dto;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

import java.math.BigDecimal;

public record MakerFilamentoRequest(
        @NotBlank String nombre,
        @NotNull Integer materialId,
        @NotNull @DecimalMin("0.00") BigDecimal precioKilo,
        @DecimalMin("0.000") BigDecimal stockKg,
        String marca,
        String color,
        Boolean activo
) {
}
