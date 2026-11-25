package com.bcsystems.siga.dto;

import jakarta.validation.Valid;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;

import java.math.BigDecimal;
import java.util.List;

public record MakerCotizacionRequest(
        String descripcion,
        @NotBlank String modo,
        @NotNull @DecimalMin("0.001") BigDecimal gramosNetos,
        @NotNull @DecimalMin("0.01") BigDecimal horasImpresion,
        @NotNull @Min(1) Integer piezasPorCama,
        @Min(0) Integer cambiosColor,
        @NotBlank String tipoPrecio,
        Integer proyectoId,
        @NotEmpty List<@Valid MakerFilamentoInput> filamentos,
        List<@Valid MakerExtraInput> extras
) {
}
