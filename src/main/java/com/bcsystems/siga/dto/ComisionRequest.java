package com.bcsystems.siga.dto;
import com.bcsystems.siga.domain.en.TipoComision; import java.math.BigDecimal; import java.time.LocalDate;
public record ComisionRequest(Integer personaId,String concepto,BigDecimal monto,BigDecimal montoBase,BigDecimal porcentaje,TipoComision tipo,LocalDate fecha){}
