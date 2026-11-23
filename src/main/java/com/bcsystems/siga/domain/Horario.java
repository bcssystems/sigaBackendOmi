package com.bcsystems.siga.domain;

import jakarta.persistence.*;
import lombok.*;
import java.util.ArrayList;
import java.util.List;

@Entity @Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
@Table(name = "horario")
public class Horario {
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Integer idHorario;
    @Column(nullable = false, length = 100)
    private String nombre;
    @Column(nullable = false)
    @Builder.Default private Boolean activo = true;
    @OneToMany(mappedBy = "horario", cascade = CascadeType.ALL, orphanRemoval = true, fetch = FetchType.EAGER)
    @Builder.Default private List<HorarioDia> dias = new ArrayList<>();
}
