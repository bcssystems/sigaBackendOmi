package com.bcsystems.siga.repository;
import com.bcsystems.siga.domain.Asistencia;
import org.springframework.data.jpa.repository.JpaRepository;
import java.time.LocalDate;
import java.util.List;
import java.util.Optional;
public interface AsistenciaRepository extends JpaRepository<Asistencia, Integer> {
    Optional<Asistencia> findByPersonaIdPersonaAndFecha(Integer idPersona, LocalDate fecha);
    List<Asistencia> findByPersonaIdPersonaOrderByFechaDesc(Integer idPersona);
}
