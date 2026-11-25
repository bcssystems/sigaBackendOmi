package com.bcsystems.siga.repository;

import com.bcsystems.siga.domain.MakerTarea;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;

public interface MakerTareaRepository extends JpaRepository<MakerTarea, Integer> {
    Page<MakerTarea> findAllByOrderByFechaCreacionDesc(Pageable pageable);

    long countByProyectoId(Integer proyectoId);

    long countByProyectoIdAndEstado(Integer proyectoId, String estado);

    List<MakerTarea> findByProyectoIdOrderByFechaCreacionDesc(Integer proyectoId);

    @Query("select distinct t from MakerTarea t join t.tareasMencionadas mencionada "
            + "where mencionada.id = :tareaId")
    List<MakerTarea> findByTareaMencionadaId(@Param("tareaId") Integer tareaId);
}
