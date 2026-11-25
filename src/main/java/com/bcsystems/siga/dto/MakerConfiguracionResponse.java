package com.bcsystems.siga.dto;

import java.math.BigDecimal;

public record MakerConfiguracionResponse(
        String clave,
        BigDecimal valor,
        String unidad,
        String descripcion
) {
}
