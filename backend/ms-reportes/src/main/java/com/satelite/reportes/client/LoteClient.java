package com.satelite.reportes.client;

import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.GetMapping;

import java.util.Map;

@FeignClient(name = "ms-lotes", fallbackFactory = LoteClientFallbackFactory.class)
public interface LoteClient {

    @GetMapping("/api/lotes/semaforo/resumen")
    Map<String, Long> getSemaforoResumen();
}
