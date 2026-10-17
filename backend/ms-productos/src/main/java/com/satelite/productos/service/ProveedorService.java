package com.satelite.productos.service;

import com.satelite.productos.entity.Proveedor;
import com.satelite.productos.exception.ProductoNotFoundException;
import com.satelite.productos.repository.ProveedorRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;


import java.util.List;

@Service
public class ProveedorService {

    private final ProveedorRepository proveedorRepository;

    public ProveedorService(ProveedorRepository proveedorRepository) {
        this.proveedorRepository = proveedorRepository;
    }

    public List<Proveedor> findAll() {
        return proveedorRepository.findAll();
    }

    public Proveedor findById(Long id) {
        return proveedorRepository.findById(id)
                .orElseThrow(() -> new ProductoNotFoundException("Proveedor no encontrado con id: " + id));
    }

    @Transactional
    public Proveedor create(Proveedor proveedor) {
        if (proveedorRepository.existsByRuc(proveedor.getRuc())) {
            throw new IllegalArgumentException("Ya existe un proveedor con RUC: " + proveedor.getRuc());
        }
        return proveedorRepository.save(proveedor);
    }

    @Transactional
    public Proveedor update(Long id, Proveedor datos) {
        Proveedor existente = findById(id);
        if (!existente.getRuc().equals(datos.getRuc()) && proveedorRepository.existsByRuc(datos.getRuc())) {
            throw new IllegalArgumentException("Ya existe un proveedor con RUC: " + datos.getRuc());
        }
        existente.setNombre(datos.getNombre());
        existente.setRuc(datos.getRuc());
        existente.setContacto(datos.getContacto());
        existente.setEmail(datos.getEmail());
        existente.setTelefono(datos.getTelefono());
        existente.setDireccion(datos.getDireccion());
        return proveedorRepository.save(existente);
    }

    @Transactional
    public void delete(Long id) {
        proveedorRepository.delete(findById(id));
    }
}
