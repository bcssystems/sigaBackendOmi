package com.bcsystems.siga.repository;
import com.bcsystems.siga.domain.Comision; import com.bcsystems.siga.domain.en.EstadoComision; import org.springframework.data.jpa.repository.JpaRepository; import java.util.*;
public interface ComisionRepository extends JpaRepository<Comision,Long>{ List<Comision> findByPersonaIdPersonaAndEstadoComision(Integer id,EstadoComision e); List<Comision> findByNominaIdNomina(Long id); List<Comision> findByPersonaIdPersona(Integer id); boolean existsByVentaIdVenta(Integer ventaId); List<Comision> findByVentaIdVenta(Integer ventaId); }
