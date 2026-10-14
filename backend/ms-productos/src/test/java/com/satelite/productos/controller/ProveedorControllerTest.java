package com.satelite.productos.controller;

import com.satelite.productos.config.SecurityConfig;
import com.satelite.productos.service.ProveedorService;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(ProveedorController.class)
@Import(SecurityConfig.class)
class ProveedorControllerTest {

    @Autowired private MockMvc mvc;
    @MockBean private ProveedorService proveedorService;

    @Test
    @DisplayName("POST /api/proveedores con un RUC que no tiene 11 dígitos responde 400")
    void rucInvalido() throws Exception {
        mvc.perform(post("/api/proveedores").contentType(MediaType.APPLICATION_JSON)
                        .content("{\"nombre\":\"Gloria S.A.\",\"ruc\":\"12345\"}"))
                .andExpect(status().isBadRequest());
        verify(proveedorService, never()).create(any());
    }
}
