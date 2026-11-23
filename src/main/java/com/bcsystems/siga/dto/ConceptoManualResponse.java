package com.bcsystems.siga.dto;

import java.math.BigDecimal;
import java.time.LocalDate;

public record ConceptoManualResponse(Long id, Integer personaId, String personaNombre,
                                     String concepto, BigDecimal monto, LocalDate fecha,
                                     String estado, String origen) {}
