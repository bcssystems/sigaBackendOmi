package com.bcsystems.siga.domain;

import com.bcsystems.siga.domain.en.*;
import com.fasterxml.jackson.annotation.JsonIgnore;
import jakarta.persistence.*;
import lombok.*;
import java.math.BigDecimal;
import java.time.LocalDateTime;

@Entity @Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
@Table(name = "marcaje")
public class Marcaje {
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY) private Integer idMarcaje;
    @ManyToOne(optional = false, fetch = FetchType.LAZY) @JoinColumn(name = "id_asistencia") @JsonIgnore private Asistencia asistencia;
    @Enumerated(EnumType.STRING) @Column(nullable = false, length = 10) private TipoMarcaje tipo;
    @Column(nullable = false) private LocalDateTime marcadoEn;
    @Column(nullable = false, precision = 10, scale = 7) private BigDecimal latitud;
    @Column(nullable = false, precision = 10, scale = 7) private BigDecimal longitud;
    private BigDecimal precisionMetros;
    private Double distanciaMetros;
    @Enumerated(EnumType.STRING) @Column(nullable = false, length = 15) private EstadoAsistencia estadoUbicacion;
    @ManyToOne(fetch = FetchType.EAGER) @JoinColumn(name = "id_sucursal") private Sucursal sucursal;
}
