# Session Handoff Summary — SIFFT WBP Assignments

> Purpose: a context handoff so another AI model (or a human) can pick up this work. Written
> 2026-10-26. Covers two linked university assignments about the same workplace system.
> Note: this summary is written from the working session's own context; it is not a retrieval of
> any stored session by ID (no such lookup tool was available).

## Who / context
- User: Jason Edwards (student ID 2545959), Swansea University.
- Two work-based portfolio (WBP) assignments about the same workplace system (DVLA Data Platform).
- Environment: Windows. Working directory: `C:\WSD_WBP`. A local Maven + Java 21 toolchain is
  present; builds should be run OFFLINE (`mvn -o`).

## The system
**SIFFT** (Spark Ingestion Framework For Tables) — a real DVLA Python library
(github.com/dvla/sifft) that ingests 50+ file types daily into Delta tables via a scheduled
Databricks job (Pull -> Ingest -> Cleanup, 10:30 UK). Per-feed configuration currently lives
**in code** (`FILE_MAPPINGS` + `TableWriteOptions`), so any change needs a code edit and redeploy.
**The improvement proposed in BOTH assignments: externalise that configuration into a managed
service so it can be changed as data.**

## The two assignments
- **CSF2753 (Web Services):** design a RESTful service (5–10 unique resources) and build a Spring
  Boot proof-of-concept (5 endpoints; >=2 parameterised and performing server-side processing;
  validated parameters; screenshots of each HTTP code per endpoint). Report max 3000 words.
  Draft due 28 Oct 2026, final 30 Nov 2026. AI permitted.
- **CSF2702 (Object-Oriented Design):** report with system class diagram (5–10 classes, detailed
  low-level, getters/setters/constructors), one improvement/extension, THREE design patterns each
  with its own 5–10 class diagram (30 marks), and an evaluation (20 marks). Report max 3000 words.
  Due 30 Nov 2026.

## AI-permission status (IMPORTANT)
- The CSF2702 written brief contains a clause "The use of generative AI is forbidden for the
  entirety of this assignment."
- This is OVERRIDDEN by an emailed clarification from the module contact (Adrian) naming both
  CSF2702_WBP and CSF2753_WBP as permitted. Saved as `C:\WSD_WBP\AI_Approval.txt` and
  `AI_Approval_update.txt`. KEEP BOTH as evidence; complete any AI-use declaration the module
  requires.

## Key decision: pitch everything to the TAUGHT level
Taught so far (verified from course materials):
- REST: basic Spring Boot (`FirstREST`/`FirstRESTv2` examples = one controller + one model +
  in-memory List + inline `ResponseEntity` validation). NO JPA, NO DTOs, NO global exception
  handler, NO repositories.
- UML class diagrams: "Intro to UML and Class Diagrams" and "Class Diagram Part 2" (visibility
  +/-/#/~, multiplicities, aggregation=open diamond, composition=filled diamond, inheritance=
  hollow triangle, dependency=dashed arrow, interfaces <<interface>>, abstract in italics).
- Design patterns: ONLY Strategy taught (SimUDuck example). Decorator scheduled next week; others
  later. So only Strategy can be fully written up now; the other two are provisional.

## Two code versions
- `C:\WSD_WBP\sifft-config-api` — ORIGINAL, over-engineered (Spring Data JPA + H2, DTO records,
  @RestControllerAdvice + custom exceptions, 36 MockMvc tests, ~24 types). ABOVE taught level.
  Kept only for reference; NOT the submission.
- `C:\WSD_WBP\sifft_config_rewrite` — CURRENT / SUBMITTED version, simplified to taught level:
  - `controllers/` ReadConfigController, WriteConfigController (thin; inject services)
  - `services/` ReadConfigService, WriteConfigService (in-memory List, validation, the test
    processing)
  - `models/` BaseConfig (abstract parent holding `id`), ReadConfig, WriteConfig, ConfigTestResult
  - pom uses Spring Boot 3.3.5 / Java 21 (only version cached for offline build). Teaching
    examples use SB 4.1.1 — switch coordinates if going online and a marker expects 4.1.1.
  - Builds + tests pass offline (`mvn -o clean test` -> BUILD SUCCESS, 1 contextLoads test).
  - 5 PoC endpoints: R3 POST /read-configs, R4 PUT /read-configs/{id}, R5 DELETE
    /read-configs/{id}, R6 POST /read-configs/{id}/test (server-side processing: splits sample
    line on stored delimiter), W3 POST /write-configs. Status codes produced: 200/201/204/400/
    404/409/422.

## Data sources used (all read-only)
- `C:\WSD_WBP\sifft-config-api\assignments\CSF2702_WBP (1).pdf` (text-extractable).
- CSF2753 brief: original PDF is image-only; user supplied `CSF200-2753 web Services.txt`.
- Lecture PDFs: `C:\WSD_WBP\L03/L04/L05` (REST); `C:\WSD_WBP\OOD_Lecture_info\` (UML Parts 1–2);
  `C:\WSD_WBP\Introduction to Design Patterns and Strategy Pattern-3up.pdf`;
  `C:\WSD_WBP\Decorator Pattern.pdf` (not yet used — Decorator taught next week).
- `C:\WSD_WBP\sifft-workshop.py` — SIFFT creators' Databricks demo notebook. Key evidence for the
  as-is config-in-code state and production details.
- `C:\sifft-main` — cloned SIFFT library (user cloned 25 Oct 2026). Verified files:
  `table_writing/config.py` (FORMATS/MODES/SCHEMA_MODES), `table_writing/models.py`
  (`TableWriteOptions` dataclass), `table_writing/format_handlers.py` (`FormatHandler` Protocol +
  Delta/Parquet/Orc handlers = genuine Strategy precedent), `file_processing/config.py`,
  `file_processing/registry.py` (read-side prioritised handler registry — a DIFFERENT mechanism),
  `table_writing/mode_handlers.py`.
- `C:\WSD_WBP\sifft-config-api-session.json` (~176 MB) — NEVER opened; unknown contents.

## Verified facts and corrections (use these; do not overstate)
- `WriteConfig` (Java) is a SIMPLIFIED SUBSET of SIFFT's `TableWriteOptions` (keeps targetTable,
  format, mode, schemaMode, mergeKeys, addSourceMetadata; omits partition_by, create_if_not_exists,
  source_metadata_columns, extra_metadata). NOT a 1:1 copy.
- `ReadConfig` (Java) is a NEW CONSOLIDATION of the job's `FILE_MAPPINGS` entry shape +
  `file_processing` defaults. SIFFT has NO single read-config class.
- `FILE_MAPPINGS` lives in the consuming JOB (shown in the workshop), NOT in the library — cite
  the workshop for it; cite the library for `TableWriteOptions`/vocabulary.
- Strategy-pattern evidence must cite `table_writing/format_handlers.py` (write-format handlers),
  NOT the read-side `register_handler(..., priority=10)`.
- `FormatHandler` is a Python Protocol (structural interface), NOT a Java interface — phrase as
  "a Protocol, Python's form of an interface".
- How the config was identified as "what to change": from the WORKSHOP + project docs (and later
  confirmed against the cloned library), NOT by reverse-engineering the library in the first
  instance. Report wording should reflect that honestly.

## Output produced
In `C:\WSD_WBP\sifft-config-api\docs\report\`:
- CSF2753 section drafts (Intro, Web Service Design, JSON I/O, Reflection, Security/Maint/Version),
  Postman runbook, report docx V0.1–V0.3 (CSF2702).
In `C:\WSD_WBP\sifft_config_rewrite\docs\`:
- `CSF2702_Sections1-4_clean.md`, `CSF2702_Sections5-6_clean.md` (~2400 words total; under 3000
  cap; duck example REMOVED; Section 5.2 structured to the High-First rubric).
- `CSF2702_Section3_ClassDescriptions_plain.md` (plain-English class descriptions).
- `4.5 evidence update note.md`, `5.2 evidence update note.md` (cite format_handlers.py).
- `CSF2753_identification_chain.md`, `CSF2753_workshop_supplement.md`,
  `CSF2702_workshop_supplement.md`, `SIFFT_WORKSHOP_EVIDENCE.md`.
UML:
- `C:\WSD_WBP\sifft_config_rewrite\sifft_rewrite_uml\SYSTEM_CLASS_DIAGRAM.drawio` (8 classes,
  opens in draw.io — must be pure ASCII and `<diagram>` must directly contain `<mxGraphModel>`,
  else draw.io throws an atob error).
- `C:\WSD_WBP\sifft-config-api\docs\uml\` — Mermaid/PlantUML system diagrams + `PATTERN_1_STRATEGY`.

## Outstanding work
- CSF2753: reconcile JSON I/O section (rewrite returns PLAIN-TEXT errors, not a JSON ApiError
  object, so the "uniform JSON error" text is now inaccurate); capture ~14 Postman screenshots
  (one per HTTP code per endpoint — the critical path for the 28 Oct draft); single consistent
  reference list.
- CSF2702: develop the 2 remaining patterns (Decorator after its lecture + one more) each with a
  5–10 class diagram for the full 30 marks (currently Strategy worked + Decorator/Observer
  provisional); insert the Strategy diagram as Figure 2; write Section 2 (diagram) and a single
  consistent numbered reference list; align "8 classes/7 relationships" wording with the inserted
  diagram.
- Both reports are at/over the 3000-word cap in places — keep trimming.
- User has a DRAFT REVIEW with Adrian on Wednesday (week of 26 Oct).

## Candidate references gathered (verify before citing)
- Gamma, Helm, Johnson, Vlissides (1994) Design Patterns (GoF book) and the 1993 ECOOP paper.
- Christopoulou et al. (2012) "Automated refactoring to the Strategy design pattern",
  Information and Software Technology — directly supports replacing conditionals with Strategy.
  (Volume/pages/DOI were inferred from a Scholar snippet — MUST be verified against the source.)

## Guardrails that shaped the work (keep applying)
- Keep code and report AT the taught level; avoid concepts the user cannot defend.
- Keep the diagram consistent with the submitted code.
- Patterns in CSF2702 are PROPOSED designs ("how it could be applied"), not in the prototype code.
- Honesty on sourcing: cite workshop vs library correctly; don't claim library source analysis
  where it wasn't the basis; verify citation details before use.
