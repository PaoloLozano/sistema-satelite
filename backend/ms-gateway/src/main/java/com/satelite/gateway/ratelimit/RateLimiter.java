package com.satelite.gateway.ratelimit;

import org.springframework.stereotype.Component;

import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicLong;

// Un cubo por cliente. Vive en la memoria de esta instancia del gateway (con varias réplicas habría que usar Redis)
@Component
public class RateLimiter {

    private static final long INACTIVIDAD_NANOS = TimeUnit.MINUTES.toNanos(10);

    private final ConcurrentHashMap<String, TokenBucket> cubos = new ConcurrentHashMap<>();
    private final AtomicLong llamadas = new AtomicLong();

    // 0 si la petición pasa; si no, los segundos que el cliente debe esperar
    public long verificar(String clave, RateLimitProperties.Limit limite) {
        if (llamadas.incrementAndGet() % 1000 == 0) {
            cubos.values().removeIf(c -> c.nanosSinUso() > INACTIVIDAD_NANOS);
        }
        return cubos.computeIfAbsent(clave, k -> new TokenBucket(limite.getRequests(), limite.getWindowSeconds()))
                .intentarConsumir();
    }
}
