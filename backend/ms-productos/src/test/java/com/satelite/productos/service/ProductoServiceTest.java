package com.satelite.productos.service;

import com.satelite.productos.entity.Producto;
import com.satelite.productos.repository.ProductoRepository;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class ProductoServiceTest {

    @Mock
    private ProductoRepository productoRepository;

    @InjectMocks
    private ProductoService productoService;

    @Test
    @DisplayName("No se puede crear un producto con un EAN que ya existe")
    void eanRepetido() {
        Producto producto = new Producto();
        producto.setEan("7751519000017");
        producto.setNombre("Leche Gloria Entera 1L");
        when(productoRepository.existsByEan("7751519000017")).thenReturn(true);

        assertThatThrownBy(() -> productoService.create(producto))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("7751519000017");
        verify(productoRepository, never()).save(any());
    }
}
