package com.satelite.cotizaciones.client;

import com.satelite.cotizaciones.exception.ServicioNoDisponibleException;
import lombok.extern.slf4j.Slf4j;
import org.springframework.cloud.openfeign.FallbackFactory;
import org.springframework.stereotype.Component;

@Component
@Slf4j
public class ProductoClientFallbackFactory implements FallbackFactory<ProductoClient> {

    @Override
    public ProductoClient create(Throwable cause) {
        return ean -> {
            log.warn("ms-productos no respondió al consultar el EAN {}: {}", ean, cause.toString());
            throw ServicioNoDisponibleException.traducir("ms-productos", cause);
        };
    }
}
