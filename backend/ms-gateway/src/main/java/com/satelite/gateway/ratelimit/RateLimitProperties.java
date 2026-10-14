package com.satelite.gateway.ratelimit;

import lombok.Getter;
import lombok.Setter;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

// Valores por defecto: si ms-config no entrega la sección rate-limit, el límite sigue activo
@Component
@ConfigurationProperties(prefix = "rate-limit")
@Getter
@Setter
public class RateLimitProperties {

    private boolean enabled = true;
    private Limit general = new Limit(200, 60);
    private Limit login = new Limit(5, 60);

    @Getter
    @Setter
    public static class Limit {
        private int requests;
        private int windowSeconds;

        public Limit() {}

        public Limit(int requests, int windowSeconds) {
            this.requests = requests;
            this.windowSeconds = windowSeconds;
        }
    }
}
