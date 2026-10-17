package com.satelite.cotizaciones.event;

import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.io.Serializable;
import java.math.BigDecimal;

@Getter
@Setter
@NoArgsConstructor
public class CotizacionProcesadaEvent implements Serializable {

    private Long cotizacionId;
    private String proveedorNombre;
    private int totalItems;
    private BigDecimal ahorroTotal;

    public CotizacionProcesadaEvent(Long cotizacionId, String proveedorNombre, int totalItems, BigDecimal ahorroTotal) {
        this.cotizacionId = cotizacionId;
        this.proveedorNombre = proveedorNombre;
        this.totalItems = totalItems;
        this.ahorroTotal = ahorroTotal;
    }
}
