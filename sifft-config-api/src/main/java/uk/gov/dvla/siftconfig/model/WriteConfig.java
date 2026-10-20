package uk.gov.dvla.siftconfig.model;

import java.util.ArrayList;
import java.util.List;

import jakarta.persistence.CollectionTable;
import jakarta.persistence.Column;
import jakarta.persistence.ElementCollection;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.Table;

/**
 * Persistent representation of a write config — how the resulting table is written.
 * Mirrors SIFFT's {@code TableWriteOptions}.
 */
@Entity
@Table(name = "write_config")
public class WriteConfig {

    @Id
    @Column(length = 36)
    private String id;

    @Column(nullable = false, length = 255)
    private String targetTable;

    @Column(nullable = false, length = 16)
    private String format;

    @Column(nullable = false, length = 16)
    private String mode;

    @Column(nullable = false, length = 16)
    private String schemaMode;

    @ElementCollection(fetch = FetchType.EAGER)
    @CollectionTable(name = "write_config_partition_by", joinColumns = @JoinColumn(name = "write_config_id"))
    @Column(name = "partition_column")
    private List<String> partitionBy = new ArrayList<>();

    @ElementCollection(fetch = FetchType.EAGER)
    @CollectionTable(name = "write_config_merge_keys", joinColumns = @JoinColumn(name = "write_config_id"))
    @Column(name = "merge_key")
    private List<String> mergeKeys = new ArrayList<>();

    @Column(nullable = false)
    private boolean addSourceMetadata;

    public WriteConfig() {
    }

    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }

    public String getTargetTable() {
        return targetTable;
    }

    public void setTargetTable(String targetTable) {
        this.targetTable = targetTable;
    }

    public String getFormat() {
        return format;
    }

    public void setFormat(String format) {
        this.format = format;
    }

    public String getMode() {
        return mode;
    }

    public void setMode(String mode) {
        this.mode = mode;
    }

    public String getSchemaMode() {
        return schemaMode;
    }

    public void setSchemaMode(String schemaMode) {
        this.schemaMode = schemaMode;
    }

    public List<String> getPartitionBy() {
        return partitionBy;
    }

    public void setPartitionBy(List<String> partitionBy) {
        this.partitionBy = partitionBy;
    }

    public List<String> getMergeKeys() {
        return mergeKeys;
    }

    public void setMergeKeys(List<String> mergeKeys) {
        this.mergeKeys = mergeKeys;
    }

    public boolean isAddSourceMetadata() {
        return addSourceMetadata;
    }

    public void setAddSourceMetadata(boolean addSourceMetadata) {
        this.addSourceMetadata = addSourceMetadata;
    }
}
