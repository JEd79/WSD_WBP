package uk.gov.dvla.sifftconfig.models;

import java.util.ArrayList;

public class ReadConfig {
    private int id;
    private String pattern;
    private String delimiter;
    private String header;
    private ArrayList<String> columns;
    private int writeConfigId;

    public ReadConfig() {
    }

    public int getId() {
        return id;
    }

    public void setId(int id) {
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

    public ArrayList<String> getColumns() {
        return columns;
    }

    public void setColumns(ArrayList<String> columns) {
        this.columns = columns;
    }

    public int getWriteConfigId() {
        return writeConfigId;
    }

    public void setWriteConfigId(int writeConfigId) {
        this.writeConfigId = writeConfigId;
    }
}
