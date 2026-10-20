# OOD System Class Diagram — Blueprint (planning reference)

> **Status / AI note:** This is *planning reference material* extracted from the existing
> Java source — not a submittable diagram. The actual UML artefact is on hold pending
> Adrian's confirmation that the generative-AI waiver applies to CSF2702. Once confirmed,
> this blueprint converts directly into the diagram.

Scope: 8 classes selected from the 24 in `sifft-config-api` (brief requires 5–10), chosen so
every class has genuine getters/setters/constructors and the relationship set is rich.

UML visibility: `+` public, `-` private, `~` package, `#` protected.

---

## Class 1 — ReadConfig  «entity»
Attributes (all `-` private):
- `- id : String`
- `- pattern : String`
- `- delimiter : String`
- `- header : String`
- `- columns : List<String>`
- `- maxCorruptPercent : double`
- `- writeConfigId : String`

Operations:
- `+ ReadConfig()`  «constructor»
- `+ getId() : String` / `+ setId(String) : void`
- `+ getPattern() : String` / `+ setPattern(String) : void`
- `+ getDelimiter() : String` / `+ setDelimiter(String) : void`
- `+ getHeader() : String` / `+ setHeader(String) : void`
- `+ getColumns() : List<String>` / `+ setColumns(List<String>) : void`
- `+ getMaxCorruptPercent() : double` / `+ setMaxCorruptPercent(double) : void`
- `+ getWriteConfigId() : String` / `+ setWriteConfigId(String) : void`

## Class 2 — WriteConfig  «entity»
Attributes (all `-` private):
- `- id : String`
- `- targetTable : String`
- `- format : String`
- `- mode : String`
- `- schemaMode : String`
- `- partitionBy : List<String>`
- `- mergeKeys : List<String>`
- `- addSourceMetadata : boolean`

Operations:
- `+ WriteConfig()`  «constructor»
- `+ getId()/setId`, `+ getTargetTable()/setTargetTable`, `+ getFormat()/setFormat`,
  `+ getMode()/setMode`, `+ getSchemaMode()/setSchemaMode`,
  `+ getPartitionBy()/setPartitionBy`, `+ getMergeKeys()/setMergeKeys`,
  `+ isAddSourceMetadata()/setAddSourceMetadata`  (full getter/setter pairs + ctor)

## Class 3 — ReadConfigService
Attributes:
- `- repository : ReadConfigRepository`
- `- writeConfigRepository : WriteConfigRepository`
- `- IDENTIFIER : Pattern` «static final»

Operations:
- `+ ReadConfigService(ReadConfigRepository, WriteConfigRepository)` «constructor»
- `+ findAll(patternFilter : String) : List<ReadConfig>`
- `+ findById(id : String) : ReadConfig`
- `+ create(request : ReadConfigRequest) : ReadConfig`
- `+ replace(id : String, request : ReadConfigRequest) : ReadConfig`
- `+ delete(id : String) : void`
- `+ test(id : String, sampleLine : String) : ConfigTestResponse`
- `- validate(r : ReadConfigRequest) : void`
- `- apply(entity : ReadConfig, request : ReadConfigRequest) : void`

## Class 4 — WriteConfigService
Attributes:
- `- repository : WriteConfigRepository`
- `- IDENTIFIER : Pattern` «static final»
- `- MAX_IDENTIFIER_LENGTH : int` «static final»

Operations:
- `+ WriteConfigService(WriteConfigRepository)` «constructor»
- `+ findAll() : List<WriteConfig>`
- `+ findById(id : String) : WriteConfig`
- `+ create(request : WriteConfigRequest) : WriteConfig`
- `+ replace(id : String, request : WriteConfigRequest) : WriteConfig`
- `+ delete(id : String) : void`
- `- validate(r : WriteConfigRequest) : void`
- `- apply(entity : WriteConfig, request : WriteConfigRequest) : void`
- `- validateIdentifierList(values : List<String>, field : String) : void`

## Class 5 — ReadConfigRepository  «interface»
Stereotype: extends `JpaRepository<ReadConfig, String>`
Operations:
- `+ findByPattern(pattern : String) : Optional<ReadConfig>`
- `+ existsByPattern(pattern : String) : boolean`
- (inherited: `save`, `findById`, `findAll`, `deleteById`, `existsById`)

## Class 6 — WriteConfigRepository  «interface»
Stereotype: extends `JpaRepository<WriteConfig, String>`
Operations: (inherited CRUD from JpaRepository)

## Class 7 — ReadConfigController  «RestController»
Attributes:
- `- service : ReadConfigService`

Operations:
- `+ ReadConfigController(ReadConfigService)` «constructor»
- `+ list(pattern : String) : List<ReadConfigResponse>`
- `+ getOne(id : String) : ReadConfigResponse`
- `+ create(request : ReadConfigRequest) : ResponseEntity<ReadConfigResponse>`
- `+ replace(id : String, request : ReadConfigRequest) : ReadConfigResponse`
- `+ delete(id : String) : ResponseEntity<Void>`
- `+ test(id : String, request : ConfigTestRequest) : ConfigTestResponse`

## Class 8 — ConfigVocabulary  «utility / constants»
Attributes (all `+ static final`):
- `+ DELIMITERS : List<String>`
- `+ HEADER_OPTIONS : List<String>`
- `+ FORMATS : Set<String>`
- `+ MODES : Set<String>`
- `+ SCHEMA_MODES : Set<String>`
- `+ MAX_CORRUPT_PERCENT_DEFAULT : double`
- `+ FORMAT_DELTA : String`
- `+ MODE_MERGE : String`

Operations:
- `- ConfigVocabulary()` «private constructor — prevents instantiation»

---

## Relationships (aim: ~8–10, matching the class count)

| From | To | UML type | Multiplicity | Notation |
|---|---|---|---|---|
| ReadConfigController | ReadConfigService | association (holds ref) | 1 → 1 | solid line, arrow to Service |
| ReadConfigService | ReadConfigRepository | association | 1 → 1 | solid line, arrow |
| ReadConfigService | WriteConfigRepository | dependency (validates reference) | 1 → 1 | dashed arrow |
| WriteConfigService | WriteConfigRepository | association | 1 → 1 | solid line, arrow |
| ReadConfigRepository | ReadConfig | association (manages) | 1 → 0..* | solid line, arrow to entity |
| WriteConfigRepository | WriteConfig | association (manages) | 1 → 0..* | solid line, arrow |
| ReadConfig | WriteConfig | association (via writeConfigId) | 0..* → 0..1 | solid line, labelled "writes via" |
| ReadConfigService | ConfigVocabulary | dependency (validates against) | → | dashed arrow |
| WriteConfigService | ConfigVocabulary | dependency (validates against) | → | dashed arrow |
| ReadConfig | «columns : String» | composition | 1 → 1..* | filled diamond at ReadConfig |

Notes for correct notation (what OOD markers look for):
- Use a **filled diamond** (composition) for the owned value-collections (columns, mergeKeys,
  partitionBy) — they have no independent life outside their owning config.
- Use **dashed arrows** for dependencies (Service → Vocabulary; Service → the other
  Repository) versus **solid arrows** for held-reference associations.
- Put **multiplicities at both ends** where meaningful (e.g. Repository `1` — `0..*` entity).
- Mark interfaces with the `«interface»` stereotype; `ConfigVocabulary` with `«utility»`.

## Why this satisfies the High-First criterion (for the Class Descriptions section)
- **5–10 classes:** exactly 8.
- **Setters/getters/constructors present:** ReadConfig and WriteConfig carry full sets;
  services and controller have constructors (dependency injection).
- **Implementable by a developer:** it is a faithful reverse-engineering of working,
  tested Java — so it is provably implementable.
- **Full syntax:** associations, dependencies, composition, multiplicities, visibility,
  stereotypes, interfaces all present.
