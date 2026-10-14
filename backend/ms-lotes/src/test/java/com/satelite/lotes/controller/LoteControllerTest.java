package com.satelite.lotes.controller;

import com.satelite.lotes.config.SecurityConfig;
import com.satelite.lotes.entity.Lote;
import com.satelite.lotes.service.LoteService;
import com.satelite.lotes.service.ResumenService;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

import java.time.LocalDate;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(LoteController.class)
@Import(SecurityConfig.class)
class LoteControllerTest {

    @Autowired private MockMvc mvc;
    @MockBean private LoteService loteService;
    @MockBean private ResumenService resumenService;

    @Test
    @DisplayName("POST /api/lotes con datos válidos responde 201 con el lote creado")
    void crearLote() throws Exception {
        Lote guardado = new Lote();
        guardado.setId(10L);
        guardado.setEan("7751519000017");
        guardado.setProductoNombre("Leche Gloria Entera 1L");
        guardado.setCantidad(48);
        guardado.setFechaVencimiento(LocalDate.now().plusDays(3));
        guardado.actualizarEstado();
        when(loteService.create(any(Lote.class))).thenReturn(guardado);

        mvc.perform(post("/api/lotes").contentType(MediaType.APPLICATION_JSON).content("""
                        {"ean":"7751519000017","productoNombre":"Leche Gloria Entera 1L",
                         "cantidad":48,"costoUnitario":3.40,"fechaVencimiento":"%s"}
                        """.formatted(LocalDate.now().plusDays(3))))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.estado").value("ROJO"));
    }
}
