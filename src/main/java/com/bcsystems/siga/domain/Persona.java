package com.bcsystems.siga.domain;

import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.envers.Audited;
import com.fasterxml.jackson.annotation.JsonIgnore;

import java.time.LocalDateTime;
import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;

@Audited
@Entity
@Getter
@Setter
@Builder
@AllArgsConstructor
@NoArgsConstructor
@Table(name = "persona")
public class Persona {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Integer idPersona;

    @Column(nullable = false, length = 100)
    private String nombre;

    @Column(nullable = false, length = 100)
    private String apellido;

    @ManyToOne(fetch = FetchType.EAGER)
    @JoinColumn(name = "id_rol")
    private Rol rol;

    @Column(nullable = false)
    private Boolean activa;

    @CreationTimestamp
    @Column(nullable = false, updatable = false)
    private LocalDateTime fechaRegistro;

    @Column(nullable = false, unique = true, length = 100)
    private String usuario;

    @Column(length = 100)
    @JsonIgnore
    private String password;

    @Column(precision = 12, scale = 2)
    private BigDecimal sueldoBase;

    @Column(precision = 12, scale = 2)
    private BigDecimal sueldoSemanal;

    @Column(precision = 12, scale = 2)
    private BigDecimal sueldoHora;

    @OneToMany(mappedBy = "persona", cascade = CascadeType.ALL, orphanRemoval = true)
    @Builder.Default
    private List<PermisoAdicional> permisosAdicionales = new ArrayList<>();
}
