#!/usr/bin/env bash
#
# Runs a complete transfer per adapter and exits non-zero if any of them fails.
# No provider credentials, no local JDK, no local Python.
#
#   ./e2e/run.sh                  # every adapter
#   ./e2e/run.sh imgur            # just one
#   ./e2e/run.sh offline-demo imgur
#
# Each adapter gets its own freshly started `dtp` container. That is not
# tidiness: LocalJobStore keeps jobs in private static maps and LocalTempFileStore
# keeps files on disk, and nothing clears either between jobs, so a shared
# server would make isolation a matter of luck and ordering. A cold JVM per
# adapter costs a few seconds and removes the question.
#
# Logs and mock request journals land in e2e/.logs/ afterwards, per adapter,
# whether the run passed or failed.
set -uo pipefail    # deliberately not -e: every adapter runs, then we report

cd "$(dirname "${BASH_SOURCE[0]}")/.."

LOG_DIR="e2e/.logs"

# Services to start alongside `dtp`, per adapter, and the host port their mock
# admin API is published on. Adding an adapter is one line in each -- the
# driver itself stays free of provider names.
declare -A MOCKS=(   [offline-demo]=""              [imgur]="wiremock-imgur" )
declare -A MOCK_PORT=( [offline-demo]=""            [imgur]="18080" )

ALL_ADAPTERS=(offline-demo imgur)
ADAPTERS=("$@")
[[ ${#ADAPTERS[@]} -eq 0 ]] && ADAPTERS=("${ALL_ADAPTERS[@]}")

for adapter in "${ADAPTERS[@]}"; do
  if [[ -z ${MOCKS[$adapter]+set} ]]; then
    echo "Unknown adapter '$adapter'. Known: ${ALL_ADAPTERS[*]}" >&2
    exit 2
  fi
done

teardown() {
  # No -v: that would also delete gradle-cache and make every run a cold build.
  docker compose --profile "$1" down --remove-orphans >/dev/null 2>&1 || true
}

# Capture before teardown, and on the host so the artifacts end up owned by you
# rather than by root. `dtp` tees to a fixed path on a shared volume and
# truncates on restart, so there is no second chance once the next adapter starts.
capture() {
  local adapter=$1
  mkdir -p "$LOG_DIR"
  docker compose logs --no-color --no-log-prefix dtp \
    > "$LOG_DIR/dtp-$adapter.log" 2>/dev/null || true
  local port=${MOCK_PORT[$adapter]}
  if [[ -n $port ]]; then
    # The mock's record of what actually arrived -- the assertion surface the
    # offline-demo suite has to approximate by grepping a log.
    curl -s "http://localhost:$port/__admin/requests" \
      > "$LOG_DIR/$adapter-requests.json" 2>/dev/null || true
  fi
}

cleanup_all() { for a in "${ADAPTERS[@]}"; do teardown "$a"; done; }
trap cleanup_all EXIT

cleanup_all

# Built through the `gradle` service, whose ENTRYPOINT is already ./gradlew, so
# these arguments pass straight through. That single-sources the pinned Gradle
# 6.9.2 / JDK 11 toolchain from the root Dockerfile and reuses the warm
# gradle-cache volume, which a `docker build` stage could not mount.
#
# shadowJar has no path to copyWebApp, so no Node or Angular toolchain is
# involved -- only `dockerize` drags those in.
echo "==> Building the demo-server jar (offline-demo, cleartext)"
docker compose run --rm gradle --no-daemon \
  :distributions:demo-server:shadowJar \
  -PofflineData=true \
  -PencryptionScheme=cleartext || exit 1

failed=()
for adapter in "${ADAPTERS[@]}"; do
  echo
  echo "==> $adapter"
  teardown "$adapter"
  # shellcheck disable=SC2086 # MOCKS entries are deliberately word-split
  docker compose --profile "$adapter" up -d dtp ${MOCKS[$adapter]} >/dev/null || {
    failed+=("$adapter"); continue
  }

  # Marker names use underscores: `-m offline-demo` would parse as the
  # expression `offline and (not demo)`.
  E2E_MARKER="${adapter//-/_}" docker compose run --rm e2e || failed+=("$adapter")

  capture "$adapter"
  teardown "$adapter"
done

echo
if [[ ${#failed[@]} -gt 0 ]]; then
  echo "FAILED: ${failed[*]}"
  echo "Logs: $LOG_DIR/"
  exit 1
fi
echo "All adapters passed: ${ADAPTERS[*]}"
