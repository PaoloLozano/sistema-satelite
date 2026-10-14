package com.satelite.cotizaciones.exception;

public class CotizacionNotFoundException extends RuntimeException {
    public CotizacionNotFoundException(String message) {
        super(message);
    }
}
