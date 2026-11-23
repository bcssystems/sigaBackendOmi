package com.bcsystems.siga.dto;
import com.bcsystems.siga.domain.en.TipoMarcaje;
import java.time.LocalDateTime;
public record MarcajeResponse(Integer asistenciaId, Integer marcajeId, TipoMarcaje tipo, LocalDateTime marcadoEn,
                              LocalDateTime entrada, LocalDateTime salida, Double distanciaMetros,
                              String estadoUbicacion, String estado, boolean fueraDePunto, String puntoNombre) {}
