package com.satelite.reportes.exception;

import feign.FeignException;

public class ServicioNoDisponibleException extends RuntimeException {

    public ServicioNoDisponibleException(String servicio, Throwable causa) {
        super(servicio + " no está disponible en este momento. Intenta de nuevo en unos segundos.", causa);
    }

    // Un 4xx del otro servicio (p. ej. 404) no es una caída: se devuelve tal cual
    public static RuntimeException traducir(String servicio, Throwable causa) {
        for (Throwable t = causa; t != null; t = t.getCause()) {
            if (t instanceof FeignException fe && fe.status() >= 400 && fe.status() < 500) {
                return fe;
            }
        }
        return new ServicioNoDisponibleException(servicio, causa);
    }
}
