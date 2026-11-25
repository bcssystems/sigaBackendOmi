package com.bcsystems.siga.service;

import com.bcsystems.siga.domain.MakerCotizacion;
import com.bcsystems.siga.domain.MakerNotificacion;
import com.bcsystems.siga.domain.MakerProyecto;
import com.bcsystems.siga.domain.MakerTarea;
import com.bcsystems.siga.domain.Persona;
import com.bcsystems.siga.dto.MakerNotificacionResponse;
import com.bcsystems.siga.dto.MakerProyectoRequest;
import com.bcsystems.siga.dto.MakerProyectoResponse;
import com.bcsystems.siga.dto.MakerTareaRequest;
import com.bcsystems.siga.dto.MakerTareaResponse;
import com.bcsystems.siga.dto.MakerUserResponse;
import com.bcsystems.siga.exception.InvalidEntryException;
import com.bcsystems.siga.exception.NotFoundException;
import com.bcsystems.siga.repository.MakerCotizacionRepository;
import com.bcsystems.siga.repository.MakerNotificacionRepository;
import com.bcsystems.siga.repository.MakerProyectoRepository;
import com.bcsystems.siga.repository.MakerTareaRepository;
import com.bcsystems.siga.repository.PersonaRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.TreeMap;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class MakerWorkspaceService {
    private static final Set<String> PROJECT_STATES = Set.of("COTIZACION", "MUESTREO", "PRODUCCION", "ENTREGA");
    private static final Set<String> TASK_STATES = Set.of("SIN_COMENZAR", "EN_PROGRESO", "HECHO");
    private static final Set<String> PRIORITIES = Set.of("BAJA", "MEDIA", "ALTA", "URGENTE");

    private final MakerProyectoRepository projectRepository;
    private final MakerTareaRepository taskRepository;
    private final MakerNotificacionRepository notificationRepository;
    private final MakerCotizacionRepository quoteRepository;
    private final PersonaRepository personaRepository;
    private final MakerStockService stockService;

    @Transactional(readOnly = true)
    public List<MakerUserResponse> users() {
        return personaRepository.findByActivaTrueOrderByUsuarioAsc().stream()
                .map(this::userResponse).toList();
    }

    @Transactional(readOnly = true)
    public Page<MakerProyectoResponse> projects(Pageable pageable) {
        return projectRepository.findAllByOrderByFechaCreacionDesc(pageable).map(this::projectResponse);
    }

    @Transactional(readOnly = true)
    public MakerProyectoResponse project(Integer id) {
        return projectResponse(projectRepository.findById(id)
                .orElseThrow(() -> new NotFoundException("Proyecto no encontrado")));
    }

    @Transactional
    public MakerProyectoResponse saveProject(Integer id, MakerProyectoRequest request, String actor) {
        validate(request.estado(), request.prioridad(), PROJECT_STATES);
        if (request.fechaFin() != null && request.fechaFin().isBefore(LocalDate.now())) {
            throw new InvalidEntryException("La fecha límite no puede ser menor a hoy");
        }

        boolean nuevo = id == null;
        MakerProyecto project = nuevo
                ? MakerProyecto.builder().fechaCreacion(LocalDateTime.now()).filamentoDescontado(false).build()
                : projectRepository.findByIdForUpdate(id)
                .orElseThrow(() -> new NotFoundException("Proyecto no encontrado"));
        if (nuevo) {
            project.setCreador(findPersona(actor));
        }
        project.setNombre(request.nombre().trim());
        project.setDescripcion(request.descripcion());
        project.setEstado(normalize(request.estado()));
        project.setPrioridad(normalize(request.prioridad()));
        project.setColor(request.color());
        project.setFechaFin(request.fechaFin());

        MakerCotizacion previousQuote = project.getCotizacion();
        MakerCotizacion newQuote = request.cotizacionId() == null ? null : quoteRepository.findById(request.cotizacionId())
                .orElseThrow(() -> new NotFoundException("Cotización no encontrada"));
        project.setCotizacion(newQuote);
        if (previousQuote != null && previousQuote != newQuote) {
            previousQuote.setProyecto(null);
            quoteRepository.save(previousQuote);
        }

        project.getAsignados().clear();
        project.getAsignados().addAll(resolvePeople(request.usuarioIds()));
        if ("PRODUCCION".equals(project.getEstado())) {
            // En un proyecto nuevo se necesita el ID persistido para que el
            // movimiento de consumo quede correctamente referenciado.
            if (project.getId() == null) {
                projectRepository.saveAndFlush(project);
            }
            consumeFilamentForProduction(project, actor);
        }
        MakerProyecto saved = projectRepository.save(project);
        if (saved.getCotizacion() != null) {
            replaceQuoteProject(saved.getCotizacion(), saved);
            quoteRepository.save(saved.getCotizacion());
        }
        notifyPeople(saved.getAsignados(), "Proyecto asignado",
                "Fuiste asignado al proyecto: " + saved.getNombre(), "PROYECTO", saved.getId(), actor);
        return projectResponse(saved);
    }

    @Transactional
    public void deleteProject(Integer id) {
        MakerProyecto project = projectRepository.findById(id)
                .orElseThrow(() -> new NotFoundException("Proyecto no encontrado"));
        if (project.getCotizacion() != null) {
            MakerCotizacion quote = project.getCotizacion();
            quote.setProyecto(null);
            quoteRepository.save(quote);
            project.setCotizacion(null);
        }
        taskRepository.findByProyectoIdOrderByFechaCreacionDesc(id).forEach(task -> {
            task.setProyecto(null);
            taskRepository.save(task);
        });
        project.getAsignados().clear();
        taskRepository.flush();
        projectRepository.flush();
        projectRepository.delete(project);
        projectRepository.flush();
    }

    @Transactional(readOnly = true)
    public Page<MakerTareaResponse> tasks(Pageable pageable) {
        return taskRepository.findAllByOrderByFechaCreacionDesc(pageable).map(this::taskResponse);
    }

    @Transactional(readOnly = true)
    public List<MakerTareaResponse> projectTasks(Integer projectId) {
        if (!projectRepository.existsById(projectId)) {
            throw new NotFoundException("Proyecto no encontrado");
        }
        return taskRepository.findByProyectoIdOrderByFechaCreacionDesc(projectId).stream()
                .map(this::taskResponse).toList();
    }

    @Transactional(readOnly = true)
    public MakerTareaResponse task(Integer id) {
        return taskResponse(taskRepository.findById(id)
                .orElseThrow(() -> new NotFoundException("Tarea no encontrada")));
    }

    @Transactional
    public MakerTareaResponse saveTask(Integer id, MakerTareaRequest request, String actor) {
        validate(request.estado(), request.prioridad(), TASK_STATES);
        if (request.fechaFin() != null && request.fechaFin().isBefore(LocalDate.now())) {
            throw new InvalidEntryException("La fecha límite no puede ser menor a hoy");
        }
        MakerTarea task = id == null
                ? MakerTarea.builder().fechaCreacion(LocalDateTime.now()).build()
                : taskRepository.findById(id)
                .orElseThrow(() -> new NotFoundException("Tarea no encontrada"));
        task.setNombre(request.nombre().trim());
        task.setDescripcion(request.descripcion());
        task.setChecklist(normalizeChecklist(request.checklist()));
        task.setEstado(normalize(request.estado()));
        task.setPrioridad(normalize(request.prioridad()));
        task.setColor(request.color());
        task.setFechaFin(request.fechaFin());
        task.setProyecto(request.proyectoId() == null ? null : projectRepository.findById(request.proyectoId())
                .orElseThrow(() -> new NotFoundException("Proyecto no encontrado")));
        task.getAsignados().clear();
        task.getAsignados().addAll(resolvePeople(request.usuarioIds()));
        task.getTareasMencionadas().clear();
        if (request.tareasMencionadasIds() != null && !request.tareasMencionadasIds().isEmpty()) {
            List<MakerTarea> mentioned = taskRepository.findAllById(request.tareasMencionadasIds());
            if (mentioned.size() != new HashSet<>(request.tareasMencionadasIds()).size()) {
                throw new NotFoundException("Una o más tareas mencionadas no existen");
            }
            task.getTareasMencionadas().addAll(mentioned);
        }
        MakerTarea saved = taskRepository.save(task);
        notifyPeople(saved.getAsignados(), "Tarea asignada",
                "Fuiste asignado a la tarea: " + saved.getNombre(), "TAREA", saved.getId(), actor);
        if (request.usuariosMencionadosIds() != null) {
            notifyPeople(resolvePeople(request.usuariosMencionadosIds()), "Te mencionaron",
                    "Fuiste mencionado en la tarea: " + saved.getNombre(), "TAREA", saved.getId(), actor);
        }
        for (MakerTarea mentioned : saved.getTareasMencionadas()) {
            notifyPeople(mentioned.getAsignados(), "Tarea relacionada",
                    "La tarea " + saved.getNombre() + " mencionó tu tarea: " + mentioned.getNombre(),
                    "TAREA", saved.getId(), actor);
        }
        return taskResponse(saved);
    }

    @Transactional
    public void deleteTask(Integer id) {
        MakerTarea task = taskRepository.findById(id)
                .orElseThrow(() -> new NotFoundException("Tarea no encontrada"));
        taskRepository.findByTareaMencionadaId(id).forEach(referencingTask ->
                referencingTask.getTareasMencionadas()
                        .removeIf(mentioned -> id.equals(mentioned.getId())));
        task.getTareasMencionadas().clear();
        task.getAsignados().clear();
        taskRepository.flush();
        taskRepository.delete(task);
        taskRepository.flush();
    }

    @Transactional(readOnly = true)
    public Page<MakerNotificacionResponse> notifications(String username, Pageable pageable) {
        findPersona(username);
        return notificationRepository.findForUsuario(username, pageable).map(this::notificationResponse);
    }

    @Transactional(readOnly = true)
    public long unreadNotifications(String username) {
        findPersona(username);
        return notificationRepository.countNoLeidas(username);
    }

    @Transactional
    public void readNotification(Integer id, String username) {
        MakerNotificacion notification = notificationRepository.findById(id)
                .orElseThrow(() -> new NotFoundException("Notificación no encontrada"));
        checkOwner(notification, username);
        notification.setLeida(true);
        notificationRepository.save(notification);
    }

    @Transactional
    public void readAllNotifications(String username) {
        findPersona(username);
        notificationRepository.marcarTodasLeidas(username);
    }

    private void consumeFilamentForProduction(MakerProyecto project, String actor) {
        if (Boolean.TRUE.equals(project.getFilamentoDescontado()) || project.getCotizacion() == null) {
            return;
        }
        MakerCotizacion quote = project.getCotizacion();
        if (quote.getFilamentos() == null || quote.getFilamentos().isEmpty()) {
            return;
        }
        Map<Integer, BigDecimal> requiredKg = new TreeMap<>();
        BigDecimal net = quote.getGramosNetos() == null ? BigDecimal.ZERO : quote.getGramosNetos();
        BigDecimal waste = quote.getGramosMerma() == null ? BigDecimal.ZERO : quote.getGramosMerma();
        for (var line : quote.getFilamentos()) {
            if (line.getFilamento() == null) {
                continue;
            }
            BigDecimal lineWaste = net.signum() == 0 ? BigDecimal.ZERO
                    : waste.multiply(line.getGramos()).divide(net, 6, RoundingMode.HALF_UP);
            BigDecimal kilograms = line.getGramos().add(lineWaste)
                    .divide(BigDecimal.valueOf(1000), 6, RoundingMode.HALF_UP);
            requiredKg.merge(line.getFilamento().getId(), kilograms, BigDecimal::add);
        }
        for (Map.Entry<Integer, BigDecimal> entry : requiredKg.entrySet()) {
            stockService.consumir(entry.getKey(), entry.getValue(), "PROYECTO-" + project.getId(),
                    "Consumo de producción de la cotización " + quote.getFolio(), actor);
        }
        project.setFilamentoDescontado(true);
    }

    private void notifyPeople(List<Persona> people, String title, String message, String referenceType,
                              Integer referenceId, String actor) {
        Set<Integer> unique = new HashSet<>();
        for (Persona person : people) {
            if (person != null && unique.add(person.getIdPersona())) {
                notificationRepository.save(MakerNotificacion.builder()
                        .persona(person)
                        .titulo(title)
                        .mensaje(message)
                        .tipo("MAKER")
                        .referenciaTipo(referenceType)
                        .referenciaId(referenceId)
                        .leida(false)
                        .fecha(LocalDateTime.now())
                        .build());
            }
        }
    }

    private void checkOwner(MakerNotificacion notification, String username) {
        if (notification.getPersona() == null || notification.getPersona().getUsuario() == null
                || !notification.getPersona().getUsuario().equalsIgnoreCase(username)) {
            throw new InvalidEntryException("La notificación no pertenece al usuario");
        }
    }

    private void validate(String state, String priority, Set<String> validStates) {
        if (!validStates.contains(normalize(state))) {
            throw new InvalidEntryException("Estado no válido");
        }
        if (!PRIORITIES.contains(normalize(priority))) {
            throw new InvalidEntryException("Prioridad no válida");
        }
    }

    private String normalize(String value) {
        return value == null ? "" : value.trim().toUpperCase(Locale.ROOT).replace(' ', '_');
    }

    private String normalizeChecklist(String value) {
        if (value == null || value.isBlank()) {
            return null;
        }
        return Arrays.stream(value.split("\\R"))
                .map(String::trim)
                .filter(line -> !line.isBlank())
                .map(line -> line.replaceFirst("^\\[[ xX]\\]\\s*", ""))
                .map(line -> "[ ] " + line)
                .collect(Collectors.joining("\n"));
    }

    private List<Persona> resolvePeople(List<Integer> ids) {
        if (ids == null || ids.isEmpty()) {
            return new ArrayList<>();
        }
        List<Integer> uniqueIds = ids.stream().filter(java.util.Objects::nonNull).distinct().toList();
        List<Persona> people = personaRepository.findAllById(uniqueIds);
        if (people.size() != uniqueIds.size()) {
            throw new NotFoundException("Uno o más usuarios no existen");
        }
        return people;
    }

    private Persona findPersona(String username) {
        return personaRepository.findByUsuarioIgnoreCase(username)
                .orElseThrow(() -> new NotFoundException("Usuario no encontrado"));
    }

    private void replaceQuoteProject(MakerCotizacion quote, MakerProyecto project) {
        MakerProyecto previousProject = quote.getProyecto();
        if (previousProject != null && previousProject != project) {
            previousProject.setCotizacion(null);
            projectRepository.save(previousProject);
        }
        quote.setProyecto(project);
    }

    private MakerUserResponse userResponse(Persona person) {
        return new MakerUserResponse(person.getIdPersona(), person.getUsuario());
    }

    private MakerProyectoResponse projectResponse(MakerProyecto project) {
        long total = taskRepository.countByProyectoId(project.getId());
        long done = taskRepository.countByProyectoIdAndEstado(project.getId(), "HECHO");
        return new MakerProyectoResponse(
                project.getId(), project.getNombre(), project.getDescripcion(), project.getEstado(),
                project.getPrioridad(), project.getColor(), project.getFechaFin(), project.getFechaCreacion(),
                project.getCotizacion() == null ? null : project.getCotizacion().getId(),
                project.getCotizacion() == null ? null : project.getCotizacion().getFolio(),
                project.getAsignados().stream().map(this::userResponse).toList(), total, done,
                Boolean.TRUE.equals(project.getFilamentoDescontado()));
    }

    private MakerTareaResponse taskResponse(MakerTarea task) {
        return new MakerTareaResponse(
                task.getId(), task.getNombre(), task.getDescripcion(), task.getChecklist(), task.getEstado(),
                task.getPrioridad(), task.getColor(), task.getFechaFin(), task.getFechaCreacion(),
                task.getProyecto() == null ? null : task.getProyecto().getId(),
                task.getProyecto() == null ? null : task.getProyecto().getNombre(),
                task.getAsignados().stream().map(this::userResponse).toList(),
                task.getTareasMencionadas().stream()
                        .map(mentioned -> new MakerTareaResponse.MakerTareaMentionResponse(
                                mentioned.getId(), mentioned.getNombre())).toList());
    }

    private MakerNotificacionResponse notificationResponse(MakerNotificacion notification) {
        return new MakerNotificacionResponse(notification.getId(), notification.getTitulo(),
                notification.getMensaje(), notification.getTipo(), notification.getReferenciaTipo(),
                notification.getReferenciaId(), notification.getLeida(), notification.getFecha());
    }
}
