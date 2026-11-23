package com.bcsystems.siga.controller;
import com.bcsystems.siga.dto.*;
import com.bcsystems.siga.service.HorarioLaboralService;
import org.springframework.http.*;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;
@RestController @RequestMapping("/api/v1/horarios")
public class HorarioLaboralController {
    private final HorarioLaboralService service;
    public HorarioLaboralController(HorarioLaboralService service){this.service=service;}
    @GetMapping @PreAuthorize("hasAuthority('PERSONAS_VER')") public Object listar(){return service.listar();}
    @GetMapping("/{id}") @PreAuthorize("hasAuthority('PERSONAS_VER')") public Object obtener(@PathVariable Integer id){return service.obtener(id);}
    @PostMapping @PreAuthorize("hasAuthority('PERSONAS_EDITAR')") public ResponseEntity<?> crear(@RequestBody HorarioRequest r){return ResponseEntity.status(HttpStatus.CREATED).body(service.crear(r));}
    @PutMapping("/{id}") @PreAuthorize("hasAuthority('PERSONAS_EDITAR')") public Object actualizar(@PathVariable Integer id,@RequestBody HorarioRequest r){return service.actualizar(id,r);}
    @DeleteMapping("/{id}") @PreAuthorize("hasAuthority('PERSONAS_EDITAR')") public ResponseEntity<Void> eliminar(@PathVariable Integer id){service.desactivar(id);return ResponseEntity.noContent().build();}
}
