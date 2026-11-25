package com.bcsystems.siga.domain;

import jakarta.persistence.*;
import lombok.*;

import java.math.BigDecimal;

@Entity
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Table(name = "maker_filamento", indexes = {
        @Index(name = "idx_maker_filamento_activo_nombre", columnList = "activo,nombre")
})
public class MakerFilamento {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Integer id;

    @Column(nullable = false, length = 150)
    private String nombre;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "material_id", nullable = false)
    private MakerMaterial material;

    @Column(nullable = false, precision = 12, scale = 2)
    private BigDecimal precioKilo;

    @Column(nullable = false, precision = 12, scale = 3)
    @Builder.Default
    private BigDecimal stockKg = BigDecimal.ZERO;

    @Column(length = 100)
    private String marca;

    @Column(length = 80)
    private String color;

    @Column(nullable = false)
    @Builder.Default
    private Boolean activo = true;
}
