package com.satelite.alertas.messaging;

import com.satelite.alertas.config.RabbitMQConfig;
import com.satelite.alertas.event.ResumenVencimientosEvent;
import com.satelite.alertas.service.AlertaService;
import com.satelite.alertas.telegram.TelegramService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.stereotype.Component;

import java.util.Objects;

@Component
@Slf4j
public class ResumenVencimientosConsumer {

    private final AlertaService alertaService;
    private final TelegramService telegramService;

    public ResumenVencimientosConsumer(AlertaService alertaService, TelegramService telegramService) {
        this.alertaService = alertaService;
        this.telegramService = telegramService;
    }

    // Cada lote en riesgo queda como su propia alerta en el historial; a Telegram va un solo mensaje con el resumen
    @RabbitListener(queues = RabbitMQConfig.RESUMEN_QUEUE)
    public void consumir(ResumenVencimientosEvent event) {
        long nuevas = event.getLotes() == null ? 0 : event.getLotes().stream()
                .map(lote -> alertaService.registrarAlertaDeLote(lote, false))
                .filter(Objects::nonNull)
                .count();
        telegramService.notificarTexto(event.getDetalle());
        log.info("{} ({} alertas nuevas en el historial)", event.getResumen(), nuevas);
    }
}
