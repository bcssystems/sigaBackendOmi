package com.bcsystems.siga.domain;

import com.bcsystems.siga.domain.en.EstadoNomina;
import jakarta.persistence.*;
import lombok.*;
import java.math.BigDecimal;
import java.time.LocalDate;

@Entity @Getter @Setter @Builder @NoArgsConstructor @AllArgsConstructor
@Table(name="nomina")
public class Nomina {
    @Id @GeneratedValue(strategy=GenerationType.IDENTITY) private Long idNomina;
    @ManyToOne(fetch=FetchType.LAZY, optional=false) @JoinColumn(name="id_persona", nullable=false) private Persona persona;
    @Column(nullable=false) private LocalDate fechaNomina;
    @Column(nullable=false) private LocalDate fechaInicio;
    @Column(nullable=false) private LocalDate fechaFin;
    @Column(nullable=false, precision=12, scale=2) private BigDecimal sueldoBase;
    @Column(nullable=false, precision=12, scale=2) private BigDecimal totalComisiones;
    @Column(nullable=false, precision=12, scale=2) private BigDecimal totalDeducciones;
    @Column(nullable=false, precision=12, scale=2) private BigDecimal sueldo;
    @Enumerated(EnumType.STRING) @Column(nullable=false, length=25) private EstadoNomina estado;
}
