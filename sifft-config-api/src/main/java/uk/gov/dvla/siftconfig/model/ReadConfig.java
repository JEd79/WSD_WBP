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
import jakarta.persistence.OrderColumn;
import jakarta.persistence.Table;

/**
 * Persistent representation of a read config — how an inbound file is parsed.
 * Mirrors SIFFT's per-file-type read options (delimiter, header, columns).
 */
@Entity
@Table(name = "read_config")
public class ReadConfig {

    @Id
    @Column(length = 36)
    private String id;

    @Column(nullable = false, unique = true)
    private String pattern;

    @Column(nullable = false, length = 8)
    private String delimiter;

    @Column(nullable = false, length = 8)
    private String header;

    @ElementCollection(fetch = FetchType.EAGER)
    @CollectionTable(name = "read_config_columns", joinColumns = @JoinColumn(name = "read_config_id"))
    @OrderColumn(name = "col_order")
    @Column(name = "column_name")
    private List<String> columns = new ArrayList<>();

    @Column(nullable = false)
    private double maxCorruptPercent;

    @Column(length = 36)
    private String writeConfigId;

    public ReadConfig() {
    }

    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }

    public String getPattern() {
        return pattern;
    }

    public void setPattern(String pattern) {
        this.pattern = pattern;
    }

    public String getDelimiter() {
        return delimiter;
    }

    public void setDelimiter(String delimiter) {
        this.delimiter = delimiter;
    }

    public String getHeader() {
        return header;
    }

    public void setHeader(String header) {
        this.header = header;
    }

    public List<String> getColumns() {
        return columns;
    }

    public void setColumns(List<String> columns) {
        this.columns = columns;
    }

    public double getMaxCorruptPercent() {
        return maxCorruptPercent;
    }

    public void setMaxCorruptPercent(double maxCorruptPercent) {
        this.maxCorruptPercent = maxCorruptPercent;
    }

    public String getWriteConfigId() {
        return writeConfigId;
    }

    public void setWriteConfigId(String writeConfigId) {
        this.writeConfigId = writeConfigId;
    }
}
