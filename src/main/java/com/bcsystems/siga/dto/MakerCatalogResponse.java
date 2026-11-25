package com.bcsystems.siga.dto;

import java.math.BigDecimal;

public record MakerCatalogResponse(
        Integer id,
        String nombre,
        String material,
        Integer materialId,
        BigDecimal precioKilo,
        BigDecimal stockKg,
        String marca,
        String color,
        Boolean activo
) {
}
