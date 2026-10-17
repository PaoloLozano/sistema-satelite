package com.satelite.lotes.event;

import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.io.Serializable;
import java.util.List;

// Resumen diario: "detalle" es el texto para Telegram y "lotes" cada lote en ROJO o AMARILLO (una alerta por lote)
@Getter
@Setter
@NoArgsConstructor
public class ResumenVencimientosEvent implements Serializable {

    private int productosRojo;
    private int productosAmarillo;
    private String resumen;
    private String detalle;
    private List<LoteVencimientoEvent> lotes;

    public ResumenVencimientosEvent(int productosRojo, int productosAmarillo, String resumen, String detalle, List<LoteVencimientoEvent> lotes) {
        this.productosRojo = productosRojo;
        this.productosAmarillo = productosAmarillo;
        this.resumen = resumen;
        this.detalle = detalle;
        this.lotes = lotes;
    }
}
