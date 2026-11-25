package com.bcsystems.siga.repository;

import com.bcsystems.siga.domain.MakerProyecto;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import jakarta.persistence.LockModeType;
import java.util.List;
import java.util.Optional;

public interface MakerProyectoRepository extends JpaRepository<MakerProyecto, Integer> {
    Page<MakerProyecto> findAllByOrderByFechaCreacionDesc(Pageable pageable);

    List<MakerProyecto> findByCotizacionId(Integer cotizacionId);

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("select p from MakerProyecto p where p.id = :id")
    Optional<MakerProyecto> findByIdForUpdate(@Param("id") Integer id);
}
