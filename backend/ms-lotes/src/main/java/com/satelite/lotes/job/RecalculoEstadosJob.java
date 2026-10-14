package com.satelite.lotes.job;

import com.satelite.lotes.service.LoteService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.context.event.EventListener;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

@Component
@Slf4j
@RequiredArgsConstructor
public class RecalculoEstadosJob {

    private final LoteService loteService;

    // Todos los días a las 00:05 (hora de Lima); el cron se puede cambiar con lotes.recalculo.cron
    @Scheduled(cron = "${lotes.recalculo.cron:0 5 0 * * *}", zone = "America/Lima")
    public void recalcularDiario() {
        ejecutar("programado");
    }

    // Al arrancar, por si el servicio estuvo apagado a medianoche
    @EventListener(ApplicationReadyEvent.class)
    public void alIniciar() {
        ejecutar("arranque");
    }

    private void ejecutar(String origen) {
        try {
            int cambios = loteService.recalcularEstados();
            log.info("Recálculo de semáforo ({}): {} lote(s) cambiaron de estado", origen, cambios);
        } catch (Exception e) {
            log.error("Falló el recálculo de semáforo ({}): {}", origen, e.getMessage());
        }
    }
}
