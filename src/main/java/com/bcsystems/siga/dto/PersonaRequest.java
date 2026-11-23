package com.bcsystems.siga.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

import java.util.List;
import java.math.BigDecimal;

public record PersonaRequest(
    @NotBlank String nombre,
    @NotBlank String apellido,
    @NotBlank String usuario,
    String password,
    @NotNull Integer idRol,
    List<Integer> permisosAdicionales,
    Boolean activa,
    BigDecimal sueldoBase,
    BigDecimal sueldoSemanal,
    BigDecimal sueldoHora
) {}
