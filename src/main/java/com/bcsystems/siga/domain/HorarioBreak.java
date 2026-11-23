package com.bcsystems.siga.domain;

import com.fasterxml.jackson.annotation.JsonIgnore;
import jakarta.persistence.*;
import lombok.*;
import java.time.LocalTime;

@Entity @Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
@Table(name = "horario_break")
public class HorarioBreak {
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Integer idHorarioBreak;
    @ManyToOne(optional = false, fetch = FetchType.LAZY) @JoinColumn(name = "id_horario_dia") @JsonIgnore
    private HorarioDia horarioDia;
    @Column(nullable = false) private LocalTime inicio;
    @Column(nullable = false) private LocalTime fin;
}
