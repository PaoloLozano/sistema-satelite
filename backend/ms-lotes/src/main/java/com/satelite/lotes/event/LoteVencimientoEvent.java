package com.satelite.lotes.event;

import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.io.Serializable;

@Getter
@Setter
@NoArgsConstructor
public class LoteVencimientoEvent implements Serializable {

    private Long loteId;
    private String ean;
    private String productoNombre;
    private long diasRestantes;
    private String estado;
    private String proveedor;

    public LoteVencimientoEvent(Long loteId, String ean, String productoNombre, long diasRestantes, String estado, String proveedor) {
        this.loteId = loteId;
        this.ean = ean;
        this.productoNombre = productoNombre;
        this.diasRestantes = diasRestantes;
        this.estado = estado;
        this.proveedor = proveedor;
    }
}
