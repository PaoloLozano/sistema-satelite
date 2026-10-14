package com.satelite.cotizaciones.dto;

import java.math.BigDecimal;
import java.time.LocalDate;

public record RankingItemDTO(String proveedor, String productoNombre, String presentacion,
                             BigDecimal precio, LocalDate fecha) {
}
