package com.bcsystems.siga.service;

import com.bcsystems.siga.domain.*;
import com.bcsystems.siga.exception.NotFoundException;
import com.bcsystems.siga.repository.*;
import com.itextpdf.kernel.pdf.*;
import com.itextpdf.kernel.geom.PageSize;
import com.itextpdf.layout.Document;
import com.itextpdf.layout.element.*;
import com.itextpdf.layout.properties.TextAlignment;
import com.itextpdf.layout.properties.UnitValue;
import java.io.*;
import java.util.List;
import org.springframework.stereotype.Service;

@Service
public class NominaPdfService {
 private final NominaRepository repo; private final ComisionRepository comisiones; private final DeduccionRepository deducciones;
 public NominaPdfService(NominaRepository r,ComisionRepository c,DeduccionRepository d){repo=r;comisiones=c;deducciones=d;}
 public byte[] generar(Long id){try{return generarLote(List.of(id));}catch(Exception e){throw new IllegalStateException("No se pudo generar el recibo",e);}}
 public byte[] generarLote(List<Long> ids){try{ByteArrayOutputStream out=new ByteArrayOutputStream();PdfDocument pdf=new PdfDocument(new PdfWriter(out));Document d=new Document(pdf,PageSize.A6);for(int i=0;i<ids.size();i++){if(i>0)d.add(new AreaBreak());Nomina n=repo.findById(ids.get(i)).orElseThrow(()->new NotFoundException("Nómina no encontrada"));d.add(new Paragraph("RECIBO DE NÓMINA").setBold().setTextAlignment(TextAlignment.CENTER));d.add(new Paragraph("Empleado: "+n.getPersona().getNombre()+" "+n.getPersona().getApellido()));d.add(new Paragraph("Periodo: "+n.getFechaInicio()+" al "+n.getFechaFin()));Table t=new Table(UnitValue.createPercentArray(new float[]{65,35})).useAllAvailableWidth();row(t,"Sueldo base",n.getSueldoBase());d.add(t);d.add(new Paragraph("COMISIONES").setBold());Table ct=new Table(UnitValue.createPercentArray(new float[]{70,30})).useAllAvailableWidth();comisiones.findByNominaIdNomina(n.getIdNomina()).forEach(x->row(ct,x.getConcepto(),x.getMontoComision()));row(ct,"Total comisiones",n.getTotalComisiones());d.add(ct);d.add(new Paragraph("DEDUCCIONES").setBold());Table dt=new Table(UnitValue.createPercentArray(new float[]{70,30})).useAllAvailableWidth();deducciones.findByNominaIdNomina(n.getIdNomina()).forEach(x->row(dt,x.getConcepto(),x.getMonto()));row(dt,"Total deducciones",n.getTotalDeducciones());d.add(dt);Table total=new Table(UnitValue.createPercentArray(new float[]{65,35})).useAllAvailableWidth();row(total,"NETO",n.getSueldo());d.add(total);d.add(new Paragraph("Firma del empleado: ____________________"));d.add(new Paragraph("Firma del responsable: __________________"));}d.close();return out.toByteArray();}catch(Exception e){throw new IllegalStateException("No se pudo generar el lote PDF",e);}}
 private void row(Table t,String label,Object v){t.addCell(new Cell().add(new Paragraph(String.valueOf(label))));t.addCell(new Cell().add(new Paragraph(String.valueOf(v==null?0:v))));}
}
