package com.bcsystems.siga.controller;

import com.bcsystems.siga.dto.MakerConfiguracionRequest;
import com.bcsystems.siga.dto.MakerConfiguracionResponse;
import com.bcsystems.siga.dto.MakerCotizacionRequest;
import com.bcsystems.siga.dto.MakerFilamentoMovimientoRequest;
import com.bcsystems.siga.dto.MakerFilamentoMovimientoResponse;
import com.bcsystems.siga.dto.MakerFilamentoRequest;
import com.bcsystems.siga.dto.MakerMaterialRequest;
import com.bcsystems.siga.dto.MakerNotificacionResponse;
import com.bcsystems.siga.dto.MakerPrecioFinalRequest;
import com.bcsystems.siga.dto.MakerProyectoRequest;
import com.bcsystems.siga.dto.MakerProyectoResponse;
import com.bcsystems.siga.dto.MakerResponse;
import com.bcsystems.siga.dto.MakerTareaRequest;
import com.bcsystems.siga.dto.MakerTareaResponse;
import com.bcsystems.siga.dto.MakerUserResponse;
import com.bcsystems.siga.domain.MakerMaterial;
import com.bcsystems.siga.service.MakerService;
import com.bcsystems.siga.service.MakerStockService;
import com.bcsystems.siga.service.MakerWorkspaceService;
import jakarta.validation.Valid;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.security.Principal;
import java.util.List;

@RestController
@RequestMapping("/api/v1/maker")
public class MakerController {
    private final MakerService service;
    private final MakerStockService stockService;
    private final MakerWorkspaceService workspace;

    public MakerController(MakerService service, MakerStockService stockService,
                           MakerWorkspaceService workspace) {
        this.service = service;
        this.stockService = stockService;
        this.workspace = workspace;
    }

    @GetMapping("/materiales")
    @PreAuthorize("hasAuthority('MAKER_VER')")
    public ResponseEntity<List<MakerMaterial>> materiales(
            @RequestParam(defaultValue = "false") boolean todos, Authentication authentication) {
        return ResponseEntity.ok(service.materials(todos && canAdminCatalog(authentication)));
    }

    @PostMapping("/materiales")
    @PreAuthorize("hasAuthority('MAKER_CATALOGO_CREAR')")
    public ResponseEntity<MakerMaterial> crearMaterial(@Valid @RequestBody MakerMaterialRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED).body(service.saveMaterial(null, request));
    }

    @PutMapping("/materiales/{id}")
    @PreAuthorize("hasAuthority('MAKER_CATALOGO_EDITAR')")
    public ResponseEntity<MakerMaterial> actualizarMaterial(@PathVariable Integer id,
                                                             @Valid @RequestBody MakerMaterialRequest request) {
        return ResponseEntity.ok(service.saveMaterial(id, request));
    }

    @DeleteMapping("/materiales/{id}")
    @PreAuthorize("hasAuthority('MAKER_CATALOGO_ELIMINAR')")
    public ResponseEntity<Void> desactivarMaterial(@PathVariable Integer id) {
        service.deleteMaterial(id);
        return ResponseEntity.noContent().build();
    }

    @GetMapping("/filamentos")
    @PreAuthorize("hasAuthority('MAKER_VER')")
    public ResponseEntity<?> filamentos(@RequestParam(defaultValue = "false") boolean todos,
                                        Authentication authentication) {
        return ResponseEntity.ok(service.filaments(todos && canAdminCatalog(authentication)));
    }

    @PostMapping("/filamentos")
    @PreAuthorize("hasAuthority('MAKER_CATALOGO_CREAR')")
    public ResponseEntity<?> crearFilamento(@Valid @RequestBody MakerFilamentoRequest request,
                                            Principal principal, Authentication authentication) {
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(service.saveFilament(null, request, username(principal), canManageStock(authentication)));
    }

    @PutMapping("/filamentos/{id}")
    @PreAuthorize("hasAuthority('MAKER_CATALOGO_EDITAR')")
    public ResponseEntity<?> actualizarFilamento(@PathVariable Integer id,
                                                 @Valid @RequestBody MakerFilamentoRequest request,
                                                 Principal principal, Authentication authentication) {
        return ResponseEntity.ok(service.saveFilament(id, request, username(principal), canManageStock(authentication)));
    }

    @DeleteMapping("/filamentos/{id}")
    @PreAuthorize("hasAuthority('MAKER_CATALOGO_ELIMINAR')")
    public ResponseEntity<Void> desactivarFilamento(@PathVariable Integer id) {
        service.deleteFilament(id);
        return ResponseEntity.noContent().build();
    }

    @GetMapping("/configuracion")
    @PreAuthorize("hasAuthority('MAKER_CONFIGURACION_VER')")
    public ResponseEntity<List<MakerConfiguracionResponse>> configuracion() {
        return ResponseEntity.ok(service.settings());
    }

    @GetMapping("/configuracion/publica")
    @PreAuthorize("hasAuthority('MAKER_VER')")
    public ResponseEntity<List<MakerConfiguracionResponse>> configuracionPublica() {
        return ResponseEntity.ok(service.settings());
    }

    @PutMapping("/configuracion/{clave}")
    @PreAuthorize("hasAuthority('MAKER_CONFIGURACION_EDITAR')")
    public ResponseEntity<MakerConfiguracionResponse> actualizarConfiguracion(
            @PathVariable String clave, @Valid @RequestBody MakerConfiguracionRequest request) {
        return ResponseEntity.ok(service.updateSetting(clave, request));
    }

    @PostMapping("/cotizaciones")
    @PreAuthorize("hasAuthority('MAKER_COTIZACION_CREAR')")
    public ResponseEntity<MakerResponse> crearCotizacion(@Valid @RequestBody MakerCotizacionRequest request,
                                                         Principal principal) {
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(service.createQuote(request, username(principal)));
    }

    @GetMapping("/cotizaciones")
    @PreAuthorize("hasAuthority('MAKER_VER')")
    public ResponseEntity<?> cotizaciones(@RequestParam(defaultValue = "0") int page,
                                          @RequestParam(defaultValue = "20") int size) {
        return ResponseEntity.ok(service.quotes(pageRequest(page, size, "fecha")));
    }

    @GetMapping("/cotizaciones/{id}")
    @PreAuthorize("hasAuthority('MAKER_VER')")
    public ResponseEntity<MakerResponse> cotizacion(@PathVariable Integer id) {
        return ResponseEntity.ok(service.quote(id));
    }

    @PutMapping("/cotizaciones/{id}")
    @PreAuthorize("hasAuthority('MAKER_COTIZACION_EDITAR')")
    public ResponseEntity<MakerResponse> actualizarCotizacion(@PathVariable Integer id,
                                                              @Valid @RequestBody MakerCotizacionRequest request) {
        return ResponseEntity.ok(service.updateQuote(id, request));
    }

    @GetMapping(value = "/cotizaciones/{id}/pdf", produces = MediaType.APPLICATION_PDF_VALUE)
    @PreAuthorize("hasAuthority('MAKER_VER')")
    public ResponseEntity<byte[]> pdfCotizacion(@PathVariable Integer id) {
        byte[] pdf = service.quotePdf(id);
        return ResponseEntity.ok()
                .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=cotizacion-maker-" + id + ".pdf")
                .body(pdf);
    }

    @DeleteMapping("/cotizaciones/{id}")
    @PreAuthorize("hasAuthority('MAKER_COTIZACION_ELIMINAR')")
    public ResponseEntity<Void> eliminarCotizacion(@PathVariable Integer id) {
        service.deleteQuote(id);
        return ResponseEntity.noContent().build();
    }

    @PatchMapping("/cotizaciones/{id}/precio-final")
    @PreAuthorize("hasAuthority('MAKER_COTIZACION_EDITAR')")
    public ResponseEntity<MakerResponse> actualizarPrecioFinal(@PathVariable Integer id,
                                                                @Valid @RequestBody MakerPrecioFinalRequest request) {
        return ResponseEntity.ok(service.updateFinalPrice(id, request));
    }

    @GetMapping("/stock/movimientos")
    @PreAuthorize("hasAuthority('MAKER_STOCK_VER')")
    public ResponseEntity<?> movimientosStock(@RequestParam(required = false) Integer filamentoId,
                                               @RequestParam(defaultValue = "0") int page,
                                               @RequestParam(defaultValue = "50") int size) {
        return ResponseEntity.ok(stockService.listar(filamentoId, pageRequest(page, size, "fechaMovimiento")));
    }

    @GetMapping("/filamentos/{filamentoId}/movimientos")
    @PreAuthorize("hasAuthority('MAKER_STOCK_VER')")
    public ResponseEntity<?> movimientosFilamento(@PathVariable Integer filamentoId,
                                                  @RequestParam(defaultValue = "0") int page,
                                                  @RequestParam(defaultValue = "50") int size) {
        return ResponseEntity.ok(stockService.listar(filamentoId, pageRequest(page, size, "fechaMovimiento")));
    }

    @PostMapping("/stock/movimientos")
    @PreAuthorize("hasAuthority('MAKER_STOCK_MOVIMIENTO')")
    public ResponseEntity<MakerFilamentoMovimientoResponse> registrarMovimiento(
            @Valid @RequestBody MakerFilamentoMovimientoRequest request, Principal principal) {
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(toResponse(stockService.registrar(request, username(principal))));
    }

    @GetMapping("/usuarios")
    @PreAuthorize("hasAuthority('MAKER_VER')")
    public ResponseEntity<List<MakerUserResponse>> usuarios() {
        return ResponseEntity.ok(workspace.users());
    }

    @GetMapping("/proyectos")
    @PreAuthorize("hasAuthority('MAKER_VER')")
    public ResponseEntity<?> proyectos(@RequestParam(defaultValue = "0") int page,
                                       @RequestParam(defaultValue = "100") int size) {
        return ResponseEntity.ok(workspace.projects(pageRequest(page, size, "fechaCreacion")));
    }

    @GetMapping("/proyectos/{id}")
    @PreAuthorize("hasAuthority('MAKER_VER')")
    public ResponseEntity<MakerProyectoResponse> proyecto(@PathVariable Integer id) {
        return ResponseEntity.ok(workspace.project(id));
    }

    @PostMapping("/proyectos")
    @PreAuthorize("hasAuthority('MAKER_PROYECTO_CREAR')")
    public ResponseEntity<MakerProyectoResponse> crearProyecto(@Valid @RequestBody MakerProyectoRequest request,
                                                               Principal principal) {
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(workspace.saveProject(null, request, username(principal)));
    }

    @PutMapping("/proyectos/{id}")
    @PreAuthorize("hasAuthority('MAKER_PROYECTO_EDITAR')")
    public ResponseEntity<MakerProyectoResponse> actualizarProyecto(@PathVariable Integer id,
                                                                     @Valid @RequestBody MakerProyectoRequest request,
                                                                     Principal principal) {
        return ResponseEntity.ok(workspace.saveProject(id, request, username(principal)));
    }

    @DeleteMapping("/proyectos/{id}")
    @PreAuthorize("hasAuthority('MAKER_PROYECTO_ELIMINAR')")
    public ResponseEntity<Void> eliminarProyecto(@PathVariable Integer id) {
        workspace.deleteProject(id);
        return ResponseEntity.noContent().build();
    }

    @GetMapping("/proyectos/{id}/tareas")
    @PreAuthorize("hasAuthority('MAKER_VER')")
    public ResponseEntity<List<MakerTareaResponse>> tareasProyecto(@PathVariable Integer id) {
        return ResponseEntity.ok(workspace.projectTasks(id));
    }

    @GetMapping("/tareas")
    @PreAuthorize("hasAuthority('MAKER_VER')")
    public ResponseEntity<?> tareas(@RequestParam(defaultValue = "0") int page,
                                    @RequestParam(defaultValue = "100") int size) {
        return ResponseEntity.ok(workspace.tasks(pageRequest(page, size, "fechaCreacion")));
    }

    @GetMapping("/tareas/{id}")
    @PreAuthorize("hasAuthority('MAKER_VER')")
    public ResponseEntity<MakerTareaResponse> tarea(@PathVariable Integer id) {
        return ResponseEntity.ok(workspace.task(id));
    }

    @PostMapping("/tareas")
    @PreAuthorize("hasAuthority('MAKER_TAREA_CREAR')")
    public ResponseEntity<MakerTareaResponse> crearTarea(@Valid @RequestBody MakerTareaRequest request,
                                                         Principal principal) {
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(workspace.saveTask(null, request, username(principal)));
    }

    @PutMapping("/tareas/{id}")
    @PreAuthorize("hasAuthority('MAKER_TAREA_EDITAR')")
    public ResponseEntity<MakerTareaResponse> actualizarTarea(@PathVariable Integer id,
                                                              @Valid @RequestBody MakerTareaRequest request,
                                                              Principal principal) {
        return ResponseEntity.ok(workspace.saveTask(id, request, username(principal)));
    }

    @DeleteMapping("/tareas/{id}")
    @PreAuthorize("hasAuthority('MAKER_TAREA_ELIMINAR')")
    public ResponseEntity<Void> eliminarTarea(@PathVariable Integer id) {
        workspace.deleteTask(id);
        return ResponseEntity.noContent().build();
    }

    @GetMapping("/notificaciones")
    @PreAuthorize("hasAuthority('MAKER_NOTIFICACIONES_VER')")
    public ResponseEntity<?> notificaciones(@RequestParam(defaultValue = "0") int page,
                                            @RequestParam(defaultValue = "20") int size,
                                            Principal principal) {
        return ResponseEntity.ok(workspace.notifications(username(principal), pageRequest(page, size, "fecha")));
    }

    @GetMapping("/notificaciones/no-leidas")
    @PreAuthorize("hasAuthority('MAKER_NOTIFICACIONES_VER')")
    public ResponseEntity<Long> notificacionesNoLeidas(Principal principal) {
        return ResponseEntity.ok(workspace.unreadNotifications(username(principal)));
    }

    @PatchMapping("/notificaciones/{id}/leida")
    @PreAuthorize("hasAuthority('MAKER_NOTIFICACIONES_EDITAR')")
    public ResponseEntity<Void> marcarNotificacion(@PathVariable Integer id, Principal principal) {
        workspace.readNotification(id, username(principal));
        return ResponseEntity.noContent().build();
    }

    @PatchMapping("/notificaciones/leer-todas")
    @PreAuthorize("hasAuthority('MAKER_NOTIFICACIONES_EDITAR')")
    public ResponseEntity<Void> marcarTodas(Principal principal) {
        workspace.readAllNotifications(username(principal));
        return ResponseEntity.noContent().build();
    }

    private PageRequest pageRequest(int page, int size, String property) {
        int safePage = Math.max(0, page);
        int safeSize = Math.min(Math.max(1, size), 200);
        return PageRequest.of(safePage, safeSize, Sort.by(property).descending());
    }

    private String username(Principal principal) {
        return principal == null ? null : principal.getName();
    }

    private boolean canAdminCatalog(Authentication authentication) {
        if (authentication == null) {
            return false;
        }
        return authentication.getAuthorities().stream()
                .anyMatch(authority -> authority.getAuthority().equals("MAKER_CATALOGO_EDITAR")
                        || authority.getAuthority().equals("MAKER_CATALOGO_ELIMINAR"));
    }

    private boolean canManageStock(Authentication authentication) {
        return authentication != null && authentication.getAuthorities().stream()
                .anyMatch(authority -> authority.getAuthority().equals("MAKER_STOCK_MOVIMIENTO"));
    }

    private MakerFilamentoMovimientoResponse toResponse(
            com.bcsystems.siga.domain.MakerFilamentoMovimiento movement) {
        var filament = movement.getFilamento();
        return new MakerFilamentoMovimientoResponse(
                movement.getId(), filament == null ? null : filament.getId(),
                filament == null ? null : filament.getNombre(), movement.getTipo().name(),
                movement.getCantidad(), movement.getStockAnterior(), movement.getStockNuevo(),
                movement.getReferencia(), movement.getObservacion(), movement.getUsuario(),
                movement.getFechaMovimiento());
    }
}
