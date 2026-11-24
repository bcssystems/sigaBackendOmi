package com.bcsystems.siga.service;

import com.bcsystems.siga.domain.*;
import com.bcsystems.siga.domain.en.*;
import com.bcsystems.siga.dto.*;
import com.bcsystems.siga.exception.NotFoundException;
import com.bcsystems.siga.repository.*;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.io.FileSystemResource;
import org.springframework.core.io.Resource;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;
import java.io.IOException;
import java.math.BigDecimal;
import java.nio.file.*;
import java.time.*;
import java.util.*;

@Service
public class AsistenciaLaboralService {
    private final PersonaRepository personas; private final AsistenciaRepository asistencias; private final MarcajeRepository marcajes;
    private final EvidenciaAsistenciaRepository evidencias; private final com.bcsystems.siga.repository.SucursalRepository sucursales; private final UsuarioHorarioRepository asignaciones;
    private final HorarioLaboralService horarios; private final ConfiguracionService configuracion; private final Path uploadDir;
    public AsistenciaLaboralService(PersonaRepository personas, AsistenciaRepository asistencias, MarcajeRepository marcajes,
                                    EvidenciaAsistenciaRepository evidencias, com.bcsystems.siga.repository.SucursalRepository sucursales, UsuarioHorarioRepository asignaciones,
                                    HorarioLaboralService horarios, ConfiguracionService configuracion,
                                    @Value("${app.attendance.upload.dir:./uploads/privados/asistencias}") String uploadDir) {
        this.personas=personas; this.asistencias=asistencias; this.marcajes=marcajes; this.evidencias=evidencias; this.sucursales=sucursales; this.asignaciones=asignaciones; this.horarios=horarios; this.configuracion=configuracion; this.uploadDir=Paths.get(uploadDir).toAbsolutePath().normalize();
    }
    private Persona persona(String username) { return personas.findByUsuarioIgnoreCase(username).orElseThrow(() -> new NotFoundException("Usuario no encontrado")); }
    private int intConfig(String key, int def) { return configuracion.getValorInt(key, def); }

    @Transactional
    public MarcajeResponse marcar(String username, TipoMarcaje tipo, MultipartFile selfie, BigDecimal lat, BigDecimal lon, BigDecimal precision) {
        if (selfie == null || selfie.isEmpty()) throw new IllegalArgumentException("La selfie es obligatoria");
        if (lat == null || lon == null) throw new IllegalArgumentException("La ubicación es obligatoria");
        Persona user=persona(username); LocalDateTime now=LocalDateTime.now(); LocalDate date=now.toLocalDate();
        UsuarioHorario assignment=asignaciones.findFirstByPersonaIdPersonaAndActivoTrueOrderByIdUsuarioHorarioDesc(user.getIdPersona()).orElseThrow(() -> new IllegalArgumentException("No tienes un horario asignado"));
        if (assignment.getVigenteDesde()!=null && date.isBefore(assignment.getVigenteDesde())) throw new IllegalArgumentException("Tu horario aún no está vigente");
        if (assignment.getVigenteHasta()!=null && date.isAfter(assignment.getVigenteHasta())) throw new IllegalArgumentException("Tu horario ya no está vigente");
        if (configuracion.getValor("asistenciaFechaInicioOperaciones", "").length()>0 && date.isBefore(LocalDate.parse(configuracion.getValor("asistenciaFechaInicioOperaciones", "")))) throw new IllegalArgumentException("La asistencia aún no está habilitada");
        DiaSemana day=DiaSemana.values()[date.getDayOfWeek().getValue()-1]; HorarioDia schedule=assignment.getHorario().getDias().stream().filter(d -> d.getDia()==day).findFirst().orElse(null);
        if(schedule==null) throw new IllegalArgumentException("Hoy no tienes jornada asignada");
        Asistencia attendance=asistencias.findByPersonaIdPersonaAndFecha(user.getIdPersona(),date).orElseGet(() -> asistencias.save(Asistencia.builder().persona(user).fecha(date).build()));
        if(tipo==TipoMarcaje.SALIDA && attendance.getEntrada()==null) throw new IllegalArgumentException("No puedes registrar salida sin entrada");
        if(marcajes.existsByAsistenciaIdAsistenciaAndTipo(attendance.getIdAsistencia(),tipo)) throw new IllegalArgumentException("El marcaje ya existe para hoy");
        if(tipo==TipoMarcaje.ENTRADA) attendance.setEntrada(now); else attendance.setSalida(now);
        com.bcsystems.siga.domain.Sucursal closest=null; double distance=Double.MAX_VALUE;
        for(com.bcsystems.siga.domain.Sucursal branch:sucursales.findByActivaTrueOrderByNombreAsc()){
            if(branch.getLatitud()==null||branch.getLongitud()==null) continue;
            double d=distance(lat.doubleValue(),lon.doubleValue(),branch.getLatitud().doubleValue(),branch.getLongitud().doubleValue());
            if(d<distance){distance=d;closest=branch;}
        }
        boolean inside=closest!=null && distance<=intConfig("asistenciaRadioMaximo",100);
        Marcaje mark=marcajes.save(Marcaje.builder().asistencia(attendance).tipo(tipo).marcadoEn(now).latitud(lat).longitud(lon).precisionMetros(precision).distanciaMetros(closest==null?null:distance).estadoUbicacion(inside?EstadoAsistencia.APROBADA:EstadoAsistencia.PENDIENTE).sucursal(closest).build());
        if(selfie!=null&&!selfie.isEmpty()) guardarEvidencia(selfie,mark,now);
        attendance.setFueraDePunto(!inside); attendance.setEstado(inside?EstadoAsistencia.APROBADA:EstadoAsistencia.PENDIENTE);
        if(tipo==TipoMarcaje.ENTRADA) attendance.setMinutosRetardo((int)Math.max(0,Duration.between(schedule.getEntrada(),now.toLocalTime()).toMinutes()-intConfig("asistenciaToleranciaEntrada",0)));
        else attendance.setMinutosSalidaTemprana((int)Math.max(0,Duration.between(now.toLocalTime(),schedule.getSalida()).toMinutes()-intConfig("asistenciaToleranciaSalida",0)));
        asistencias.save(attendance);
        return new MarcajeResponse(attendance.getIdAsistencia(),mark.getIdMarcaje(),tipo,now,attendance.getEntrada(),attendance.getSalida(),mark.getDistanciaMetros(),mark.getEstadoUbicacion().name(),attendance.getEstado().name(),!inside,closest==null?null:closest.getNombre());
    }
    private void guardarEvidencia(MultipartFile file, Marcaje mark, LocalDateTime now){try{Files.createDirectories(uploadDir);String ext=".jpg";String original=file.getOriginalFilename();if(original!=null&&original.lastIndexOf('.')>=0)ext=original.substring(original.lastIndexOf('.')).replaceAll("[^a-zA-Z0-9.]","");String name=UUID.randomUUID()+ext;Path target=uploadDir.resolve(name).normalize();if(!target.startsWith(uploadDir))throw new IOException("Ruta inválida");file.transferTo(target);evidencias.save(EvidenciaAsistencia.builder().marcaje(mark).nombreArchivo(name).rutaArchivo(target.toString()).creadoEn(now).build());}catch(IOException e){throw new IllegalStateException("No se pudo guardar la evidencia",e);}}
    public List<Asistencia> propias(String username){return asistencias.findByPersonaIdPersonaOrderByFechaDesc(persona(username).getIdPersona());}
    public List<Asistencia> todas(){return asistencias.findAll();}
    public Map<String,Object> tablero(String username){Persona p=persona(username);UsuarioHorario a=asignaciones.findFirstByPersonaIdPersonaAndActivoTrueOrderByIdUsuarioHorarioDesc(p.getIdPersona()).orElse(null);LocalDate today=LocalDate.now();DiaSemana day=DiaSemana.values()[today.getDayOfWeek().getValue()-1];HorarioDia jornada=a==null?null:a.getHorario().getDias().stream().filter(x->x.getDia()==day).findFirst().orElse(null);Asistencia actual=asistencias.findByPersonaIdPersonaAndFecha(p.getIdPersona(),today).orElse(null);boolean entrada=actual!=null&&actual.getEntrada()!=null,salida=actual!=null&&actual.getSalida()!=null,hayJornada=jornada!=null;return Map.of("asistencias",propias(username),"horario",a==null?Map.of():a,"horasLaboralesSemanales",a==null?BigDecimal.ZERO:horarios.horasSemanales(a.getHorario()),"tieneJornadaHoy",hayJornada,"tieneEntradaHoy",entrada,"tieneSalidaHoy",salida,"puedeRegistrarEntrada",hayJornada&&!entrada,"puedeRegistrarSalida",hayJornada&&entrada&&!salida);}
    public List<EvidenciaResponse> evidencias(boolean pendientes){List<EvidenciaAsistencia> list=pendientes?evidencias.findByEstadoOrderByCreadoEnDesc(EstadoAsistencia.PENDIENTE):evidencias.findAllByOrderByCreadoEnDesc();return list.stream().map(e->{Marcaje m=e.getMarcaje();Asistencia a=m.getAsistencia();Persona p=a.getPersona();return new EvidenciaResponse(e.getIdEvidencia(),m.getIdMarcaje(),a.getIdAsistencia(),p.getIdPersona(),p.getNombre()+" "+p.getApellido(),m.getTipo(),m.getMarcadoEn(),m.getDistanciaMetros(),m.getEstadoUbicacion().name(),e.getEstado(),m.getSucursal()==null?null:m.getSucursal().getNombre());}).toList();}
    public Resource archivo(Integer id){EvidenciaAsistencia e=evidencias.findById(id).orElseThrow(()->new NotFoundException("Evidencia no encontrada"));Resource r=new FileSystemResource(e.getRutaArchivo());if(!r.exists()||!r.isReadable())throw new NotFoundException("Archivo no encontrado");return r;}
    @Transactional public EvidenciaAsistencia revisar(Integer id,boolean aprobar){EvidenciaAsistencia e=evidencias.findById(id).orElseThrow(()->new NotFoundException("Evidencia no encontrada"));e.setEstado(aprobar?EstadoAsistencia.APROBADA:EstadoAsistencia.RECHAZADA);Asistencia a=e.getMarcaje().getAsistencia();a.setEstado(aprobar&&e.getMarcaje().getEstadoUbicacion()==EstadoAsistencia.APROBADA?EstadoAsistencia.APROBADA:aprobar?EstadoAsistencia.PENDIENTE:EstadoAsistencia.RECHAZADA);asistencias.save(a);return evidencias.save(e);}
    private double distance(double lat1,double lon1,double lat2,double lon2){double r=6371000,p=Math.PI/180;double a=.5-Math.cos((lat2-lat1)*p)/2+Math.cos(lat1*p)*Math.cos(lon1*p)*(1-Math.cos((lon2-lon1)*p))/2;return 2*r*Math.asin(Math.sqrt(a));}
}
