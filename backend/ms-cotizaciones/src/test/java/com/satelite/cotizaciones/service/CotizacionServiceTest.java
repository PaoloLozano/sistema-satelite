package com.satelite.cotizaciones.service;

import com.satelite.cotizaciones.client.ProductoClient;
import com.satelite.cotizaciones.config.RabbitMQConfig;
import com.satelite.cotizaciones.dto.ProductoDTO;
import com.satelite.cotizaciones.entity.Cotizacion;
import com.satelite.cotizaciones.entity.EstadoCotizacion;
import com.satelite.cotizaciones.entity.ItemCotizacion;
import com.satelite.cotizaciones.event.CotizacionProcesadaEvent;
import com.satelite.cotizaciones.repository.CotizacionRepository;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.amqp.rabbit.core.RabbitTemplate;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

// ms-productos (OpenFeign), la base y RabbitMQ se simulan
@ExtendWith(MockitoExtension.class)
class CotizacionServiceTest {

    @Mock private CotizacionRepository cotizacionRepository;
    @Mock private ProductoClient productoClient;
    @Mock private RabbitTemplate rabbitTemplate;

    @InjectMocks
    private CotizacionService cotizacionService;

    private Cotizacion cotizacionConUnItem(String precioCotizado, String precioActual) {
        ItemCotizacion item = new ItemCotizacion();
        item.setEan("7751519000017");
        item.setPrecioCotizacion(new BigDecimal(precioCotizado));
        if (precioActual != null) item.setPrecioActual(new BigDecimal(precioActual));
        Cotizacion c = new Cotizacion();
        c.setId(1L);
        c.setProveedorNombre("Mayorista Norte");
        c.setItems(new ArrayList<>(List.of(item)));
        item.setCotizacion(c);
        return c;
    }

    @Test
    @DisplayName("Al crear, pide el precio actual a ms-productos por OpenFeign")
    void crearCompletaPrecioActual() {
        ProductoDTO leche = new ProductoDTO();
        leche.setNombre("Leche Gloria Entera 1L");
        leche.setPrecio(new BigDecimal("3.40"));
        when(productoClient.getByEan("7751519000017")).thenReturn(leche);
        when(cotizacionRepository.save(any(Cotizacion.class))).thenAnswer(inv -> inv.getArgument(0));

        Cotizacion c = cotizacionService.create(cotizacionConUnItem("3.20", null));

        assertThat(c.getItems().get(0).getPrecioActual()).isEqualByComparingTo("3.40");
        assertThat(c.getItems().get(0).getProductoNombre()).isEqualTo("Leche Gloria Entera 1L");
    }

    @Test
    @DisplayName("Al confirmar, publica 'cotizacion.procesada' en RabbitMQ con el ahorro")
    void confirmarPublicaEvento() {
        Cotizacion c = cotizacionConUnItem("3.20", "3.40");
        when(cotizacionRepository.findById(1L)).thenReturn(Optional.of(c));
        when(cotizacionRepository.save(c)).thenReturn(c);

        cotizacionService.confirmar(1L);

        assertThat(c.getEstado()).isEqualTo(EstadoCotizacion.CONFIRMADA);
        ArgumentCaptor<CotizacionProcesadaEvent> evento = ArgumentCaptor.forClass(CotizacionProcesadaEvent.class);
        verify(rabbitTemplate).convertAndSend(eq(RabbitMQConfig.EXCHANGE), eq(RabbitMQConfig.ROUTING_KEY_COTIZACION), evento.capture());
        assertThat(evento.getValue().getAhorroTotal()).isEqualByComparingTo("0.20");
    }
}
