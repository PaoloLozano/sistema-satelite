package com.satelite.productos.service;

import com.satelite.productos.entity.Producto;
import com.satelite.productos.exception.ProductoNotFoundException;
import com.satelite.productos.repository.ProductoRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;


import java.util.List;

@Service
public class ProductoService {

    private final ProductoRepository productoRepository;

    public ProductoService(ProductoRepository productoRepository) {
        this.productoRepository = productoRepository;
    }

    public List<Producto> findAll() {
        return productoRepository.findAll();
    }

    public Producto findById(Long id) {
        return productoRepository.findById(id)
                .orElseThrow(() -> new ProductoNotFoundException("Producto no encontrado con id: " + id));
    }

    public Producto findByEan(String ean) {
        return productoRepository.findByEan(ean)
                .orElseThrow(() -> new ProductoNotFoundException("Producto no encontrado con EAN: " + ean));
    }

    @Transactional
    public Producto create(Producto producto) {
        if (productoRepository.existsByEan(producto.getEan())) {
            throw new IllegalArgumentException("Ya existe un producto con EAN: " + producto.getEan());
        }
        return productoRepository.save(producto);
    }

    @Transactional
    public Producto update(Long id, Producto productoActualizado) {
        Producto existente = findById(id);

        if (!existente.getEan().equals(productoActualizado.getEan()) &&
                productoRepository.existsByEan(productoActualizado.getEan())) {
            throw new IllegalArgumentException("Ya existe un producto con EAN: " + productoActualizado.getEan());
        }

        existente.setNombre(productoActualizado.getNombre());
        existente.setEan(productoActualizado.getEan());
        existente.setCategoria(productoActualizado.getCategoria());
        existente.setProveedor(productoActualizado.getProveedor());
        existente.setStockActual(productoActualizado.getStockActual());
        existente.setStockMinimo(productoActualizado.getStockMinimo());
        existente.setPrecio(productoActualizado.getPrecio());

        return productoRepository.save(existente);
    }

    @Transactional
    public void delete(Long id) {
        if (!productoRepository.existsById(id)) {
            throw new ProductoNotFoundException("Producto no encontrado con id: " + id);
        }
        productoRepository.deleteById(id);
    }

    public long count() {
        return productoRepository.count();
    }
}
