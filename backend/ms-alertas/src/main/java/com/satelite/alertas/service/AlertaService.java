package com.satelite.alertas.service;

import com.satelite.alertas.entity.Alerta;
import com.satelite.alertas.entity.TipoAlerta;
import com.satelite.alertas.event.LoteVencimientoEvent;
import com.satelite.alertas.exception.AlertaNotFoundException;
import com.satelite.alertas.repository.AlertaRepository;
import com.satelite.alertas.telegram.TelegramService;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import lombok.RequiredArgsConstructor;

import java.util.List;
import java.util.Map;

@Service
@RequiredArgsConstructor
public class AlertaService {

    private final AlertaRepository alertaRepository;
    private final TelegramService telegramService;

    public List<Alerta> findAll() {
        return alertaRepository.findAllByOrderByEnviadoAtDesc();
    }

    public List<Alerta> findByTipo(TipoAlerta tipo) {
        return alertaRepository.findByTipoOrderByEnviadoAtDesc(tipo);
    }

    @Transactional
    public Alerta save(Alerta alerta) {
        Alerta guardada = alertaRepository.save(alerta);
        telegramService.notificar(guardada);
        return guardada;
    }


    // Un lote genera una sola alerta por estado: si ya hay una AMARILLO no se repite al editarlo o en el resumen diario,
    // pero cuando pasa a ROJO se registra la nueva. Devuelve null si ya existía.
    @Transactional
    public Alerta registrarAlertaDeLote(LoteVencimientoEvent e, boolean enviarATelegram) {
        TipoAlerta tipo = "ROJO".equals(e.getEstado()) ? TipoAlerta.ROJO : TipoAlerta.AMARILLO;
        if (e.getLoteId() != null && alertaRepository.existsByReferenciaIdAndTipo(e.getLoteId(), tipo)) {
            return null;
        }
        long dias = e.getDiasRestantes();
        String cuando = dias < 0 ? "venció hace " + (-dias) + " días" : dias == 0 ? "vence hoy" : dias + " días restantes";
        String mensaje = String.format("Lote próximo a vencer: %s (EAN: %s) - %s. Proveedor: %s",
                e.getProductoNombre(), e.getEan(), cuando, e.getProveedor() != null ? e.getProveedor() : "N/A");
        Alerta guardada = alertaRepository.save(new Alerta(tipo, mensaje, "ms-lotes", e.getLoteId()));
        if (enviarATelegram) telegramService.notificar(guardada);
        return guardada;
    }

    // Reenvía a Telegram las alertas elegidas en la pantalla (en cola, con pausa entre mensajes)
    public Map<String, Object> enviarATelegram(List<Long> ids) {
        if (!telegramService.estaConfigurado()) {
            return Map.of("telegram", "NO_CONFIGURADO", "enviadas", 0);
        }
        List<Alerta> alertas = alertaRepository.findAllById(ids);
        alertas.forEach(telegramService::notificar);
        return Map.of("telegram", "EN_COLA", "enviadas", alertas.size());
    }

    @Transactional
    public void delete(Long id) {
        if (!alertaRepository.existsById(id)) {
            throw new AlertaNotFoundException("Alerta no encontrada con id: " + id);
        }
        alertaRepository.deleteById(id);
    }
}
