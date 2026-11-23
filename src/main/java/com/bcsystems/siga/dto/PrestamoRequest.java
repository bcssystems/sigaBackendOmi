package com.bcsystems.siga.dto;
import java.math.BigDecimal; import java.time.LocalDate;
public record PrestamoRequest(Integer personaId,BigDecimal montoPrincipal,BigDecimal tasaInteresPorcentaje,Integer numeroParcialidades,LocalDate fechaPrimerPago){}
