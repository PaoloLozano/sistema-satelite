package com.satelite.alertas.controller;

import com.satelite.alertas.dto.EnviarAlertasRequest;
import com.satelite.alertas.entity.Alerta;
import com.satelite.alertas.entity.TipoAlerta;
import com.satelite.alertas.service.AlertaService;
import com.satelite.alertas.telegram.TelegramService;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import lombok.RequiredArgsConstructor;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/alertas")
@RequiredArgsConstructor
public class AlertaController {

    private final AlertaService alertaService;
    private final TelegramService telegramService;

    @GetMapping
    public ResponseEntity<List<Alerta>> findAll(@RequestParam(required = false) String tipo) {
        if (tipo != null && !tipo.isBlank()) {
            try {
                TipoAlerta tipoAlerta = TipoAlerta.valueOf(tipo.toUpperCase());
                return ResponseEntity.ok(alertaService.findByTipo(tipoAlerta));
            } catch (IllegalArgumentException e) {
                return ResponseEntity.badRequest().build();
            }
        }
        return ResponseEntity.ok(alertaService.findAll());
    }

    @PostMapping("/enviar")
    public Map<String, Object> enviarSeleccionadas(@Valid @RequestBody EnviarAlertasRequest request) {
        return alertaService.enviarATelegram(request.ids());
    }

    @GetMapping("/telegram/estado")
    public Map<String, Object> estadoTelegram() {
        return telegramService.estado();
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Map<String, String>> delete(@PathVariable Long id) {
        alertaService.delete(id);
        return ResponseEntity.ok(Map.of("mensaje", "Alerta eliminada exitosamente"));
    }
}
