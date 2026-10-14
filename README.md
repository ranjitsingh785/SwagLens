# Swagger Coverage Checker for Burp Suite

A Burp Suite extension (Montoya API) that answers one question:

> **Which of my documented API endpoints did my testing actually hit?**

Load an OpenAPI 3.x or Swagger 2.0 spec, then browse, crawl, scan, or fuzz as
usual. The extension passively watches every request Burp sends and shows a
coverage report, with the **missing** endpoints listed first.

---

## Table of contents

1. [How it works](#how-it-works)
2. [Features](#features)
3. [Read this first: matching rules and gotchas](#read-this-first-matching-rules-and-gotchas)
4. [Requirements](#requirements)
5. [Build](#build)
6. [Install in Burp](#install-in-burp)
7. [Usage](#usage)
8. [Try it with the example spec](#try-it-with-the-example-spec)
9. [Troubleshooting](#troubleshooting)
10. [Project layout](#project-layout)
11. [Regenerating the project with `bootstrap.sh`](#regenerating-the-project-with-bootstrapsh)
12. [Known limitations](#known-limitations)

---

## How it works

```
 Spec file (.yaml/.json) ──► SpecParser ──► Set of documented endpoints
                                              (METHOD + path template)
                                                       │
 Burp traffic (Proxy, Repeater, Scanner, ──► CoverageHttpHandler ──► EndpointRegistry
 Intruder, ...) + existing Site Map          SiteMapAnalyzer           │
                                                                       ▼
                                                     "Swagger Coverage" tab
                                                     (Missing ▲ / Covered ▼)
```

1. `SpecParser` reads the spec with `swagger-parser` and extracts every
   `METHOD /path/template` pair (GET, POST, PUT, DELETE, PATCH, OPTIONS, HEAD, TRACE).
2. Each template is compiled to a regex. `{param}` matches exactly one path segment.
3. `CoverageHttpHandler` sees every outgoing request from every Burp tool, strips
   the query string, and marks the first matching endpoint as **covered**.
4. `SiteMapAnalyzer` (via the **Re-analyze Site Map** button) does the same for
   traffic already stored in Burp's site map.
5. The extension never modifies, delays, or drops traffic.

---

## Features

- **Passive coverage tracking**: every request Burp sends is checked against the spec.
- **Missing endpoints first**: red `❌ MISSING` rows sort above green `✅ Covered` rows.
- **Retroactive site map scan**: credits traffic captured before you loaded the spec.
- **Path parameter matching**: `/pets/{petId}` matches `/pets/42`.
- **OpenAPI 3.x and Swagger 2.0**, YAML or JSON, through `swagger-parser`.
- **Read-only**: Burp's traffic is never altered.

---

## Read this first: matching rules and gotchas

Most "why is this endpoint still missing?" questions are answered here.

| Rule | What it means for you |
|---|---|
| **The table does not auto-refresh.** | Requests are recorded in the background, but the table only redraws when you click **Load**, **Re-analyze Site Map**, or **Reset Observed**. Click **Re-analyze Site Map** whenever you want an up-to-date view. |
| **The spec's `servers` URL / `basePath` is NOT applied.** | Spec paths are matched against the **full** request path. If the spec says `servers: [{url: https://host/api/v1}]` with path `/pets`, a real request to `/api/v1/pets` will **not** match. Fix: use a copy of the spec whose paths include the prefix (e.g. `/api/v1/pets`). |
| **The host is ignored.** | Any request with a matching method and path counts, whatever its host, including unrelated sites passing through the proxy. Use a dedicated Burp project or browser profile for accurate numbers. |
| **Status codes are ignored.** | A request counts as soon as it is *sent*. A `401`, `404`, or `500` response still marks the endpoint covered. "Covered" means "requested", not "tested successfully". |
| **Methods must match exactly.** | `GET /pets` does not cover `POST /pets`. CORS preflight requests from a browser can mark `OPTIONS` endpoints covered. |
| **Matching is case-sensitive.** | `/Pets` does not match `/pets`. |
| **Trailing slash is optional.** | `/pets` and `/pets/` both match `/pets`. |
| **Query strings are ignored.** | `/pets?limit=10` matches `/pets`. |
| **`{param}` matches exactly one segment.** | `/files/{name}` matches `/files/a.txt` but not `/files/dir/a.txt`. Empty values (`/pets/`) do not match `/pets/{petId}`. |
| **Overlapping templates are ambiguous.** | If the spec has both `/pets/mine` and `/pets/{petId}`, a request to `/pets/mine` credits only **one** of them, and which one is not guaranteed. Request both explicitly if you need both covered. |
| **No spec loaded = nothing recorded.** | Traffic sent before a spec is loaded is not tracked live. Only traffic in the site map can be recovered later (see below). |

### What "Re-analyze Site Map" can and cannot recover

- ✅ Proxy traffic and crawler/scanner discoveries are stored in Burp's site map, so they can be credited after the fact.
- ⚠️ Repeater and Intruder requests usually do **not** end up in the site map. Load the spec **before** using those tools.

---

## Requirements

| Component | Version | Notes |
|---|---|---|
| Burp Suite | Professional or Community, **2024.x or newer** (latest recommended) | Community has no crawler/scanner, but Proxy, Repeater, and Intruder are tracked. |
| JDK | **17 or newer** | Needed only to **build**. Burp ships its own Java runtime. |
| Gradle | **7.6 – 8.x** (8.14.3 recommended) | **Not Gradle 9+.** `build.gradle` uses the top-level `sourceCompatibility` property, which Gradle 9 removed. |
| Internet access | During the first build | To download dependencies from Maven Central. |

> **Note:** this repository does **not** include a Gradle wrapper (`gradlew`).
> Running `./gradlew` before creating one fails with `no such file or directory`.
> See [step 3](#3-optional-but-recommended-create-the-gradle-wrapper) below.

---

## Build

### 1. Install a JDK (17+)

Check what you already have:

```bash
java -version     # must print 17 or higher
javac -version    # must exist; a JRE alone is not enough
```

On macOS, `Unable to locate a Java Runtime` means no JDK is installed. Pick one option:

```bash
# macOS (Homebrew)
brew install openjdk@17
sudo ln -sfn "$(brew --prefix)/opt/openjdk@17/libexec/openjdk.jdk" \
     /Library/Java/JavaVirtualMachines/openjdk-17.jdk

# Debian / Ubuntu
sudo apt install openjdk-17-jdk

# Any OS (SDKMAN, https://sdkman.io)
sdk install java 17.0.12-tem   # run 'sdk list java' if this id is no longer offered
```

Windows: install Temurin 17 from <https://adoptium.net> and tick "Set JAVA_HOME".

### 2. Install Gradle 8.x

```bash
# Any OS (SDKMAN), recommended because it pins an exact version
sdk install gradle 8.14.3

gradle --version   # must show Gradle 8.x and JVM 17+
```

If `gradle --version` shows 9.x, install 8.x alongside it and use that one.

### 3. (Optional but recommended) Create the Gradle wrapper

Do this once. After that, every build uses the same Gradle version, even on machines without Gradle installed:

```bash
cd swagger-coverage-burp
gradle wrapper --gradle-version 8.14.3
```

From then on, use `./gradlew` (macOS/Linux) or `gradlew.bat` (Windows) in place of `gradle`.

### 4. Build the JAR

```bash
cd swagger-coverage-burp
gradle clean jar          # or: ./gradlew clean jar
```

Output (a single "fat" JAR with all dependencies bundled):

```
build/libs/swagger-coverage-1.0.0.jar
```

Verify it:

```bash
ls -lh build/libs/
unzip -l build/libs/swagger-coverage-1.0.0.jar | grep SwaggerCoverageExtension
# expected: com/example/swaggercoverage/SwaggerCoverageExtension.class
```

Prefer `jar` over `build`. `build` also runs the (empty) test suite and the `application` plugin's distribution tasks. No `mainClass` is configured, so those tasks add nothing for Burp and may fail on some Gradle versions.

---

## Install in Burp

1. Open Burp Suite.
2. Go to **Extensions → Installed → Add**.
3. Set **Extension type** to **Java**.
4. Click **Select file…** and choose `build/libs/swagger-coverage-1.0.0.jar`.
5. Click **Next**. The **Output** tab should show:
   ```
   Swagger Coverage Checker loaded. Open the 'Swagger Coverage' tab to begin.
   ```
   The **Errors** tab should be empty.
6. A new top-level tab named **Swagger Coverage** appears.

To update the extension after rebuilding, remove it and add it again (or untick and re-tick **Loaded**). **All coverage data is lost when the extension is unloaded or Burp is closed.** Nothing is saved to the Burp project file.

---

## Usage

### Recommended workflow

1. **Load the spec first.** In the **Swagger Coverage** tab, click
   **Load OpenAPI/Swagger Spec…** and pick a `.yaml`, `.yml`, or `.json` file.
   The summary line shows `Total: N` and every row starts as `❌ MISSING`.
2. **Check the base path.** Compare the spec's paths with the real request paths in Burp's site map. If the real API lives under a prefix such as `/api/v1`, edit a copy of the spec first (see [gotchas](#read-this-first-matching-rules-and-gotchas)).
3. **Credit existing traffic.** Click **Re-analyze Site Map**. The Output tab logs
   `Site map re-analysis: N requests processed.`
4. **Test.** Browse through the Proxy, run the crawler/scanner, use Repeater or Intruder.
5. **Refresh.** Click **Re-analyze Site Map** to update the table and summary.
6. **Close the gaps.** Work through the red rows at the top. Each one is a documented endpoint that no request has reached yet.

### The tab

| Control | What it does |
|---|---|
| **Load OpenAPI/Swagger Spec…** | Opens a file picker, parses the spec, and **replaces** the documented endpoint list. Previously observed endpoints that also exist in the new spec stay covered. |
| **Re-analyze Site Map** | Replays every request in Burp's site map through the matcher, then redraws the table. Safe to click as often as you like (it never double-counts). On very large projects Burp's UI may pause briefly while it runs. |
| **Reset Observed** | Marks every endpoint as missing again. Note: the next **Re-analyze Site Map** re-credits everything still in the site map. For a truly clean slate, also clear the site map or start a new Burp project. |
| Summary line | `Total | Covered | Missing | Coverage %`, plus a progress bar. |
| Table | `Status`, `Method`, `Path`. Click a column header to sort. Missing rows first by default. |

### Loading a spec from a URL

The file picker only accepts local files. To use a remote spec, download it first:

```bash
curl -o spec.json https://target.example.com/v3/api-docs
```

Specs with relative `$ref`s to other files are fine as long as those files sit next to the main spec.

---

## Try it with the example spec

`examples/petstore.yaml` documents **9 endpoints**:

| Method | Path |
|---|---|
| GET, POST | `/pets` |
| GET, DELETE, PATCH | `/pets/{petId}` |
| POST | `/pets/{petId}/photo` |
| POST | `/store/order` |
| GET | `/user/login` |
| GET | `/user/logout` |

Quick check that matching works (no real Petstore server needed):

1. Load `examples/petstore.yaml`. The summary shows `Total: 9 | Covered: 0 | Missing: 9 | Coverage: 0.0%`.
2. In **Repeater**, send these to any host you are allowed to test, e.g. a throwaway local server started with `python3 -m http.server 8000` and targeted as `http://localhost:8000`. The 404 responses don't matter:
   ```
   GET /pets?limit=5 HTTP/1.1
   GET /pets/42 HTTP/1.1
   GET /user/login HTTP/1.1
   ```
3. Click **Re-analyze Site Map**. Expected: `Covered: 3 | Missing: 6 | Coverage: 33.3%`.

> The example's `servers` URL is `/api/v1`. As explained above, that prefix is **not** applied, so `GET /api/v1/pets` would **not** match. This is expected behaviour.

---

## Troubleshooting

### Build problems

| Symptom | Cause | Fix |
|---|---|---|
| `zsh: no such file or directory: ./gradlew` | No wrapper in the repo | Use `gradle`, or create the wrapper ([step 3](#3-optional-but-recommended-create-the-gradle-wrapper)). |
| `Unable to locate a Java Runtime` / `JAVA_HOME is not set` | No JDK installed or not on `PATH` | Install JDK 17+ ([step 1](#1-install-a-jdk-17)). |
| `Could not set unknown property 'sourceCompatibility'` | Gradle 9+ | Use Gradle 8.x. |
| `invalid source release: 17` / `Unsupported class file major version` | Gradle is running on a JDK older than 17, or a JDK too new for that Gradle version | Run `gradle --version` and check the `JVM:` line. Point `JAVA_HOME` at JDK 17 or 21. |
| `Could not find net.portswigger.burp.extensions:montoya-api:<version>` | That version isn't published | Set a version listed at <https://central.sonatype.com/artifact/net.portswigger.burp.extensions/montoya-api/versions> in `build.gradle`. |
| `Could not resolve …` / timeouts | No internet access, or a corporate proxy | Configure `systemProp.https.proxyHost` / `proxyPort` in `~/.gradle/gradle.properties`. |
| Build OK but JAR is tiny (a few KB) | Wrong JAR picked up | Use `build/libs/swagger-coverage-1.0.0.jar`, not a file from `build/distributions/`. |

### Loading in Burp

| Symptom | Cause | Fix |
|---|---|---|
| No **Swagger Coverage** tab appears | The extension failed to load | Check **Extensions → Installed → (select it) → Errors**. |
| `UnsupportedClassVersionError` | Your Burp's Java is older than 17 | Update Burp. Current versions bundle Java 21. |
| `NoSuchMethodError` / `ClassNotFoundException: burp.api.montoya…` | Burp is older than the Montoya API the JAR was built against | Update Burp to the latest version. |
| `SecurityException: Invalid signature file digest` | A signed dependency's signature files were copied into the fat JAR | Add `exclude 'META-INF/*.SF', 'META-INF/*.DSA', 'META-INF/*.RSA'` inside the `jar { }` block of `build.gradle` and rebuild. |

### Spec and coverage problems

| Symptom | Cause | Fix |
|---|---|---|
| "Failed to parse spec: …" dialog | Invalid YAML/JSON or not an OpenAPI/Swagger document | Validate the file at <https://editor.swagger.io>. The full message is also in the extension's **Errors** tab. |
| "Failed to parse spec" mentioning `null` or `getPaths()` | The spec has no `paths:` section | Add at least one path. |
| `Total: 0` after loading | The spec defines paths with no operations | Each path needs at least one method (`get:`, `post:`, …). |
| Endpoint stays **MISSING** though you requested it | Usually the base path, method, or case (see [gotchas](#read-this-first-matching-rules-and-gotchas)) | Compare the exact request line in Proxy history with the `Method` and `Path` columns. |
| Numbers don't change while testing | The table doesn't auto-refresh | Click **Re-analyze Site Map**. |
| Unrelated endpoints show as **Covered** | The host is ignored, so other sites' traffic matched | Use a dedicated Burp project / browser profile. |
| Status column shows boxes instead of ❌/✅ | Your UI font lacks those glyphs | Cosmetic only. Rows are still red (missing) or green (covered). |
| Coverage resets to 0 after restarting Burp | State isn't persisted | Reload the spec, then **Re-analyze Site Map** (the site map *is* saved in Pro project files). |

---

## Project layout

```
swagger-coverage-burp/
├── build.gradle                 # Dependencies + fat-JAR packaging
├── settings.gradle
├── gradle.properties
├── examples/
│   └── petstore.yaml            # 9-endpoint sample spec
├── scripts/                     # (empty)
└── src/main/java/com/example/swaggercoverage/
    ├── SwaggerCoverageExtension.java  # Entry point: registers handler + tab
    ├── CoverageHttpHandler.java       # Passive listener for all Burp tools
    ├── SiteMapAnalyzer.java           # Retroactive site map scan
    ├── SpecParser.java                # OpenAPI/Swagger → Set<Endpoint>
    ├── Endpoint.java                  # METHOD + path template → regex
    ├── EndpointRegistry.java          # Thread-safe documented/observed sets
    └── CoverageTab.java               # Swing UI
```

### Dependencies

| Library | Version | Scope |
|---|---|---|
| `net.portswigger.burp.extensions:montoya-api` | 2026.7 | `compileOnly` (provided by Burp at runtime) |
| `io.swagger.parser.v3:swagger-parser` | 2.1.37 | bundled |
| `com.fasterxml.jackson.core:jackson-databind` | 2.17.2 | bundled |
| `org.yaml:snakeyaml` | 2.3 | bundled |
| `org.junit.jupiter:junit-jupiter` | 5.10.2 | test only (no tests yet) |

---

## Regenerating the project with `bootstrap.sh`

The parent directory contains `bootstrap.sh`, which writes this whole project from scratch.

> ⚠️ **It starts with `rm -rf swagger-coverage-burp`.** Running it **deletes**
> this directory, including any local changes, build output, and the Gradle
> wrapper. Commit or back up your work first.

```bash
cd ..                    # the directory that contains bootstrap.sh
bash bootstrap.sh
```

Changes to source files or this README should be made in **both** places (or only in `bootstrap.sh`, then regenerate), otherwise the next run overwrites them.

---

## Known limitations

- No automatic table refresh (use **Re-analyze Site Map**).
- `servers` / `basePath` prefixes are not applied to spec paths.
- No host or scope filtering. Burp's target scope is ignored.
- Coverage doesn't consider response status, parameters, or request bodies.
- Spec can only be loaded from a local file through the UI.
- No export (CSV/JSON/HTML) of the coverage report.
- Coverage state is held in memory only and lost on unload or restart.
- Site map re-analysis runs on Burp's UI thread.
- No automated tests yet.
