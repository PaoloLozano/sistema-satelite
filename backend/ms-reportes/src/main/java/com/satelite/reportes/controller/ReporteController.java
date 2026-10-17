package com.satelite.reportes.controller;

import com.satelite.reportes.service.ReporteService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.time.YearMonth;
import java.util.Map;

@RestController
@RequestMapping("/api/reportes")
public class ReporteController {

    private final ReporteService reporteService;

    public ReporteController(ReporteService reporteService) {
        this.reporteService = reporteService;
    }

    // mes en formato YYYY-MM (por defecto el mes actual)
    @GetMapping("/ahorro")
    public ResponseEntity<Map<String, Object>> getAhorro(@RequestParam(required = false) String mes) {
        String periodo = (mes == null || mes.isBlank()) ? YearMonth.now().toString() : mes;
        return ResponseEntity.ok(reporteService.getAhorro(periodo));
    }

    @GetMapping("/dashboard/kpis")
    public ResponseEntity<Map<String, Object>> getDashboardKPIs() {
        return ResponseEntity.ok(reporteService.getDashboardKPIs());
    }
}
