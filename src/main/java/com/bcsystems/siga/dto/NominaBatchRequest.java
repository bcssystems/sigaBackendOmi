package com.bcsystems.siga.dto;
import java.time.LocalDate;
public record NominaBatchRequest(LocalDate fechaInicio,LocalDate fechaFin,LocalDate fechaPago){}
