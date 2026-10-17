package com.satelite.lotes.job;

import com.satelite.lotes.service.ResumenService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

@Component
@Slf4j
public class ResumenDiarioJob {

    private final ResumenService resumenService;

    public ResumenDiarioJob(ResumenService resumenService) {
        this.resumenService = resumenService;
    }

    // Todos los días a las 07:00 (hora de Lima), después del recálculo de las 00:05; se cambia con lotes.resumen.cron
    @Scheduled(cron = "${lotes.resumen.cron:0 0 7 * * *}", zone = "America/Lima")
    public void enviarResumen() {
        try {
            resumenService.generarYPublicar();
        } catch (Exception e) {
            log.error("Falló el resumen diario: {}", e.getMessage());
        }
    }
}
