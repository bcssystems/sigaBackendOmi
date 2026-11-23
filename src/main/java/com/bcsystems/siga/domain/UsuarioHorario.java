package com.bcsystems.siga.domain;

import com.fasterxml.jackson.annotation.JsonIgnore;
import jakarta.persistence.*;
import lombok.*;
import java.time.LocalDate;

@Entity @Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
@Table(name = "usuario_horario")
public class UsuarioHorario {
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Integer idUsuarioHorario;
    @ManyToOne(optional = false, fetch = FetchType.LAZY) @JoinColumn(name = "id_persona") @JsonIgnore
    private Persona persona;
    @ManyToOne(optional = false, fetch = FetchType.EAGER) @JoinColumn(name = "id_horario")
    private Horario horario;
    @Column(nullable = false) private LocalDate vigenteDesde;
    private LocalDate vigenteHasta;
    @Column(nullable = false) @Builder.Default private Boolean activo = true;
}
