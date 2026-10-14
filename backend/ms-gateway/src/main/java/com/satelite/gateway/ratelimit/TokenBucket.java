package com.satelite.gateway.ratelimit;

// Cubo de fichas: se llena a un ritmo constante hasta su capacidad y cada petición gasta una ficha
class TokenBucket {

    private final double capacidad;
    private final double fichasPorNano;
    private double fichas;
    private long ultimaRecarga;

    TokenBucket(int peticiones, int ventanaSegundos) {
        this.capacidad = peticiones;
        this.fichasPorNano = peticiones / (ventanaSegundos * 1_000_000_000.0);
        this.fichas = peticiones;
        this.ultimaRecarga = System.nanoTime();
    }

    // 0 si la petición pasa; si no, los segundos que faltan para tener una ficha
    synchronized long intentarConsumir() {
        long ahora = System.nanoTime();
        fichas = Math.min(capacidad, fichas + (ahora - ultimaRecarga) * fichasPorNano);
        ultimaRecarga = ahora;
        if (fichas >= 1) {
            fichas -= 1;
            return 0;
        }
        return (long) Math.ceil((1 - fichas) / fichasPorNano / 1_000_000_000.0);
    }

    synchronized long nanosSinUso() {
        return System.nanoTime() - ultimaRecarga;
    }
}
