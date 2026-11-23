package com.bcsystems.siga.domain;

import com.bcsystems.siga.domain.en.EstadoAsistencia;
import com.fasterxml.jackson.annotation.JsonIgnore;
import jakarta.persistence.*;
import lombok.*;
import java.time.LocalDateTime;

@Entity @Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
@Table(name = "evidencia_asistencia")
public class EvidenciaAsistencia {
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY) private Integer idEvidencia;
    @OneToOne(optional = false) @JoinColumn(name = "id_marcaje", unique = true) @JsonIgnore private Marcaje marcaje;
    @Column(nullable = false, length = 255) private String nombreArchivo;
    @Column(nullable = false, length = 500) private String rutaArchivo;
    @Enumerated(EnumType.STRING) @Column(nullable = false, length = 15) @Builder.Default private EstadoAsistencia estado = EstadoAsistencia.PENDIENTE;
    @Column(nullable = false) private LocalDateTime creadoEn;
}
