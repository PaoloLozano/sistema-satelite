package com.satelite.cotizaciones.entity;

import com.fasterxml.jackson.annotation.JsonIgnore;
import jakarta.persistence.*;
import jakarta.validation.constraints.NotBlank;

import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.math.BigDecimal;

@Entity
@Table(name = "items_cotizacion")
@Getter
@Setter
@NoArgsConstructor
public class ItemCotizacion {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "cotizacion_id", nullable = false)
    @JsonIgnore
    private Cotizacion cotizacion;

    @NotBlank
    @Column(nullable = false, length = 50)
    private String ean;

    @Column(name = "producto_nombre", length = 200)
    private String productoNombre;

    @Column(name = "precio_actual", precision = 10, scale = 2)
    private BigDecimal precioActual;

    @Column(name = "precio_cotizacion", precision = 10, scale = 2)
    private BigDecimal precioCotizacion;

    @Column(length = 100)
    private String presentacion;

    public BigDecimal getAhorro() {
        if (precioActual != null && precioCotizacion != null) {
            return precioActual.subtract(precioCotizacion);
        }
        return BigDecimal.ZERO;
    }
}
