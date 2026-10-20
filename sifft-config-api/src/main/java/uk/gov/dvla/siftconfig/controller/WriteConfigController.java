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
import org.springframework.web.bind.annotation.RestController;

import uk.gov.dvla.siftconfig.dto.WriteConfigRequest;
import uk.gov.dvla.siftconfig.dto.WriteConfigResponse;
import uk.gov.dvla.siftconfig.model.WriteConfig;
import uk.gov.dvla.siftconfig.service.WriteConfigService;

/**
 * REST resource for write configs.
 * Endpoints: W1 (GET list), W2 (GET one), W3 (POST), W4 (PUT), W5 (DELETE).
 */
@RestController
@RequestMapping("/api/v1/write-configs")
public class WriteConfigController {

    private final WriteConfigService service;

    public WriteConfigController(WriteConfigService service) {
        this.service = service;
    }

    // W1 — list all
    @GetMapping
    public List<WriteConfigResponse> list() {
        return service.findAll().stream().map(WriteConfigResponse::fromEntity).toList();
    }

    // W2 — fetch one (200 / 404)
    @GetMapping("/{id}")
    public WriteConfigResponse getOne(@PathVariable String id) {
        return WriteConfigResponse.fromEntity(service.findById(id));
    }

    // W3 — create (201 / 400 / 422)
    @PostMapping
    public ResponseEntity<WriteConfigResponse> create(@Valid @RequestBody WriteConfigRequest request) {
        WriteConfig created = service.create(request);
        return ResponseEntity
                .created(URI.create("/api/v1/write-configs/" + created.getId()))
                .body(WriteConfigResponse.fromEntity(created));
    }

    // W4 — replace (200 / 400 / 404 / 422)
    @PutMapping("/{id}")
    public WriteConfigResponse replace(@PathVariable String id, @Valid @RequestBody WriteConfigRequest request) {
        return WriteConfigResponse.fromEntity(service.replace(id, request));
    }

    // W5 — delete (204 / 404)
    @DeleteMapping("/{id}")
    public ResponseEntity<Void> delete(@PathVariable String id) {
        service.delete(id);
        return ResponseEntity.noContent().build();
    }
}
