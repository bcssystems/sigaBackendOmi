package com.bcsystems.siga.domain;
import com.bcsystems.siga.domain.en.*; import com.fasterxml.jackson.annotation.JsonIgnore; import jakarta.persistence.*; import lombok.*; import java.math.BigDecimal; import java.time.LocalDate;
@Entity @Getter @Setter @Builder @NoArgsConstructor @AllArgsConstructor
@Table(name="comision", uniqueConstraints = @UniqueConstraint(name="uk_comision_venta", columnNames="venta_id"))
public class Comision {
 @Id @GeneratedValue(strategy=GenerationType.IDENTITY) private Long idComision;
 @ManyToOne(fetch=FetchType.LAZY,optional=false) @JoinColumn(name="id_persona",nullable=false) @JsonIgnore private Persona persona;
 @ManyToOne(fetch=FetchType.LAZY) @JoinColumn(name="venta_id") @JsonIgnore private Venta venta;
 @ManyToOne(fetch=FetchType.LAZY) @JoinColumn(name="nomina_id") @JsonIgnore private Nomina nomina;
 @Enumerated(EnumType.STRING) private TipoComision tipoComision; private String concepto; private BigDecimal montoBase; private BigDecimal porcentajeComision;
 @Column(nullable=false,precision=12,scale=2) private BigDecimal montoComision; @Enumerated(EnumType.STRING) private EstadoComision estadoComision;
 @Column(nullable=false) private LocalDate fechaComision; private LocalDate fechaPago;
}
