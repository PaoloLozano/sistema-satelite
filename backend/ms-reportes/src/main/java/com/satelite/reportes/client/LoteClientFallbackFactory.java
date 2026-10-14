package com.satelite.reportes.client;

import com.satelite.reportes.exception.ServicioNoDisponibleException;
import org.springframework.cloud.openfeign.FallbackFactory;
import org.springframework.stereotype.Component;

@Component
public class LoteClientFallbackFactory implements FallbackFactory<LoteClient> {

    @Override
    public LoteClient create(Throwable cause) {
        return () -> {
            throw ServicioNoDisponibleException.traducir("ms-lotes", cause);
        };
    }
}
