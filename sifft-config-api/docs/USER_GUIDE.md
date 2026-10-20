# How to Run and Use the Config Web Service

A step-by-step guide to start the service and use it as a user. No prior Java knowledge
assumed. Base URL once running: `http://localhost:8080`.

---

## Step 1 — Start the service

Open a terminal **in the project folder** and start it. Pick ONE method.

**Option A — Maven (terminal):**
```
cd C:\WSD_WBP\sifft-config-api
set JAVA_HOME=C:\Program Files\Java\jdk-25
mvnw spring-boot:run
```
(If `mvnw` isn't present, use `mvn spring-boot:run`.)

**Option B — VS Code:** open the folder, open
`src/main/java/uk/gov/dvla/siftconfig/SiftConfigApiApplication.java`, and click the
**Run** link above `public static void main`.

The service is ready when you see a line like:
`Started SiftConfigApiApplication in X seconds` and `Tomcat started on port 8080`.

To stop it: press `Ctrl+C` in the terminal (or the stop button in VS Code).

---

## Step 2 — Check it's alive

In a browser, go to:  `http://localhost:8080/api/v1/health`
You should see: `{"status":"UP","service":"sifft-config-api", ...}`

---

## Step 3 — See the allowed values (GET)

Browser:  `http://localhost:8080/api/v1/config-options`
This returns the delimiters, formats, modes and schema modes you're allowed to use when
creating configs. (Reading these in a browser works because it's a GET request.)

---

## Step 4 — Create a WRITE config (POST)

Creating/updating needs to send JSON, so a browser won't do — use **Postman** (easiest, has
a UI) or **curl** in a terminal.

**What you're doing:** telling the system how a table should be written.

**Postman:** New request → method `POST` → URL `http://localhost:8080/api/v1/write-configs`
→ Body → raw → JSON → paste:
```json
{
  "targetTable": "drivers.curated.customers",
  "format": "delta",
  "mode": "append",
  "schemaMode": "strict",
  "addSourceMetadata": true
}
```
Send. You should get **201 Created** and a response containing an `"id"`. **Copy that id.**

**curl (PowerShell):**
```
curl -Method POST http://localhost:8080/api/v1/write-configs `
  -ContentType "application/json" `
  -Body '{"targetTable":"drivers.curated.customers","format":"delta","mode":"append","schemaMode":"strict","addSourceMetadata":true}'
```

---

## Step 5 — Create a READ config (POST)

**What you're doing:** telling the system how an incoming file should be read, and linking it
to the write config from Step 4 (paste its id into `writeConfigId`).

`POST http://localhost:8080/api/v1/read-configs`
```json
{
  "pattern": "Customers\\.csv",
  "delimiter": ",",
  "header": "auto",
  "columns": ["customer_id", "name", "age", "status"],
  "writeConfigId": "PASTE-THE-WRITE-CONFIG-ID-HERE"
}
```
Expect **201 Created** with the stored read config (note its `id` too).

---

## Step 6 — Read it back (GET)

- All read configs:  `http://localhost:8080/api/v1/read-configs`
- One by id:  `http://localhost:8080/api/v1/read-configs/{id}`  (paste the id)

---

## Step 7 — Test the config against a sample line (POST)

**What you're doing:** checking the config actually splits a line into the right number of
fields — real server-side processing.

`POST http://localhost:8080/api/v1/read-configs/{id}/test`
```json
{ "sampleLine": "1,Alice,34,active" }
```
Expect **200 OK** with `"detectedFieldCount": 4`, `"expectedFieldCount": 4`,
`"matches": true`.

---

## Step 8 — Update a config (PUT)

`PUT http://localhost:8080/api/v1/read-configs/{id}` with a full JSON body (same shape as
Step 5). Replaces the config. Expect **200 OK**.

---

## Step 9 — Delete a config (DELETE)

`DELETE http://localhost:8080/api/v1/read-configs/{id}`  → **204 No Content** (empty body).
GET it again and you'll get **404 Not Found**, confirming it's gone.

---

## Step 10 — See the stored data (optional)

Browser:  `http://localhost:8080/h2-console`
- JDBC URL: `jdbc:h2:mem:siftconfig`
- User: `sa`  — Password: (leave blank) — Connect.
- Run `SELECT * FROM READ_CONFIG;` or `SELECT * FROM WRITE_CONFIG;` to see your rows.

---

## Try the ERRORS too (great for screenshots)

| Try this | Expect |
|---|---|
| POST read-config with `"delimiter": ";;"` | 400 (delimiter must be single char) |
| POST write-config with `"format": "avro"` | 422 (format not allowed) |
| POST write-config `"mode":"merge"` with no `mergeKeys` | 422 (merge needs keys) |
| POST the same read-config pattern twice | 409 (duplicate) |
| GET `/read-configs/does-not-exist` | 404 |

---

## Notes
- The database is **in-memory**: everything you create disappears when you stop the service.
  That's intentional for a demo — restart gives a clean slate.
- All requests/responses are JSON. For GETs a browser is fine; for POST/PUT/DELETE use
  Postman or curl because they send a body / non-GET method.
