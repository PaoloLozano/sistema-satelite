package com.satelite.lotes.entity;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;

import java.time.LocalDate;

import static org.assertj.core.api.Assertions.assertThat;

class LoteTest {

    // Regla del semáforo: ROJO ≤ 7 días, AMARILLO 8 a 30, VERDE > 30
    @ParameterizedTest(name = "vence en {0} días → {1}")
    @DisplayName("El color del semáforo depende de los días que faltan para vencer")
    @CsvSource({"3, ROJO", "7, ROJO", "8, AMARILLO", "30, AMARILLO", "31, VERDE"})
    void estadoSegunDiasRestantes(int dias, EstadoLote esperado) {
        Lote lote = new Lote();
        lote.setFechaVencimiento(LocalDate.now().plusDays(dias));

        lote.actualizarEstado();

        assertThat(lote.getEstado()).isEqualTo(esperado);
    }
}
