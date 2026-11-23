package com.bcsystems.siga.controller;
import com.bcsystems.siga.dto.AsignarHorarioRequest;
import com.bcsystems.siga.service.HorarioLaboralService;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;
@RestController @RequestMapping("/api/v1/personas/{personaId}/horario")
public class UsuarioHorarioController {
    private final HorarioLaboralService service;
    public UsuarioHorarioController(HorarioLaboralService service){this.service=service;}
    @GetMapping @PreAuthorize("hasAuthority('PERSONAS_VER')") public Object obtener(@PathVariable Integer personaId){return service.asignado(personaId);}
    @PostMapping @PreAuthorize("hasAuthority('PERSONAS_EDITAR')") public ResponseEntity<?> asignar(@PathVariable Integer personaId,@RequestBody AsignarHorarioRequest r){return ResponseEntity.ok(service.asignar(personaId,r));}
}
