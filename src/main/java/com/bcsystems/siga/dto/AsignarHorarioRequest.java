package com.bcsystems.siga.dto;
import java.time.LocalDate;
public record AsignarHorarioRequest(Integer horarioId, LocalDate vigenteDesde, LocalDate vigenteHasta) {}
