package com.bcsystems.siga.dto;

import jakarta.validation.constraints.NotBlank;

public record MakerMaterialRequest(
        @NotBlank String nombre,
        Boolean activo
) {
}
