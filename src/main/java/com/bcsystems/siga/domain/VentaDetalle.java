package com.bcsystems.siga.domain;

import jakarta.persistence.*;
import lombok.*;

@Entity
@Getter
@Setter
@Builder
@AllArgsConstructor
@NoArgsConstructor
@Table(name = "venta_detalle")
public class VentaDetalle {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Integer idVentaDetalle;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "id_venta", nullable = false)
    private Venta venta;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "id_producto")
    private Producto producto;

    @Column(length = 300)
    private String descripcion;

    @Column(nullable = false, precision = 19, scale = 2)
    private java.math.BigDecimal cantidad;

    @Column(nullable = false)
    private Double precioUnitario;

    @Column(nullable = false)
    private Double subtotal;

    /**
     * Costo unitario utilizado para conservar la utilidad de la venta.
     * Para productos normales se toma de costoPromedio y para ventas rápidas
     * del precio de compra capturado en el carrito.
     */
    @Column(name = "costo_unitario", precision = 19, scale = 2)
    private java.math.BigDecimal costoUnitario;

    @Column(columnDefinition = "TEXT")
    private String atributosText;
}
