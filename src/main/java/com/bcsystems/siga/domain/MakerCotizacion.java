package com.bcsystems.siga.domain;

import jakarta.persistence.*;
import lombok.*;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

@Entity
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Table(name = "maker_cotizacion", indexes = {
        @Index(name = "idx_maker_cotizacion_fecha", columnList = "fecha"),
        @Index(name = "idx_maker_cotizacion_folio", columnList = "folio")
})
public class MakerCotizacion {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Integer id;

    @Column(nullable = false, unique = true, length = 40)
    private String folio;

    @Column(nullable = false)
    private LocalDateTime fecha;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "persona_id")
    private Persona usuario;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "proyecto_id")
    private MakerProyecto proyecto;

    @Column(length = 160)
    private String descripcion;

    @Column(nullable = false, length = 20)
    private String modo;

    @Column(name = "gramos_netos", nullable = false, precision = 12, scale = 3)
    private BigDecimal gramosNetos;

    @Column(name = "gramos_merma", nullable = false, precision = 12, scale = 3)
    private BigDecimal gramosMerma;

    @Column(name = "gramos_totales", nullable = false, precision = 12, scale = 3)
    private BigDecimal gramosTotales;

    @Column(name = "horas_impresion", nullable = false, precision = 10, scale = 2)
    private BigDecimal horasImpresion;

    @Column(name = "piezas_por_cama", nullable = false)
    private Integer piezasPorCama;

    @Column(name = "consumo_kw", nullable = false, precision = 12, scale = 4)
    private BigDecimal consumoKw;

    @Column(name = "costo_kwh", nullable = false, precision = 12, scale = 4)
    private BigDecimal costoKwh;

    @Column(name = "costo_material", nullable = false, precision = 12, scale = 2)
    private BigDecimal costoMaterial;

    @Column(name = "costo_electricidad", nullable = false, precision = 12, scale = 2)
    private BigDecimal costoElectricidad;

    @Column(name = "costo_extras", nullable = false, precision = 12, scale = 2)
    private BigDecimal costoExtras;

    @Column(name = "costo_cama", nullable = false, precision = 12, scale = 2)
    private BigDecimal costoCama;

    @Column(name = "costo_pieza", nullable = false, precision = 12, scale = 2)
    private BigDecimal costoPieza;

    @Column(name = "tipo_precio", nullable = false, length = 20)
    private String tipoPrecio;

    @Column(nullable = false, precision = 6, scale = 4)
    private BigDecimal margen;

    @Column(name = "precio_cama", nullable = false, precision = 12, scale = 2)
    private BigDecimal precioCama;

    @Column(name = "precio_pieza", nullable = false, precision = 12, scale = 2)
    private BigDecimal precioPieza;

    @Column(name = "precio_cama_final", precision = 12, scale = 2)
    private BigDecimal precioCamaFinal;

    @Column(name = "precio_pieza_final", precision = 12, scale = 2)
    private BigDecimal precioPiezaFinal;

    @OneToMany(mappedBy = "cotizacion", cascade = CascadeType.ALL, orphanRemoval = true)
    @Builder.Default
    private List<MakerCotizacionFilamento> filamentos = new ArrayList<>();

    @OneToMany(mappedBy = "cotizacion", cascade = CascadeType.ALL, orphanRemoval = true)
    @Builder.Default
    private List<MakerCotizacionExtra> extras = new ArrayList<>();
}
