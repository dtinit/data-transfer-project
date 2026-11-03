#!/usr/bin/env bash
#
# Runs a complete transfer per adapter and exits non-zero if any of them fails.
# No provider credentials, no local JDK, no local Python.
#
#   ./e2e/run.sh                  # every adapter
#   ./e2e/run.sh offline-demo     # just the ones named
#
# An adapter is a suite at e2e/driver/test_<adapter>.py (hyphens become
# underscores). Each gets a fresh dtp -- see "One dtp per adapter" in
# e2e/README.md. Server logs land in e2e/.logs/dtp-<adapter>.log afterwards,
# whether the run passed or failed.
set -euo pipefail

cd "$(dirname "${BASH_SOURCE[0]}")/.."

LOG_DIR="e2e/.logs"

# Services an adapter needs beside dtp, typically its mock provider. None yet.
mocks_for() { :; }

suite_for() { echo "e2e/driver/test_${1//-/_}.py"; }

ADAPTERS=("$@")
if [[ ${#ADAPTERS[@]} -eq 0 ]]; then
  for suite in e2e/driver/test_*.py; do
    name=${suite#e2e/driver/test_}
    name=${name%.py}
    ADAPTERS+=("${name//_/-}")
  done
fi

for adapter in "${ADAPTERS[@]}"; do
  if [[ ! -f $(suite_for "$adapter") ]]; then
    echo "Unknown adapter '$adapter': no $(suite_for "$adapter")" >&2
    exit 2
  fi
done

teardown() {
  # No -v: that would also delete gradle-cache and make every run a cold build.
  docker compose down --remove-orphans >/dev/null 2>&1 || true
}

# Before teardown, since `down` removes the container and its logs. On the
# host, so the file ends up owned by you rather than by root.
capture() {
  mkdir -p "$LOG_DIR"
  docker compose logs --no-color --no-log-prefix dtp \
    > "$LOG_DIR/dtp-$1.log" 2>/dev/null || true
}

trap teardown EXIT
teardown

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

failed=()
for adapter in "${ADAPTERS[@]}"; do
  echo
  echo "==> $adapter"
  rc=0
  # shellcheck disable=SC2046 # mocks_for's output is deliberately word-split
  if docker compose up -d dtp $(mocks_for "$adapter") >/dev/null; then
    docker compose run --rm e2e "$(suite_for "$adapter")" || rc=$?
  else
    rc=1
  fi
  capture "$adapter"
  teardown
  # compose exits 130 on Ctrl-C, and bash never sees the signal itself.
  if [[ $rc -eq 130 ]]; then exit 130; fi
  if [[ $rc -ne 0 ]]; then failed+=("$adapter"); fi
done

echo
if [[ ${#failed[@]} -gt 0 ]]; then
  echo "FAILED: ${failed[*]}"
  echo "Logs: $LOG_DIR/"
  exit 1
fi
echo "All adapters passed: ${ADAPTERS[*]}"
