package com.bcsystems.siga.controller;

import com.bcsystems.siga.dto.PaisResponse;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import org.springframework.beans.factory.annotation.Value;

@RestController
@RequestMapping("/api/v1/catalogos")
public class CatalogosController {

    @Value("${app.google.maps.api-key:}")
    private String googleMapsApiKey;

    @GetMapping("/paises")
    public ResponseEntity<List<PaisResponse>> paises() {
        return ResponseEntity.ok(List.of(
                new PaisResponse("MX", "México", "+52"),
                new PaisResponse("US", "Estados Unidos", "+1"),
                new PaisResponse("GT", "Guatemala", "+502"),
                new PaisResponse("HN", "Honduras", "+504"),
                new PaisResponse("SV", "El Salvador", "+503"),
                new PaisResponse("NI", "Nicaragua", "+505"),
                new PaisResponse("CR", "Costa Rica", "+506"),
                new PaisResponse("PA", "Panamá", "+507"),
                new PaisResponse("CO", "Colombia", "+57"),
                new PaisResponse("ES", "España", "+34")
        ));
    }

    @GetMapping("/google-maps-key")
    public ResponseEntity<?> googleMapsKey() {
        return ResponseEntity.ok(java.util.Map.of("apiKey", googleMapsApiKey == null ? "" : googleMapsApiKey));
    }
}
