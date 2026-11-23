package com.bcsystems.siga.dto;
import java.math.BigDecimal; import java.util.List;
public record NominaDesgloseResponse(Integer personaId,String personaNombre,BigDecimal sueldoBase,List<ConceptoResponse> comisiones,List<ConceptoResponse> deducciones,BigDecimal totalComisiones,BigDecimal totalDeducciones){}
