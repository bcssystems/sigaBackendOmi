package com.bcsystems.siga.domain;
import com.bcsystems.siga.domain.en.*; import com.fasterxml.jackson.annotation.JsonIgnore; import jakarta.persistence.*; import lombok.*; import java.math.BigDecimal; import java.time.LocalDate;
@Entity @Getter @Setter @Builder @NoArgsConstructor @AllArgsConstructor @Table(name="prestamo_parcialidad",uniqueConstraints=@UniqueConstraint(columnNames={"prestamo_id","numero"}))
public class PrestamoParcialidad {
 @Id @GeneratedValue(strategy=GenerationType.IDENTITY) private Long idParcialidad;
 @ManyToOne(fetch=FetchType.LAZY,optional=false) @JoinColumn(name="prestamo_id",nullable=false) @JsonIgnore private PrestamoEmpleado prestamo;
 private Integer numero; private BigDecimal capital; private BigDecimal interes; private BigDecimal monto; private BigDecimal saldo; private LocalDate fechaVencimiento;
 @Enumerated(EnumType.STRING) private EstadoParcialidad estado;
}
