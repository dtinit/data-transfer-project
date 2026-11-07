# End-to-end transfer harness

Runs a complete DTP transfer — job creation, auth, worker claim, export, import —
and asserts that the exported payload actually arrives.

```bash
./e2e/run.sh                  # every adapter
./e2e/run.sh imgur            # just one
```

No provider credentials, no local JDK, no local Python. About 55 seconds for
both adapters with a warm Gradle cache. Server logs and mock request journals
land in `e2e/.logs/`, one set per adapter, whether the run passed or failed.

Two adapters are covered:

| Adapter | What it proves |
|---|---|
| `offline-demo` | the machinery — a credential-free transfer with no HTTP surface at all |
| `imgur` | a real data path — a real, unmodified adapter paginating on two axes, recursing into sub-resources, and round-tripping image bytes through the temp store |

**Each adapter gets its own freshly started `dtp` container.** That is not
tidiness: `LocalJobStore` keeps jobs in `private static` maps and
`LocalTempFileStore` keeps files on disk, and nothing clears either between
jobs. A shared server would make isolation a matter of luck and ordering, so
`run.sh` restarts one per adapter and each suite runs against a cold JVM. Every
adapter runs even if an earlier one fails, and the exit code reflects all of
them.

This is also the shortest path to watching DTP do something real:

```bash
docker compose run --rm gradle --no-daemon \
  :distributions:demo-server:shadowJar -PofflineData=true -PencryptionScheme=cleartext
docker compose --profile offline-demo up dtp   # API on https://localhost:8080 (self-signed)
```

## How it fits together

Three services in the root `docker-compose.yml`, two of them sharing the repo's
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
- **Cover any vertical but `PHOTOS` and `OFFLINE_DATA`**, or any adapter that
  does not talk plain JSON over OkHttp.
- **Cover the published image**, since there isn't one.
- **Cover JWE.** Only `cleartext` is exercised; the scheme is fixed at build
  time, so the alternative needs a second jar.

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

The driver is adapter-agnostic — service ids, vertical and encryption scheme are
parameters, and `dtp.py` names no provider. Adding one costs a compose service
and a directory of JSON. `imgur` is the worked example; copy its shape.

1. **Make the adapter's URLs configurable.** Most are a single
   `private static final`, e.g. `MicrosoftTransferExtension.BASE_GRAPH_URL`.
   They should read from `TransferServiceConfig`, defaulting to today's value —
   the convention Flickr, Deezer and Synology already use for other settings.
   Note that no adapter in the repo had a configurable URL on an *export* path
   before Imgur; only import-only adapters (`Generic`, Synology) had adopted it.
2. **Point the adapter at the mock** with a `config/<service>.yaml` under
   `e2e/config/` — see below.
3. **Add a WireMock service** to `docker-compose.yml` under a profile named
   after the adapter, with the endpoints as `mappings/` and any binary payloads
   as `__files/`. No code.
4. **Add a suite** beside `test_imgur.py`, marked `@pytest.mark.<adapter>` (the
   marker must be registered in `pytest.ini`), and one line each in `run.sh`'s
   `MOCKS` and `MOCK_PORT` tables.

### How the mock URL reaches the adapter

Config resolution is classpath-only — there is no environment-variable override
anywhere in the chain. `TransferServiceConfig.getForService(service)` reads
`config/<service>.yaml` with the *singular* `getResourceAsStream`, so the first
match per filename wins. The `dtp` service therefore runs

```
java -cp /workspace/e2e/config:<jar> org.datatransferproject.bootstrap.vm.SingleVMMain
```

rather than `java -jar`, which ignores `-cp` entirely.

Two things to know:

- **The path is doubled.** `e2e/config` is the classpath entry and
  `config/imgur.yaml` is the resource name, so the file lives at
  `e2e/config/config/imgur.yaml`.
- **Prefer adding a file over shadowing one.** Adding a file the jar does not
  ship (Imgur's case) is inert for everything else. Shadowing one it *does*
  ship — `deezer.yaml`, `flickr.yaml`, `synology.yaml` — replaces it wholesale
  rather than merging, silently dropping settings like `perUserRateLimit`.

### Seeding fixtures

Seed **more than one page** on every axis the adapter paginates, and assert that
the page *past* the seeded data was requested. Asserting on page 1 is not
enough: Imgur's exporter infers "there is more" from the current page being
non-empty, so it always requests page 1 even when page 0 was the last page with
data. Requesting page 2 is the first request that proves page 1 had content.

Adapters that infer the end of a listing this way also need an **empty-page
terminator** stub, or the export never stops asking.

Derive request and response shapes from the adapter's existing `MockWebServer`
tests rather than from the client code.

### Asserting on what arrived

WireMock's `/__admin/requests` returns every request it received with bodies
intact — the assertion surface for "what actually arrived", and a far better one
than a log line. `wiremock.py` wraps it; `run.sh` saves the journal to
`e2e/.logs/<adapter>-requests.json`.

One caveat learned the hard way: if a test compares delivered bytes against the
fixture file the mock serves, the fixture is its own oracle, and corrupting it
corrupts both sides equally. That assertion still proves each distinct fixture
arrived exactly once, unmodified — enough to catch temp-store cross-talk,
truncation and duplicate imports — but to check that it bites, break the
*pipeline* (make the mock serve the wrong file), not the fixture.

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
  `OAuth2ServiceExtension.initialize` catches the missing-credential
  `IOException`, logs it at INFO, and returns *without* setting `initialized`.
  The failure then surfaces much later, and nowhere near its cause, as
  `Cannot get OAuth2DataGenerator before initialization` on the first
  `POST /api/transfer`. Hence the dummy `IMGUR_KEY`/`IMGUR_SECRET` on the `dtp`
  service — their values are never checked against anything.
- **A clean run is no longer entirely free of `SEVERE` lines.** On a transfer
  where export and import are the *same* service, `WorkerModule` resolves one
  extension instance and calls `initialize()` on it more than once;
  `ImgurTransferExtension` logs each repeat at `SEVERE`. The fail-fast pattern
  is `SEVERE[^\n]*<job_id>` and those lines carry no job id, so it does not
  trip — but the "a clean run emits zero SEVERE lines" assumption this harness
  was built on is now only true per-job, not globally.
- **A failed job can take the whole JVM with it.**
  `JobCancelWatchingService` calls `System.exit(0)` on `ERROR`, and
  `SingleVMMain`'s worker loop means that kills the API too. The driver reports
  a server that stopped answering as a job failure rather than a flake.
