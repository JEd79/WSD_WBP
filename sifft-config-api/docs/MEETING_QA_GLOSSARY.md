# Q&A Prep + Glossary — Back-up Sheet

*Keep this to hand during the meeting. Answers are in plain English, in your own voice.*

---

## Part 1 — Likely questions and how to answer them

**Q: Is this complex enough for the WBP?**
A: Yes — it's three different resources, not one repeated CRUD table. Each has its own
shape and rules, there's real server-side validation, and it uses the full range of HTTP
methods and response codes. The proof-of-concept already has five endpoints and 36 passing
tests.

**Q: Why a web service? Why not just keep editing the config in code?**
A: Editing code means a redeploy every time, needs developer access, and a typo can break
the daily pipeline for 50+ feeds. A web service lets the Data Platform team change settings
safely through a validated API — and the service rejects a bad setting immediately, instead
of it failing later when the batch runs.

**Q: Isn't this just basic CRUD?**
A: The create/read/update/delete actions are the backbone, but the value is in the
*validation* behind them — the service enforces the same rules SIFFT itself relies on (for
example, a delimiter must be a single character; a table name can only contain safe
characters). So it's CRUD plus meaningful server-side processing.

**Q: Does it actually load data / run Spark?**
A: No — and I'm deliberately clear about that. The proof-of-concept manages the
*configuration* layer. It doesn't run Spark or connect to Databricks. That's the right slice
to prove the web-service design without needing a Spark cluster, which is out of scope for a
PoC.

**Q: Where does the configuration live?**
A: In the service's own small database. For the PoC I use an in-memory database so it's easy
to run and demonstrate. In a real deployment the Databricks notebooks would read the config
the service manages.

**Q: What happens if someone sends bad data?**
A: The service validates it and returns a clear error with the right HTTP code — 400 if the
request is malformed, 422 if a value isn't allowed, 404 if the thing doesn't exist, 409 if
it would create a duplicate. I can show each of these with a screenshot.

**Q: How do you know it works?**
A: 36 automated tests cover every endpoint — both the success cases and every error case.
The tests actually caught a real bug during development, which I fixed.

**Q: Is it secure?**
A: In the report I cover it in context — only authorised staff should change config, the
service validates table names to prevent injection into the database commands, and secrets
(like credentials) are never part of the config. The PoC focuses on the design; production
security is discussed rather than fully implemented.

**Q: How would you change it later without breaking users?**
A: The URLs are versioned (/api/v1/...). Small additions stay in v1; anything that would
break existing users would go to v2, with v1 kept running during the switch-over.

---

## Part 2 — Plain-English glossary (so no term catches you out)

| Term | Say it like this |
|---|---|
| **Web service / API** | A program other programs talk to over the web, by sending requests and getting responses. |
| **REST / RESTful** | A common, tidy style for web services: you act on "things" (resources) using standard web verbs. |
| **Resource** | A "thing" the service manages — here, a read-config, a write-config, or the options list. |
| **Endpoint** | One specific address + action the service offers, e.g. "create a read-config". |
| **HTTP methods** | The verbs: GET (fetch), POST (create), PUT (replace), DELETE (remove). |
| **HTTP status codes** | The numeric result: 200 OK, 201 Created, 204 No Content, 400/404/409/422 errors, 500 server error. |
| **JSON** | The simple text format used for the request and response data. |
| **Parameterised endpoint** | An endpoint that takes an input, e.g. an id in the URL or a body to process. |
| **Validation** | Checking a request is allowed before accepting it. |
| **Idempotent** | Doing it twice has the same effect as once (true for PUT and DELETE; not for POST). |
| **CRUD** | Create, Read, Update, Delete — the four basic operations on data. |
| **Spring Boot** | The Java framework I used to build the service. |
| **SIFFT** | DVLA's existing Python tool that reads files and loads them into tables. |
| **Delimiter** | The character separating columns in a file, e.g. a comma or a pipe. |
| **Delta table / Databricks** | The storage and platform where SIFFT writes the loaded data. |
| **Proof of concept (PoC)** | A small working version built to prove the idea, not the finished product. |

---

## Part 3 — Two things to remember to say
1. **Lead with the honest scope** ("it manages config; it doesn't run Spark") — it reads as
   competence, not a gap.
2. **Ask the closing questions** (brief §6) so Adrian's agreement is on record — that's the
   "meaningful engagement" the rubric rewards.
