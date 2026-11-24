package com.bcsystems.siga.dto;
import com.bcsystems.siga.domain.en.EstadoAsistencia;
import com.bcsystems.siga.domain.en.TipoMarcaje;
import java.time.LocalDateTime;
public record EvidenciaResponse(Integer evidenciaId, Integer marcajeId, Integer asistenciaId, Integer usuarioId,
                                String usuarioNombre, TipoMarcaje tipo, LocalDateTime marcadoEn,
                                 Double distanciaMetros, String estadoUbicacion, EstadoAsistencia estado,
                                 String puntoNombre) {}
