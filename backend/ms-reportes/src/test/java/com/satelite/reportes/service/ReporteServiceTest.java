package com.satelite.reportes.service;

import com.satelite.reportes.client.CotizacionClient;
import com.satelite.reportes.client.LoteClient;
import com.satelite.reportes.client.ProductoClient;
import com.satelite.reportes.dto.CotizacionDTO;
import com.satelite.reportes.exception.ServicioNoDisponibleException;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.when;

// Los tres microservicios que consulta por OpenFeign se simulan
@ExtendWith(MockitoExtension.class)
class ReporteServiceTest {

    @Mock private LoteClient loteClient;
    @Mock private CotizacionClient cotizacionClient;
    @Mock private ProductoClient productoClient;

    @InjectMocks
    private ReporteService reporteService;

    @Test
    @DisplayName("El ahorro del mes suma solo las cotizaciones confirmadas")
    void ahorroSoloDeConfirmadas() {
        CotizacionDTO.ItemCotizacionDTO leche = new CotizacionDTO.ItemCotizacionDTO();
        leche.setPrecioActual(new BigDecimal("3.40"));
        leche.setPrecioCotizacion(new BigDecimal("3.20"));
        CotizacionDTO confirmada = new CotizacionDTO();
        confirmada.setProveedorNombre("Mayorista Norte");
        confirmada.setEstado("CONFIRMADA");
        confirmada.setItems(List.of(leche));
        CotizacionDTO pendiente = new CotizacionDTO();
        pendiente.setEstado("PENDIENTE");
        pendiente.setItems(List.of(leche));
        when(cotizacionClient.getCotizacionesByMes("2026-10")).thenReturn(List.of(confirmada, pendiente));

        Map<String, Object> reporte = reporteService.getAhorro("2026-10");

        assertThat((BigDecimal) reporte.get("ahorroTotal")).isEqualByComparingTo("0.20");
    }

    @Test
    @DisplayName("Si ms-lotes está caído, el dashboard igual responde con ceros en esa parte")
    void dashboardConServicioCaido() {
        when(loteClient.getSemaforoResumen()).thenThrow(new ServicioNoDisponibleException("ms-lotes", null));
        when(productoClient.getCount()).thenReturn(Map.of("total", 6L));
        when(productoClient.getProveedores()).thenReturn(List.of());
        when(cotizacionClient.getAllCotizaciones()).thenReturn(List.of());

        Map<String, Object> kpis = reporteService.getDashboardKPIs();

        assertThat(kpis).containsEntry("lotesRojo", 0).containsEntry("totalProductos", 6L);
    }
}
