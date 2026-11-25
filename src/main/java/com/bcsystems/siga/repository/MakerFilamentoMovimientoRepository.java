package com.bcsystems.siga.repository;

import com.bcsystems.siga.domain.MakerFilamentoMovimiento;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

public interface MakerFilamentoMovimientoRepository extends JpaRepository<MakerFilamentoMovimiento, Integer> {
    Page<MakerFilamentoMovimiento> findAllByOrderByFechaMovimientoDesc(Pageable pageable);

    Page<MakerFilamentoMovimiento> findByFilamentoIdOrderByFechaMovimientoDesc(Integer filamentoId, Pageable pageable);
}
