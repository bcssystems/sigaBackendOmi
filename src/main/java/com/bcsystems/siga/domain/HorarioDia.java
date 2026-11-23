package com.bcsystems.siga.domain;

import com.bcsystems.siga.domain.en.DiaSemana;
import com.fasterxml.jackson.annotation.JsonIgnore;
import jakarta.persistence.*;
import lombok.*;
import java.time.LocalTime;
import java.util.ArrayList;
import java.util.List;

@Entity @Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
@Table(name = "horario_dia", uniqueConstraints = @UniqueConstraint(columnNames = {"id_horario", "dia"}))
public class HorarioDia {
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Integer idHorarioDia;
    @ManyToOne(optional = false, fetch = FetchType.LAZY) @JoinColumn(name = "id_horario") @JsonIgnore
    private Horario horario;
    @Enumerated(EnumType.STRING) @Column(nullable = false, length = 15)
    private DiaSemana dia;
    @Column(nullable = false) private LocalTime entrada;
    @Column(nullable = false) private LocalTime salida;
    @OneToMany(mappedBy = "horarioDia", cascade = CascadeType.ALL, orphanRemoval = true, fetch = FetchType.EAGER)
    @Builder.Default private List<HorarioBreak> breaks = new ArrayList<>();
}
