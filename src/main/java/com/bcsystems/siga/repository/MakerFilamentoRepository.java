package com.bcsystems.siga.repository;

import com.bcsystems.siga.domain.MakerFilamento;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import jakarta.persistence.LockModeType;
import java.util.List;
import java.util.Optional;

public interface MakerFilamentoRepository extends JpaRepository<MakerFilamento, Integer> {
    List<MakerFilamento> findAllByOrderByNombreAsc();

    List<MakerFilamento> findByActivoTrueOrderByNombreAsc();

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("select f from MakerFilamento f where f.id = :id")
    Optional<MakerFilamento> findByIdForUpdate(@Param("id") Integer id);
}
