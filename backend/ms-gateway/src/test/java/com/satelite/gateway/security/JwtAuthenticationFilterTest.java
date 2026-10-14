package com.satelite.gateway.security;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.cloud.gateway.filter.GatewayFilterChain;
import org.springframework.http.HttpStatus;
import org.springframework.mock.http.server.reactive.MockServerHttpRequest;
import org.springframework.mock.web.server.MockServerWebExchange;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;

class JwtAuthenticationFilterTest {

    @Test
    @DisplayName("Una petición sin token recibe 401 y no llega al microservicio")
    void sinTokenResponde401() {
        JwtAuthenticationFilter filtro = new JwtAuthenticationFilter(new JwtUtil());
        MockServerWebExchange peticion = MockServerWebExchange.from(MockServerHttpRequest.get("/api/lotes"));
        GatewayFilterChain microservicio = mock(GatewayFilterChain.class);

        filtro.filter(peticion, microservicio).block();

        assertThat(peticion.getResponse().getStatusCode()).isEqualTo(HttpStatus.UNAUTHORIZED);
        verify(microservicio, never()).filter(any());
    }
}
