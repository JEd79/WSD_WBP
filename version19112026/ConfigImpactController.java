package uk.gov.dvla.sifftconfig.controllers;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import uk.gov.dvla.sifftconfig.services.ConfigImpactService;
import uk.gov.dvla.sifftconfig.services.WriteConfigService;
import uk.gov.dvla.sifftconfig.exceptions.InvalidConfigException;
import uk.gov.dvla.sifftconfig.exceptions.ConfigNotFoundException;
import java.util.HashMap;

@RestController
@RequestMapping("/api/v1/config-impact")
public class ConfigImpactController {

    private ConfigImpactService impactService;
    private WriteConfigService writeConfigService;

    public ConfigImpactController(
            ConfigImpactService impactService,
            WriteConfigService writeConfigService) {

        this.impactService = impactService;
        this.writeConfigService = writeConfigService;
    }

    @GetMapping
    public ResponseEntity<HashMap<String, Object>> getConfigImpact(
            @RequestParam int writeConfigId) {

        if (writeConfigId <= 0) {
            throw new InvalidConfigException(
                    "Invalid write configuration ID");
        }

        if (writeConfigService.getWriteConfig(writeConfigId) == null) {
            throw new ConfigNotFoundException(
                    "Write configuration not found");
        }

        HashMap<String, Object> result =
                impactService.getConfigImpact(writeConfigId);

        return ResponseEntity.ok(result);
    }
}
