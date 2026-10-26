# Databricks notebook source
# MAGIC %md
# MAGIC # SIFFT — Spark Ingestion Framework For Tables
# MAGIC
# MAGIC This notebook demonstrates how the Data Platform ingests files reliably using SIFFT.
# MAGIC
# MAGIC **What you'll see:**
# MAGIC 1. Reading files — any format, zero config
# MAGIC 2. Validating data — catch bad data before it lands
# MAGIC 3. Writing to Delta — with automatic lineage
# MAGIC 4. Tracking — no duplicates, safe to re-run
# MAGIC
# MAGIC ---

# COMMAND ----------

# MAGIC %md
# MAGIC ## Setup
# MAGIC
# MAGIC Create a temporary schema for this demo. Everything here gets cleaned up at the end.

# COMMAND ----------

# Setup — create a temporary schema for the demo (follows development naming convention)
CATALOG = "<input-dev-catalog>"

current_user = spark.sql("SELECT current_user()").collect()[0][0]
user_prefix = current_user.split("@")[0].replace(".", "_")
SCHEMA = f"dev_{user_prefix}_workshop_sifft"
VOLUME_PATH = f"/Volumes/{CATALOG}/{SCHEMA}/landing"

spark.sql(f"CREATE SCHEMA IF NOT EXISTS `{CATALOG}`.`{SCHEMA}`")
spark.sql(f"CREATE VOLUME IF NOT EXISTS `{CATALOG}`.`{SCHEMA}`.landing")

print(f"User:   {current_user}")
print(f"Schema: {CATALOG}.{SCHEMA}")
print(f"Volume: {VOLUME_PATH}")

# COMMAND ----------

# MAGIC %md
# MAGIC ### Create sample files
# MAGIC
# MAGIC In production, files arrive via SFTP or direct upload. For this demo we'll create them directly.

# COMMAND ----------

import json
import os


def create_sample_files(volume_path: str) -> None:
    """Create sample files in the landing volume for the workshop demo."""
    os.makedirs(volume_path, exist_ok=True)

    # Simple CSV with header
    with open(f"{volume_path}/Date.csv", "w") as f:
        f.write("date_key,date_value,day_of_week\n")
        f.write("1,2026-01-01,Thursday\n")
        f.write("2,2026-01-02,Friday\n")
        f.write("3,2026-01-03,Saturday\n")
        f.write("4,2026-01-04,Sunday\n")
        f.write("5,2026-01-05,Monday\n")

    # Pipe-delimited file
    with open(f"{volume_path}/Priorities.dat", "w") as f:
        f.write("priority_code|priority_name|sort_order\n")
        f.write("1|Critical|1\n")
        f.write("2|High|2\n")
        f.write("3|Medium|3\n")
        f.write("4|Low|4\n")
        f.write("5|Informational|5\n")

    # CSV that passes validation
    with open(f"{volume_path}/Customers.csv", "w") as f:
        f.write("customer_id,name,age,status\n")
        f.write("1,Alice,34,active\n")
        f.write("2,Bob,28,active\n")
        f.write("3,Charlie,45,inactive\n")
        f.write("4,Diana,31,active\n")
        f.write("5,Eve,22,active\n")

    # CSV that fails validation
    with open(f"{volume_path}/Customers_bad.csv", "w") as f:
        f.write("customer_id,name,age,status\n")
        f.write("1,,34,active\n")           # missing name (required)
        f.write("2,Bob,-5,active\n")        # age below minimum
        f.write("3,Charlie,45,deleted\n")   # status not in enum
        f.write("1,Diana,31,active\n")      # duplicate primary key
        f.write("5,Eve,200,active\n")       # age above maximum

    # CSVW metadata — defines what "valid" looks like
    metadata = {
        "@context": "http://www.w3.org/ns/csvw",
        "tableSchema": {
            "columns": [
                {
                    "name": "customer_id",
                    "datatype": "integer",
                    "required": True,
                },
                {
                    "name": "name",
                    "datatype": "string",
                    "required": True,
                    "constraints": {"minLength": 1},
                },
                {
                    "name": "age",
                    "datatype": "integer",
                    "constraints": {"minimum": 0, "maximum": 150},
                },
                {
                    "name": "status",
                    "datatype": "string",
                    "constraints": {"enum": ["active", "inactive"]},
                },
            ],
            "primaryKey": "customer_id",
        },
    }

    with open(f"{volume_path}/Customers.csv-metadata.json", "w") as f:
        json.dump(metadata, f, indent=2)

    files = [f for f in os.listdir(volume_path) if os.path.isfile(os.path.join(volume_path, f))]
    print(f"Created {len(files)} sample files in {volume_path}:")
    for name in sorted(files):
        print(f"  • {name}")


create_sample_files(VOLUME_PATH)

print("Sample files uploaded.")

# COMMAND ----------

# MAGIC %md
# MAGIC ---
# MAGIC ## 1. Reading Files
# MAGIC
# MAGIC One function. Any delimited file. It figures out the format.

# COMMAND ----------

from file_processing import process_file

# Read a CSV — no delimiter specified, no schema, no config
result = process_file(f"{VOLUME_PATH}/Date.csv", spark)

result.dataframe.display()

# COMMAND ----------

# MAGIC %md
# MAGIC Same function, completely different format:

# COMMAND ----------

# Read a pipe-delimited .dat file — auto-detected from the extension
result_dat = process_file(f"{VOLUME_PATH}/Priorities.dat", spark)

result_dat.dataframe.display()

# COMMAND ----------

# MAGIC %md
# MAGIC ### The result object
# MAGIC
# MAGIC Every call returns a structured result — not just a DataFrame. It tells you exactly what happened.

# COMMAND ----------

print(f"Success:        {result.success}")
print(f"File:           {result.file}")
print(f"Rows processed: {result.rows_processed}")
print(f"Rows corrupt:   {result.rows_corrupt}")
print(f"Checksum:       {result.checksum}")
print(f"Message:        {result.message}")

# COMMAND ----------

# MAGIC %md
# MAGIC ### Process a whole directory
# MAGIC
# MAGIC Point it at a folder. It handles everything — detecting formats, parsing each file, reporting results.

# COMMAND ----------

from file_processing import process_directory

results = process_directory(VOLUME_PATH, spark)

for r in results:
    status = "✓" if r.success else "✗"
    print(f"  {status} {r.file.split('/')[-1]:30s} — {r.rows_processed} rows")

# COMMAND ----------

# MAGIC %md
# MAGIC ---
# MAGIC ## 2. Validation
# MAGIC
# MAGIC SIFFT supports CSVW metadata — a standard for describing tabular data.
# MAGIC Define your constraints once, validate every time.
# MAGIC
# MAGIC We created a `Customers.csv-metadata.json` alongside `Customers.csv` — it defines what "valid" looks like:
# MAGIC required fields, allowed values, data types, and primary keys.

# COMMAND ----------

# MAGIC %md
# MAGIC ### Validation — passing

# COMMAND ----------

from dataframe_validation import validate_csvw_constraints

# Process the file
result_valid = process_file(f"{VOLUME_PATH}/Customers.csv", spark)

# Here's what the data looks like
result_valid.dataframe.display()

# COMMAND ----------

# The CSVW metadata — defines what "valid" means for this file
# In production this sits alongside the file as Customers.csv-metadata.json
metadata = {
    "@context": "http://www.w3.org/ns/csvw",
    "tableSchema": {
        "columns": [
            {
                "name": "customer_id",
                "datatype": "integer",
                "required": True,
            },
            {
                "name": "name",
                "datatype": "string",
                "required": True,
                "constraints": {"minLength": 1},
            },
            {
                "name": "age",
                "datatype": "integer",
                "constraints": {"minimum": 0, "maximum": 150},
            },
            {
                "name": "status",
                "datatype": "string",
                "constraints": {"enum": ["active", "inactive"]},
            },
        ],
        "primaryKey": "customer_id",
    },
}

print(json.dumps(metadata, indent=2))

# COMMAND ----------

# MAGIC %md
# MAGIC This metadata is a [CSVW](https://www.w3.org/TR/tabular-data-primer/) file — a W3C standard for describing tabular data.
# MAGIC In production, it lives alongside the CSV as `Customers.csv-metadata.json` and is auto-discovered by SIFFT.
# MAGIC
# MAGIC It defines:
# MAGIC - **Data types** — integer, string
# MAGIC - **Required fields** — customer_id and name must not be null
# MAGIC - **Constraints** — age between 0–150, status must be "active" or "inactive"
# MAGIC - **Primary key** — customer_id must be unique

# COMMAND ----------

# Validate against constraints
report = validate_csvw_constraints(result_valid.dataframe, metadata)

print(f"Valid: {report.valid}")
print(f"Violations: {len(report.violations)}")

# COMMAND ----------

# MAGIC %md
# MAGIC ### Validation — failing
# MAGIC
# MAGIC Now let's see what happens with bad data.

# COMMAND ----------

# Process the bad file
result_bad = process_file(f"{VOLUME_PATH}/Customers_bad.csv", spark)

# Look at the data — can you spot the problems?
result_bad.dataframe.display()

# COMMAND ----------

# Validate against the same constraints (same contract, different data)
report_bad = validate_csvw_constraints(result_bad.dataframe, metadata)

print(f"Valid: {report_bad.valid}")
print(f"Violations: {len(report_bad.violations)}")
print()
for v in report_bad.violations:
    print(f"  ❌ {v.column}: {v.constraint_type} — {v.message} ({v.violating_rows} rows)")

# COMMAND ----------

# MAGIC %md
# MAGIC Bad data is caught **before** it reaches the table. No silent corruption.
# MAGIC
# MAGIC ---
# MAGIC ## 3. Writing to Delta
# MAGIC
# MAGIC One call to write a validated DataFrame to a Delta table — with automatic source lineage.

# COMMAND ----------

from table_writing import write_table, TableWriteOptions

options = TableWriteOptions(
    mode="overwrite",
    add_source_metadata=True,
)

write_result = write_table(
    result_valid.dataframe,
    f"{CATALOG}.{SCHEMA}.customers",
    spark,
    options,
    source_file_path=f"{VOLUME_PATH}/Customers.csv",
)

print(f"Success: {write_result.success}")
print(f"Rows written: {write_result.rows_written}")
  

# COMMAND ----------

# MAGIC %md
# MAGIC ### Source lineage
# MAGIC
# MAGIC Every row knows where it came from. No manual tracking needed.

# COMMAND ----------

spark.sql(f"SELECT * FROM `{CATALOG}`.`{SCHEMA}`.customers").display()

# COMMAND ----------

# MAGIC %md
# MAGIC The `dp_metadata_*` columns tell you:
# MAGIC - Which file this row came from
# MAGIC - When it was ingested
# MAGIC - The directory it was in
# MAGIC
# MAGIC "Where did this data come from?" is always answerable.
# MAGIC
# MAGIC ---
# MAGIC ## 4. Tracking
# MAGIC
# MAGIC Process a file, confirm it worked, and SIFFT remembers.
# MAGIC Re-run the same file? It skips it. No duplicates.

# COMMAND ----------

from file_processing import process_file, confirm_processed

TRACKING_TABLE = f"{CATALOG}.{SCHEMA}.file_tracking"

# First run — process the file with tracking enabled
result_tracked = process_file(
    f"{VOLUME_PATH}/Date.csv",
    spark,
    tracking="delta_table",
    tracking_location=TRACKING_TABLE,
)

print(f"First run — Success: {result_tracked.success}")
print(f"Rows: {result_tracked.rows_processed}")

# Confirm we're done (this writes the tracking record)
confirm_processed(result_tracked, spark)
print("Tracking confirmed.")

# COMMAND ----------

# MAGIC %md
# MAGIC ### Re-run the same file

# COMMAND ----------

# Second run — same file, same tracking table
result_rerun = process_file(
    f"{VOLUME_PATH}/Date.csv",
    spark,
    tracking="delta_table",
    tracking_location=TRACKING_TABLE,
)

print(f"Second run — Success: {result_rerun.success}")
print(f"Message: {result_rerun.message}")
print(f"Rows: {result_rerun.rows_processed}")

# COMMAND ----------

# MAGIC %md
# MAGIC **Zero rows processed. It knew.** The file hasn't changed, so it's skipped.
# MAGIC
# MAGIC This means:
# MAGIC - Jobs can safely re-run after failures — no duplicates
# MAGIC - If a file is resent unchanged, it's detected
# MAGIC - Tracking is based on file checksum AND path — if the content changes, it gets reprocessed
# MAGIC
# MAGIC Notice we called `confirm_processed` separately. If validation or the table write had failed,
# MAGIC no tracking record would be written — and the file would be automatically retried on the next run.

# COMMAND ----------

# MAGIC %md
# MAGIC ### The tracking table

# COMMAND ----------

spark.sql(f"SELECT * FROM `{CATALOG}`.`{SCHEMA}`.file_tracking").display()

# COMMAND ----------

# MAGIC %md
# MAGIC ---
# MAGIC ## 5. Custom Handlers
# MAGIC
# MAGIC Legacy systems output all sorts of formats — `.rpt`, `.extract`, `.dat` with custom headers.
# MAGIC If SIFFT doesn't handle it out of the box, register your own handler.
# MAGIC It takes priority and `process_file` uses it automatically.

# COMMAND ----------

from file_processing import register_handler, FileProcessingResult

# Example: we receive .rpt files that are pipe-delimited with a metadata line we need to skip
def rpt_handler(file_path, spark, options):
    """Custom handler for .rpt report files — skips metadata header line."""
    # The metadata line starts with "REPORT" — use comment option to skip it
    df = (
        spark.read
        .option("header", "true")
        .option("sep", "|")
        .option("comment", "R")  # Skip lines starting with R (the metadata line)
        .csv(file_path)
    )

    row_count = df.count()
    return FileProcessingResult(
        success=True,
        file=file_path,
        dataframe=df,
        message=f"Custom .rpt handler: {row_count} rows",
        rows_processed=row_count,
    )


# Register — priority 10 means it takes precedence over built-in handlers
register_handler(".rpt", rpt_handler, priority=10)
print("Custom handler registered for .rpt files")


# COMMAND ----------

# Create a sample .rpt file to test it
with open(f"{VOLUME_PATH}/Weekly_Report.rpt", "w") as f:
    f.write("REPORT GENERATED 2026-07-01 -- SYSTEM: LEGACY-V2\n")  # metadata line
    f.write("region_code|region_name|active_cases\n")                # header
    f.write("CY|Cymru|142\n")
    f.write("SW|South West|87\n")
    f.write("WM|West Midlands|213\n")
    f.write("SE|South East|164\n")

# Now process_file picks up our custom handler automatically
result_rpt = process_file(f"{VOLUME_PATH}/Weekly_Report.rpt", spark)

print(f"Success: {result_rpt.success}")
print(f"Message: {result_rpt.message}")
result_rpt.dataframe.display()

# COMMAND ----------

# MAGIC %md
# MAGIC That's it. Define a function, register it, and `process_file` handles the rest.
# MAGIC
# MAGIC Any legacy format your systems produce — if you can write the parsing logic,
# MAGIC SIFFT will integrate it into the same pipeline as everything else.
# MAGIC Same result object. Same tracking. Same validation.

# COMMAND ----------

# MAGIC %md
# MAGIC ---
# MAGIC ## In Production
# MAGIC
# MAGIC This is running in production today across Drivers, Vehicles, and Lookups — handling 50+ file types daily.
# MAGIC
# MAGIC Here's the real code. Three tasks run as a scheduled Databricks job:
# MAGIC **Pull** (SFTP → Volume) → **Ingest** (Volume → Delta) → **Cleanup** (Archive + SFTP delete)

# COMMAND ----------

# MAGIC %md
# MAGIC ### The entry point
# MAGIC
# MAGIC `run_ingest.py` — the entire ingestion task. List files, loop, ingest, report.
# MAGIC
# MAGIC ```python
# MAGIC # NOTE: Comments added for workshop readability — not in the production code.
# MAGIC
# MAGIC from file_management import list_files_in_directory
# MAGIC from pyspark.sql import SparkSession
# MAGIC
# MAGIC from job.ingest import IngestResult, ingest_file
# MAGIC
# MAGIC
# MAGIC def main():
# MAGIC     args = parse_args()
# MAGIC     spark = SparkSession.builder.getOrCreate()
# MAGIC     dbutils = get_dbutils()
# MAGIC
# MAGIC     # Retrieve HMAC seed for PD redaction from Azure Key Vault
# MAGIC     hmac_seed = dbutils.secrets.get(scope=args.secret_scope, key="SECRET-HMAC-SEED-ROOT")
# MAGIC
# MAGIC     # List all files in the landing volume
# MAGIC     files_result = list_files_in_directory(args.source_directory)
# MAGIC
# MAGIC     counts = {IngestResult.PROCESSED: 0, IngestResult.SKIPPED: 0, IngestResult.UNMATCHED: 0}
# MAGIC
# MAGIC     # Process each file independently
# MAGIC     for file_info in files_result.files:
# MAGIC         result = ingest_file(
# MAGIC             file_info["path"], file_info["name"],
# MAGIC             args.catalog, args.schema, spark,
# MAGIC             reprocess=False, hmac_seed=hmac_seed,
# MAGIC         )
# MAGIC         counts[result] += 1
# MAGIC
# MAGIC     print(f"{counts[IngestResult.PROCESSED]} processed, "
# MAGIC           f"{counts[IngestResult.SKIPPED]} skipped, "
# MAGIC           f"{counts[IngestResult.UNMATCHED]} unmatched")
# MAGIC ```

# COMMAND ----------

# MAGIC %md
# MAGIC ### The core function
# MAGIC
# MAGIC `ingest_file()` — match filename to config, read with tracking, apply columns, redact PD, write, confirm.
# MAGIC
# MAGIC ```python
# MAGIC # NOTE: Comments added for workshop readability — not in the production code.
# MAGIC
# MAGIC from file_processing import process_file, clear_tracking, confirm_processed
# MAGIC from table_writing import write_table, TableWriteOptions
# MAGIC
# MAGIC from .config import TRACKING_TABLE, get_file_mapping
# MAGIC from .redact import redact
# MAGIC
# MAGIC
# MAGIC def ingest_file(file_path, filename, catalog, schema, spark, reprocess=False, hmac_seed=None):
# MAGIC
# MAGIC     # Match filename against config — skip if no mapping exists
# MAGIC     mapping = get_file_mapping(filename)
# MAGIC     if not mapping:
# MAGIC         return IngestResult.UNMATCHED
# MAGIC
# MAGIC     target_table = f"{catalog}.{schema}.{mapping['target_table']}"
# MAGIC     tracking_table = f"{catalog}.{schema}.{TRACKING_TABLE}"
# MAGIC
# MAGIC     # Clear tracking record if reprocessing requested
# MAGIC     if reprocess:
# MAGIC         clear_tracking(file_path, "delta_table", tracking_table, spark)
# MAGIC
# MAGIC     # Read file — tracking will skip if already processed
# MAGIC     result = process_file(file_path, spark,
# MAGIC         read_options={"delimiter": mapping["delimiter"], "header": str(mapping["header"]).lower()},
# MAGIC         tracking="delta_table", tracking_location=tracking_table,
# MAGIC     )
# MAGIC
# MAGIC     if not result.success or result.rows_processed == 0:
# MAGIC         confirm_processed(result, spark)
# MAGIC         return IngestResult.SKIPPED
# MAGIC
# MAGIC     # Apply column names from config (files arrive without headers)
# MAGIC     result.dataframe = result.dataframe.toDF(*mapping["columns"])
# MAGIC
# MAGIC     # Redact PD fields with HMAC-SHA256 (irreversible, consistent hash)
# MAGIC     if hmac_seed:
# MAGIC         result.dataframe = redact(result.dataframe, target_table, hmac_seed)
# MAGIC
# MAGIC     # Write to Delta with source lineage
# MAGIC     write_table(result.dataframe, target_table, spark,
# MAGIC         TableWriteOptions(mode="overwrite", add_source_metadata=True, schema_mode="overwrite"),
# MAGIC         source_file_path=file_path, raise_on_error=True,
# MAGIC     )
# MAGIC
# MAGIC     # Confirm tracking only after successful write — no false positives
# MAGIC     confirm_processed(result, spark)
# MAGIC     return IngestResult.PROCESSED
# MAGIC ```

# COMMAND ----------

# MAGIC %md
# MAGIC ### The config
# MAGIC
# MAGIC Each file is a pattern match → target table → column names. That's all you define.
# MAGIC
# MAGIC ```python
# MAGIC FILE_MAPPINGS = [
# MAGIC     {
# MAGIC         "pattern": r"Date\.csv",
# MAGIC         "target_table": "date",
# MAGIC         "delimiter": ",",
# MAGIC         "header": "auto",
# MAGIC         "columns": ["date_key", "date_value", "date_name", "day_of_month_key", ...],
# MAGIC     },
# MAGIC     {
# MAGIC         "pattern": r"Lkp_Channel\.csv",
# MAGIC         "target_table": "lkp_channel",
# MAGIC         "delimiter": ",",
# MAGIC         "header": "auto",
# MAGIC         "columns": ["channel", "channel_description", "date_time_created"],
# MAGIC     },
# MAGIC     {
# MAGIC         "pattern": r"Lkp_UserID\.csv",
# MAGIC         "target_table": "lkp_user_id",
# MAGIC         "delimiter": ",",
# MAGIC         "header": "auto",
# MAGIC         "columns": ["user_id", "system", "valid_from", "valid_to",
# MAGIC                     "original_staff_key", "date_time_created"],
# MAGIC     },
# MAGIC     # ... 40+ file mappings total
# MAGIC ]
# MAGIC
# MAGIC TRACKING_TABLE = "file_tracking"
# MAGIC ```

# COMMAND ----------

# MAGIC %md
# MAGIC ### The job
# MAGIC
# MAGIC Deployed as a Databricks Asset Bundle. Three tasks, scheduled daily at 10:30.
# MAGIC
# MAGIC ```yaml
# MAGIC resources:
# MAGIC   jobs:
# MAGIC     job:
# MAGIC       tasks:
# MAGIC         - task_key: pull
# MAGIC           spark_python_task:
# MAGIC             python_file: src/run_pull.py         # SFTP → Volume
# MAGIC
# MAGIC         - task_key: ingest
# MAGIC           spark_python_task:
# MAGIC             python_file: src/run_ingest.py       # Volume → Delta tables
# MAGIC           depends_on:
# MAGIC             - task_key: pull
# MAGIC
# MAGIC         - task_key: cleanup
# MAGIC           spark_python_task:
# MAGIC             python_file: src/run_cleanup.py      # Archive + SFTP delete
# MAGIC           depends_on:
# MAGIC             - task_key: ingest
# MAGIC
# MAGIC targets:
# MAGIC   production:
# MAGIC     schedule:
# MAGIC       quartz_cron_expression: "0 30 10 * * ?"   # Daily 10:30 UK
# MAGIC ```
# MAGIC
# MAGIC That's it. Config + SIFFT = production pipeline.

# COMMAND ----------

# MAGIC %md
# MAGIC ---
# MAGIC ## Wrap-up
# MAGIC
# MAGIC **SIFFT gives you:**
# MAGIC - Read any delimited file with one function call
# MAGIC - Validate against defined constraints before writing
# MAGIC - Write to Delta tables with automatic source lineage
# MAGIC - Track what's been processed — safe to re-run, no duplicates
# MAGIC
# MAGIC **SIFFT is installed on all general purpose clusters.** Import it and go.
# MAGIC
# MAGIC **Questions?** Talk to the Data Platform team.

# COMMAND ----------

# MAGIC %md
# MAGIC ---
# MAGIC ## Cleanup

# COMMAND ----------

spark.sql(f"DROP SCHEMA IF EXISTS `{CATALOG}`.`{SCHEMA}` CASCADE")
print("Cleaned up.")
