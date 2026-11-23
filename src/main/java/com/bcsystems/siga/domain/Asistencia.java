package com.bcsystems.siga.domain;

import com.bcsystems.siga.domain.en.EstadoAsistencia;
import jakarta.persistence.*;
import lombok.*;
import java.time.LocalDate;
import java.time.LocalDateTime;

@Entity @Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
@Table(name = "asistencia", uniqueConstraints = @UniqueConstraint(columnNames = {"id_persona", "fecha"}))
public class Asistencia {
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY) private Integer idAsistencia;
    @ManyToOne(optional = false, fetch = FetchType.EAGER) @JoinColumn(name = "id_persona") private Persona persona;
    @Column(nullable = false) private LocalDate fecha;
    private LocalDateTime entrada;
    private LocalDateTime salida;
    @Builder.Default private Integer minutosRetardo = 0;
    @Builder.Default private Integer minutosSalidaTemprana = 0;
    @Builder.Default private Boolean fueraDePunto = false;
    @Enumerated(EnumType.STRING) @Column(nullable = false, length = 15)
    @Builder.Default private EstadoAsistencia estado = EstadoAsistencia.PENDIENTE;
}
