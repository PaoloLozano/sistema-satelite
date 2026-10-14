package com.satelite.reportes.client;

import com.satelite.reportes.dto.CotizacionDTO;
import com.satelite.reportes.exception.ServicioNoDisponibleException;
import org.springframework.cloud.openfeign.FallbackFactory;
import org.springframework.stereotype.Component;

import java.util.List;

@Component
public class CotizacionClientFallbackFactory implements FallbackFactory<CotizacionClient> {

    @Override
    public CotizacionClient create(Throwable cause) {
        return new CotizacionClient() {
            @Override
            public List<CotizacionDTO> getAllCotizaciones() {
                throw ServicioNoDisponibleException.traducir("ms-cotizaciones", cause);
            }

            @Override
            public List<CotizacionDTO> getCotizacionesByMes(String mes) {
                throw ServicioNoDisponibleException.traducir("ms-cotizaciones", cause);
            }
        };
    }
}
