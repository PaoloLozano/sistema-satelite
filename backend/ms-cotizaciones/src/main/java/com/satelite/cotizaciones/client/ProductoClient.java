package com.satelite.cotizaciones.client;

import com.satelite.cotizaciones.dto.ProductoDTO;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;

@FeignClient(name = "ms-productos", fallbackFactory = ProductoClientFallbackFactory.class)
public interface ProductoClient {

    @GetMapping("/api/productos/ean/{ean}")
    ProductoDTO getByEan(@PathVariable("ean") String ean);
}
