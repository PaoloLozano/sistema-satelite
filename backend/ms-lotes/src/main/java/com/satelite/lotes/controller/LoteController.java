package com.satelite.lotes.controller;

import com.satelite.lotes.entity.Lote;
import com.satelite.lotes.service.LoteService;
import com.satelite.lotes.service.ResumenService;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;


import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/lotes")
public class LoteController {

    private final LoteService loteService;
    private final ResumenService resumenService;

    public LoteController(LoteService loteService, ResumenService resumenService) {
        this.loteService = loteService;
        this.resumenService = resumenService;
    }

    @GetMapping
    public ResponseEntity<List<Lote>> findAll() {
        return ResponseEntity.ok(loteService.findAll());
    }

    @GetMapping("/{id}")
    public ResponseEntity<Lote> findById(@PathVariable Long id) {
        return ResponseEntity.ok(loteService.findById(id));
    }

    @GetMapping("/semaforo/resumen")
    public ResponseEntity<Map<String, Long>> getSemaforoResumen() {
        return ResponseEntity.ok(loteService.getSemaforoResumen());
    }

    // X-Auth-Role lo inyecta ms-gateway a partir del JWT
    @PostMapping("/recalcular")
    public ResponseEntity<Map<String, Object>> recalcular(
            @RequestHeader(value = "X-Auth-Role", required = false) String rol) {
        if (!"ADMIN".equals(rol)) {
            return ResponseEntity.status(HttpStatus.FORBIDDEN)
                    .body(Map.of("error", "Solo un ADMIN puede recalcular el semáforo"));
        }
        return ResponseEntity.ok(Map.of("actualizados", loteService.recalcularEstados()));
    }

    // Envía ahora el mismo resumen que el job diario (útil para probar o para la demo)
    @PostMapping("/resumen")
    public ResponseEntity<Map<String, Object>> enviarResumen(
            @RequestHeader(value = "X-Auth-Role", required = false) String rol) {
        if (!"ADMIN".equals(rol)) {
            return ResponseEntity.status(HttpStatus.FORBIDDEN)
                    .body(Map.of("error", "Solo un ADMIN puede enviar el resumen"));
        }
        ResumenService.Resultado r = resumenService.generarYPublicar();
        return ResponseEntity.ok(Map.of("rojo", r.rojo(), "amarillo", r.amarillo(), "enviado", r.enviado()));
    }

    @PostMapping
    public ResponseEntity<Lote> create(@Valid @RequestBody Lote lote) {
        return ResponseEntity.status(HttpStatus.CREATED).body(loteService.create(lote));
    }

    @PutMapping("/{id}")
    public ResponseEntity<Lote> update(@PathVariable Long id, @Valid @RequestBody Lote lote) {
        return ResponseEntity.ok(loteService.update(id, lote));
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Map<String, String>> delete(@PathVariable Long id) {
        loteService.delete(id);
        return ResponseEntity.ok(Map.of("mensaje", "Lote eliminado exitosamente"));
    }
}
