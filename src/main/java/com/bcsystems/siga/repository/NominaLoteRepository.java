package com.bcsystems.siga.repository;

import com.bcsystems.siga.domain.NominaLote;
import org.springframework.data.jpa.repository.JpaRepository;
import java.time.LocalDate;
import java.util.Optional;

public interface NominaLoteRepository extends JpaRepository<NominaLote, Long> {
    Optional<NominaLote> findByFechaInicioAndFechaFin(LocalDate inicio, LocalDate fin);
}
