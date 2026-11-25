package com.bcsystems.siga.repository;

import com.bcsystems.siga.domain.MakerCotizacion;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

public interface MakerCotizacionRepository extends JpaRepository<MakerCotizacion, Integer> {
    Page<MakerCotizacion> findAllByOrderByFechaDesc(Pageable pageable);
}
