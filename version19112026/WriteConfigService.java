
package uk.gov.dvla.sifftconfig.services;

import org.springframework.stereotype.Service;
import uk.gov.dvla.sifftconfig.models.WriteConfig;
import uk.gov.dvla.sifftconfig.exceptions.InvalidConfigException;
import java.util.HashMap;

@Service
public class WriteConfigService {

    private HashMap<Integer, WriteConfig> writeConfigs;
    private int nextId = 1;

    public WriteConfigService(ConfigLoaderService loader) {

        writeConfigs = loader.loadWriteConfigs();

        for (Integer id : writeConfigs.keySet()) {
            if (id >= nextId) {
                nextId = id + 1;
            }
        }

        System.out.println("Loaded " + writeConfigs.size()
                + " write configurations");
    }

    public HashMap<Integer, WriteConfig> getWriteConfigs() {
        return writeConfigs;
    }

    public WriteConfig getWriteConfig(int id) {
        return writeConfigs.get(id);
    }

    public void validateWriteConfig(WriteConfig config) {

        if (config == null) {
            throw new InvalidConfigException(
                    "Write configuration is required");
        }

        if (config.getTargetTable() == null ||
                config.getTargetTable().isBlank()) {
            throw new InvalidConfigException(
                    "Target table is required");
        }

        if (!"delta".equals(config.getFormat())) {
            throw new InvalidConfigException(
                    "Unsupported format");
        }

        if (!"overwrite".equals(config.getMode()) &&
                !"append".equals(config.getMode())) {
            throw new InvalidConfigException(
                    "Unsupported write mode");
        }

        if (!"overwrite".equals(config.getSchemaMode()) &&
                !"merge".equals(config.getSchemaMode())) {
            throw new InvalidConfigException(
                    "Unsupported schema mode");
        }
    }

    public boolean targetTableExists(String targetTable) {

        for (WriteConfig config : writeConfigs.values()) {

            if (config.getTargetTable()
                    .equalsIgnoreCase(targetTable)) {
                return true;
            }
        }

        return false;
    }

    public WriteConfig addWriteConfig(WriteConfig config) {

        config.setId(nextId);
        nextId++;

        writeConfigs.put(config.getId(), config);

        return config;
    }
}
