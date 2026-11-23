package com.bcsystems.siga.dto;

import java.math.BigDecimal;

public record SucursalResponse(
    Integer idSucursal,
    String nombre,
    String direccion,
    String telefono,
    Boolean activa,
    BigDecimal latitud,
    BigDecimal longitud
) {}
