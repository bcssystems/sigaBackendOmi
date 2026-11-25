package com.bcsystems.siga.domain;

import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

@Entity
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Table(name = "maker_proyecto", indexes = {
        @Index(name = "idx_maker_proyecto_estado", columnList = "estado"),
        @Index(name = "idx_maker_proyecto_fecha", columnList = "fecha_fin")
})
public class MakerProyecto {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Integer id;

    @Column(nullable = false, length = 160)
    private String nombre;

    @Lob
    private String descripcion;

    @Column(nullable = false, length = 20)
    private String estado;

    @Column(nullable = false, length = 20)
    private String prioridad;

    @Column(nullable = false, length = 20)
    private String color;

    @Column(name = "fecha_fin")
    private LocalDate fechaFin;

    @Column(name = "fecha_creacion", nullable = false, updatable = false)
    private LocalDateTime fechaCreacion;

    @Column(name = "filamento_descontado", nullable = false)
    @Builder.Default
    private Boolean filamentoDescontado = false;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "persona_id")
    private Persona creador;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "cotizacion_id")
    private MakerCotizacion cotizacion;

    @ManyToMany
    @JoinTable(name = "maker_proyecto_persona",
            joinColumns = @JoinColumn(name = "proyecto_id"),
            inverseJoinColumns = @JoinColumn(name = "persona_id"))
    @Builder.Default
    private List<Persona> asignados = new ArrayList<>();
}
