package com.satelite.reportes.dto;

import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

@Getter
@Setter
@NoArgsConstructor
public class CotizacionDTO {

    private Long id;
    private String proveedorNombre;
    private LocalDate fechaCotizacion;
    private String estado;
    private LocalDateTime creadoEn;
    private List<ItemCotizacionDTO> items;

    @Getter
    @Setter
    @NoArgsConstructor
    public static class ItemCotizacionDTO {
        private String ean;
        private String productoNombre;
        private BigDecimal precioActual;
        private BigDecimal precioCotizacion;

        public BigDecimal getAhorro() {
            if (precioActual != null && precioCotizacion != null) {
                return precioActual.subtract(precioCotizacion);
            }
            return BigDecimal.ZERO;
        }
    }
}
