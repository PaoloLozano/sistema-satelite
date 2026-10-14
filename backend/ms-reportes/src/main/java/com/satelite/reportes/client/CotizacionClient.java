package com.satelite.reportes.client;

import com.satelite.reportes.dto.CotizacionDTO;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;

import java.util.List;

@FeignClient(name = "ms-cotizaciones", fallbackFactory = CotizacionClientFallbackFactory.class)
public interface CotizacionClient {

    @GetMapping("/api/cotizaciones")
    List<CotizacionDTO> getAllCotizaciones();

    @GetMapping("/api/cotizaciones/mes/{mes}")
    List<CotizacionDTO> getCotizacionesByMes(@PathVariable("mes") String mes);
}
