package com.satelite.cotizaciones.service;

import com.satelite.cotizaciones.client.ProductoClient;
import com.satelite.cotizaciones.config.RabbitMQConfig;
import com.satelite.cotizaciones.dto.ProductoDTO;
import com.satelite.cotizaciones.entity.Cotizacion;
import com.satelite.cotizaciones.entity.EstadoCotizacion;
import com.satelite.cotizaciones.entity.ItemCotizacion;
import com.satelite.cotizaciones.event.CotizacionProcesadaEvent;
import com.satelite.cotizaciones.exception.CotizacionNotFoundException;
import com.satelite.cotizaciones.repository.CotizacionRepository;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.satelite.cotizaciones.dto.RankingItemDTO;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

import java.math.BigDecimal;
import java.time.YearMonth;
import java.time.format.DateTimeParseException;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

@Service
@Slf4j
@RequiredArgsConstructor
public class CotizacionService {

    private final CotizacionRepository cotizacionRepository;
    private final ProductoClient productoClient;
    private final RabbitTemplate rabbitTemplate;

    public List<Cotizacion> findAll() {
        return cotizacionRepository.findAll();
    }

    // mes en formato YYYY-MM
    public List<Cotizacion> findByMes(String mes) {
        YearMonth ym;
        try {
            ym = YearMonth.parse(mes);
        } catch (DateTimeParseException e) {
            throw new IllegalArgumentException("Mes inválido, usa el formato YYYY-MM: " + mes);
        }
        return cotizacionRepository.findByRangoFecha(ym.atDay(1), ym.plusMonths(1).atDay(1));
    }

    public Cotizacion findById(Long id) {
        return cotizacionRepository.findById(id)
                .orElseThrow(() -> new CotizacionNotFoundException("Cotizacion no encontrada con id: " + id));
    }

    // Último precio confirmado de cada proveedor para el EAN, del más barato al más caro
    public List<RankingItemDTO> rankingPorEan(String ean) {
        Map<String, RankingItemDTO> ultimoPorProveedor = new LinkedHashMap<>();
        for (ItemCotizacion item : cotizacionRepository.findItemsByEan(ean, EstadoCotizacion.CONFIRMADA)) {
            Cotizacion c = item.getCotizacion();
            ultimoPorProveedor.putIfAbsent(c.getProveedorNombre(), new RankingItemDTO(
                    c.getProveedorNombre(), item.getProductoNombre(), item.getPresentacion(),
                    item.getPrecioCotizacion(), c.getFechaCotizacion()));
        }
        return ultimoPorProveedor.values().stream()
                .filter(r -> r.precio() != null)
                .sorted(Comparator.comparing(RankingItemDTO::precio))
                .toList();
    }

    @Transactional
    public Cotizacion create(Cotizacion cotizacion) {
        // Enrich items with current product prices via Feign
        if (cotizacion.getItems() != null) {
            for (ItemCotizacion item : cotizacion.getItems()) {
                item.setCotizacion(cotizacion);
                try {
                    ProductoDTO producto = productoClient.getByEan(item.getEan());
                    if (item.getProductoNombre() == null || item.getProductoNombre().isBlank()) {
                        item.setProductoNombre(producto.getNombre());
                    }
                    item.setPrecioActual(producto.getPrecio());
                } catch (Exception e) {
                    log.warn("No se pudo obtener precio para EAN {}: {}", item.getEan(), e.getMessage());
                }
            }
        }
        return cotizacionRepository.save(cotizacion);
    }

    @Transactional
    public Cotizacion confirmar(Long id) {
        Cotizacion cotizacion = findById(id);

        if (cotizacion.getEstado() != EstadoCotizacion.PENDIENTE) {
            throw new IllegalStateException("Solo se pueden confirmar cotizaciones en estado PENDIENTE");
        }

        cotizacion.setEstado(EstadoCotizacion.CONFIRMADA);
        Cotizacion saved = cotizacionRepository.save(cotizacion);

        // Calculate total savings
        BigDecimal ahorroTotal = saved.getItems().stream()
                .map(ItemCotizacion::getAhorro)
                .reduce(BigDecimal.ZERO, BigDecimal::add);

        // Publish event
        CotizacionProcesadaEvent event = new CotizacionProcesadaEvent(
                saved.getId(),
                saved.getProveedorNombre(),
                saved.getItems().size(),
                ahorroTotal
        );

        try {
            rabbitTemplate.convertAndSend(
                    RabbitMQConfig.EXCHANGE,
                    RabbitMQConfig.ROUTING_KEY_COTIZACION,
                    event
            );
            log.info("Evento cotizacion.procesada publicado para cotizacion {}", saved.getId());
        } catch (Exception e) {
            log.error("Error publicando evento cotizacion: {}", e.getMessage());
        }

        return saved;
    }
}
