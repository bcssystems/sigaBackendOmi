package com.bcsystems.siga.repository;
import com.bcsystems.siga.domain.EvidenciaAsistencia;
import com.bcsystems.siga.domain.en.EstadoAsistencia;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List;
public interface EvidenciaAsistenciaRepository extends JpaRepository<EvidenciaAsistencia, Integer> {
    List<EvidenciaAsistencia> findAllByOrderByCreadoEnDesc();
    List<EvidenciaAsistencia> findByEstadoOrderByCreadoEnDesc(EstadoAsistencia estado);
}
