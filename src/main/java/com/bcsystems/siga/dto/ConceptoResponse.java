package com.bcsystems.siga.dto;
import java.math.BigDecimal; import java.time.LocalDate;
public record ConceptoResponse(Long id,String concepto,BigDecimal monto,LocalDate fecha,String estado,String origen){}
