# Report Supporting Material

Reference notes for the CSF2000 report, grounded in the implemented PoC
(`C:\WSD_WBP\sifft-config-api`) and the real SIFFT system. Organised by rubric section.
These are structured notes and tables for you to draw on — not finished prose.

---

## A. Method semantics table (Design & Justification — 10 marks)

| Method | Safe | Idempotent | Used on | Why |
|---|---|---|---|---|
| GET | Yes | Yes | collection + item + config-options | Retrieval must not change state; repeatable |
| POST | No | No | collection (create) | Creates a new resource with a server-assigned id; two calls = two resources |
| PUT | No | Yes | item (replace) | Full replacement at a known id; repeating yields the same state |
| DELETE | No | Yes | item | Removal; repeating leaves the resource absent |

Talking points:
- **POST on the collection, PUT on the item.** Create targets `/read-configs` (no id yet);
  replace targets `/read-configs/{id}` (id known). This is the canonical REST split.
- **Idempotency matters operationally.** The SIFFT batch job runs daily and may retry.
  A client that re-sends a PUT after a timeout must not corrupt state — PUT's idempotency
  guarantees that. POST is deliberately *not* idempotent, which is why creates can 409 on a
  duplicate pattern rather than silently making a second row.
- **PUT chosen over PATCH.** Config documents are small and always sent whole; full
  replacement is unambiguous. PATCH (partial update) was considered and rejected to avoid
  merge-semantics complexity.
- **config-options is GET-only by design** — server-owned reference data; exposing write
  methods would let clients corrupt the vocabulary that validation depends on.

---

## B. JSON representation rationale (Inputs & Outputs — 15 marks)

Points to make, each with evidence from the implementation:

1. **Headers.** Requests use `Content-Type: application/json` and `Accept: application/json`.
   201 responses carry a `Location` header pointing at the new resource
   (`ResponseEntity.created(URI...)` in both create controllers) — correct HTTP semantics
   for resource creation.

2. **Typed fields, not free strings.** `columns`/`partitionBy`/`mergeKeys` are JSON arrays;
   `maxCorruptPercent` is a number bounded 0.0–1.0; `addSourceMetadata` is boolean. Types are
   enforced both by Jackson deserialization (wrong type → 400) and Bean Validation.

3. **Request vs response asymmetry.** The request omits `id` (server-assigned); the response
   includes it. This is why create is POST — the client cannot know the id in advance.

4. **Uniform error representation.** Every error returns the same shape
   (`timestamp, status, errorType, message, details, path`). The `errorType/message/details`
   triad deliberately mirrors SIFFT's own `Exception.to_dict()`, so the API speaks the same
   error language as the system it fronts. One predictable error contract across all
   endpoints aids client handling.

5. **Resource linkage.** `read-config.writeConfigId` is a soft reference to a write-config,
   validated server-side (422 if the target does not exist) — representing the real
   relationship between how a file is read and how its table is written.

6. **Example interactions** (one success + the representative error per code) are in
   `DESIGN_SPEC.md` §5–§6 and can be screenshotted from Postman for the PoC section.

---

## C. Implementation Reflection (15 marks)

### C.1 Layered architecture (separation of concerns)
```
Controller  — HTTP concern only: routing, status codes, (de)serialisation
Service     — business rules + SIFFT-derived validation; throws typed exceptions
Repository  — persistence (Spring Data JPA); no business logic
Model       — JPA entities (ReadConfig, WriteConfig)
DTO         — request/response records; decouple the wire contract from entities
Exception   — ApiError + GlobalExceptionHandler centralise error translation
```
Why it matters: each layer changes independently. Adding a validation rule touches only the
service; changing the storage engine touches only the repository/entity; changing the JSON
shape touches only the DTOs — the entity stays stable.

### C.2 DTOs vs entities
Requests/responses are Java records, separate from JPA entities. This stops internal
persistence concerns (ids, collection mapping) leaking into the API and lets the two evolve
independently — a maintainability best practice.

### C.3 Centralised error handling
No controller contains a try/catch. All translation lives in `GlobalExceptionHandler`
(`@RestControllerAdvice`). Adding a new error type is one method in one place, and every
endpoint automatically returns the uniform `ApiError`.

### C.4 Validation strategy — two layers, two status codes
- **Bean Validation** (`@NotBlank`, `@NotEmpty`, `@DecimalMin/Max`) handles structural
  problems → **400**.
- **Service-layer business validation** mirrors SIFFT and splits into:
  - structural/format violations → **400** (e.g. multi-character delimiter)
  - semantic/vocabulary violations → **422** (e.g. `format` not in the allowed set)
The 400-vs-422 distinction is deliberate and defensible.

### C.5 A defect the tests caught (strong reflection point)
The PUT-update test initially failed with HTTP 500. Root cause: the service populated JPA
`@ElementCollection` fields with immutable `List.of()`, which Hibernate cannot manage on
update — it threw `UnsupportedOperationException`. Fix: use mutable `new ArrayList<>(...)`.
A happy-path manual demo would not have revealed this; the automated test did. This
demonstrates the value of testing every path, not just the success path — directly relevant
to "best practice" and "maintainability" in the rubric.

### C.6 Test coverage
36 MockMvc tests exercise success and every error path across the five endpoints
(200/201/204/400/404/409/422), using an in-memory H2 database reset per test for isolation.

---

## D. Security, Maintainability, Versioning (15 marks)

### D.1 Security (with project-specific examples)
- **Authentication/authorisation.** Editing ingestion config is privileged: a wrong
  delimiter or target table silently breaks the daily batch for Drivers/Vehicles/Lookups.
  Design: GET open to readers; POST/PUT/DELETE restricted to the Data Platform team via
  OAuth2 bearer tokens / API keys and role-based access. Contrast with today, where config
  changes require source-repo and deployment access — a coarser, less auditable control.
- **Injection defence.** `targetTable` is validated against `^[a-zA-Z0-9_.]+$` (≤255 chars),
  mirroring SIFFT's `_validate_identifier`. This is not cosmetic: in SIFFT the table name is
  interpolated into `spark.sql("CREATE TABLE ...")` in `delta_tracking.py`, so an unvalidated
  identifier is a real SQL-injection vector. The API rejects it before it can ever reach Spark.
- **Secrets stay out of config.** Cloud credentials (`storage_options`) and the HMAC
  redaction seed live in Azure Key Vault in the production job and must never appear in a
  read/write-config body or response. The config resource models *what* to ingest, never
  *credentials*.
- **Transport.** HTTPS/TLS enforced; reject plaintext. Validation errors return structured
  messages without leaking stack traces (the generic handler returns only the exception
  class name, not the trace).

### D.2 Maintainability (with project-specific examples)
- **Externalised configuration removes redeploys.** Today, onboarding a new feed means
  editing the `FILE_MAPPINGS` list (40+ entries) and redeploying the Databricks Asset Bundle.
  With the service it is a single `POST /read-configs`. Decommissioning is a `DELETE`.
- **Validation at write time, not run time.** SIFFT currently discovers a bad config when the
  batch runs and fails. The service rejects invalid config *on save* (400/422), so the batch
  cannot be broken by malformed configuration — the failure moves left, to a safe moment.
- **Single source of truth for the vocabulary.** `ConfigVocabulary` holds the allowed
  delimiters/formats/modes/schema-modes once; both the validators and the `config-options`
  endpoint read from it, so published options and enforced rules cannot drift apart.
- **Layered design + tests** make change safe and regressions visible (see C).

### D.3 Versioning (with project-specific examples)
- **URI versioning `/api/v1/`** chosen for visibility and cache/proxy friendliness.
  Alternatives considered: header versioning and media-type versioning — rejected as less
  transparent for a small internal API.
- **Backward-compatible evolution.** Additive optional fields (e.g. a future
  `quoteChar` on read-config) are non-breaking and stay in v1. Breaking changes — removing or
  renaming a field, or tightening a rule — trigger `/api/v2`, with v1 kept alive during
  migration so existing notebooks keep working.
- **Tie to SIFFT's own evolution.** SIFFT's `FORMATS`/`MODES` constants may grow (e.g. a new
  table format). Because the vocabulary is centralised and published via `config-options`,
  clients can discover new allowed values without a breaking change — the API version only
  needs to bump if the *contract shape* changes, not when the *vocabulary* grows.

---

## E. Introduction & Description seeds (10 marks)

- **What:** a RESTful web service that manages SIFFT's file-ingestion configuration.
- **Function:** create/read/update/delete read-configs and write-configs; publish the allowed
  vocabulary; test a config against a sample line.
- **Business purpose:** DVLA's Data Platform ingests 50+ file types daily across Drivers,
  Vehicles and Lookups via a scheduled Pull→Ingest→Cleanup Databricks job built on SIFFT.
  Configuration currently lives in code and requires a redeploy to change.
- **Criticality:** these feeds populate curated Delta tables used across the business; broken
  config breaks the daily pipeline. Managing config safely (validated, auditable, no redeploy)
  is therefore operationally significant.
- **End users:** the Data Platform team (config authors); indirectly, every downstream
  consumer of the ingested tables.
- **Requirements:** CRUD on both config types; server-side validation mirroring SIFFT rules;
  a controlled vocabulary; clear error semantics; versioned, secured endpoints.

---

## F. Screenshot checklist for the PoC section (20 marks)

For each of the five endpoints, capture one request/response per distinct HTTP code
(Postman recommended; H2 console at `/h2-console` for showing stored data):

- POST /read-configs → 201, 400, 409, 422
- PUT /read-configs/{id} → 200, 400, 404
- DELETE /read-configs/{id} → 204, 404
- POST /write-configs → 201, 400, 422
- POST /read-configs/{id}/test → 200, 404 (and 400 for missing sampleLine)

Each figure needs a caption describing what is shown and why. Use realistic DVLA-style data
(e.g. `Customers.csv`, `drivers.curated.customers`) so the demonstration data is credible.
