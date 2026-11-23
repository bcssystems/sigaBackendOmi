package com.bcsystems.siga.repository;
import com.bcsystems.siga.domain.Nomina; import com.bcsystems.siga.domain.en.EstadoNomina; import org.springframework.data.jpa.repository.JpaRepository; import java.time.LocalDate; import java.util.*;
public interface NominaRepository extends JpaRepository<Nomina,Long>{ Optional<Nomina> findByPersonaIdPersonaAndFechaInicioAndFechaFin(Integer id,LocalDate inicio,LocalDate fin); List<Nomina> findByEstado(EstadoNomina estado); List<Nomina> findByPersonaIdPersona(Integer id); }
