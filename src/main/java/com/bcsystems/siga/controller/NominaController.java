package com.bcsystems.siga.controller;
import com.bcsystems.siga.dto.*; import com.bcsystems.siga.service.*; import org.springframework.http.*; import org.springframework.security.access.prepost.PreAuthorize; import org.springframework.web.bind.annotation.*; import java.time.LocalDate; import java.util.*;
@RestController @RequestMapping("/api/v1/nomina") public class NominaController { private final NominaService s; private final NominaPdfService p; public NominaController(NominaService s,NominaPdfService p){this.s=s;this.p=p;}
 @GetMapping @PreAuthorize("hasAuthority('NOMINAS_VER')") public List<NominaResponse> listar(){return s.listar();}
 @PostMapping("/preparar-lote") @PreAuthorize("hasAuthority('NOMINAS_CREAR')") public NominaBatchResponse preparar(@RequestBody NominaBatchRequest r){return s.preparar(r);}
 @GetMapping("/desglose/{personaId}") @PreAuthorize("hasAuthority('NOMINAS_VER')") public NominaDesgloseResponse desglose(@PathVariable Integer personaId,@RequestParam LocalDate inicio,@RequestParam LocalDate fin){return s.desglose(personaId,inicio,fin);}
 @GetMapping("/{id}/desglose") @PreAuthorize("hasAuthority('NOMINAS_VER')") public NominaDesgloseResponse desgloseNomina(@PathVariable Long id){return s.desgloseNomina(id);}
 @PostMapping("/{id}/revisar") @PreAuthorize("hasAuthority('NOMINAS_REVISAR')") public NominaResponse revisar(@PathVariable Long id,@RequestBody(required=false) NominaReviewRequest r){return s.revisar(id,r);}
 @PostMapping("/generar-lote") @PreAuthorize("hasAuthority('NOMINAS_CREAR')") public NominaBatchResponse generar(@RequestBody List<Long> ids){return s.generar(ids);}
 @PostMapping("/{id}/cancelar") @PreAuthorize("hasAuthority('NOMINAS_CANCELAR')") public NominaResponse cancelar(@PathVariable Long id){return s.cancelar(id);}
 @GetMapping("/{id}/pdf") @PreAuthorize("hasAuthority('NOMINAS_VER')") public ResponseEntity<byte[]> pdf(@PathVariable Long id){return ResponseEntity.ok().contentType(MediaType.APPLICATION_PDF).header(HttpHeaders.CONTENT_DISPOSITION,"attachment; filename=nomina-"+id+".pdf").body(p.generar(id));}
 @PostMapping("/lote/pdf") @PreAuthorize("hasAuthority('NOMINAS_VER')") public ResponseEntity<byte[]> pdfLote(@RequestBody List<Long> ids){return ResponseEntity.ok().contentType(MediaType.APPLICATION_PDF).header(HttpHeaders.CONTENT_DISPOSITION,"attachment; filename=nominas-lote.pdf").body(p.generarLote(ids));}
}
