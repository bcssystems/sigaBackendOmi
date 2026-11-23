package com.bcsystems.siga.repository;
import com.bcsystems.siga.domain.UsuarioHorario;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List;
import java.util.Optional;
public interface UsuarioHorarioRepository extends JpaRepository<UsuarioHorario, Integer> {
    Optional<UsuarioHorario> findFirstByPersonaIdPersonaAndActivoTrueOrderByIdUsuarioHorarioDesc(Integer idPersona);
    List<UsuarioHorario> findByHorarioIdHorario(Integer idHorario);
}
