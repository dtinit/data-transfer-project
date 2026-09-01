# End-to-end transfer harness

Runs a complete DTP transfer — job creation, auth, worker claim, export, import —
and asserts that the exported payload actually arrives.

```bash
./e2e/run.sh
```

No provider credentials, no local JDK, no local Python. About 35 seconds with a
warm Gradle cache. The server log lands in `e2e/.logs/dtp.log` afterwards
whether the run passed or failed.

This is also the shortest path to watching DTP do something real:

```bash
docker compose run --rm gradle --no-daemon \
  :distributions:demo-server:shadowJar -PofflineData=true -PencryptionScheme=cleartext
docker compose up dtp          # API on https://localhost:8080 (self-signed)
```

## How it fits together

Three services in the root `docker-compose.yml`, two of them sharing the repo's
single `Dockerfile`:

| Service | What it is |
|---|---|
| `gradle` | the Gradle build. Its `ENTRYPOINT` is `./gradlew`, so anything you pass it is a Gradle argument. |
| `dtp` | the same image, with the entrypoint overridden to `java -jar` the shadowJar that `gradle` produced. |
| `e2e` | stock `python:3.12-slim`, pytest, no Dockerfile. |

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

**Does:** a job was created and authorized over HTTP, a worker claimed it, the
exporter produced data, the copier moved it, and the importer received the
exact expected payload.

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
- **Cover any real provider adapter.** `offline-demo` has no HTTP surface, no
  pagination and no sub-resources. It exercises the machinery, not a data path.
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
are parameters, and `dtp.py` names no provider. Adding one should cost a compose
service and a directory of JSON, not a driver change:

1. Add a **WireMock standalone** service to `docker-compose.yml` with the
   adapter's endpoints as `mappings/` (and `__files/` for any binary payloads).
   No code — WireMock is configured entirely by JSON.
2. Make the adapter's base URL configurable. Most are a single
   `private static final` constant, e.g. `ImgurTransferExtension.BASE_URL`;
   they should read from `TransferServiceConfig`, defaulting to today's value.
3. Add a test module beside `test_offline_demo.py` with that adapter's ids and
   fixtures.

For the import side, WireMock's `/__admin/requests` admin API returns every
request it received with bodies intact — that is the assertion surface for
"what actually arrived", and a far better one than a log line.

Derive mock request and response shapes from the adapter's existing
`MockWebServer` tests rather than from the client code, and seed **more than one
page** of data — otherwise pagination never fires and its absence passes.

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
- **A failed job can take the whole JVM with it.**
  `JobCancelWatchingService` calls `System.exit(0)` on `ERROR`, and
  `SingleVMMain`'s worker loop means that kills the API too. The driver reports
  a server that stopped answering as a job failure rather than a flake.
