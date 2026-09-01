#!/usr/bin/env bash
#
# Runs a complete offline-demo -> offline-demo transfer and exits non-zero if it
# does not arrive. No provider credentials, no local JDK, no local Python.
#
#   ./e2e/run.sh
#
# The server log lands in e2e/.logs/dtp.log afterwards, whether the run passed
# or failed.
set -euo pipefail

cd "$(dirname "${BASH_SOURCE[0]}")/.."

LOG_DIR="e2e/.logs"

# Capture the server log before tearing anything down, so a failure is
# diagnosable without re-running. The redirect happens here on the host, which
# is also what keeps the file owned by you rather than by root.
capture_and_clean() {
  local rc=$?
  mkdir -p "$LOG_DIR"
  docker compose logs --no-color --no-log-prefix dtp > "$LOG_DIR/dtp.log" 2>/dev/null || true
  # No -v: that would also drop gradle-cache and make every run a cold build.
  docker compose down --remove-orphans >/dev/null 2>&1 || true
  if [[ $rc -ne 0 ]]; then
    echo
    echo "FAILED. Server log: $LOG_DIR/dtp.log"
  fi
  exit "$rc"
}
trap capture_and_clean EXIT

docker compose down --remove-orphans >/dev/null 2>&1 || true

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
  -PencryptionScheme=cleartext

echo
echo "==> Running the transfer"
docker compose run --rm e2e
