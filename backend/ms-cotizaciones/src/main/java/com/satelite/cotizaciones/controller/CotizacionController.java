package com.satelite.cotizaciones.controller;

import com.satelite.cotizaciones.dto.RankingItemDTO;
import com.satelite.cotizaciones.entity.Cotizacion;
import com.satelite.cotizaciones.service.CotizacionService;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;


import java.util.List;

@RestController
@RequestMapping("/api/cotizaciones")
public class CotizacionController {

    private final CotizacionService cotizacionService;

    public CotizacionController(CotizacionService cotizacionService) {
        this.cotizacionService = cotizacionService;
    }

    @GetMapping
    public ResponseEntity<List<Cotizacion>> findAll() {
        return ResponseEntity.ok(cotizacionService.findAll());
    }

    @GetMapping("/{id}")
    public ResponseEntity<Cotizacion> findById(@PathVariable Long id) {
        return ResponseEntity.ok(cotizacionService.findById(id));
    }

    @GetMapping("/mes/{mes}")
    public ResponseEntity<List<Cotizacion>> findByMes(@PathVariable String mes) {
        return ResponseEntity.ok(cotizacionService.findByMes(mes));
    }

    @GetMapping("/ranking/{ean}")
    public ResponseEntity<List<RankingItemDTO>> ranking(@PathVariable String ean) {
        return ResponseEntity.ok(cotizacionService.rankingPorEan(ean));
    }

    @PostMapping
    public ResponseEntity<Cotizacion> create(@Valid @RequestBody Cotizacion cotizacion) {
        return ResponseEntity.status(HttpStatus.CREATED).body(cotizacionService.create(cotizacion));
    }

    @PostMapping("/{id}/confirmar")
    public ResponseEntity<Cotizacion> confirmar(@PathVariable Long id) {
        return ResponseEntity.ok(cotizacionService.confirmar(id));
    }
}
