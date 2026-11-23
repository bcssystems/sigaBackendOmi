package com.bcsystems.siga.repository;
import com.bcsystems.siga.domain.PrestamoEmpleado; import com.bcsystems.siga.domain.en.EstadoPrestamo; import org.springframework.data.jpa.repository.JpaRepository; import java.util.*;
public interface PrestamoEmpleadoRepository extends JpaRepository<PrestamoEmpleado,Long>{ List<PrestamoEmpleado> findByPersonaIdPersonaAndEstado(Integer id,EstadoPrestamo e); }
