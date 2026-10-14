package com.satelite.alertas.dto;

import jakarta.validation.constraints.NotEmpty;

import java.util.List;

public record EnviarAlertasRequest(@NotEmpty(message = "Selecciona al menos una alerta") List<Long> ids) {
}
