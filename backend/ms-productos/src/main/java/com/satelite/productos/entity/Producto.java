package com.satelite.productos.entity;

import jakarta.persistence.*;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.PositiveOrZero;

import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Entity
@Table(name = "productos")
@Getter
@Setter
@NoArgsConstructor
public class Producto {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @NotBlank
    @Column(nullable = false, length = 200)
    private String nombre;

    @NotBlank
    @Column(unique = true, nullable = false, length = 50)
    private String ean;

    @Column(length = 100)
    private String categoria;

    @Column(length = 150)
    private String proveedor;

    @NotNull
    @PositiveOrZero
    @Column(name = "stock_actual", nullable = false)
    private Integer stockActual = 0;

    @PositiveOrZero
    @Column(name = "stock_minimo")
    private Integer stockMinimo = 0;

    @Column(precision = 10, scale = 2)
    private BigDecimal precio;

    @Column(name = "creado_en")
    private LocalDateTime creadoEn;

    @PrePersist
    protected void onCreate() {
        creadoEn = LocalDateTime.now();
    }
}
