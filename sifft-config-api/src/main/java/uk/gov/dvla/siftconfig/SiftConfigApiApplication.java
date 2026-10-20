package uk.gov.dvla.siftconfig;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

/**
 * Entry point for the SIFFT File Ingestion Configuration API.
 *
 * <p>This service externalises the ingestion configuration that currently lives in code
 * within the SIFFT Databricks job (FILE_MAPPINGS and TableWriteOptions), exposing it as
 * managed REST resources: read-configs, write-configs and config-options.</p>
 */
@SpringBootApplication
public class SiftConfigApiApplication {

    public static void main(String[] args) {
        SpringApplication.run(SiftConfigApiApplication.class, args);
    }
}
