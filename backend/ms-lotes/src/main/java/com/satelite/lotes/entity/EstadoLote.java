package com.satelite.lotes.entity;

public enum EstadoLote {
    ROJO,    // <= 7 días para vencer
    AMARILLO, // 8-30 días para vencer
    VERDE    // > 30 días para vencer
}
