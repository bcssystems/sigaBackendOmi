package com.bcsystems.siga.dto;
import java.math.BigDecimal; import java.util.List;
public record ComisionVentaConfiguracionRequest(BigDecimal porcentaje,List<Integer> rolIds) {}
