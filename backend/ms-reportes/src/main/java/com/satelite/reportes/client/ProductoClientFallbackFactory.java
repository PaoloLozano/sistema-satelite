package com.satelite.reportes.client;

import com.satelite.reportes.exception.ServicioNoDisponibleException;
import org.springframework.cloud.openfeign.FallbackFactory;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Map;

@Component
public class ProductoClientFallbackFactory implements FallbackFactory<ProductoClient> {

    @Override
    public ProductoClient create(Throwable cause) {
        return new ProductoClient() {
            @Override
            public Map<String, Long> getCount() {
                throw ServicioNoDisponibleException.traducir("ms-productos", cause);
            }

            @Override
            public List<Map<String, Object>> getProveedores() {
                throw ServicioNoDisponibleException.traducir("ms-productos", cause);
            }
        };
    }
}
