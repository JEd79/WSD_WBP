package uk.gov.dvla.sifftconfig.controllers;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import uk.gov.dvla.sifftconfig.models.WriteConfig;
import uk.gov.dvla.sifftconfig.services.WriteConfigService;
import uk.gov.dvla.sifftconfig.exceptions.DuplicateConfigException;
import java.util.ArrayList;

@RestController
@RequestMapping("/api/v1/write-configs")
public class WriteConfigController {

    private WriteConfigService service;

    public WriteConfigController(WriteConfigService service) {
        this.service = service;
    }

    @GetMapping
    public ResponseEntity<ArrayList<WriteConfig>> getAllWriteConfigs() {

        ArrayList<WriteConfig> configs =
                new ArrayList<>(service.getWriteConfigs().values());

        return ResponseEntity.ok(configs);
    }

    @PostMapping
    public ResponseEntity<WriteConfig> createWriteConfig( @RequestBody WriteConfig config) {
        service.validateWriteConfig(config);

        if (service.targetTableExists(config.getTargetTable())) {
            throw new DuplicateConfigException(
                    "Target table already exists");
        }
        WriteConfig created = service.addWriteConfig(config);

        return ResponseEntity.status(HttpStatus.CREATED).body(created);
    }
}
