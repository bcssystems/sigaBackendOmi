package com.bcsystems.siga.dto;
import java.math.BigDecimal; import java.time.LocalDate;
public record DeduccionRequest(Integer personaId,String concepto,BigDecimal monto,LocalDate fecha){}
