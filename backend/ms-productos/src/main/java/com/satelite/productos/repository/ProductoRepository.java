package com.satelite.productos.repository;

import com.satelite.productos.entity.Producto;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface ProductoRepository extends JpaRepository<Producto, Long> {
    Optional<Producto> findByEan(String ean);
    boolean existsByEan(String ean);
}
