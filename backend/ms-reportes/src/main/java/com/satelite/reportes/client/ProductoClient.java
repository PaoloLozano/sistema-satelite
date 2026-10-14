package com.satelite.reportes.client;

import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.GetMapping;

import java.util.List;
import java.util.Map;

@FeignClient(name = "ms-productos", fallbackFactory = ProductoClientFallbackFactory.class)
public interface ProductoClient {

    @GetMapping("/api/productos/count")
    Map<String, Long> getCount();

    @GetMapping("/api/proveedores")
    List<Map<String, Object>> getProveedores();
}
