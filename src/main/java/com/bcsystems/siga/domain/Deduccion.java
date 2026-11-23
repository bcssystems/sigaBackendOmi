package com.bcsystems.siga.domain;
import com.bcsystems.siga.domain.en.*; import com.fasterxml.jackson.annotation.JsonIgnore;
import jakarta.persistence.*; import lombok.*; import java.math.BigDecimal; import java.time.LocalDate;
@Entity @Getter @Setter @Builder @NoArgsConstructor @AllArgsConstructor @Table(name="deduccion")
public class Deduccion {
 @Id @GeneratedValue(strategy=GenerationType.IDENTITY) private Long idDeduccion;
 @ManyToOne(fetch=FetchType.LAZY,optional=false) @JoinColumn(name="id_persona",nullable=false) @JsonIgnore private Persona persona;
 @ManyToOne(fetch=FetchType.LAZY) @JoinColumn(name="nomina_id") @JsonIgnore private Nomina nomina;
 @ManyToOne(fetch=FetchType.LAZY) @JoinColumn(name="prestamo_parcialidad_id",unique=true) @JsonIgnore private PrestamoParcialidad prestamoParcialidad;
 @ManyToOne(fetch=FetchType.LAZY) @JoinColumn(name="deduccion_fija_id") @JsonIgnore private DeduccionFija deduccionFija;
 private String concepto; @Column(nullable=false,precision=12,scale=2) private BigDecimal monto;
 @Column(nullable=false) private LocalDate fecha; private LocalDate periodoInicio; private LocalDate periodoFin;
 @Enumerated(EnumType.STRING) @Column(nullable=false,length=20) private EstadoDeduccion estado;
 @Enumerated(EnumType.STRING) @Column(nullable=false,length=25) private TipoDeduccion tipo;
 @Enumerated(EnumType.STRING) @Column(nullable=false,length=20) private OrigenDeduccion origen;
}
