# File Ingestion Configuration API — Design Specification

**Module:** CSF2000-2753 Web Services
**System:** A RESTful web service that manages the ingestion configuration for SIFFT
(Spark Ingestion Framework For Tables), the DVLA Data Platform's file-ingestion library.

> This document is the design reference for both the report (Design & Justification of
> Web Service; Design & Justification of Inputs and Outputs) and the Spring Boot build.

---

## 1. Context and business rationale

SIFFT is a Python library installed on DVLA's Databricks clusters. It ingests 50+ file
types daily across Drivers, Vehicles, and Lookups via a scheduled job: **Pull** (SFTP →
Volume) → **Ingest** (Volume → Delta) → **Cleanup**.

Today the ingestion configuration lives **in code** — a `FILE_MAPPINGS` list (40+ entries)
and `TableWriteOptions` baked into the job. Onboarding or changing a file type means a
**code change and redeploy** via a Databricks Asset Bundle.

**The change (TO-BE):** externalise configuration into a managed REST service. The
service owns the configuration; SIFFT-driven notebooks consume it. Editing a feed's
delimiter, or decommissioning a feed, becomes an API call — not a redeploy.

Two configuration concepts come straight from the SIFFT developers and the code:

- **Read config** — how an inbound file is *read* (`process_file` / `read_options` +
  the per-file-type mapping): pattern, delimiter, header, columns.
- **Write config** — how the resulting table is *written* (`TableWriteOptions`):
  target table, format, mode, schema mode, partitioning, source-metadata.

A third, supporting concept is the **controlled vocabulary** (`config.py` constants):
the legal delimiters, formats, modes and schema modes that the two configs validate
against.

---

## 2. Resource model

| Resource | Collection URI | Represents | Derived from |
|---|---|---|---|
| Read config | `/api/v1/read-configs` | Inbound read contract per file type | `process_file` read_options + `FILE_MAPPINGS` |
| Write config | `/api/v1/write-configs` | Outbound persistence contract | `TableWriteOptions` |
| Config options | `/api/v1/config-options` | Read-only allowed-values vocabulary | `file_processing/config.py`, `table_writing/config.py` |

Design notes to justify in the report:
- **Resource nouns, not verbs.** Each URI names a *thing*, not an action — core REST.
- **Versioned base path `/api/v1/`.** URI versioning chosen for visibility and
  cache/proxy friendliness; alternatives (header/media-type versioning) noted and rejected
  for simplicity in the versioning section.
- **Three disparate resources, low overlap** — a read-config body looks nothing like a
  write-config body, and config-options is read-only reference data. This satisfies the
  PoC rubric's "least overlap, most complexity."

---

## 3. HTTP method semantics (design section — all methods designed, subset implemented)

| Method | Safe | Idempotent | Use here |
|---|---|---|---|
| GET | Yes | Yes | Retrieve config(s) / vocabulary |
| POST | No | No | Create a new config (server assigns id) |
| PUT | No | Yes | Replace an existing config at a known id |
| DELETE | No | Yes | Remove a config |

Justification points (for the 10-mark design section):
- **POST vs PUT:** POST creates and is non-idempotent (twice = two resources); PUT replaces
  at a known id and is idempotent (twice = same state). This is why create is POST on the
  *collection* and update is PUT on the *item*.
- **PUT vs PATCH:** PUT chosen (full replacement) over PATCH (partial) — config documents
  are small and always sent whole, so full-replace semantics are simpler and less
  ambiguous. PATCH deliberately not offered.
- **DELETE idempotency:** deleting an absent config leaves state unchanged; the second
  call returns 404 but the end state is identical.
- **config-options is GET-only** — it is server-owned reference data; clients must not
  create/modify the vocabulary, so POST/PUT/DELETE are intentionally absent.

---

## 4. Full endpoint catalogue

Legend: **[PoC]** = one of the five implemented endpoints. Others are designed and
justified in the report (and may be screenshotted as supporting data) but need not be
among the implemented five.

### 4.1 read-configs

| # | Method | Path | Purpose | Success | Errors |
|---|---|---|---|---|---|
| R1 | GET | `/api/v1/read-configs` | List all (optional `?pattern=` filter) | 200 | — |
| R2 | GET | `/api/v1/read-configs/{id}` | Fetch one | 200 | 404 |
| R3 **[PoC]** | POST | `/api/v1/read-configs` | Create | 201 + `Location` | 400, 409 |
| R4 **[PoC]** | PUT | `/api/v1/read-configs/{id}` | Replace | 200 | 400, 404 |
| R5 **[PoC]** | DELETE | `/api/v1/read-configs/{id}` | Remove | 204 | 404 |
| R6 **[PoC]** | POST | `/api/v1/read-configs/{id}/test` | Test config against a sample line | 200 | 400, 404 |

### 4.2 write-configs

| # | Method | Path | Purpose | Success | Errors |
|---|---|---|---|---|---|
| W1 | GET | `/api/v1/write-configs` | List all | 200 | — |
| W2 | GET | `/api/v1/write-configs/{id}` | Fetch one | 200 | 404 |
| W3 **[PoC]** | POST | `/api/v1/write-configs` | Create | 201 + `Location` | 400, 422 |
| W4 | PUT | `/api/v1/write-configs/{id}` | Replace | 200 | 400, 404, 422 |
| W5 | DELETE | `/api/v1/write-configs/{id}` | Remove | 204 | 404 |

### 4.3 config-options

| # | Method | Path | Purpose | Success | Errors |
|---|---|---|---|---|---|
| C1 | GET | `/api/v1/config-options` | Return allowed delimiters/formats/modes/schema-modes | 200 | — |

**The implemented five (least overlap, most complexity, ≥2 parameterised, ≥2 server-side
processing):** R3 (POST create + validation), R4 (PUT update, parameterised), R5 (DELETE,
parameterised), W3 (POST create + vocabulary validation), R6 (POST test, parameterised +
processing).

- Parameterised endpoints: R4, R5, R6 (path variable `{id}`), plus R6 processes a body.
- Server-side processing endpoints: R3/W3 (validation), R6 (applies the delimiter to a
  sample line and reports detected columns).
- HTTP codes demonstrable across the five: 200, 201, 204, 400, 404, 409, 422, 500.

---

## 5. JSON representations (Inputs & Outputs — 15-mark section)

Standard request headers: `Content-Type: application/json`, `Accept: application/json`.
Standard response header: `Content-Type: application/json`. 201 responses also return a
`Location` header pointing at the new resource.

### 5.1 Read config representation

Fields and types:

| Field | JSON type | Required | Rule (from SIFFT) |
|---|---|---|---|
| `id` | string (UUID) | server-assigned | read-only |
| `pattern` | string (regex) | yes | unique across read-configs; valid regex |
| `delimiter` | string | yes | exactly one character (Spark limitation) |
| `header` | string | yes | one of `true`, `false`, `auto` |
| `columns` | array of string | yes | non-empty; each a valid identifier |
| `maxCorruptPercent` | number | no | 0.0–1.0; default 0.1 (`MAX_CORRUPT_RECORDS_PERCENT`) |
| `writeConfigId` | string (UUID) | no | if present, must reference an existing write-config |

**POST /read-configs — request**
```json
{
  "pattern": "Customers\\.csv",
  "delimiter": ",",
  "header": "auto",
  "columns": ["customer_id", "name", "age", "status"],
  "maxCorruptPercent": 0.1,
  "writeConfigId": "7c9e6679-7425-40de-944b-e07fc1f90ae7"
}
```

**201 Created — response** (`Location: /api/v1/read-configs/3fa85f64-...`)
```json
{
  "id": "3fa85f64-5717-4562-b3fc-2c963f66afa6",
  "pattern": "Customers\\.csv",
  "delimiter": ",",
  "header": "auto",
  "columns": ["customer_id", "name", "age", "status"],
  "maxCorruptPercent": 0.1,
  "writeConfigId": "7c9e6679-7425-40de-944b-e07fc1f90ae7"
}
```

### 5.2 Write config representation

| Field | JSON type | Required | Rule (from SIFFT) |
|---|---|---|---|
| `id` | string (UUID) | server-assigned | read-only |
| `targetTable` | string | yes | `^[a-zA-Z0-9_.]+$`, ≤255 chars (`validate_write_options`) |
| `format` | string | yes | one of `delta`, `parquet`, `orc` (`FORMATS`) |
| `mode` | string | yes | one of `append`, `overwrite`, `merge`, `errorIfExists` (`MODES`) |
| `schemaMode` | string | yes | one of `strict`, `merge`, `overwrite` (`SCHEMA_MODES`) |
| `partitionBy` | array of string | no | each a valid identifier |
| `mergeKeys` | array of string | conditional | **required when `mode = merge`**; merge only valid for `delta` |
| `addSourceMetadata` | boolean | no | default false |

**POST /write-configs — request**
```json
{
  "targetTable": "drivers.curated.customers",
  "format": "delta",
  "mode": "merge",
  "schemaMode": "merge",
  "partitionBy": ["status"],
  "mergeKeys": ["customer_id"],
  "addSourceMetadata": true
}
```

**201 Created — response** mirrors the request with `id`.

### 5.3 Config options representation

**GET /api/v1/config-options — 200**
```json
{
  "delimiters": [",", "|", "\t", ";"],
  "headerOptions": ["true", "false", "auto"],
  "formats": ["delta", "parquet", "orc"],
  "modes": ["append", "overwrite", "merge", "errorIfExists"],
  "schemaModes": ["strict", "merge", "overwrite"],
  "maxCorruptPercentDefault": 0.1
}
```

### 5.4 Config-test representation (R6)

**POST /api/v1/read-configs/{id}/test — request**
```json
{ "sampleLine": "1,Alice,34,active" }
```

**200 OK — response** (server applies the stored delimiter and compares to `columns`)
```json
{
  "id": "3fa85f64-5717-4562-b3fc-2c963f66afa6",
  "delimiter": ",",
  "detectedFieldCount": 4,
  "expectedFieldCount": 4,
  "matches": true,
  "detectedFields": ["1", "Alice", "34", "active"]
}
```

### 5.5 Error representation (uniform across all endpoints)

Mirrors SIFFT's own `to_dict()` error shape (`error_type`, `message`, `details`) so the
API is faithful to the system it fronts.

```json
{
  "timestamp": "2026-10-20T08:30:00Z",
  "status": 400,
  "errorType": "invalid_delimiter",
  "message": "delimiter must be a single character",
  "details": "Received ';;' (length 2). Spark supports single-character delimiters only.",
  "path": "/api/v1/read-configs"
}
```

---

## 6. Status-code decision table (server-side processing & error handling)

| Condition | Code | Where |
|---|---|---|
| Successful GET / PUT / test | 200 | all |
| Resource created | 201 | R3, W3 |
| Successful DELETE | 204 | R5 |
| Malformed JSON / missing required field / bad type | 400 | R3, R4, W3, R6 |
| `delimiter` not single char | 400 | R3, R4 |
| `header` not in {true,false,auto} | 400 | R3, R4 |
| `columns` empty | 400 | R3, R4 |
| `id` not found | 404 | R4, R5, R6 |
| Duplicate `pattern` on create | 409 | R3 |
| `format`/`mode`/`schemaMode` not in vocabulary | 422 | W3 |
| `mode = merge` but `mergeKeys` missing | 422 | W3 |
| Unhandled server error | 500 | global handler |

Note the deliberate **400 vs 422** distinction (good for marks): 400 = the request is
malformed or structurally invalid (can't be parsed/typed); 422 = the request is
well-formed JSON but **semantically** invalid against business rules (e.g. a value outside
the allowed vocabulary). State this choice explicitly.

---

## 7. Validation rules mirrored from SIFFT (server-side processing evidence)

These are implemented server-side in the service layer, not trusted from the client:

1. **Single-character delimiter** — from `delimited_files.py` (Spark rejects multi-char).
2. **Target table identifier** — regex `^[a-zA-Z0-9_.]+$`, ≤255 chars — from
   `table_writing/validation.py` (`_validate_identifier`); also an injection guard.
3. **format ∈ FORMATS, mode ∈ MODES, schemaMode ∈ SCHEMA_MODES** — from the two
   `config.py` files.
4. **merge requires mergeKeys and format=delta** — from `_validate_merge_keys`.
5. **columns non-empty; each a valid identifier.**
6. **pattern is a compilable regex and unique** among read-configs.
7. **maxCorruptPercent in [0,1]**, default 0.1 (`MAX_CORRUPT_RECORDS_PERCENT`).

---

## 8. Mapping to the marking rubric

| Rubric section | Marks | How this design addresses it |
|---|---|---|
| Introduction & Description | 10 | §1 business context: real DVLA pipeline, criticality (50+ feeds/day), end users (Data Platform team) |
| Design & justification of web service | 10 | §2–§4: resource nouns, versioned paths, full method set with idempotency justification |
| Inputs & outputs in JSON | 15 | §5: typed representations, headers, `Location`, uniform error shape |
| Proof of Concept (Spring Boot) | 20 | §4 five endpoints, ≥2 parameterised, ≥2 server-side processing, full status spread |
| Implementation Reflection | 15 | controller/service/repository separation; validation layer mirrors SIFFT |
| Security, Maintainability, Versioning | 15 | §9 below |
| Document Structure | 10 | report formatting (separate task) |

---

## 9. Security / maintainability / versioning seeds (grounded in this project)

**Security**
- **AuthN/Z:** editing ingestion config is privileged — only the Data Platform team should
  POST/PUT/DELETE; everyone may GET. Discuss API keys / OAuth2 bearer tokens / role-based
  access. Contrast with today, where config edits require repo/deploy access.
- **Input validation as defence:** the identifier regex prevents injection into downstream
  `spark.sql` table names (real risk in `delta_tracking.py`'s `CREATE TABLE`).
- **No secrets in config:** cloud credentials (`storage_options`) and the HMAC redaction
  seed stay in Azure Key Vault, never in a read/write-config body or response.
- **Transport:** HTTPS/TLS; reject plaintext.

**Maintainability**
- **Externalised config removes redeploys:** onboarding a new feed = one POST, not a code
  change + Asset Bundle redeploy.
- **Server-side validation prevents bad config landing** — the batch job can't be broken
  by an invalid delimiter/mode because the API rejected it at write time.
- **Layered architecture** (controller → service → repository) isolates change.
- **Single source of truth for the vocabulary** via config-options.

**Versioning**
- **URI versioning `/api/v1/`** chosen; rationale and alternatives (header, media-type).
- **Backward compatibility:** additive fields are non-breaking; removing/renaming a field
  or tightening a rule triggers `/v2`. Tie to SIFFT's own evolution (e.g. new `FORMATS`).
- **Config-schema evolution** mirrors how `config.py` constants change over time.

---

## 10. Build target (next task)

Spring Boot 3.3.x, Java, Maven. Dependencies: Spring Web, Validation (Jakarta Bean
Validation), Spring Data JPA, H2 (in-memory). Layout:

```
controller/   ReadConfigController, WriteConfigController, ConfigOptionsController
service/      validation + business logic (mirrors SIFFT rules)
repository/   Spring Data JPA repositories
model/        JPA entities: ReadConfig, WriteConfig
dto/          request/response records
exception/    ApiError + @RestControllerAdvice global handler
```
