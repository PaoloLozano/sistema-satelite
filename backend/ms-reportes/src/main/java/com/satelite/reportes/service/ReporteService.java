package com.satelite.reportes.service;

import com.satelite.reportes.client.CotizacionClient;
import com.satelite.reportes.client.LoteClient;
import com.satelite.reportes.client.ProductoClient;
import com.satelite.reportes.dto.CotizacionDTO;
import org.springframework.stereotype.Service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.*;

@Service
@Slf4j
@RequiredArgsConstructor
public class ReporteService {

    private final LoteClient loteClient;
    private final CotizacionClient cotizacionClient;
    private final ProductoClient productoClient;

    // Ahorro = precio de catálogo (ms-productos, guardado al crear la cotización) − precio cotizado confirmado
    public Map<String, Object> getAhorro(String mes) {
        List<CotizacionDTO> confirmadas = cotizacionClient.getCotizacionesByMes(mes).stream()
                .filter(c -> "CONFIRMADA".equals(c.getEstado()))
                .toList();

        List<Map<String, Object>> filas = new ArrayList<>();
        Map<String, BigDecimal> ahorroPorProveedor = new LinkedHashMap<>();
        BigDecimal ahorroTotal = BigDecimal.ZERO;
        BigDecimal totalReferencia = BigDecimal.ZERO;

        for (CotizacionDTO c : confirmadas) {
            if (c.getItems() == null) continue;
            for (CotizacionDTO.ItemCotizacionDTO item : c.getItems()) {
                if (item.getPrecioActual() == null || item.getPrecioCotizacion() == null) continue;
                BigDecimal ahorro = item.getAhorro();
                Map<String, Object> fila = new LinkedHashMap<>();
                fila.put("producto", item.getProductoNombre());
                fila.put("ean", item.getEan());
                fila.put("proveedor", c.getProveedorNombre());
                fila.put("precioPagado", item.getPrecioCotizacion());
                fila.put("precioReferencia", item.getPrecioActual());
                fila.put("ahorroUnitario", ahorro);
                filas.add(fila);

                ahorroPorProveedor.merge(c.getProveedorNombre(), ahorro, BigDecimal::add);
                ahorroTotal = ahorroTotal.add(ahorro);
                totalReferencia = totalReferencia.add(item.getPrecioActual());
            }
        }

        BigDecimal porcentaje = totalReferencia.signum() == 0 ? BigDecimal.ZERO
                : ahorroTotal.multiply(BigDecimal.valueOf(100)).divide(totalReferencia, 1, RoundingMode.HALF_UP);

        Map<String, Object> resultado = new LinkedHashMap<>();
        resultado.put("mes", mes);
        resultado.put("cotizacionesConfirmadas", confirmadas.size());
        resultado.put("ahorroTotal", ahorroTotal);
        resultado.put("totalReferencia", totalReferencia);
        resultado.put("porcentaje", porcentaje);
        resultado.put("porProveedor", ahorroPorProveedor);
        resultado.put("filas", filas);
        return resultado;
    }

    public Map<String, Object> getDashboardKPIs() {
        Map<String, Object> kpis = new LinkedHashMap<>();

        // Semaforo de lotes
        try {
            Map<String, Long> semaforo = loteClient.getSemaforoResumen();
            kpis.put("lotesRojo", semaforo.getOrDefault("ROJO", 0L));
            kpis.put("lotesAmarillo", semaforo.getOrDefault("AMARILLO", 0L));
            kpis.put("lotesVerde", semaforo.getOrDefault("VERDE", 0L));
            kpis.put("totalLotes", semaforo.values().stream().mapToLong(Long::longValue).sum());
        } catch (Exception e) {
            log.error("Error obteniendo semaforo de lotes: {}", e.getMessage());
            kpis.put("lotesRojo", 0);
            kpis.put("lotesAmarillo", 0);
            kpis.put("lotesVerde", 0);
            kpis.put("totalLotes", 0);
        }

        // Total productos
        try {
            Map<String, Long> countMap = productoClient.getCount();
            kpis.put("totalProductos", countMap.getOrDefault("total", 0L));
        } catch (Exception e) {
            log.error("Error obteniendo count de productos: {}", e.getMessage());
            kpis.put("totalProductos", 0);
        }

        try {
            kpis.put("totalProveedores", productoClient.getProveedores().size());
        } catch (Exception e) {
            log.error("Error obteniendo proveedores: {}", e.getMessage());
            kpis.put("totalProveedores", 0);
        }

        // Cotizaciones pendientes
        try {
            List<CotizacionDTO> cotizaciones = cotizacionClient.getAllCotizaciones();
            long pendientes = cotizaciones.stream()
                    .filter(c -> "PENDIENTE".equals(c.getEstado()))
                    .count();
            long confirmadas = cotizaciones.stream()
                    .filter(c -> "CONFIRMADA".equals(c.getEstado()))
                    .count();
            kpis.put("cotizacionesPendientes", pendientes);
            kpis.put("cotizacionesConfirmadas", confirmadas);
            kpis.put("totalCotizaciones", cotizaciones.size());
        } catch (Exception e) {
            log.error("Error obteniendo cotizaciones: {}", e.getMessage());
            kpis.put("cotizacionesPendientes", 0);
            kpis.put("cotizacionesConfirmadas", 0);
            kpis.put("totalCotizaciones", 0);
        }

        return kpis;
    }
}
