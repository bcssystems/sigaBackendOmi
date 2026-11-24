package com.bcsystems.siga.domain;

import com.bcsystems.siga.domain.en.EstadoNomina;
import jakarta.persistence.*;
import lombok.*;
import java.math.BigDecimal;
import java.time.LocalDate;

@Entity
@Getter @Setter @Builder @NoArgsConstructor @AllArgsConstructor
@Table(name = "nomina_lote")
public class NominaLote {
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY) private Long idLote;
    @Column(nullable = false) private LocalDate fechaInicio;
    @Column(nullable = false) private LocalDate fechaFin;
    @Column(nullable = false) private LocalDate fechaPago;
    @Column(nullable = false, precision = 12, scale = 2) private BigDecimal totalSueldoBase;
    @Column(nullable = false, precision = 12, scale = 2) private BigDecimal totalComisiones;
    @Column(nullable = false, precision = 12, scale = 2) private BigDecimal totalDeducciones;
    @Column(nullable = false, precision = 12, scale = 2) private BigDecimal totalNeto;
    @Enumerated(EnumType.STRING) @Column(nullable = false, length = 25) private EstadoNomina estado;
}
