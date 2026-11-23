package com.bcsystems.siga.service;

import com.bcsystems.siga.domain.*;
import com.bcsystems.siga.dto.*;
import com.bcsystems.siga.exception.NotFoundException;
import com.bcsystems.siga.repository.*;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.Duration;
import java.time.LocalDate;

@Service
public class HorarioLaboralService {
    private final HorarioRepository horarios;
    private final PersonaRepository personas;
    private final UsuarioHorarioRepository asignaciones;

    public HorarioLaboralService(HorarioRepository horarios, PersonaRepository personas, UsuarioHorarioRepository asignaciones) {
        this.horarios = horarios; this.personas = personas; this.asignaciones = asignaciones;
    }
    public java.util.List<Horario> listar() { return horarios.findAll(); }
    public Horario obtener(Integer id) { return horarios.findById(id).orElseThrow(() -> new NotFoundException("Horario no encontrado")); }

    @Transactional public Horario crear(HorarioRequest request) {
        Horario horario = Horario.builder().nombre(request.nombre()).activo(true).build();
        llenar(horario, request);
        return horarios.save(horario);
    }
    @Transactional public Horario actualizar(Integer id, HorarioRequest request) {
        Horario horario = obtener(id);
        horario.setNombre(request.nombre()); horario.getDias().clear(); horarios.flush(); llenar(horario, request);
        Horario saved = horarios.save(horario);
        asignaciones.findByHorarioIdHorario(id).forEach(a -> recalcular(a.getPersona(), saved));
        return saved;
    }
    @Transactional public void desactivar(Integer id) { Horario h = obtener(id); h.setActivo(false); horarios.save(h); }

    private void llenar(Horario horario, HorarioRequest request) {
        if (request.dias() == null) return;
        for (HorarioRequest.DiaRequest d : request.dias()) {
            if (d.dia() == null || d.entrada() == null || d.salida() == null || !d.entrada().isBefore(d.salida()))
                throw new IllegalArgumentException("Entrada y salida inválidas");
            HorarioDia dia = HorarioDia.builder().horario(horario).dia(d.dia()).entrada(d.entrada()).salida(d.salida()).build();
            if (d.breaks() != null) for (HorarioRequest.BreakRequest b : d.breaks()) {
                if (b.inicio() == null || b.fin() == null || !b.inicio().isBefore(b.fin())) throw new IllegalArgumentException("Break inválido");
                dia.getBreaks().add(HorarioBreak.builder().horarioDia(dia).inicio(b.inicio()).fin(b.fin()).build());
            }
            horario.getDias().add(dia);
        }
    }
    @Transactional public UsuarioHorario asignar(Integer personaId, AsignarHorarioRequest request) {
        Persona persona = personas.findById(personaId).orElseThrow(() -> new NotFoundException("Usuario no encontrado"));
        Horario horario = obtener(request.horarioId());
        asignaciones.findFirstByPersonaIdPersonaAndActivoTrueOrderByIdUsuarioHorarioDesc(personaId).ifPresent(a -> { a.setActivo(false); asignaciones.save(a); });
        UsuarioHorario assignment = asignaciones.save(UsuarioHorario.builder().persona(persona).horario(horario)
                .vigenteDesde(request.vigenteDesde() == null ? LocalDate.now() : request.vigenteDesde())
                .vigenteHasta(request.vigenteHasta()).activo(true).build());
        recalcular(persona, horario); personas.save(persona); return assignment;
    }
    public UsuarioHorario asignado(Integer personaId) { return asignaciones.findFirstByPersonaIdPersonaAndActivoTrueOrderByIdUsuarioHorarioDesc(personaId).orElseThrow(() -> new NotFoundException("Horario no asignado")); }
    public UsuarioHorario asignadoPorUsuario(String username) {
        Persona persona = personas.findByUsuarioIgnoreCase(username).orElseThrow(() -> new NotFoundException("Usuario no encontrado"));
        return asignado(persona.getIdPersona());
    }
    public BigDecimal horasSemanales(Horario h) { return BigDecimal.valueOf(h.getDias().stream().mapToLong(d -> {
        long total = Duration.between(d.getEntrada(), d.getSalida()).toMinutes();
        return total - d.getBreaks().stream().mapToLong(b -> Duration.between(b.getInicio(), b.getFin()).toMinutes()).sum();
    }).sum()).divide(BigDecimal.valueOf(60), 2, RoundingMode.HALF_UP); }
    public void recalcular(Persona persona, Horario horario) {
        BigDecimal weekly = persona.getSueldoSemanal(), hours = horasSemanales(horario);
        persona.setSueldoHora(weekly == null || hours.signum() == 0 ? BigDecimal.ZERO : weekly.divide(hours, 2, RoundingMode.HALF_UP));
    }
}
