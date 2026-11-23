package com.bcsystems.siga.repository;
import com.bcsystems.siga.domain.Marcaje;
import com.bcsystems.siga.domain.en.TipoMarcaje;
import org.springframework.data.jpa.repository.JpaRepository;
public interface MarcajeRepository extends JpaRepository<Marcaje, Integer> {
    boolean existsByAsistenciaIdAsistenciaAndTipo(Integer idAsistencia, TipoMarcaje tipo);
}
