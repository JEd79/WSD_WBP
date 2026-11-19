
package uk.gov.dvla.sifftconfig.controllers;

import java.util.ArrayList;
import java.util.HashMap;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import uk.gov.dvla.sifftconfig.models.ReadConfig;
import uk.gov.dvla.sifftconfig.services.ReadConfigService;
import uk.gov.dvla.sifftconfig.exceptions.InvalidConfigException;
import uk.gov.dvla.sifftconfig.exceptions.ConfigNotFoundException;
import uk.gov.dvla.sifftconfig.exceptions.DuplicateConfigException;

@RestController
@RequestMapping("/api/v1/read-configs")
public class ReadConfigController {

    private ReadConfigService service;

    public ReadConfigController(ReadConfigService service) {
        this.service = service;
    }


    @GetMapping
    public ResponseEntity<ArrayList<ReadConfig>> getAllReadConfigs() {

        ArrayList<ReadConfig> configs =
                new ArrayList<>(service.getReadConfigs().values());

        return ResponseEntity.ok(configs);
    }


    @GetMapping("/{id}")
    public ResponseEntity<ReadConfig> getReadConfig(
            @PathVariable int id) {

        if (id <= 0) {
            throw new InvalidConfigException(
                    "Invalid read configuration ID");
        }

        ReadConfig config = service.getReadConfig(id);

        if (config == null) {
            throw new ConfigNotFoundException(
                    "Read configuration not found");
        }

        return ResponseEntity.ok(config);
    }


    @PostMapping
    public ResponseEntity<ReadConfig> createReadConfig(
            @RequestBody ReadConfig config) {

        service.validateReadConfig(config);

        if (service.patternExists(config.getPattern())) {
            throw new DuplicateConfigException(
                    "Filename pattern already exists");
        }

        ReadConfig created = service.addReadConfig(config);

        return ResponseEntity.status(HttpStatus.CREATED)
                .body(created);
    }

    @PostMapping("/{id}/test")
    public ResponseEntity<HashMap<String, Object>> testReadConfig(
            @PathVariable int id,
            @RequestParam(value = "sampleLine", defaultValue = "") String sampleLine) {

        if (id <= 0) {
            throw new InvalidConfigException(
                    "Invalid read configuration ID");
        }

        ReadConfig config = service.getReadConfig(id);

        if (config == null) {
            throw new ConfigNotFoundException(
                    "Read configuration not found");
        }

        if (sampleLine.isBlank()) {

            throw new InvalidConfigException(
                    "Sample line is required");
        }

        HashMap<String, Object> result = service.testReadConfig(
                config, sampleLine);

        return ResponseEntity.ok(result);
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<?> deleteReadConfig(@PathVariable int id, @RequestHeader(value = "Authorization",
                    defaultValue = "") String token) {

        if (!service.isAccessTokenValid(token)) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                    .build();
        }

        if (id <= 0) {
            throw new InvalidConfigException(
                    "Invalid read configuration ID");
        }

        boolean deleted = service.deleteReadConfig(id);
        if (!deleted) {
            throw new ConfigNotFoundException(
                    "Read configuration not found");
        }
        return ResponseEntity.noContent().build();
    }

    @PutMapping("/{id}")
    public ResponseEntity<ReadConfig> updateReadConfig(
            @PathVariable int id,
            @RequestBody ReadConfig config) {

        if (id <= 0) {
            throw new InvalidConfigException(
                    "Invalid read configuration ID");
        }

        if (service.getReadConfig(id) == null) {
            throw new ConfigNotFoundException(
                    "Read configuration not found");
        }

        service.validateReadConfig(config);

        if (service.patternExistsForOtherConfig(
                config.getPattern(), id)) {

            throw new DuplicateConfigException(
                    "Filename pattern already exists");
        }

        ReadConfig updated =
                service.updateReadConfig(id, config);

        return ResponseEntity.ok(updated);
    }

}
