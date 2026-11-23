package com.bcsystems.siga.controller;

import com.bcsystems.siga.dto.ConfiguracionRequest;
import com.bcsystems.siga.dto.ConfiguracionResponse;
import com.bcsystems.siga.dto.ComisionVentaConfiguracionRequest;
import com.bcsystems.siga.dto.ComisionVentaConfiguracionResponse;
import com.bcsystems.siga.dto.ComisionVentaRolResponse;
import com.bcsystems.siga.service.ConfiguracionService;
import com.bcsystems.siga.repository.RolRepository;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/api/v1/configuraciones")
public class ConfiguracionController {

    private final ConfiguracionService configuracionService;
    private final RolRepository rolRepository;

    public ConfiguracionController(ConfiguracionService configuracionService, RolRepository rolRepository) {
        this.configuracionService = configuracionService;
        this.rolRepository = rolRepository;
    }

    @GetMapping
    @PreAuthorize("hasAuthority('CONFIGURACION_VER')")
    public ResponseEntity<List<ConfiguracionResponse>> listar() {
        return ResponseEntity.ok(configuracionService.listar());
    }

    @GetMapping("/{clave}")
    @PreAuthorize("hasAuthority('CONFIGURACION_VER')")
    public ResponseEntity<ConfiguracionResponse> obtener(@PathVariable String clave) {
        return ResponseEntity.ok(configuracionService.obtener(clave));
    }

    @PutMapping("/{clave}")
    @PreAuthorize("hasAuthority('CONFIGURACION_EDITAR')")
    public ResponseEntity<ConfiguracionResponse> actualizar(
            @PathVariable String clave, @Valid @RequestBody ConfiguracionRequest request) {
        return ResponseEntity.ok(configuracionService.actualizar(clave, request));
    }

    @GetMapping("/comisiones-venta")
    @PreAuthorize("hasAuthority('CONFIGURACION_VER')")
    public ResponseEntity<ComisionVentaConfiguracionResponse> configuracionComisionesVenta() {
        Set<Integer> seleccionados = parseRoles(configuracionService.getValor("comisionVentaRoles", ""));
        java.math.BigDecimal porcentaje = java.math.BigDecimal.valueOf(configuracionService.getValorDouble("comisionVentaPorcentaje", 0));
        List<ComisionVentaRolResponse> roles = rolRepository.findByActivoTrueOrderByNombre().stream()
                .map(r -> new ComisionVentaRolResponse(r.getIdRol(), r.getNombre(), seleccionados.contains(r.getIdRol())))
                .toList();
        return ResponseEntity.ok(new ComisionVentaConfiguracionResponse(porcentaje, roles));
    }

    @PutMapping("/comisiones-venta")
    @PreAuthorize("hasAuthority('CONFIGURACION_EDITAR')")
    public ResponseEntity<ComisionVentaConfiguracionResponse> actualizarComisionesVenta(@RequestBody ComisionVentaConfiguracionRequest request) {
        if (request.porcentaje() == null || request.porcentaje().signum() < 0 || request.porcentaje().compareTo(java.math.BigDecimal.valueOf(100)) > 0) {
            throw new IllegalArgumentException("El porcentaje debe estar entre 0 y 100");
        }
        Set<Integer> ids = request.rolIds() == null ? Set.of() : new java.util.HashSet<>(request.rolIds());
        Set<Integer> validos = rolRepository.findByActivoTrueOrderByNombre().stream().map(r -> r.getIdRol()).collect(Collectors.toSet());
        if (!validos.containsAll(ids)) throw new IllegalArgumentException("Uno o más roles no son válidos");
        configuracionService.actualizar("comisionVentaPorcentaje", new ConfiguracionRequest("comisionVentaPorcentaje", request.porcentaje().toPlainString(), null));
        configuracionService.actualizar("comisionVentaRoles", new ConfiguracionRequest("comisionVentaRoles", ids.stream().sorted().map(String::valueOf).collect(Collectors.joining(",")), null));
        return configuracionComisionesVenta();
    }

    private Set<Integer> parseRoles(String value) {
        return java.util.Arrays.stream(value.split(",")).filter(s -> !s.isBlank()).map(String::trim).flatMap(s -> { try { return java.util.stream.Stream.of(Integer.valueOf(s)); } catch (NumberFormatException e) { return java.util.stream.Stream.empty(); } }).collect(Collectors.toSet());
    }
}
