package com.satelite.cotizaciones.event;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.io.Serializable;
import java.math.BigDecimal;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class CotizacionProcesadaEvent implements Serializable {

    private Long cotizacionId;
    private String proveedorNombre;
    private int totalItems;
    private BigDecimal ahorroTotal;
}
