
package uk.gov.dvla.sifftconfig.controllers;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.HashMap;

@RestController
@RequestMapping("/api/v1")
public class ConfigOptionsController {

    @GetMapping("/config-options")
    public HashMap<String, String[]> getConfigOptions() {

        HashMap<String, String[]> options = new HashMap<>();

        options.put("delimiters", new String[]{",", "|"});
        options.put("headers", new String[]{"auto", "true", "false"});
        options.put("formats", new String[]{"delta"});
        options.put("modes", new String[]{"overwrite", "append"});
        options.put("schemaModes", new String[]{"overwrite", "merge"});

        return options;
    }
}
