package com.satelite.gateway.ratelimit;

import com.satelite.gateway.security.JwtUtil;
import lombok.extern.slf4j.Slf4j;
import org.springframework.cloud.gateway.filter.GatewayFilterChain;
import org.springframework.cloud.gateway.filter.GlobalFilter;
import org.springframework.core.Ordered;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.server.reactive.ServerHttpRequest;
import org.springframework.http.server.reactive.ServerHttpResponse;
import org.springframework.stereotype.Component;
import org.springframework.web.server.ServerWebExchange;
import reactor.core.publisher.Mono;

import java.net.InetSocketAddress;
import java.nio.charset.StandardCharsets;

// Corre antes que JwtAuthenticationFilter (-1) para limitar también los intentos con token inválido
@Component
@Slf4j
public class RateLimitFilter implements GlobalFilter, Ordered {

    private static final String RUTA_LOGIN = "/api/auth/login";

    private final RateLimitProperties props;
    private final RateLimiter limiter;
    private final JwtUtil jwtUtil;

    public RateLimitFilter(RateLimitProperties props, RateLimiter limiter, JwtUtil jwtUtil) {
        this.props = props;
        this.limiter = limiter;
        this.jwtUtil = jwtUtil;
    }

    @Override
    public Mono<Void> filter(ServerWebExchange exchange, GatewayFilterChain chain) {
        ServerHttpRequest request = exchange.getRequest();
        if (!props.isEnabled() || request.getMethod() == HttpMethod.OPTIONS) {
            return chain.filter(exchange);
        }

        String ip = ipDelCliente(request);
        long espera = RUTA_LOGIN.equals(request.getPath().value())
                ? limiter.verificar("login:" + ip, props.getLogin())
                : limiter.verificar(clienteGeneral(request, ip), props.getGeneral());

        if (espera == 0) {
            return chain.filter(exchange);
        }
        log.warn("Límite de peticiones excedido: {} {} (espera {} s)", request.getMethod(), request.getPath(), espera);
        return rechazar(exchange, espera);
    }

    // Usuario del token si es válido; si no, la IP
    private String clienteGeneral(ServerHttpRequest request, String ip) {
        String auth = request.getHeaders().getFirst(HttpHeaders.AUTHORIZATION);
        if (auth != null && auth.startsWith("Bearer ")) {
            String token = auth.substring(7);
            if (jwtUtil.isTokenValid(token)) {
                return "user:" + jwtUtil.extractUsername(token);
            }
        }
        return "ip:" + ip;
    }

    // No se usa X-Forwarded-For: el cliente podría falsearlo para evadir el límite
    private String ipDelCliente(ServerHttpRequest request) {
        InetSocketAddress direccion = request.getRemoteAddress();
        return direccion != null && direccion.getAddress() != null ? direccion.getAddress().getHostAddress() : "desconocida";
    }

    private Mono<Void> rechazar(ServerWebExchange exchange, long espera) {
        ServerHttpResponse response = exchange.getResponse();
        response.setStatusCode(HttpStatus.TOO_MANY_REQUESTS);
        response.getHeaders().setContentType(MediaType.APPLICATION_JSON);
        response.getHeaders().set(HttpHeaders.RETRY_AFTER, String.valueOf(espera));
        byte[] cuerpo = String.format(
                "{\"status\":429,\"error\":\"Demasiadas peticiones. Intenta de nuevo en %d segundos.\",\"retryAfter\":%d}",
                espera, espera).getBytes(StandardCharsets.UTF_8);
        return response.writeWith(Mono.just(response.bufferFactory().wrap(cuerpo)));
    }

    @Override
    public int getOrder() {
        return -2;
    }
}
