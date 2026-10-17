package com.satelite.lotes.service;

import com.satelite.lotes.config.RabbitMQConfig;
import com.satelite.lotes.entity.EstadoLote;
import com.satelite.lotes.entity.Lote;
import com.satelite.lotes.event.LoteVencimientoEvent;
import com.satelite.lotes.event.ResumenVencimientosEvent;
import com.satelite.lotes.repository.LoteRepository;
import lombok.extern.slf4j.Slf4j;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Service
@Slf4j
public class ResumenService {

    private static final int MAX_POR_ESTADO = 25;
    private static final DateTimeFormatter FECHA = DateTimeFormatter.ofPattern("dd/MM/yyyy");

    private final LoteRepository loteRepository;
    private final RabbitTemplate rabbitTemplate;

    public ResumenService(LoteRepository loteRepository, RabbitTemplate rabbitTemplate) {
        this.loteRepository = loteRepository;
        this.rabbitTemplate = rabbitTemplate;
    }

    public record Resultado(int rojo, int amarillo, boolean enviado) {}

    // Un producto entra al resumen según su lote más próximo a vencer; si no hay ninguno en ROJO o AMARILLO no se envía nada
    public Resultado generarYPublicar() {
        Map<String, Lote> proximoPorEan = new HashMap<>();
        List<LoteVencimientoEvent> enRiesgo = new ArrayList<>();
        for (Lote lote : loteRepository.findAll()) {
            lote.actualizarEstado();
            if (lote.getEstado() != EstadoLote.VERDE) {
                enRiesgo.add(new LoteVencimientoEvent(lote.getId(), lote.getEan(), lote.getProductoNombre(),
                        lote.getDiasRestantes(), lote.getEstado().name(), lote.getProveedor()));
            }
            proximoPorEan.merge(lote.getEan(), lote,
                    (a, b) -> b.getDiasRestantes() < a.getDiasRestantes() ? b : a);
        }
        List<Lote> rojos = filtrar(proximoPorEan, EstadoLote.ROJO);
        List<Lote> amarillos = filtrar(proximoPorEan, EstadoLote.AMARILLO);
        if (rojos.isEmpty() && amarillos.isEmpty()) {
            log.info("Resumen diario: no hay productos en ROJO ni AMARILLO, no se envía");
            return new Resultado(0, 0, false);
        }

        String resumen = String.format("Resumen diario: %d producto%s en ROJO y %d en AMARILLO",
                rojos.size(), rojos.size() == 1 ? "" : "s", amarillos.size());
        StringBuilder detalle = new StringBuilder("Resumen de vencimientos - ")
                .append(LocalDate.now().format(FECHA));
        agregarSeccion(detalle, "ROJO", rojos);
        agregarSeccion(detalle, "AMARILLO", amarillos);

        try {
            rabbitTemplate.convertAndSend(RabbitMQConfig.EXCHANGE, RabbitMQConfig.ROUTING_KEY_RESUMEN,
                    new ResumenVencimientosEvent(rojos.size(), amarillos.size(), resumen, detalle.toString(), enRiesgo));
            log.info("{} (publicado)", resumen);
            return new Resultado(rojos.size(), amarillos.size(), true);
        } catch (Exception e) {
            log.error("No se pudo publicar el resumen diario: {}", e.getMessage());
            return new Resultado(rojos.size(), amarillos.size(), false);
        }
    }

    private List<Lote> filtrar(Map<String, Lote> lotes, EstadoLote estado) {
        return lotes.values().stream()
                .filter(l -> l.getEstado() == estado)
                .sorted(Comparator.comparingLong(Lote::getDiasRestantes))
                .toList();
    }

    private void agregarSeccion(StringBuilder sb, String titulo, List<Lote> lotes) {
        if (lotes.isEmpty()) return;
        sb.append("\n\n").append(titulo).append(" (").append(lotes.size())
                .append(lotes.size() == 1 ? " producto):" : " productos):");
        lotes.stream().limit(MAX_POR_ESTADO).forEach(l -> sb.append("\n- ")
                .append(l.getProductoNombre()).append(": ").append(l.getCantidad()).append(" und, ")
                .append(cuandoVence(l)).append(" (").append(l.getFechaVencimiento().format(FECHA)).append(")"));
        if (lotes.size() > MAX_POR_ESTADO) {
            sb.append("\n... y ").append(lotes.size() - MAX_POR_ESTADO).append(" más");
        }
    }

    private String cuandoVence(Lote l) {
        long dias = l.getDiasRestantes();
        if (dias < 0) return "venció hace " + (-dias) + (dias == -1 ? " día" : " días");
        if (dias == 0) return "vence hoy";
        return "vence en " + dias + (dias == 1 ? " día" : " días");
    }
}
