package com.bcsystems.siga.domain;
import jakarta.persistence.*; import lombok.*; import java.math.BigDecimal; import java.time.LocalDate; import java.util.*;
@Entity @Getter @Setter @Builder @NoArgsConstructor @AllArgsConstructor @Table(name="deduccion_fija")
public class DeduccionFija {
 @Id @GeneratedValue(strategy=GenerationType.IDENTITY) private Long idDeduccionFija;
 @Column(nullable=false) private String concepto; @Column(nullable=false,precision=12,scale=2) private BigDecimal monto;
 @Column(nullable=false) private Boolean activo; @Column(nullable=false) private LocalDate fechaInicio; private LocalDate fechaFin;
 @ManyToMany @JoinTable(name="deduccion_fija_persona",joinColumns=@JoinColumn(name="deduccion_fija_id"),inverseJoinColumns=@JoinColumn(name="persona_id"))
 @Builder.Default private Set<Persona> personas=new HashSet<>();
}
