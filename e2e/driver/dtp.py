"""Black-box client for the DTP transfer API, plus a tail-follower for the
server log.

Nothing here is adapter-specific: service ids, data type and encryption scheme
are all parameters. Adding an adapter should cost a compose service and some
fixtures, not a change to this file.

The request sequence is the one implemented in
``client-rest/src/app/transfer/initiate-transfer.component.ts``, the only other
place it exists.
"""

from __future__ import annotations

import base64
import json
import re
import time

import requests
import urllib3

# JettyRestExtension defaults useHttps to true and demo-server's generated
# api.yaml never emits an override, so the server serves TLS on 8080 with the
# bundled self-signed keystore. Nothing here is testing certificate handling.
urllib3.disable_warnings(urllib3.exceptions.InsecureRequestWarning)


class TransferFailed(AssertionError):
    """The job failed, or never finished. Carries log context."""


def decode_job_id(encoded_job_id: str) -> str:
    """Recover the job UUID from the id the API hands back.

    ``ActionUtils.encodeJobId`` is
    ``BaseEncoding.base64Url().encode(uuid.toString().getBytes(UTF_8))`` -- so
    it encodes the 36-character *string*, not the 16 raw bytes. 36 is divisible
    by 3, so there is never any padding to add back.
    """
    return base64.urlsafe_b64decode(encoded_job_id).decode("utf-8")


class DtpClient:
    """Speaks to the API under its /api/* servlet prefix."""

    def __init__(self, base_url: str, timeout: float = 30.0):
        self.base_url = base_url.rstrip("/")
        self.timeout = timeout
        self.session = requests.Session()
        self.session.verify = False

    # -- plumbing ---------------------------------------------------------

    def get(self, path: str) -> dict:
        return self._send("GET", path)

    def post(self, path: str, body: dict) -> dict:
        return self._send("POST", path, body)

    def _send(self, method: str, path: str, body: dict | None = None) -> dict:
        response = self.session.request(
            method,
            f"{self.base_url}{path}",
            json=body,
            timeout=self.timeout,
        )
        if not response.ok:
            raise AssertionError(
                f"{method} {path} returned {response.status_code}: {response.text}"
            )
        return response.json()

    def await_ready(self, timeout: float = 120.0) -> None:
        """Block until the API answers. This is the readiness gate; the compose
        file deliberately has no healthcheck."""
        deadline = time.monotonic() + timeout
        last = None
        while time.monotonic() < deadline:
            try:
                self.get("/api/datatypes")
                return
            except Exception as exc:  # noqa: BLE001 - any failure means not ready
                last = exc
                time.sleep(1)
        raise TimeoutError(f"API not ready within {timeout}s; last error: {last}")

    def is_alive(self) -> bool:
        try:
            self.get("/api/datatypes")
            return True
        except Exception:  # noqa: BLE001
            return False

    # -- the transfer sequence --------------------------------------------

    def create_job(
        self,
        export_service: str,
        import_service: str,
        data_type: str,
        encryption_scheme: str,
        callback_url: str,
    ) -> str:
        """Returns the base64url-encoded job id, which every later call reuses
        verbatim.

        ``data_type`` here is the JSON form -- the DataVertical's @JsonValue,
        e.g. "OFFLINE-DATA". In a path segment it is the enum constant instead
        (see :meth:`services_for`).
        """
        job = self.post(
            "/api/transfer",
            {
                "exportService": export_service,
                "importService": import_service,
                "exportCallbackUrl": callback_url,
                "importCallbackUrl": callback_url,
                "dataType": data_type,
                "encryptionScheme": encryption_scheme,
            },
        )
        return job["id"]

    def services_for(self, data_type_enum: str) -> dict:
        """``data_type_enum`` is the enum *constant* (e.g. "OFFLINE_DATA").

        This is a JAX-RS enum path param resolved by Enum.valueOf, so it does
        not accept the "OFFLINE-DATA" spelling a JSON body requires.
        """
        return self.get(f"/api/transfer/services/{data_type_enum}")

    def generate_auth(self, encoded_job_id: str, mode: str, callback_url: str) -> str:
        """Returns an already-serialized AuthData *string*, not an object.

        The @PathParam is never read -- the id comes from the body -- but the
        path segment is still needed for routing.
        """
        response = self.post(
            f"/api/transfer/{encoded_job_id}/generate",
            {
                "id": encoded_job_id,
                "authToken": "unused-without-a-real-oauth-flow",
                "mode": mode,
                "callbackUrl": callback_url,
            },
        )
        return response["authData"]

    def reserve_worker(self, encoded_job_id: str) -> None:
        """Moves the job to CREDS_AVAILABLE so a worker can pick it up."""
        self.post(f"/api/transfer/worker/{encoded_job_id}", {"id": encoded_job_id})

    def await_worker_claim(self, encoded_job_id: str, timeout: float = 60.0) -> str:
        """Poll until the worker has claimed the job and published its key.

        ReserveWorkerAction returns an empty-string key immediately; the real
        key only appears once the worker's JobPollingService has moved the job
        to CREDS_ENCRYPTION_KEY_GENERATED.
        """
        deadline = time.monotonic() + timeout
        while time.monotonic() < deadline:
            worker = self.get(f"/api/transfer/worker/{encoded_job_id}")
            key = worker.get("publicKey")
            if key:
                return key
            time.sleep(0.1)
        raise TimeoutError(f"No worker claimed job {encoded_job_id} within {timeout}s")

    def start_job(self, encoded_job_id: str, export_auth: str, import_auth: str) -> None:
        """Hand the worker its credentials.

        Under the cleartext scheme ``ClearTextAuthDataDecryptService.decrypt``
        is a plain ``readValue(encrypted, AuthDataPair.class)`` and the private
        key is ignored, so "encrypted" auth data is just the serialized pair.

        Note the double encoding: ``encryptedAuthData`` is a *string* holding
        JSON, and both members of that JSON are themselves *strings* holding
        serialized AuthData -- JobProcessor re-parses each one with
        ``readValue(..., AuthData.class)``.
        """
        self.post(
            f"/api/transfer/{encoded_job_id}/start",
            {
                "id": encoded_job_id,
                "encryptedAuthData": json.dumps(
                    {"exportAuthData": export_auth, "importAuthData": import_auth}
                ),
            },
        )


class ServerLog:
    """Reads the log the dtp container tees onto a shared volume.

    This is the completion signal because there is no other one: TransferJob's
    `state` field is hardcoded to CREATED with no getter, so Jackson never
    emits it and GET /api/transfer/{id} reports no progress at all.
    """

    def __init__(self, path: str):
        self.path = path

    def read(self) -> str:
        try:
            with open(self.path, "r", encoding="utf-8", errors="replace") as handle:
                return handle.read()
        except FileNotFoundError:
            return ""

    def tail(self, lines: int = 40) -> str:
        return "\n".join(self.read().splitlines()[-lines:])

    def count_matches(self, pattern: str) -> int:
        """How many lines match ``pattern``.

        Exists for the copy-iteration assertion: PortabilityAbstractInMemoryDataCopier
        logs "Copy iteration: N" once per recursion, so counting those lines is how
        an adapter proves the copier actually recursed rather than returning
        everything in one pass. Without it, an under-seeded fixture passes green
        while covering none of what it claims to.
        """
        return len(re.findall(pattern, self.read()))

    def assert_contains(self, needle: str, why: str) -> None:
        """Assert on the log without pytest dumping all of it into the report.

        A plain ``assert needle in log.read()`` prints the entire ~30KB server
        log as the assertion's left-hand side, which buries the actual failure.
        """
        if needle not in self.read():
            raise TransferFailed(
                f"{why}: {needle!r} never appeared in the server log.\n\n"
                f"--- server log (tail) ---\n{self.tail()}"
            )

    def wait_for(
        self,
        pattern: str,
        timeout: float = 60.0,
        fail_on: str | None = None,
        client: DtpClient | None = None,
    ) -> re.Match:
        """Wait for ``pattern``, failing immediately if ``fail_on`` shows up.

        Racing the two is what makes a broken transfer fail in milliseconds
        with the server's own error, rather than at timeout. It matters here:
        JobCancelWatchingService calls System.exit(0) on ERROR, and because
        SingleVMMain's WorkerRunner loops forever that takes the whole JVM --
        API included -- with it. The severe log line is written before the job
        is marked ERROR, so this sees it first.
        """
        deadline = time.monotonic() + timeout
        while time.monotonic() < deadline:
            contents = self.read()
            if fail_on:
                failure = re.search(fail_on, contents)
                if failure:
                    raise TransferFailed(
                        f"Server reported failure: {failure.group(0)}\n\n"
                        f"--- server log (tail) ---\n{self.tail()}"
                    )
            match = re.search(pattern, contents)
            if match:
                return match
            time.sleep(0.1)

        died = client is not None and not client.is_alive()
        note = (
            "\nThe server is no longer answering -- the JVM exited, which is "
            "what JobCancelWatchingService does on a failed job."
            if died
            else ""
        )
        raise TransferFailed(
            f"Timed out after {timeout}s waiting for /{pattern}/.{note}\n\n"
            f"--- server log (tail) ---\n{self.tail()}"
        )
