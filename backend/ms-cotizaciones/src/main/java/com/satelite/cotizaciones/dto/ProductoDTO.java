package com.satelite.cotizaciones.dto;

import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.math.BigDecimal;

@Getter
@Setter
@NoArgsConstructor
public class ProductoDTO {

    private Long id;
    private String nombre;
    private String ean;
    private String categoria;
    private String proveedor;
    private Integer stockActual;
    private BigDecimal precio;
}
