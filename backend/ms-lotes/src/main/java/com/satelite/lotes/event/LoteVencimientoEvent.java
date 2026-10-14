package com.satelite.lotes.event;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.io.Serializable;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class LoteVencimientoEvent implements Serializable {

    private Long loteId;
    private String ean;
    private String productoNombre;
    private long diasRestantes;
    private String estado;
    private String proveedor;
}
