package uk.gov.dvla.siftconfig.controller;

import java.net.URI;
import java.util.List;

import jakarta.validation.Valid;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import uk.gov.dvla.siftconfig.dto.ConfigTestRequest;
import uk.gov.dvla.siftconfig.dto.ConfigTestResponse;
import uk.gov.dvla.siftconfig.dto.ReadConfigRequest;
import uk.gov.dvla.siftconfig.dto.ReadConfigResponse;
import uk.gov.dvla.siftconfig.model.ReadConfig;
import uk.gov.dvla.siftconfig.service.ReadConfigService;

/**
 * REST resource for read configs.
 * Endpoints: R1 (GET list), R2 (GET one), R3 (POST), R4 (PUT), R5 (DELETE), R6 (POST test).
 */
@RestController
@RequestMapping("/api/v1/read-configs")
public class ReadConfigController {

    private final ReadConfigService service;

    public ReadConfigController(ReadConfigService service) {
        this.service = service;
    }

    // R1 — list all, optional ?pattern= filter
    @GetMapping
    public List<ReadConfigResponse> list(@RequestParam(required = false) String pattern) {
        return service.findAll(pattern).stream().map(ReadConfigResponse::fromEntity).toList();
    }

    // R2 — fetch one (200 / 404)
    @GetMapping("/{id}")
    public ReadConfigResponse getOne(@PathVariable String id) {
        return ReadConfigResponse.fromEntity(service.findById(id));
    }

    // R3 — create (201 / 400 / 409)
    @PostMapping
    public ResponseEntity<ReadConfigResponse> create(@Valid @RequestBody ReadConfigRequest request) {
        ReadConfig created = service.create(request);
        return ResponseEntity
                .created(URI.create("/api/v1/read-configs/" + created.getId()))
                .body(ReadConfigResponse.fromEntity(created));
    }

    // R4 — replace (200 / 400 / 404)
    @PutMapping("/{id}")
    public ReadConfigResponse replace(@PathVariable String id, @Valid @RequestBody ReadConfigRequest request) {
        return ReadConfigResponse.fromEntity(service.replace(id, request));
    }

    // R5 — delete (204 / 404)
    @DeleteMapping("/{id}")
    public ResponseEntity<Void> delete(@PathVariable String id) {
        service.delete(id);
        return ResponseEntity.noContent().build();
    }

    // R6 — test config against a sample line (200 / 400 / 404), server-side processing
    @PostMapping("/{id}/test")
    public ConfigTestResponse test(@PathVariable String id, @Valid @RequestBody ConfigTestRequest request) {
        return service.test(id, request.sampleLine());
    }
}
