package com.bcsystems.siga.domain;
import com.bcsystems.siga.domain.en.*; import jakarta.persistence.*; import lombok.*; import java.math.BigDecimal; import java.time.*; import java.util.*;
@Entity @Getter @Setter @Builder @NoArgsConstructor @AllArgsConstructor @Table(name="prestamo_empleado")
public class PrestamoEmpleado {
 @Id @GeneratedValue(strategy=GenerationType.IDENTITY) private Long idPrestamo;
 @ManyToOne(fetch=FetchType.LAZY,optional=false) @JoinColumn(name="id_persona",nullable=false) private Persona persona;
 @Column(nullable=false,precision=12,scale=2) private BigDecimal montoPrincipal; private BigDecimal tasaInteresPorcentaje; private Integer numeroParcialidades; private LocalDateTime fechaCreacion; private LocalDate fechaPrimerPago;
 @Enumerated(EnumType.STRING) @Column(nullable=false) private EstadoPrestamo estado;
 @OneToMany(mappedBy="prestamo",cascade=CascadeType.ALL,orphanRemoval=true) @OrderBy("numero ASC") @Builder.Default private List<PrestamoParcialidad> parcialidades=new ArrayList<>();
}
