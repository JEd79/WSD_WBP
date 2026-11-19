package uk.gov.dvla.sifftconfig.services;

import org.springframework.stereotype.Service;
import tools.jackson.databind.ObjectMapper;
import uk.gov.dvla.sifftconfig.models.ReadConfig;
import uk.gov.dvla.sifftconfig.models.WriteConfig;
import java.util.HashMap;

@Service
public class ConfigLoaderService {

    public HashMap<Integer, ReadConfig> loadReadConfigs() {
        ObjectMapper mapper = new ObjectMapper();

        ReadConfig[] configs = mapper.readValue(
                ConfigLoaderService.class.getClassLoader()
                        .getResourceAsStream("data/read-configs.json"),
                ReadConfig[].class
        );

        HashMap<Integer, ReadConfig> readConfigs = new HashMap<>();
        for (ReadConfig config : configs) {
            readConfigs.put(config.getId(), config);
        }
        return readConfigs;
    }

    public HashMap<Integer, WriteConfig> loadWriteConfigs() {
        ObjectMapper mapper = new ObjectMapper();

        WriteConfig[] configs = mapper.readValue(
                ConfigLoaderService.class.getClassLoader()
                        .getResourceAsStream("data/write-configs.json"),
                WriteConfig[].class
        );

        HashMap<Integer, WriteConfig> writeConfigs = new HashMap<>();

        for (WriteConfig config : configs) {
            writeConfigs.put(config.getId(), config);
        }

        return writeConfigs;
    }
}
