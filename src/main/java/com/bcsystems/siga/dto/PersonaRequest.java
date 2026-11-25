package com.bcsystems.siga.dto;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

import java.util.List;
import java.math.BigDecimal;

@JsonIgnoreProperties(ignoreUnknown = true)
public record PersonaRequest(
    @NotBlank String nombre,
    @NotBlank String apellido,
    String usuario,
    String password,
    @NotNull Integer idRol,
    List<Integer> permisosAdicionales,
    Boolean activa,
    BigDecimal sueldoSemanal
) {}
