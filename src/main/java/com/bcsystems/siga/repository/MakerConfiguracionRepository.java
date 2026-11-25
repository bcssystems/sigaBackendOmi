package com.bcsystems.siga.repository;

import com.bcsystems.siga.domain.Configuracion;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

/** Acceso al subconjunto Maker de la tabla general configuracion. */
public interface MakerConfiguracionRepository extends JpaRepository<Configuracion, Integer> {
    Optional<Configuracion> findByClave(String clave);

    List<Configuracion> findByClaveStartingWithOrderByClaveAsc(String prefijo);
}
