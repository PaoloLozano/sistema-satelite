package com.satelite.alertas.repository;

import com.satelite.alertas.entity.Alerta;
import com.satelite.alertas.entity.TipoAlerta;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface AlertaRepository extends JpaRepository<Alerta, Long> {
    List<Alerta> findByTipoOrderByEnviadoAtDesc(TipoAlerta tipo);
    List<Alerta> findAllByOrderByEnviadoAtDesc();
    boolean existsByReferenciaIdAndTipo(Long referenciaId, TipoAlerta tipo);
}
