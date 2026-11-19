package uk.gov.dvla.sifftconfig.models;

public class WriteConfig {

    private int id;
    private String targetTable;
    private String format;
    private String mode;
    private String schemaMode;

    public WriteConfig() {
    }

    public int getId() {
        return id;
    }

    public void setId(int id) {
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
}
