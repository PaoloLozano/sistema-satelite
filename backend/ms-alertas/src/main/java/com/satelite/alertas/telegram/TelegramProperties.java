package com.satelite.alertas.telegram;

import lombok.Getter;
import lombok.Setter;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

// El token y el chat ID llegan por variables de entorno (TELEGRAM_TOKEN, TELEGRAM_CHAT_ID); nunca se escriben en archivos
@Component
@ConfigurationProperties(prefix = "telegram")
@Getter
@Setter
public class TelegramProperties {

    private String token = "";
    private String chatId = "";
    private String apiUrl = "https://api.telegram.org";

    public boolean configurado() {
        return token != null && !token.isBlank() && chatId != null && !chatId.isBlank();
    }

    public String chatIdEnmascarado() {
        if (chatId == null || chatId.length() <= 4) return "****";
        return "****" + chatId.substring(chatId.length() - 4);
    }
}
