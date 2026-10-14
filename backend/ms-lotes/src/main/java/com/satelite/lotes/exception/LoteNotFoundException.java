package com.satelite.lotes.exception;

public class LoteNotFoundException extends RuntimeException {
    public LoteNotFoundException(String message) {
        super(message);
    }
}
