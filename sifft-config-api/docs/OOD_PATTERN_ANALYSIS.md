# OOD Design Patterns — Selection Analysis (planning reference)

> **Status / AI note:** Planning/analysis only — *not* submittable deliverables. The actual
> pattern class diagrams (30-mark section) and the written evaluation (20 marks) are on hold
> pending Adrian's confirmation that the generative-AI waiver covers CSF2702. This document
> helps you *decide and justify* the choice; it is the same kind of planning aid as the
> Web Services supporting-material doc.

Allowed patterns (from the brief): **Strategy, Decorator, Singleton, Composite, Builder,
Observer, MVC**. You must pick **three**, each with its own class diagram, mostly different
classes, with real justification against the SIFFT improvement.

---

## The improvement being supported (recap, in OO terms)
Today SIFFT's ingestion configuration (how files are read / how tables are written) and its
format/mode handling are **rigidly wired** — config is hard-coded in the notebook, and
behaviour is selected with conditionals. The improvement externalises configuration into a
managed service and makes the read/write behaviour **pluggable and extensible**. The OO goals
are: reduce coupling, raise cohesion, improve extensibility and testability.

---

## Candidate patterns ranked by fit

### TIER 1 — strongest, genuinely present in the real SIFFT code

#### 1. Strategy  ⭐ (recommend)
- **Where it lives:** SIFFT's `FormatHandler` Protocol with `DeltaFormatHandler`,
  `ParquetFormatHandler`, `OrcFormatHandler` (confirmed in `table_writing/format_handlers.py`).
  Also the write-**mode** handlers (append/overwrite/merge/errorIfExists).
- **Problem it addresses:** choosing *how* to write a table (by format or mode) without a
  growing `if/elif` chain; adding a new format/mode should not modify existing code
  (Open–Closed Principle).
- **Applied to the improvement:** `WriteConfig.format` selects a `FormatStrategy` at runtime;
  adding ORC-v2 or Iceberg means adding one strategy class, not editing a switch.
- **Classes for its diagram (~6):** `WriteService` (context), `FormatStrategy` «interface»,
  `DeltaStrategy`, `ParquetStrategy`, `OrcStrategy`, `WriteConfig`.
- **Advantages:** eliminates conditionals; each algorithm isolated & unit-testable; easy to
  extend. **Disadvantages:** more classes; client/context must know which strategy to pick.

#### 2. Builder  ⭐ (recommend)
- **Where it lives:** `TableWriteOptions` — many optional fields (format, mode, partitionBy,
  schemaMode, mergeKeys, addSourceMetadata, extra metadata). Today constructed positionally /
  with many defaults.
- **Problem it addresses:** constructing a complex object with many optional parameters
  without telescoping constructors; enforcing valid combinations at build time
  (e.g. merge ⇒ mergeKeys + delta).
- **Applied to the improvement:** a `WriteConfigBuilder` assembles a valid `WriteConfig`
  step by step and runs the cross-field validation in `build()` — mirrors the real
  `validate_write_options` rules.
- **Classes for its diagram (~5):** `WriteConfigBuilder`, `WriteConfig` (product),
  `WriteConfigService` (director/caller), `ConfigVocabulary`, (optionally) a
  `ValidationException`.
- **Advantages:** readable construction; invalid objects can't be built; immutable product
  possible. **Disadvantages:** extra builder class; overkill for simple objects.

#### 3. Singleton  ⭐ (recommend — with a critical edge for the evaluation)
- **Where it lives:** SIFFT's **global registries** — the module-level handler/mode/constraint
  registries (`_MODE_HANDLERS`, `_HANDLER_REGISTRY`, `_CONSTRAINT_VALIDATORS`) and the
  `ConfigVocabulary` constants.
- **Problem it addresses:** one shared, globally-accessible instance (the registry / the
  vocabulary) with a single point of truth.
- **Applied to the improvement:** a `HandlerRegistry` or `ConfigVocabulary` as a controlled
  Singleton instead of loose module globals.
- **Classes for its diagram (~5):** `HandlerRegistry` «singleton», `FormatStrategy`,
  the strategies it stores, a client service.
- **Advantages:** single source of truth; controlled access. **Disadvantages:** global
  mutable state → hidden coupling and test-isolation problems — *exactly the hazard we
  flagged in SIFFT's registries*. This makes Singleton your best **evaluation** target:
  you can argue it's appropriate for the read-only vocabulary but *risky* for the mutable
  registry, and show why Strategy+Builder are safer for the core change.

### TIER 2 — viable, improvement-driven (use if you want a different angle)

#### 4. Decorator
- **Where it fits:** wrapping a file processor with cross-cutting steps — tracking,
  PD redaction (the real job layer does HMAC redaction), source-metadata — as stackable
  decorators around a core `Processor`.
- **Problem:** add responsibilities to an object dynamically without subclass explosion.
- **Classes (~6):** `Processor` «interface», `BaseProcessor`, `TrackingDecorator`,
  `RedactionDecorator`, `SourceMetadataDecorator`, client.
- **Pros:** flexible composition; single-responsibility per decorator. **Cons:** many small
  classes; order-sensitivity.

#### 5. Observer
- **Where it fits:** the deferred `confirm_processed` / tracking-on-success flow — when a
  pipeline step succeeds, observers (tracking recorder, metrics, notifier) react.
- **Problem:** notify multiple dependents of a state change without tight coupling.
- **Classes (~5):** `IngestionSubject`, `Observer` «interface», `TrackingObserver`,
  `MetricsObserver`, client.
- **Pros:** decoupled event handling. **Cons:** harder to trace flow; ordering/lifecycle.

#### 6. Composite
- **Where it fits:** the CSVW constraint validation — column constraints + datatype
  constraints + primary-key constraints aggregate into one report (a tree of validators).
- **Problem:** treat individual and composed objects uniformly.
- **Classes (~6):** `Constraint` «component», `LeafConstraint` variants, `CompositeConstraint`,
  `ConstraintReport`.
- **Pros:** uniform tree handling. **Cons:** can over-generalise; type-safety trade-offs.

### TIER 3 — possible but weaker here
- **MVC** — your Spring Boot controller/service/model is MVC-flavoured, but it's an
  architectural style rather than a focused refactor of a class, so it's harder to show as a
  tight 5–10 class *pattern* diagram tied to the improvement. Usable but less crisp than the
  others. Suggest avoiding unless you want the web-layer angle.

---

## Recommended set of THREE and why

**Strategy + Builder + Singleton.**

Reasons:
1. **All three are genuinely present in SIFFT** — authentic evidence, not contrived, which
   the rubric's "supported by evidence from the existing system" rewards.
2. **They are mostly different classes** (satisfies the "diagrams should be mostly different"
   rule): Strategy centres on the format/mode handlers; Builder on `WriteConfig` construction;
   Singleton on the registry/vocabulary. Minimal overlap (`WriteConfig`/`ConfigVocabulary`
   appear in two, which the brief explicitly allows).
3. **They set up a strong evaluation (20 marks):** you can argue **Strategy is the most
   effective** change (best extensibility/testability, lowest coupling), **Builder** is a
   solid supporting improvement (safer construction), and **Singleton** is a double-edged
   choice (fine for the read-only vocabulary, risky as mutable global state) — a genuinely
   critical comparison rather than three "all good" write-ups.

Alternative if you want a more "improvement/extension" flavour than "existing code":
**Strategy + Decorator + Observer** (pluggable write behaviour + stackable processing steps +
event-driven tracking). Richer narrative, but the classes are more invented than evidenced.

---

## Evaluation angle to pre-plan (20 marks)
Likely conclusion to argue: **Strategy** is the most effective for the core improvement —
- *Coupling:* removes format/mode conditionals; context depends on an interface, not concretes.
- *Extensibility:* new format/mode = new class (Open–Closed), no edits to tested code.
- *Testability:* each strategy tested in isolation (we already proved this pays off — the
  mode-handler structure made our validation testable).
- *Trade-off:* more classes / strategy-selection responsibility — acceptable for the gain.
Contrast with Singleton's hidden-global-state risk and Builder's narrower (construction-only)
benefit.

---

## Mapping to rubric
| Section | Marks | Covered by |
|---|---|---|
| Design Pattern 1/2/3 (explain + class diagram each) | 30 | Strategy, Builder, Singleton sections above (diagrams pending waiver) |
| Evaluation | 20 | the evaluation angle above |
| Improvement/Extension | 10 | the OO-framed improvement recap |
(Intro/Description, system class diagram, class descriptions handled in OOD_CLASS_BLUEPRINT.md.)
