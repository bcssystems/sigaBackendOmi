package com.bcsystems.siga.service;

import com.bcsystems.siga.domain.Configuracion;
import com.bcsystems.siga.domain.MakerCotizacion;
import com.bcsystems.siga.domain.MakerCotizacionExtra;
import com.bcsystems.siga.domain.MakerCotizacionFilamento;
import com.bcsystems.siga.domain.MakerFilamento;
import com.bcsystems.siga.domain.MakerMaterial;
import com.bcsystems.siga.domain.MakerProyecto;
import com.bcsystems.siga.domain.Persona;
import com.bcsystems.siga.dto.MakerCatalogResponse;
import com.bcsystems.siga.dto.MakerConfiguracionRequest;
import com.bcsystems.siga.dto.MakerConfiguracionResponse;
import com.bcsystems.siga.dto.MakerCotizacionRequest;
import com.bcsystems.siga.dto.MakerExtraInput;
import com.bcsystems.siga.dto.MakerFilamentoInput;
import com.bcsystems.siga.dto.MakerFilamentoRequest;
import com.bcsystems.siga.dto.MakerMaterialRequest;
import com.bcsystems.siga.dto.MakerPrecioFinalRequest;
import com.bcsystems.siga.dto.MakerResponse;
import com.bcsystems.siga.exception.InvalidEntryException;
import com.bcsystems.siga.exception.NotFoundException;
import com.bcsystems.siga.repository.MakerConfiguracionRepository;
import com.bcsystems.siga.repository.MakerCotizacionRepository;
import com.bcsystems.siga.repository.MakerFilamentoRepository;
import com.bcsystems.siga.repository.MakerMaterialRepository;
import com.bcsystems.siga.repository.MakerProyectoRepository;
import com.bcsystems.siga.repository.PersonaRepository;
import com.itextpdf.kernel.colors.ColorConstants;
import com.itextpdf.kernel.pdf.PdfDocument;
import com.itextpdf.kernel.pdf.PdfWriter;
import com.itextpdf.layout.Document;
import com.itextpdf.layout.element.Paragraph;
import com.itextpdf.layout.element.Table;
import com.itextpdf.layout.properties.TextAlignment;
import com.itextpdf.layout.properties.UnitValue;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.io.ByteArrayOutputStream;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class MakerService {
    private static final int MONEY_SCALE = 2;
    private static final int GRAMS_SCALE = 3;
    private static final RoundingMode ROUNDING = RoundingMode.HALF_UP;
    private static final String CONFIG_PREFIX = "maker.";

    private static final List<ConfigDefault> DEFAULTS = List.of(
            new ConfigDefault("costo_kwh", "3.50", "MXN/kWh", "Costo de electricidad"),
            new ConfigDefault("consumo_kw", "0.095", "kW", "Consumo promedio de Bambu Lab A1 durante impresión PLA (95 W)"),
            new ConfigDefault("merma_monocolor", "0.03", "%", "Merma proporcional para impresión monocolor"),
            new ConfigDefault("merma_multicolor", "0.15", "%", "Merma proporcional para impresión multicolor"),
            new ConfigDefault("purga_por_cambio", "3.00", "g", "Gramos de purga por cambio de color"),
            new ConfigDefault("margen_publico", "0.50", "%", "Margen para público"),
            new ConfigDefault("margen_mayoreo", "0.35", "%", "Margen para mayoreo"),
            new ConfigDefault("margen_distribuidor", "0.20", "%", "Margen para distribuidor")
    );

    private final MakerMaterialRepository materialRepository;
    private final MakerFilamentoRepository filamentoRepository;
    private final MakerConfiguracionRepository configRepository;
    private final MakerCotizacionRepository cotizacionRepository;
    private final MakerProyectoRepository proyectoRepository;
    private final PersonaRepository personaRepository;
    private final MakerStockService stockService;

    @Transactional
    public void ensureDefaults() {
        for (ConfigDefault defaultValue : DEFAULTS) {
            String clave = CONFIG_PREFIX + defaultValue.sufijo();
            if (configRepository.findByClave(clave).isEmpty()) {
                configRepository.save(Configuracion.builder()
                        .clave(clave)
                        .valor(defaultValue.valor())
                        .descripcion(defaultValue.descripcion())
                        .actualizadaEn(LocalDateTime.now())
                        .build());
            }
        }
    }

    @Transactional(readOnly = true)
    public List<MakerMaterial> materials(boolean all) {
        return all
                ? materialRepository.findAllByOrderByNombreAsc()
                : materialRepository.findByActivoTrueOrderByNombreAsc();
    }

    @Transactional
    public MakerMaterial saveMaterial(Integer id, MakerMaterialRequest request) {
        String nombre = request.nombre().trim();
        if ((id == null && materialRepository.existsByNombreIgnoreCase(nombre))
                || (id != null && materialRepository.existsByNombreIgnoreCaseAndIdNot(nombre, id))) {
            throw new InvalidEntryException("Ya existe un material con ese nombre");
        }
        MakerMaterial material = id == null
                ? new MakerMaterial()
                : materialRepository.findById(id)
                .orElseThrow(() -> new NotFoundException("Material no encontrado"));
        material.setNombre(nombre);
        material.setActivo(request.activo() == null || request.activo());
        return materialRepository.save(material);
    }

    @Transactional
    public void deleteMaterial(Integer id) {
        MakerMaterial material = materialRepository.findById(id)
                .orElseThrow(() -> new NotFoundException("Material no encontrado"));
        material.setActivo(false);
        materialRepository.save(material);
    }

    @Transactional(readOnly = true)
    public List<MakerCatalogResponse> filaments(boolean all) {
        return (all ? filamentoRepository.findAllByOrderByNombreAsc()
                : filamentoRepository.findByActivoTrueOrderByNombreAsc())
                .stream().map(this::filamentResponse).toList();
    }

    @Transactional
    public MakerCatalogResponse saveFilament(Integer id, MakerFilamentoRequest request, String usuario,
                                             boolean canManageStock) {
        MakerMaterial material = materialRepository.findById(request.materialId())
                .orElseThrow(() -> new NotFoundException("Material no encontrado"));
        boolean nuevo = id == null;
        MakerFilamento filament = nuevo
                ? MakerFilamento.builder().stockKg(BigDecimal.ZERO).activo(true).build()
                : filamentoRepository.findById(id)
                .orElseThrow(() -> new NotFoundException("Filamento no encontrado"));

        if (request.stockKg() != null && !canManageStock) {
            BigDecimal requestedStock = request.stockKg().setScale(GRAMS_SCALE, ROUNDING);
            BigDecimal currentStock = filament.getStockKg() == null
                    ? BigDecimal.ZERO.setScale(GRAMS_SCALE, ROUNDING)
                    : filament.getStockKg().setScale(GRAMS_SCALE, ROUNDING);
            if (requestedStock.compareTo(currentStock) != 0) {
                throw new InvalidEntryException("Se requiere permiso de movimiento de stock para cambiar existencias");
            }
        }

        filament.setNombre(request.nombre().trim());
        filament.setMaterial(material);
        filament.setPrecioKilo(request.precioKilo().setScale(MONEY_SCALE, ROUNDING));
        filament.setMarca(blankToNull(request.marca()));
        filament.setColor(blankToNull(request.color()));
        filament.setActivo(request.activo() == null || request.activo());
        if (filament.getStockKg() == null) {
            filament.setStockKg(BigDecimal.ZERO.setScale(GRAMS_SCALE, ROUNDING));
        }
        MakerFilamento saved = filamentoRepository.save(filament);

        if (request.stockKg() != null && canManageStock) {
            BigDecimal target = request.stockKg().setScale(GRAMS_SCALE, ROUNDING);
            if (nuevo) {
                if (target.signum() > 0) {
                    stockService.registrarEntradaInicial(saved.getId(), target, "ALTA_FILAMENTO", usuario);
                }
            } else if (saved.getStockKg().compareTo(target) != 0) {
                stockService.ajustar(saved.getId(), target, "EDICION_FILAMENTO",
                        "Ajuste realizado desde el catálogo de filamentos", usuario);
            }
        }
        return filamentResponse(filamentoRepository.findById(saved.getId()).orElse(saved));
    }

    @Transactional
    public void deleteFilament(Integer id) {
        MakerFilamento filament = filamentoRepository.findById(id)
                .orElseThrow(() -> new NotFoundException("Filamento no encontrado"));
        filament.setActivo(false);
        filamentoRepository.save(filament);
    }

    @Transactional
    public List<MakerConfiguracionResponse> settings() {
        ensureDefaults();
        return configRepository.findByClaveStartingWithOrderByClaveAsc(CONFIG_PREFIX)
                .stream().map(this::configurationResponse).toList();
    }

    @Transactional
    public MakerConfiguracionResponse updateSetting(String clave, MakerConfiguracionRequest request) {
        String normalized = normalizeConfigKey(clave);
        Configuracion setting = configRepository.findByClave(normalized)
                .orElseThrow(() -> new NotFoundException("Configuración Maker no encontrada"));
        setting.setValor(request.valor().stripTrailingZeros().toPlainString());
        setting.setActualizadaEn(LocalDateTime.now());
        return configurationResponse(configRepository.save(setting));
    }

    @Transactional
    public MakerResponse createQuote(MakerCotizacionRequest request, String username) {
        ensureDefaults();
        Calc calc = calculate(request);
        Persona persona = findPersona(username);
        MakerCotizacion quote = MakerCotizacion.builder()
                .folio(nextFolio())
                .fecha(LocalDateTime.now())
                .usuario(persona)
                .descripcion(blankToNull(request.descripcion()))
                .modo(normalize(request.modo()))
                .gramosNetos(scaleGrams(request.gramosNetos()))
                .gramosMerma(calc.waste())
                .gramosTotales(calc.totalGrams())
                .horasImpresion(request.horasImpresion())
                .piezasPorCama(request.piezasPorCama())
                .consumoKw(calc.kw())
                .costoKwh(calc.kwh())
                .costoMaterial(calc.materialCost())
                .costoElectricidad(calc.electricCost())
                .costoExtras(calc.extraCost())
                .costoCama(calc.bedCost())
                .costoPieza(calc.unitCost())
                .tipoPrecio(normalize(request.tipoPrecio()))
                .margen(calc.margin())
                .precioCama(calc.saleBed())
                .precioPieza(calc.saleUnit())
                .precioCamaFinal(calc.saleBed())
                .precioPiezaFinal(calc.saleUnit())
                .build();

        MakerProyecto project = request.proyectoId() == null ? null : proyectoRepository.findById(request.proyectoId())
                .orElseThrow(() -> new NotFoundException("Proyecto no encontrado"));
        quote.setProyecto(project);
        addQuoteLines(quote, request, calc);
        MakerCotizacion saved = cotizacionRepository.save(quote);
        if (project != null) {
            replaceProjectQuote(project, saved);
            proyectoRepository.save(project);
        }
        return response(saved);
    }

    @Transactional
    public MakerResponse updateQuote(Integer id, MakerCotizacionRequest request) {
        MakerCotizacion quote = cotizacionRepository.findById(id)
                .orElseThrow(() -> new NotFoundException("Cotización no encontrada"));
        ensureDefaults();
        Calc calc = calculate(request);
        quote.setDescripcion(blankToNull(request.descripcion()));
        quote.setModo(normalize(request.modo()));
        quote.setGramosNetos(scaleGrams(request.gramosNetos()));
        quote.setGramosMerma(calc.waste());
        quote.setGramosTotales(calc.totalGrams());
        quote.setHorasImpresion(request.horasImpresion());
        quote.setPiezasPorCama(request.piezasPorCama());
        quote.setConsumoKw(calc.kw());
        quote.setCostoKwh(calc.kwh());
        quote.setCostoMaterial(calc.materialCost());
        quote.setCostoElectricidad(calc.electricCost());
        quote.setCostoExtras(calc.extraCost());
        quote.setCostoCama(calc.bedCost());
        quote.setCostoPieza(calc.unitCost());
        quote.setTipoPrecio(normalize(request.tipoPrecio()));
        quote.setMargen(calc.margin());
        quote.setPrecioCama(calc.saleBed());
        quote.setPrecioPieza(calc.saleUnit());
        quote.setPrecioCamaFinal(calc.saleBed());
        quote.setPrecioPiezaFinal(calc.saleUnit());

        MakerProyecto previousProject = quote.getProyecto();
        MakerProyecto newProject = request.proyectoId() == null ? null : proyectoRepository.findById(request.proyectoId())
                .orElseThrow(() -> new NotFoundException("Proyecto no encontrado"));
        quote.setProyecto(newProject);
        if (previousProject != null && previousProject != newProject
                && previousProject.getCotizacion() == quote) {
            previousProject.setCotizacion(null);
            proyectoRepository.save(previousProject);
        }
        quote.getFilamentos().clear();
        quote.getExtras().clear();
        addQuoteLines(quote, request, calc);
        MakerCotizacion saved = cotizacionRepository.save(quote);
        if (newProject != null) {
            replaceProjectQuote(newProject, saved);
            proyectoRepository.save(newProject);
        }
        return response(saved);
    }

    @Transactional(readOnly = true)
    public Page<MakerResponse> quotes(Pageable pageable) {
        return cotizacionRepository.findAllByOrderByFechaDesc(pageable).map(this::response);
    }

    @Transactional(readOnly = true)
    public MakerResponse quote(Integer id) {
        return response(cotizacionRepository.findById(id)
                .orElseThrow(() -> new NotFoundException("Cotización no encontrada")));
    }

    @Transactional(readOnly = true)
    public byte[] quotePdf(Integer id) {
        MakerCotizacion quote = cotizacionRepository.findById(id)
                .orElseThrow(() -> new NotFoundException("Cotización no encontrada"));
        ByteArrayOutputStream output = new ByteArrayOutputStream();
        try (PdfDocument pdf = new PdfDocument(new PdfWriter(output));
             Document document = new Document(pdf)) {
            document.add(new Paragraph("COTIZACIÓN MAKER")
                    .setBold().setFontSize(20).setTextAlignment(TextAlignment.CENTER));
            document.add(new Paragraph(""));
            Table table = new Table(UnitValue.createPercentArray(new float[]{35, 65}))
                    .useAllAvailableWidth();
            pdfRow(table, "Folio", quote.getFolio());
            pdfRow(table, "Fecha", quote.getFecha() == null ? ""
                    : quote.getFecha().format(DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm")));
            pdfRow(table, "Descripción", quote.getDescripcion() == null ? "Impresión 3D" : quote.getDescripcion());
            pdfRow(table, "Gramos totales", String.valueOf(quote.getGramosTotales()));
            pdfRow(table, "Piezas por cama", String.valueOf(quote.getPiezasPorCama()));
            pdfRow(table, "Precio final por pieza", "$" + money(quote.getPrecioPiezaFinal(), quote.getPrecioPieza()));
            document.add(table);
            document.add(new Paragraph("Precio final comercial")
                    .setBold().setFontSize(16).setFontColor(ColorConstants.BLACK)
                    .setTextAlignment(TextAlignment.RIGHT));
            return output.toByteArray();
        } catch (Exception ex) {
            throw new IllegalStateException("No se pudo generar el PDF de la cotización", ex);
        }
    }

    @Transactional
    public void deleteQuote(Integer id) {
        MakerCotizacion quote = cotizacionRepository.findById(id)
                .orElseThrow(() -> new NotFoundException("Cotización no encontrada"));
        proyectoRepository.findByCotizacionId(id).forEach(project -> {
            project.setCotizacion(null);
            proyectoRepository.save(project);
        });
        cotizacionRepository.delete(quote);
    }

    @Transactional
    public MakerResponse updateFinalPrice(Integer id, MakerPrecioFinalRequest request) {
        MakerCotizacion quote = cotizacionRepository.findById(id)
                .orElseThrow(() -> new NotFoundException("Cotización no encontrada"));
        BigDecimal finalUnit = request.precioPiezaFinal().setScale(MONEY_SCALE, ROUNDING);
        quote.setPrecioPiezaFinal(finalUnit);
        quote.setPrecioCamaFinal(finalUnit.multiply(BigDecimal.valueOf(quote.getPiezasPorCama()))
                .setScale(MONEY_SCALE, ROUNDING));
        return response(cotizacionRepository.save(quote));
    }

    private void addQuoteLines(MakerCotizacion quote, MakerCotizacionRequest request, Calc calc) {
        for (MakerFilamentoInput input : request.filamentos()) {
            MakerFilamento filament = filamentoRepository.findById(input.filamentoId())
                    .orElseThrow(() -> new NotFoundException("Filamento no encontrado"));
            BigDecimal grams = scaleGrams(input.gramos());
            quote.getFilamentos().add(MakerCotizacionFilamento.builder()
                    .cotizacion(quote)
                    .filamento(filament)
                    .nombreFilamento(filament.getNombre())
                    .material(filament.getMaterial().getNombre())
                    .marca(filament.getMarca())
                    .color(filament.getColor())
                    .precioKilo(filament.getPrecioKilo())
                    .gramos(grams)
                    .costo(lineCost(filament.getPrecioKilo(), grams, request.gramosNetos(), calc.waste()))
                    .build());
        }
        if (request.extras() != null) {
            for (MakerExtraInput extra : request.extras()) {
                BigDecimal importe = extra.cantidad().multiply(extra.precioUnitario())
                        .setScale(MONEY_SCALE, ROUNDING);
                quote.getExtras().add(MakerCotizacionExtra.builder()
                        .cotizacion(quote)
                        .nombre(extra.nombre().trim())
                        .cantidad(extra.cantidad())
                        .precioUnitario(extra.precioUnitario())
                        .importe(importe)
                        .build());
            }
        }
    }

    private Calc calculate(MakerCotizacionRequest request) {
        String mode = normalize(request.modo());
        if (!Set.of("MONOCOLOR", "MULTICOLOR").contains(mode)) {
            throw new InvalidEntryException("El modo debe ser MONOCOLOR o MULTICOLOR");
        }
        String priceType = normalize(request.tipoPrecio());
        if (!Set.of("PUBLICO", "MAYOREO", "DISTRIBUIDOR").contains(priceType)) {
            throw new InvalidEntryException("El tipo de precio no es válido");
        }

        BigDecimal filamentGrams = request.filamentos().stream()
                .map(MakerFilamentoInput::gramos)
                .reduce(BigDecimal.ZERO, BigDecimal::add);
        if (mode.equals("MULTICOLOR")) {
            Set<Integer> ids = new HashSet<>();
            for (MakerFilamentoInput line : request.filamentos()) {
                if (!ids.add(line.filamentoId())) {
                    throw new InvalidEntryException("En modo multicolor no puedes repetir el mismo filamento");
                }
            }
        }
        if (filamentGrams.subtract(request.gramosNetos()).abs().compareTo(new BigDecimal("0.01")) > 0) {
            throw new InvalidEntryException("La suma de gramos de los filamentos debe coincidir con los gramos netos");
        }

        BigDecimal wasteRate = config(mode.equals("MULTICOLOR") ? "merma_multicolor" : "merma_monocolor");
        int changes = Optional.ofNullable(request.cambiosColor()).orElse(0);
        BigDecimal purge = mode.equals("MULTICOLOR")
                ? config("purga_por_cambio").multiply(BigDecimal.valueOf(Math.max(0, changes)))
                : BigDecimal.ZERO;
        BigDecimal waste = request.gramosNetos().multiply(wasteRate).add(purge).setScale(GRAMS_SCALE, ROUNDING);
        BigDecimal total = request.gramosNetos().add(waste).setScale(GRAMS_SCALE, ROUNDING);

        BigDecimal material = BigDecimal.ZERO;
        for (MakerFilamentoInput line : request.filamentos()) {
            MakerFilamento filament = filamentoRepository.findById(line.filamentoId())
                    .orElseThrow(() -> new NotFoundException("Filamento no encontrado"));
            material = material.add(lineCost(filament.getPrecioKilo(), line.gramos(), request.gramosNetos(), waste));
        }
        material = material.setScale(MONEY_SCALE, ROUNDING);
        BigDecimal kw = config("consumo_kw");
        BigDecimal kwh = config("costo_kwh");
        BigDecimal electricity = kw.multiply(request.horasImpresion()).multiply(kwh)
                .setScale(MONEY_SCALE, ROUNDING);
        BigDecimal extras = Optional.ofNullable(request.extras()).orElse(List.of()).stream()
                .map(extra -> extra.cantidad().multiply(extra.precioUnitario()))
                .reduce(BigDecimal.ZERO, BigDecimal::add)
                .setScale(MONEY_SCALE, ROUNDING);
        BigDecimal bed = material.add(electricity).add(extras).setScale(MONEY_SCALE, ROUNDING);
        BigDecimal unit = bed.divide(BigDecimal.valueOf(request.piezasPorCama()), MONEY_SCALE, ROUNDING);
        BigDecimal margin = config("margen_" + priceType.toLowerCase(Locale.ROOT));
        if (margin.compareTo(BigDecimal.ONE) >= 0) {
            throw new InvalidEntryException("El margen debe ser menor a 100%");
        }
        BigDecimal saleBed = bed.divide(BigDecimal.ONE.subtract(margin), MONEY_SCALE, ROUNDING);
        BigDecimal saleUnit = saleBed.divide(BigDecimal.valueOf(request.piezasPorCama()), MONEY_SCALE, ROUNDING);
        return new Calc(waste, total, kw, kwh, material, electricity, extras, bed, unit,
                margin, saleBed, saleUnit);
    }

    private BigDecimal lineCost(BigDecimal price, BigDecimal grams, BigDecimal net, BigDecimal waste) {
        BigDecimal lineWaste = net.signum() == 0
                ? BigDecimal.ZERO
                : waste.multiply(grams).divide(net, 6, ROUNDING);
        return grams.add(lineWaste).multiply(price)
                .divide(BigDecimal.valueOf(1000), MONEY_SCALE, ROUNDING);
    }

    private BigDecimal config(String suffix) {
        return configRepository.findByClave(CONFIG_PREFIX + suffix.toLowerCase(Locale.ROOT))
                .map(Configuracion::getValor)
                .map(this::decimal)
                .orElse(BigDecimal.ZERO);
    }

    private MakerConfiguracionResponse configurationResponse(Configuracion config) {
        ConfigDefault metadata = DEFAULTS.stream()
                .filter(item -> (CONFIG_PREFIX + item.sufijo()).equals(config.getClave()))
                .findFirst().orElse(null);
        return new MakerConfiguracionResponse(
                config.getClave(),
                decimal(config.getValor()),
                metadata == null ? null : metadata.unidad(),
                config.getDescripcion());
    }

    private String normalizeConfigKey(String key) {
        String normalized = key == null ? "" : key.trim().toLowerCase(Locale.ROOT);
        if (!normalized.startsWith(CONFIG_PREFIX)) {
            normalized = CONFIG_PREFIX + normalized;
        }
        return normalized;
    }

    private String nextFolio() {
        return "MK-" + LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyyMMddHHmmssSSS"))
                + "-" + UUID.randomUUID().toString().substring(0, 6).toUpperCase(Locale.ROOT);
    }

    private Persona findPersona(String username) {
        if (username == null || username.isBlank()) {
            return null;
        }
        return personaRepository.findByUsuarioIgnoreCase(username).orElse(null);
    }

    private void replaceProjectQuote(MakerProyecto project, MakerCotizacion quote) {
        MakerCotizacion previous = project.getCotizacion();
        if (previous != null && previous != quote) {
            previous.setProyecto(null);
            cotizacionRepository.save(previous);
        }
        project.setCotizacion(quote);
    }

    private MakerCatalogResponse filamentResponse(MakerFilamento filament) {
        return new MakerCatalogResponse(
                filament.getId(), filament.getNombre(), filament.getMaterial().getNombre(),
                filament.getMaterial().getId(), filament.getPrecioKilo(), filament.getStockKg(),
                filament.getMarca(), filament.getColor(), filament.getActivo());
    }

    private MakerResponse response(MakerCotizacion quote) {
        return new MakerResponse(
                quote.getId(), quote.getFolio(), quote.getFecha(),
                quote.getUsuario() == null ? null : quote.getUsuario().getUsuario(),
                quote.getProyecto() == null ? null : quote.getProyecto().getId(),
                quote.getDescripcion(), quote.getModo(), quote.getGramosNetos(), quote.getGramosMerma(),
                quote.getGramosTotales(), quote.getHorasImpresion(), quote.getPiezasPorCama(), quote.getConsumoKw(),
                quote.getCostoKwh(), quote.getCostoMaterial(), quote.getCostoElectricidad(), quote.getCostoExtras(),
                quote.getCostoCama(), quote.getCostoPieza(), quote.getTipoPrecio(), quote.getMargen(),
                quote.getPrecioCama(), quote.getPrecioPieza(),
                quote.getFilamentos().stream().map(line -> new MakerResponse.MakerFilamentoResponse(
                        line.getFilamento() == null ? null : line.getFilamento().getId(), line.getNombreFilamento(),
                        line.getMaterial(), line.getMarca(), line.getColor(), line.getGramos(),
                        line.getPrecioKilo(), line.getCosto())).toList(),
                quote.getExtras().stream().map(extra -> new MakerResponse.MakerExtraResponse(
                        extra.getNombre(), extra.getCantidad(), extra.getPrecioUnitario(), extra.getImporte())).toList(),
                quote.getPrecioCamaFinal(), quote.getPrecioPiezaFinal());
    }

    private String normalize(String value) {
        return value == null ? "" : value.trim().toUpperCase(Locale.ROOT).replace(' ', '_');
    }

    private String blankToNull(String value) {
        return value == null || value.isBlank() ? null : value.trim();
    }

    private BigDecimal scaleGrams(BigDecimal value) {
        return value.setScale(GRAMS_SCALE, ROUNDING);
    }

    private BigDecimal decimal(String value) {
        if (value == null || value.isBlank()) {
            return BigDecimal.ZERO;
        }
        try {
            return new BigDecimal(value.trim());
        } catch (NumberFormatException ex) {
            return BigDecimal.ZERO;
        }
    }

    private String money(BigDecimal value, BigDecimal fallback) {
        BigDecimal amount = value == null ? fallback : value;
        return amount == null ? "0.00" : amount.setScale(MONEY_SCALE, ROUNDING).toPlainString();
    }

    private void pdfRow(Table table, String label, String value) {
        table.addCell(new Paragraph(label).setBold());
        table.addCell(new Paragraph(value == null ? "" : value));
    }

    private record ConfigDefault(String sufijo, String valor, String unidad, String descripcion) {
    }

    private record Calc(BigDecimal waste, BigDecimal totalGrams, BigDecimal kw, BigDecimal kwh,
                        BigDecimal materialCost, BigDecimal electricCost, BigDecimal extraCost,
                        BigDecimal bedCost, BigDecimal unitCost, BigDecimal margin,
                        BigDecimal saleBed, BigDecimal saleUnit) {
    }
}
