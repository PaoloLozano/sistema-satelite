package com.satelite.alertas.service;

import com.satelite.alertas.entity.Alerta;
import com.satelite.alertas.entity.TipoAlerta;
import com.satelite.alertas.event.LoteVencimientoEvent;
import com.satelite.alertas.repository.AlertaRepository;
import com.satelite.alertas.telegram.TelegramService;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class AlertaServiceTest {

    @Mock private AlertaRepository alertaRepository;
    @Mock private TelegramService telegramService;

    @InjectMocks
    private AlertaService alertaService;

    private LoteVencimientoEvent loteRojo() {
        LoteVencimientoEvent e = new LoteVencimientoEvent();
        e.setLoteId(10L);
        e.setEan("7751519000017");
        e.setProductoNombre("Leche Gloria Entera 1L");
        e.setDiasRestantes(3);
        e.setEstado("ROJO");
        e.setProveedor("Gloria S.A.");
        return e;
    }

    @Test
    @DisplayName("Un lote en ROJO crea su alerta y la envía a Telegram")
    void creaAlertaYEnviaATelegram() {
        when(alertaRepository.save(any(Alerta.class))).thenAnswer(inv -> inv.getArgument(0));

        Alerta alerta = alertaService.registrarAlertaDeLote(loteRojo(), true);

        assertThat(alerta.getTipo()).isEqualTo(TipoAlerta.ROJO);
        assertThat(alerta.getMensaje()).contains("Leche Gloria Entera 1L", "3 días restantes");
        verify(telegramService).notificar(alerta);
    }

    @Test
    @DisplayName("Si el lote ya tenía alerta de ese color, no se duplica")
    void noDuplicaAlertas() {
        when(alertaRepository.existsByReferenciaIdAndTipo(10L, TipoAlerta.ROJO)).thenReturn(true);

        assertThat(alertaService.registrarAlertaDeLote(loteRojo(), true)).isNull();
        verify(alertaRepository, never()).save(any());
        verify(telegramService, never()).notificar(any());
    }
}
