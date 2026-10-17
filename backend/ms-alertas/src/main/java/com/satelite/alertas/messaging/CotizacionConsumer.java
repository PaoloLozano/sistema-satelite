package com.satelite.alertas.messaging;

import com.satelite.alertas.config.RabbitMQConfig;
import com.satelite.alertas.entity.Alerta;
import com.satelite.alertas.entity.TipoAlerta;
import com.satelite.alertas.event.CotizacionProcesadaEvent;
import com.satelite.alertas.service.AlertaService;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.stereotype.Component;
import lombok.extern.slf4j.Slf4j;

@Component
@Slf4j
public class CotizacionConsumer {

    private final AlertaService alertaService;

    public CotizacionConsumer(AlertaService alertaService) {
        this.alertaService = alertaService;
    }

    @RabbitListener(queues = RabbitMQConfig.COTIZACION_QUEUE)
    public void consumir(CotizacionProcesadaEvent event) {
        log.info("Cotizacion confirmada: {} - {} ítems - ahorro S/. {}",
                event.getProveedorNombre(), event.getTotalItems(), event.getAhorroTotal());

        String mensaje = String.format(
                "Cotización confirmada con %s: %d ítems procesados. Ahorro total: S/. %s",
                event.getProveedorNombre(),
                event.getTotalItems(),
                event.getAhorroTotal() != null ? event.getAhorroTotal().toPlainString() : "0.00"
        );

        Alerta alerta = new Alerta(TipoAlerta.INFO, mensaje, "ms-cotizaciones", event.getCotizacionId());
        alertaService.save(alerta);
        log.info("Alerta INFO guardada para cotizacion {}", event.getCotizacionId());
    }
}
