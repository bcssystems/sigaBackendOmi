package com.bcsystems.siga.repository;

import com.bcsystems.siga.domain.MakerNotificacion;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface MakerNotificacionRepository extends JpaRepository<MakerNotificacion, Integer> {
    @Query("select n from MakerNotificacion n "
            + "where lower(n.persona.usuario) = lower(:usuario) order by n.fecha desc")
    Page<MakerNotificacion> findForUsuario(@Param("usuario") String usuario, Pageable pageable);

    @Query("select count(n) from MakerNotificacion n "
            + "where lower(n.persona.usuario) = lower(:usuario) and n.leida = false")
    long countNoLeidas(@Param("usuario") String usuario);

    @Modifying
    @Query("update MakerNotificacion n set n.leida = true "
            + "where lower(n.persona.usuario) = lower(:usuario) and n.leida = false")
    int marcarTodasLeidas(@Param("usuario") String usuario);
}
