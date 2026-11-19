package uk.gov.dvla.sifftconfig.services;

import org.springframework.stereotype.Service;
import uk.gov.dvla.sifftconfig.models.ReadConfig;
import java.util.ArrayList;
import java.util.HashMap;

@Service
public class ConfigImpactService {

    private final ReadConfigService readConfigService;

    public ConfigImpactService(ReadConfigService readConfigService) {
        this.readConfigService = readConfigService;
    }

    public HashMap<String, Object> getConfigImpact(int writeConfigId) {
        ArrayList<String> patterns = new ArrayList<>();
        for (ReadConfig config :
                readConfigService.getReadConfigs().values()) {
            if (config.getWriteConfigId() == writeConfigId) {
                patterns.add(config.getPattern());
            }
        }
        int count = patterns.size();

        HashMap<String, Object> result = new HashMap<>();
        result.put("writeConfigId", writeConfigId);
        result.put("dependentReadConfigCount", count);
        result.put("readConfigPatterns", patterns);

        return result;
    }
}
