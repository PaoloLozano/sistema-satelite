package com.satelite.alertas.messaging;

import com.satelite.alertas.config.RabbitMQConfig;
import com.satelite.alertas.entity.Alerta;
import com.satelite.alertas.event.LoteVencimientoEvent;
import com.satelite.alertas.service.AlertaService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.stereotype.Component;

@Component
@Slf4j
@RequiredArgsConstructor
public class LoteVencimientoConsumer {

    private final AlertaService alertaService;

    @RabbitListener(queues = RabbitMQConfig.LOTE_QUEUE)
    public void consumir(LoteVencimientoEvent event) {
        Alerta alerta = alertaService.registrarAlertaDeLote(event, true);
        if (alerta == null) {
            log.info("Lote {} ya tenía alerta {}: no se repite", event.getLoteId(), event.getEstado());
        } else {
            log.info("Alerta {} registrada para lote {} ({})", alerta.getTipo(), event.getLoteId(), event.getProductoNombre());
        }
    }
}
