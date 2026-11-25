package com.bcsystems.siga.domain;

import com.bcsystems.siga.domain.en.TipoMovimientoMaker;
import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Entity
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Table(name = "maker_filamento_movimiento", indexes = {
        @Index(name = "idx_maker_movimiento_filamento_fecha", columnList = "filamento_id,fecha_movimiento"),
        @Index(name = "idx_maker_movimiento_tipo_fecha", columnList = "tipo,fecha_movimiento")
})
public class MakerFilamentoMovimiento {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Integer id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "filamento_id", nullable = false)
    private MakerFilamento filamento;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private TipoMovimientoMaker tipo;

    /** Importe positivo de la operación; para AJUSTE es la diferencia absoluta. */
    @Column(nullable = false, precision = 12, scale = 3)
    private BigDecimal cantidad;

    @Column(name = "stock_anterior", nullable = false, precision = 12, scale = 3)
    private BigDecimal stockAnterior;

    @Column(name = "stock_nuevo", nullable = false, precision = 12, scale = 3)
    private BigDecimal stockNuevo;

    @Column(length = 80)
    private String referencia;

    @Column(length = 100)
    private String usuario;

    @Column(columnDefinition = "TEXT")
    private String observacion;

    @CreationTimestamp
    @Column(name = "fecha_movimiento", nullable = false, updatable = false)
    private LocalDateTime fechaMovimiento;
}
