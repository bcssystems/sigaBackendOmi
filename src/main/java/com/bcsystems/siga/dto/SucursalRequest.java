package com.bcsystems.siga.dto;

import jakarta.validation.constraints.NotBlank;
import java.math.BigDecimal;

public record SucursalRequest(
    @NotBlank String nombre,
    String direccion,
    String telefono,
    Boolean activa,
    BigDecimal latitud,
    BigDecimal longitud
) {}
