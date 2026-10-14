package com.satelite.cotizaciones.repository;

import com.satelite.cotizaciones.entity.Cotizacion;
import com.satelite.cotizaciones.entity.EstadoCotizacion;
import com.satelite.cotizaciones.entity.ItemCotizacion;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDate;
import java.util.List;

@Repository
public interface CotizacionRepository extends JpaRepository<Cotizacion, Long> {

    @Query("SELECT c FROM Cotizacion c WHERE c.fechaCotizacion >= :inicio AND c.fechaCotizacion < :fin")
    List<Cotizacion> findByRangoFecha(@Param("inicio") LocalDate inicio, @Param("fin") LocalDate fin);

    @Query("SELECT i FROM ItemCotizacion i JOIN FETCH i.cotizacion c " +
           "WHERE i.ean = :ean AND c.estado = :estado ORDER BY c.fechaCotizacion DESC, c.id DESC")
    List<ItemCotizacion> findItemsByEan(@Param("ean") String ean, @Param("estado") EstadoCotizacion estado);
}
