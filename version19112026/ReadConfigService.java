
package uk.gov.dvla.sifftconfig.services;

import org.springframework.stereotype.Service;
import uk.gov.dvla.sifftconfig.models.ReadConfig;
import uk.gov.dvla.sifftconfig.exceptions.InvalidConfigException;
import java.util.HashMap;

@Service
public class ReadConfigService {

    private HashMap<Integer, ReadConfig> readConfigs;
    private WriteConfigService writeConfigService;
    private int nextId = 1;
    private static final String DELETE_TOKEN = "sifftdelete!";

    public ReadConfigService(ConfigLoaderService loader,
                             WriteConfigService writeConfigService) {

        this.writeConfigService = writeConfigService;
        readConfigs = loader.loadReadConfigs();

        for (Integer id : readConfigs.keySet()) {
            if (id >= nextId) {
                nextId = id + 1;
            }
        }

        System.out.println("Loaded " + readConfigs.size()
                + " read configurations");
    }

    public HashMap<Integer, ReadConfig> getReadConfigs() {
        return readConfigs;
    }

    public ReadConfig getReadConfig(int id) {
        return readConfigs.get(id);
    }

    public void validateReadConfig(ReadConfig config) {

        if (config == null) {
            throw new InvalidConfigException(
                    "Read configuration is required");
        }

        if (config.getPattern() == null ||
                config.getPattern().isBlank()) {
            throw new InvalidConfigException(
                    "Filename pattern is required");
        }

        if (!",".equals(config.getDelimiter()) &&
                !"|".equals(config.getDelimiter())) {
            throw new InvalidConfigException(
                    "Unsupported delimiter");
        }

        if (!"auto".equals(config.getHeader()) &&
                !"true".equals(config.getHeader()) &&
                !"false".equals(config.getHeader())) {
            throw new InvalidConfigException(
                    "Unsupported header setting");
        }

        if (config.getColumns() == null ||
                config.getColumns().isEmpty()) {
            throw new InvalidConfigException(
                    "At least one column is required");
        }

        for (String column : config.getColumns()) {
            if (column == null || column.isBlank()) {
                throw new InvalidConfigException(
                        "Column names cannot be empty");
            }
        }

        if (writeConfigService.getWriteConfig(
                config.getWriteConfigId()) == null) {
            throw new InvalidConfigException(
                    "Referenced write configuration does not exist");
        }
    }

    public boolean patternExists(String pattern) {

        for (ReadConfig config : readConfigs.values()) {
            if (config.getPattern().equalsIgnoreCase(pattern)) {
                return true;
            }
        }

        return false;
    }

    public ReadConfig addReadConfig(ReadConfig config) {

        config.setId(nextId);
        nextId++;

        readConfigs.put(config.getId(), config);

        return config;
    }


    public HashMap<String, Object> testReadConfig(
            ReadConfig config, String sampleLine) {

        String[] fields;

        if ("|".equals(config.getDelimiter())) {
            fields = sampleLine.split("\\|", -1);
        } else {
            fields = sampleLine.split(",", -1);
        }

        int expected = config.getColumns().size();
        int detected = fields.length;

        boolean matches = expected == detected;

        HashMap<String, Object> result = new HashMap<>();

        result.put("readConfigId", config.getId());
        result.put("expectedFieldCount", expected);
        result.put("detectedFieldCount", detected);
        result.put("matches", matches);

        return result;
    }


    public boolean deleteReadConfig(int id) {
        if (readConfigs.containsKey(id)) {
            readConfigs.remove(id);
            return true;
        }
        return false;
    }


    public boolean patternExistsForOtherConfig(String pattern, int id) {

        for (ReadConfig config : readConfigs.values()) {
            if (config.getId() != id &&
                    config.getPattern().equalsIgnoreCase(pattern)) {
                return true;
            }
        }
        return false;
    }


    public ReadConfig updateReadConfig(int id, ReadConfig config) {
        config.setId(id);
        readConfigs.put(id, config);
        return config;
    }



    public boolean isAccessTokenValid(String token) {
        return DELETE_TOKEN.equals(token);
    }

}
