package com.satelite.lotes.service;

import com.satelite.lotes.config.RabbitMQConfig;
import com.satelite.lotes.entity.EstadoLote;
import com.satelite.lotes.entity.Lote;
import com.satelite.lotes.event.LoteVencimientoEvent;
import com.satelite.lotes.exception.LoteNotFoundException;
import com.satelite.lotes.repository.LoteRepository;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import lombok.extern.slf4j.Slf4j;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

@Service
@Slf4j
public class LoteService {

    private final LoteRepository loteRepository;
    private final RabbitTemplate rabbitTemplate;

    public LoteService(LoteRepository loteRepository, RabbitTemplate rabbitTemplate) {
        this.loteRepository = loteRepository;
        this.rabbitTemplate = rabbitTemplate;
    }

    public List<Lote> findAll() {
        return loteRepository.findAll();
    }

    public Lote findById(Long id) {
        return loteRepository.findById(id)
                .orElseThrow(() -> new LoteNotFoundException("Lote no encontrado con id: " + id));
    }

    @Transactional
    public Lote create(Lote lote) {
        lote.actualizarEstado();
        Lote saved = loteRepository.save(lote);
        publicarEventoSiNecesario(saved);
        return saved;
    }

    @Transactional
    public Lote update(Long id, Lote loteActualizado) {
        Lote existente = findById(id);

        existente.setEan(loteActualizado.getEan());
        existente.setProductoNombre(loteActualizado.getProductoNombre());
        existente.setCantidad(loteActualizado.getCantidad());
        existente.setCostoUnitario(loteActualizado.getCostoUnitario());
        existente.setFechaVencimiento(loteActualizado.getFechaVencimiento());
        existente.setProveedor(loteActualizado.getProveedor());
        existente.actualizarEstado();

        Lote saved = loteRepository.save(existente);
        publicarEventoSiNecesario(saved);
        return saved;
    }

    @Transactional
    public void delete(Long id) {
        if (!loteRepository.existsById(id)) {
            throw new LoteNotFoundException("Lote no encontrado con id: " + id);
        }
        loteRepository.deleteById(id);
    }

    // Recalcula el color de todos los lotes según los días que les quedan y avisa solo de los que cambiaron
    @Transactional
    public int recalcularEstados() {
        List<Lote> cambiados = new ArrayList<>();
        for (Lote lote : loteRepository.findAll()) {
            EstadoLote anterior = lote.getEstado();
            lote.actualizarEstado();
            if (lote.getEstado() != anterior) {
                cambiados.add(lote);
            }
        }
        loteRepository.saveAll(cambiados);
        cambiados.forEach(this::publicarEventoSiNecesario);
        return cambiados.size();
    }

    public Map<String, Long> getSemaforoResumen() {
        return Map.of(
                "ROJO", loteRepository.countByEstado(EstadoLote.ROJO),
                "AMARILLO", loteRepository.countByEstado(EstadoLote.AMARILLO),
                "VERDE", loteRepository.countByEstado(EstadoLote.VERDE)
        );
    }
    private void publicarEventoSiNecesario(Lote lote) {
        if (lote.getEstado() == EstadoLote.ROJO || lote.getEstado() == EstadoLote.AMARILLO) {
            String routingKey = lote.getEstado() == EstadoLote.ROJO
                    ? RabbitMQConfig.ROUTING_KEY_ROJO
                    : RabbitMQConfig.ROUTING_KEY_AMARILLO;
            LoteVencimientoEvent event = new LoteVencimientoEvent(
                    lote.getId(),
                    lote.getEan(),
                    lote.getProductoNombre(),
                    lote.getDiasRestantes(),
                    lote.getEstado().name(),
                    lote.getProveedor()
            );
            try {
                rabbitTemplate.convertAndSend(RabbitMQConfig.EXCHANGE, routingKey, event);
                log.info("Evento publicado: {} para lote {} ({})", routingKey, lote.getId(), lote.getProductoNombre());
            } catch (Exception e) {
                log.error("Error publicando evento RabbitMQ: {}", e.getMessage());
            }
        }
    }
}
