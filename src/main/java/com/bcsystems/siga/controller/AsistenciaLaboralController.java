package com.bcsystems.siga.controller;
import com.bcsystems.siga.domain.en.TipoMarcaje;
import com.bcsystems.siga.service.AsistenciaLaboralService;
import com.bcsystems.siga.service.HorarioLaboralService;
import org.springframework.core.io.Resource;
import org.springframework.http.*;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;
import java.math.BigDecimal;
@RestController @RequestMapping("/api/v1")
public class AsistenciaLaboralController {
    private final AsistenciaLaboralService service;
    private final HorarioLaboralService horarios;
    public AsistenciaLaboralController(AsistenciaLaboralService service, HorarioLaboralService horarios){this.service=service;this.horarios=horarios;}
    @PostMapping(value="/checador/entrada",consumes=MediaType.MULTIPART_FORM_DATA_VALUE) public Object entrada(Authentication a,@RequestPart(value="selfie",required=false) MultipartFile selfie,@RequestParam BigDecimal latitud,@RequestParam BigDecimal longitud,@RequestParam(required=false) BigDecimal precisionMetros){return service.marcar(a.getName(),TipoMarcaje.ENTRADA,selfie,latitud,longitud,precisionMetros);}
    @PostMapping(value="/checador/salida",consumes=MediaType.MULTIPART_FORM_DATA_VALUE) public Object salida(Authentication a,@RequestPart(value="selfie",required=false) MultipartFile selfie,@RequestParam BigDecimal latitud,@RequestParam BigDecimal longitud,@RequestParam(required=false) BigDecimal precisionMetros){return service.marcar(a.getName(),TipoMarcaje.SALIDA,selfie,latitud,longitud,precisionMetros);}
    @GetMapping("/checador/mi-tablero") public Object tablero(Authentication a){return service.tablero(a.getName());}
    @GetMapping("/checador/mi-horario") public Object horario(Authentication a){return horarios.asignadoPorUsuario(a.getName());}
    @GetMapping("/asistencias") @PreAuthorize("hasAuthority('PERSONAS_VER')") public Object asistencias(){return service.todas();}
    @GetMapping("/asistencias/evidencias") @PreAuthorize("hasAuthority('PERSONAS_VER')") public Object evidencias(){return service.evidencias(false);}
    @GetMapping("/asistencias/evidencias/pendientes") @PreAuthorize("hasAuthority('PERSONAS_VER')") public Object pendientes(){return service.evidencias(true);}
    @GetMapping("/asistencias/evidencias/{id}/archivo") @PreAuthorize("hasAuthority('PERSONAS_VER')") public ResponseEntity<Resource> archivo(@PathVariable Integer id){return ResponseEntity.ok().contentType(MediaType.IMAGE_JPEG).body(service.archivo(id));}
    @PostMapping("/asistencias/evidencias/{id}/aprobar") @PreAuthorize("hasAuthority('PERSONAS_EDITAR')") public Object aprobar(@PathVariable Integer id){return service.revisar(id,true);}
    @PostMapping("/asistencias/evidencias/{id}/rechazar") @PreAuthorize("hasAuthority('PERSONAS_EDITAR')") public Object rechazar(@PathVariable Integer id){return service.revisar(id,false);}
}
