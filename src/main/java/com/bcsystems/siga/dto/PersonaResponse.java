package com.bcsystems.siga.dto;

import java.time.LocalDateTime;
import java.util.List;
import java.math.BigDecimal;

public record PersonaResponse(
    Integer idPersona,
    String nombre,
    String apellido,
    String usuario,
    RolResponse rol,
    Boolean activa,
    LocalDateTime fechaRegistro,
    List<Integer> permisosAdicionales,
    BigDecimal sueldoBase,
    BigDecimal sueldoSemanal,
    BigDecimal sueldoHora
) {}
