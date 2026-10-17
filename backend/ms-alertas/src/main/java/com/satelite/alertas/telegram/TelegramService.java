package com.satelite.alertas.telegram;

import com.satelite.alertas.entity.Alerta;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestClient;

import jakarta.annotation.PreDestroy;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

@Service
@Slf4j
public class TelegramService {

    // Telegram recomienda como máximo ~1 mensaje por segundo hacia un mismo chat
    private static final long PAUSA_ENTRE_MENSAJES_MS = 1100;

    private final TelegramProperties props;

    public TelegramService(TelegramProperties props) {
        this.props = props;
    }
    private final ExecutorService cola = Executors.newSingleThreadExecutor(r -> {
        Thread t = new Thread(r, "telegram-envio");
        t.setDaemon(true);
        return t;
    });

    private RestClient http() {
        return RestClient.builder().baseUrl(props.getApiUrl()).build(); 
    }

    public boolean estaConfigurado() {
        return props.configurado();
    }

    public String formatear(Alerta a) {
        String fuente = a.getFuente() != null ? "\nFuente: " + a.getFuente() : "";
        return "ALERTA " + a.getTipo() + "\n" + a.getMensaje() + fuente;
    }

    // Envío en segundo plano y en orden, para no frenar al consumidor de RabbitMQ ni pasarse del límite de Telegram
    public void notificar(Alerta alerta) {
        notificarTexto(formatear(alerta));
    }

    public void notificarTexto(String textoOriginal) {
        if (!estaConfigurado() || textoOriginal == null) return;
        // Telegram acepta hasta 4096 caracteres por mensaje
        String texto = textoOriginal.length() > 4000 ? textoOriginal.substring(0, 4000) + "\n..." : textoOriginal;
        cola.submit(() -> {
            try {
                enviar(texto);
                Thread.sleep(PAUSA_ENTRE_MENSAJES_MS);
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
            } catch (Exception e) {
                log.warn("No se pudo enviar la alerta a Telegram: {}", limpiar(e.getMessage()));
            }
        });
    }

    // Estado para el panel de la pantalla de alertas
    public Map<String, Object> estado() {
        Map<String, Object> r = new LinkedHashMap<>();
        r.put("configurado", estaConfigurado());
        r.put("conectado", false);
        if (!estaConfigurado()) return r;
        r.put("chatId", props.chatIdEnmascarado());
        try {
            Map<?, ?> resp = http().get().uri(ruta("getMe"))
                    .retrieve().body(Map.class);
            Object resultado = resp != null ? resp.get("result") : null;
            if (resultado instanceof Map<?, ?> bot) {
                r.put("conectado", true);
                r.put("bot", bot.get("username"));
            }
        } catch (Exception e) {
            r.put("error", limpiar(e.getMessage()));
        }
        return r;
    }

    private void enviar(String texto) {
        http().post().uri(ruta("sendMessage"))
                .contentType(MediaType.APPLICATION_JSON)
                .body(Map.of("chat_id", props.getChatId(), "text", texto))
                .retrieve().toBodilessEntity();
    }

    // El token lleva ":" y debe ir tal cual en la URL (con un parámetro de plantilla se codificaría como %3A)
    private String ruta(String metodo) {
        return "/bot" + props.getToken() + "/" + metodo;
    }

    // Los mensajes de error de HTTP incluyen la URL, que contiene el token: nunca debe llegar a logs ni a la pantalla
    private String limpiar(String mensaje) {
        if (mensaje == null) return "error desconocido";
        String token = props.getToken();
        if (token == null || token.isBlank()) return mensaje;
        return mensaje.replace(token, "***").replace(URLEncoder.encode(token, StandardCharsets.UTF_8), "***");
    }

    @PreDestroy
    void cerrar() {
        cola.shutdownNow();
    }
}
