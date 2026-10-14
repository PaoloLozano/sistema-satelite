package com.satelite.lotes.repository;

import com.satelite.lotes.entity.EstadoLote;
import com.satelite.lotes.entity.Lote;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface LoteRepository extends JpaRepository<Lote, Long> {

    List<Lote> findByEstado(EstadoLote estado);

    long countByEstado(EstadoLote estado);

    List<Lote> findByEan(String ean);

}
