# Talking-Points Brief — Meeting with Adrian

*One page to speak from. Audience: tutor/assessor. Plain English. Read the diagram
(TO_BE_ARCHITECTURE) alongside this.*

---

## 1. The one-sentence pitch
"I want to build a small web service that manages the *configuration* for SIFFT — the
tool DVLA's Data Platform uses to load data files into tables — so that settings can be
changed through an API instead of by editing code and redeploying."

## 2. The problem (why this is worth doing)
- SIFFT ingests 50+ file types a day across Drivers, Vehicles and Lookups.
- How each file is read (its delimiter, columns) and written (which table, which format)
  is **hard-coded** in the pipeline today.
- Changing anything — a new file type, a changed delimiter — means **editing code and
  redeploying**. That's slow, needs developer access, and a mistake can break the daily job.

## 3. The proposal (what I'll build)
A RESTful web service that holds this configuration as **three resources**:
- **read-configs** — how an incoming file is read (pattern, delimiter, header, columns)
- **write-configs** — how its table is written (target table, format, mode)
- **config-options** — the list of allowed values (read-only)

Users create, update and delete configs over HTTP. The service **validates** every change,
so a bad setting is rejected immediately instead of breaking the pipeline later.

## 4. Why it's a viable WBP (ties to the rubric — say this plainly)
- **Real business need** in my own organisation (DVLA Data Platform).
- **Properly RESTful**: clean resources with GET/POST/PUT/DELETE, each justified.
- **Complex enough**: three different resources, server-side validation, full range of
  HTTP response codes — not just simple create-and-read.
- **Proof of concept already works**: five endpoints built in Spring Boot, 36 automated
  tests passing.

## 5. Honest scope — what the PoC does and does NOT do (say this up front)
- It **does**: manage the configuration, validate it, and return proper JSON and HTTP codes.
- It **does not**: run Spark or touch Databricks. The PoC *models the configuration layer*;
  it doesn't execute the actual data loading.
- Why that's fine: the assignment asks for a proof-of-concept web service, not a production
  system. Modelling the config layer is the right, self-contained slice to prove the design.

## 6. What I'd like from you (close the meeting)
- Does this sound like a suitable, complex-enough system for the WBP?
- Is the scope (config layer, not Spark) acceptable as a proof of concept?
- Any concerns about the resource/endpoint design before I finalise the report?

---
*Fallback line if I get stuck: "The short version is — SIFFT's settings live in code today;
I'm exposing them through a validated web service so they can be managed safely without a
redeploy."*
