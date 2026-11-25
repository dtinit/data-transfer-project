# End-to-end transfer harness

Runs a complete DTP transfer — job creation, auth, worker claim, export, import —
and asserts that the exported payload actually arrives.

```bash
./e2e/run.sh                  # every adapter
./e2e/run.sh imgur            # just the ones named
```

No provider credentials, no local JDK, no local Python. About a minute for
both adapters with a warm Gradle cache. Each adapter's server log lands in
`e2e/.logs/dtp-<adapter>.log` afterwards, and each mock's request journal in
`e2e/.logs/<mock>.log`, whether the run passed or failed.

| Adapter | What it proves |
|---|---|
| `offline-demo` | the machinery — a credential-free transfer with no HTTP surface at all |
| `imgur` | a real data path — the unmodified Imgur adapter paginating on two axes, recursing into albums, and round-tripping image bytes through the temp store |

**One dtp per adapter.** `run.sh` starts a fresh `dtp` for each adapter's
suite and tears it down after, which costs about 17 seconds per adapter. Job
data itself is already keyed by job id, so this isn't about the stores. It is
about how the driver observes DTP: through one server log shared by every job,
and a single worker that a job left behind by a failed suite could hold. Note
that `run.sh` runs `docker compose down` for the whole project, so it also stops
a `dtp` you started by hand.

This is also the shortest path to watching DTP do something real:

```bash
docker compose run --rm gradle --no-daemon \
  :distributions:demo-server:shadowJar -PofflineData=true -PencryptionScheme=cleartext
docker compose up dtp          # API on https://localhost:8080 (self-signed)
```

## How it fits together

Four services in the root `docker-compose.yml`, two of them sharing the repo's
single `Dockerfile`:

| Service | What it is |
|---|---|
| `gradle` | the Gradle build. Its `ENTRYPOINT` is `./gradlew`, so anything you pass it is a Gradle argument. |
| `dtp` | the same image, with the entrypoint overridden to run the shadowJar that `gradle` produced. |
| `e2e` | stock `python:3.12-slim`, pytest, no Dockerfile. |
| `wiremock-imgur` | `wiremock/wiremock`, standing in for `api.imgur.com`. Configured entirely by JSON. |

`dtp` tees its output to a shared volume; `e2e` reads that file. Both also
bind-mount the repo, so no image needs rebuilding when a test changes.

The transfer runs as a single JVM because it has to: `LocalJobStore` shares
state through `private static` maps and `JobMetadata` is a static singleton, so
the API and the worker only ever see the same job when co-located. That is what
`SingleVMMain` is for.

### Why there is no dedicated server image

The repository publishes no container image today — `release.yml` pushes Maven
artifacts, and `docker-build.yml` pushes nothing. The one image *definition*
that exists, `datatransferproject/demo:latest`, is generated at build time by
`:distributions:demo-server:dockerize`, which pulls in an Angular toolchain the
repo's own pinned dev image does not have, and never exposes 8080.

So this harness pins a **contract**, not an artifact: *a service that answers
the DTP API on 8080 and writes its log where the driver can read it*. When a
real server image exists, replace `build`/`entrypoint`/`command` on the `dtp`
service with `image:` and nothing else here changes.

It does already run the real packaged jar, including the `mergeServiceFiles()`
`META-INF/services` merge that `ServiceLoader` extension discovery depends on.

## What a green run does and does not prove

**Does:** a job was created and authorized over HTTP — including a real OAuth2
token exchange, for Imgur — a worker claimed it, the exporter produced data, the
copier recursed over paginated listings and sub-resources, image bytes made a
round trip through `LocalTempFileStore`, and the importer delivered every one of
them to the right album, byte for byte.

**Does not:**

- **Prove much from the job's own "success".** `Finished processing jobId: … with
  0 error(s).` is *necessary but not sufficient*. That count is the size of
  `copier.getErrors()`, so a job that never copied anything reports zero too.
  Build the jar with `-PencryptionScheme=jwe` while the driver still posts
  `cleartext` and you get exactly that: `JobProcessor` logs `No auth decrypter
  found for scheme …`, returns without transferring, and its `finally` block
  still prints the clean-looking line. This is why the delivered payload is
  asserted separately, and why failure is detected from `SEVERE` lines rather
  than from the absence of a success line.
- **Cover a real provider's API.** The Imgur suite drives the real, unmodified
  `ImgurPhotosExporter` and `ImgurPhotosImporter`, but against a mock built from
  the adapter's own test fixtures. It tests DTP against *our reading* of Imgur's
  API, so it catches DTP regressions and not Imgur changing under us.
- **Cover any vertical but `PHOTOS` and `OFFLINE_DATA`.**
- **Cover the published image**, since there isn't one.

### Why the completion signal is a log line

Because there is no other one. `TransferJob.state` is
`private final State state = State.CREATED` with no getter, so Jackson never
emits it and `GET /api/transfer/{id}` reports no progress at all —
`GetTransferJobAction` fetches the real `PortabilityJob` and discards
everything but the ids (`TODO(#553)`).

Giving `TransferJob` a real, nullable `state` and passing `job.state()` is
roughly fifteen lines; the route is already wired and the action already has
the job. It would let this harness poll HTTP instead of grepping, and would let
the Angular client show transfer progress, which it currently cannot. That is a
change to shipped code, so it is deliberately not bundled here.

## Adding an adapter

The driver is adapter-agnostic — service ids, vertical and encryption scheme
are parameters, and `dtp.py` and `wiremock.py` name no provider. `imgur` is the
worked example; copy its shape:

1. Make the adapter's base URL configurable: read it from
   `TransferServiceConfig`, defaulting to today's value. `ImgurTransferExtension`
   is the pattern to copy (#1519); most other adapters still hardcode a single
   `private static final`, e.g. `MicrosoftTransferExtension.BASE_GRAPH_URL`.
2. Point the adapter at the mock with a `config/<service>.yaml` under
   `e2e/config/` — see below.
3. Add a **WireMock standalone** service to `docker-compose.yml` with the
   adapter's endpoints as `mappings/` (and `__files/` for any binary payloads).
   Run it with `--verbose`: `run.sh` saves its log, which then has every request
   and body.
4. Add `e2e/driver/test_<adapter>.py` with that adapter's ids and fixtures —
   `run.sh` picks up every `test_*.py` as an adapter — and list its mock service
   in `run.sh`'s `mocks_for`.

### How the mock URL reaches the adapter

There is no environment-variable override anywhere in DTP's config chain:
`TransferServiceConfig.getForService(service)` and `ConfigUtils` read
`config/<name>.yaml` from the classpath with the singular `getResourceAsStream`,
so the first match per filename wins. `dtp` therefore runs
`java -cp /workspace/e2e/config:<jar> …SingleVMMain` rather than `java -jar`,
which ignores `-cp`. Two things to know:

- **The path is doubled.** `e2e/config` is the classpath entry and
  `config/imgur.yaml` is the resource name, so the file lives at
  `e2e/config/config/imgur.yaml`.
- **Prefer adding a file over shadowing one.** Adding a file the jar does not
  ship (Imgur's case) is inert for everything else. Shadowing one it *does*
  ship — `deezer.yaml`, `flickr.yaml`, `synology.yaml` — replaces it wholesale
  rather than merging, silently dropping settings like `perUserRateLimit`.

### Fixtures and assertions

- Derive request and response shapes from the adapter's existing
  `MockWebServer` tests rather than from the client code.
- Include only the fields the adapter reads, and template URLs the adapter
  follows from `{{request.baseUrl}}` so the mock's hostname lives in one place.
- Seed data that exists **only on the last non-empty page** of each axis the
  adapter paginates, and assert it arrives. Adapters that stop on an empty page
  also need an empty-page terminator stub.
- Assert on WireMock's `/__admin/requests` journal, which `wiremock.py` wraps —
  every request the mock received, bodies intact.
- An import that is deduplicated by the idempotent executor never reaches the
  mock, so a duplicate *export* can't be seen in the uploads. Assert on what the
  exporter fetched as well.
- When negative-checking a bytes-match assertion, break the pipeline (serve the
  wrong file), not the fixture: the fixture is both sides of the comparison.

Note the fidelity limit that comes with all of this: a mock built from adapter
code tests DTP against *our reading* of a provider's API. It catches DTP
regressions. It does not catch the provider changing under us.

## Traps encoded in the driver

Each of these is a real inconsistency in the API, and each cost a debugging
session:

- **Service ids differ in case.** `OfflineDemoAuthServiceExtension` declares
  `OFFLINE-DEMO`; `OfflineDemoTransferExtension` declares `offline-demo`.
  `PortabilityAuthServiceProviderRegistry` does an exact-match lookup while
  `TransferExtension.supportsService` lowercases both sides, so only the auth
  extension's spelling satisfies both.
- **The vertical is spelled two ways.** `OFFLINE-DATA` (the `@JsonValue`) in a
  JSON body; `OFFLINE_DATA` in a path segment, where JAX-RS resolves the enum
  with `Enum.valueOf`.
- **`encryptedAuthData` is triple-encoded.** It is a *string* holding JSON whose
  two members are themselves *strings* holding serialized `AuthData` —
  `JobProcessor` re-parses each with `readValue(…, AuthData.class)`.
- **The `{id}` path parameter is never read** on the POSTs. The id comes from the
  body; the segment only matters for routing.
- **Job ids are base64url of the UUID's 36-character string**, not of its 16
  bytes.
- **An adapter with real OAuth needs credentials set, even fake ones.**
  Without them `OAuth2ServiceExtension.initialize` quietly gives up, and the
  failure surfaces on the first `POST /api/transfer` as `Cannot get
  OAuth2DataGenerator before initialization`. Hence the dummy
  `IMGUR_KEY`/`IMGUR_SECRET` on `dtp`.
- **A clean Imgur run logs `SEVERE` lines.** On a same-service transfer
  `ImgurTransferExtension` logs each repeated `initialize()` at `SEVERE`. They
  carry no job id, so the `SEVERE[^\n]*<job_id>` fail-fast pattern ignores them.
- **A failed job can take the whole JVM with it.**
  `JobCancelWatchingService` calls `System.exit(0)` on `ERROR`, and
  `SingleVMMain`'s worker loop means that kills the API too. The driver reports
  a server that stopped answering as a job failure rather than a flake.
