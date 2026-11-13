# SIFFT Config Web Service — End-to-End Proof of Concept

## 1. Purpose and scope

This proof of concept (PoC) demonstrates that the bespoke **SIFFT configuration web
service** can drive the installed **SIFFT library** to ingest data files into Delta
tables, without Databricks or any cloud service. It is a deliberately **reduced-
functionality** PoC: it proves the integration pattern end-to-end rather than every
SIFFT feature.

In scope:
- Retrieving the ingestion configuration from the running web service over HTTP
  (`GET /api/v1/read-configs` and `GET /api/v1/write-configs`), using the service's
  `ReadConfig` and `WriteConfig` data model.
- Using SIFFT to read delimited files (both comma- and pipe-delimited).
- Applying column names from configuration.
- Using SIFFT to write the results to Delta tables on the local filesystem,
  with automatic source-lineage metadata.
- Exercising the service's full sample configuration: three read configurations
  (`Customers\.csv`, `Customers_Archive\.csv`, `Vehicles\.csv`) mapped to two
  write configurations (`demo_customers`, `demo_vehicles`).

Explicitly out of scope (noted as future work in section 6):
- CSVW schema/constraint validation.
- File tracking / de-duplication.
- Personal-data redaction.
- Source acquisition (e.g. SFTP pull) and scheduling.

## 2. How the two components fit together

The web service owns the **configuration** — what to read, how to parse it, and where
to write it. The SIFFT library performs the **ingestion**. A thin Python driver script
acts as the glue between them, retrieving the configuration from the running service
over HTTP:

    Config web service (REST, :8080)          SIFFT library (local Spark)
    --------------------------------          ----------------------------
    GET /api/v1/read-configs    ─┐
    GET /api/v1/write-configs   ─┘──▶ driver ──▶ process_file(...)        (read + parse)
                                               ──▶ df.toDF(*columns)       (apply columns)
                                               ──▶ write_table(...)        (write to Delta)
                                               ──▶ Delta tables on disk

The driver reproduces, at reduced scope, the same sequence used by SIFFT's production
`ingest_file()` function: match a filename against the configured regex pattern, read
the file with the configured delimiter/header, apply the configured column names, then
write to the configured target table with the configured write options. The driver
iterates over every read configuration returned by the service.

The sample configuration served by the web service, and exercised by this PoC:

| Read config pattern      | Delimiter | Columns                                   | Write config → table            | Mode      |
|--------------------------|-----------|-------------------------------------------|---------------------------------|-----------|
| `Customers\.csv`         | `,`       | customer_id, name, status                 | 123 → `demo_customers`          | overwrite |
| `Customers_Archive\.csv` | `,`       | customer_id, name, status                 | 123 → `demo_customers`          | overwrite |
| `Vehicles\.csv`          | `\|`      | vehicle_id, registration, vehicle_type    | 124 → `demo_vehicles`           | append    |

(The `\\.` seen in the service's JSON is JSON escaping for a single backslash; the
effective regex pattern is `\.`, i.e. a literal dot.)

**Expected behaviour note (not a defect):** both `Customers\.csv` and
`Customers_Archive\.csv` map to the same table `demo_customers` with mode `overwrite`.
Because the driver processes files sequentially, with `overwrite` the last file
processed replaces the table contents — so `demo_customers` ends with the rows of
whichever customers file was processed last, not the union of both. This is a direct
consequence of the sample configuration (two patterns → one table, overwrite) and
correctly demonstrates overwrite semantics. Using `append` on that write config would
instead accumulate both files.

## 3. Configuration-to-SIFFT field mapping (verified)

The following mapping was verified against the installed package **sifft 0.9.3** by
inspecting the real function signatures:

| Web service config field      | SIFFT usage                                                |
|-------------------------------|------------------------------------------------------------|
| `ReadConfig.pattern`          | Regex match to select the input file (as `get_file_mapping`)|
| `ReadConfig.delimiter`        | `process_file(..., read_options={"delimiter": ...})`       |
| `ReadConfig.header`           | `process_file(..., read_options={"header": ...})`          |
| `ReadConfig.columns`          | `df = result.dataframe.toDF(*columns)`                     |
| `WriteConfig.targetTable`     | `write_table(df, table_name, ...)`                         |
| `WriteConfig.format`          | `TableWriteOptions(format=...)`  (PoC: `delta`)            |
| `WriteConfig.mode`            | `TableWriteOptions(mode=...)`    (PoC: `overwrite`/`append`)|
| `WriteConfig.schemaMode`      | `TableWriteOptions(schema_mode=...)` (PoC: `overwrite`/`merge`)|

The reduced value sets accepted by the web service (`delta` only; modes
`overwrite`/`append`; schema modes `overwrite`/`merge`) are all valid subsets of
SIFFT's supported options, so the reduced configuration is fully compatible with the
library.

## 4. Important implementation note — there is no `import sifft`

A key correction established during this work: the PyPI package `sifft` does **not**
expose a module called `sifft`. Installing `sifft[pyspark3]` places four modules at the
top level of the environment:

    file_processing      table_writing      dataframe_validation      file_management

The driver therefore imports directly from these, matching the SIFFT workshop notebook:

    from file_processing import process_file
    from table_writing import write_table, TableWriteOptions

Verified signatures used by the PoC:

    process_file(file_path, spark, read_options=None, metadata_path=None,
                 raise_on_error=False, tracking=None, tracking_location=None,
                 storage_options=None) -> FileProcessingResult

    write_table(df, table_name, spark, options=None, source_file_path=None,
                file_size=None, raise_on_error=False) -> TableWriteResult

    TableWriteOptions(format='delta', mode='append', partition_by=None,
                      schema_mode='strict', merge_keys=None,
                      create_if_not_exists=True, add_source_metadata=False,
                      source_metadata_columns=None, extra_metadata=None)

## 5. Execution environment and result

The PoC runs on local Apache Spark 3.5.x with delta-spark 3.3.x (pulled in automatically
by `sifft[pyspark3]`). The Spring Boot config web service runs locally on port 8080 and
is started first (`mvnw spring-boot:run`); the driver then fetches configuration from it
over HTTP before driving SIFFT. On Windows, local Spark additionally requires the Hadoop
`winutils.exe` / `hadoop.dll` native helper (`HADOOP_HOME`); on Linux/WSL this is not
needed. To avoid installing third-party binaries on the corporate network device, the
end-to-end run was performed on a separate stand-alone machine.

**Expected / observed successful output:**

    Fetched 3 read config(s) and 2 write config(s) from http://localhost:8080
    PROCESS Customers.csv -> table demo_customers success= True rows= 3
    WRITE demo_customers mode= overwrite success= True rows_written= 3
    PROCESS Customers_Archive.csv -> table demo_customers success= True rows= 2
    WRITE demo_customers mode= overwrite success= True rows_written= 2
    PROCESS Vehicles.csv -> table demo_vehicles success= True rows= 3
    WRITE demo_vehicles mode= append success= True rows_written= 3

    --- READBACK (final table contents) ---
    TABLE demo_customers: count= 2   (overwrite: last customers file wins; see note in §2)
    TABLE demo_vehicles:  count= 3
    (table rows shown, including dp_metadata_* source-lineage columns)
    E2E_PoC_DONE

Two Delta tables are produced — at `output/demo_customers/` and `output/demo_vehicles/`
— each containing a `_delta_log/` directory with the commit log plus parquet data files,
confirming genuine Delta writes driven entirely by the web service configuration.

> **Evidence — insert screenshots here:**
> - [ ] Figure 1: Postman showing `GET /api/v1/read-configs` (3 items) and
>       `GET /api/v1/write-configs` (2 items) — the configuration the driver consumes.
> - [ ] Figure 2: Console output of `driver.py` showing PROCESS / WRITE for all three
>       files and the READBACK of both tables.
> - [ ] Figure 3: Directory listings of `output/demo_customers/` and
>       `output/demo_vehicles/`, each including `_delta_log/`.

## 6. Reduced-functionality scope and future work

This PoC proves the **configuration-driven ingestion pattern** end-to-end but
intentionally omits parts of SIFFT's full capability. The most significant gap is that
SIFFT's richer, authoritative configuration artifact is the **CSVW metadata file**
(per-column data types and constraints such as required/min/max/enum/pattern/unique,
plus a primary key and dialect). The current web service models the file-to-table
*mapping* configuration (pattern → target table → columns → write options) rather than
CSVW validation metadata.

Extending the web service to manage CSVW metadata would require a new column/constraint
data model and corresponding validation surface — a substantial addition beyond the
agreed PoC. It is recommended as the primary candidate for future work, alongside file
tracking/de-duplication, redaction, and source acquisition (SFTP) if a fuller solution
is pursued.

## 7. Summary

The PoC demonstrates a complete, if limited, flow: configuration is served by the
bespoke web service over REST, fetched by a driver over HTTP, and used to drive the
installed SIFFT library to ingest three files into two Delta tables on local Spark. The
web service and SIFFT integrate cleanly with no changes required to the web service,
confirming the design is viable at the agreed reduced scope.
