package com.satelite.gateway.ratelimit;

import com.satelite.gateway.security.JwtUtil;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.cloud.gateway.filter.GatewayFilterChain;
import org.springframework.http.HttpStatus;
import org.springframework.mock.http.server.reactive.MockServerHttpRequest;
import org.springframework.mock.web.server.MockServerWebExchange;
import reactor.core.publisher.Mono;

import java.net.InetSocketAddress;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class RateLimitFilterTest {

    @Test
    @DisplayName("El sexto intento de login en un minuto desde la misma IP recibe 429")
    void sextoLoginEsBloqueado() {
        RateLimitFilter filtro = new RateLimitFilter(new RateLimitProperties(), new RateLimiter(), new JwtUtil());
        GatewayFilterChain siguiente = mock(GatewayFilterChain.class);
        when(siguiente.filter(any())).thenReturn(Mono.empty());

        MockServerWebExchange intento = null;
        for (int i = 1; i <= 6; i++) {
            intento = MockServerWebExchange.from(MockServerHttpRequest.post("/api/auth/login")
                    .remoteAddress(new InetSocketAddress("10.0.0.1", 40000)));
            filtro.filter(intento, siguiente).block();
        }

        assertThat(intento.getResponse().getStatusCode()).isEqualTo(HttpStatus.TOO_MANY_REQUESTS);
    }
}
