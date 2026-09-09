"""A complete offline-demo -> offline-demo transfer, driven over HTTP.

This is the credential-free end-to-end path: OfflineDemoAuthServiceExtension
bypasses OAuth entirely -- its "authorization URL" points straight back at the
local callback with a hardcoded code, and generateAuthData returns a fixed
token -- so no provider API keys are involved at any step.
"""

import re

from dtp import decode_job_id

# OfflineDemoAuthServiceExtension declares "OFFLINE-DEMO" while
# OfflineDemoTransferExtension declares "offline-demo".
# PortabilityAuthServiceProviderRegistry does an exact-match MapBinder lookup
# with no normalisation, whereas TransferExtension.supportsService lowercases
# both sides -- so only the auth extension's spelling satisfies both.
SERVICE = "OFFLINE-DEMO"

# The same vertical, spelled two ways. In a JSON body Jackson wants the
# DataVertical's @JsonValue; in a path segment JAX-RS resolves the enum
# constant with Enum.valueOf.
DATA_TYPE_JSON = "OFFLINE-DATA"
DATA_TYPE_ENUM = "OFFLINE_DATA"

# Must match the security extension baked into the jar (see e2e/run.sh).
# JobProcessor.getAuthDecryptService compares with String.equals and, on a
# mismatch, returns *without transferring* -- the finally block still marks the
# job ERROR, so it is a silent no-transfer rather than a silent success.
ENCRYPTION_SCHEME = "cleartext"

# offline-demo never dereferences this; it exists because the API requires it.
CALLBACK_URL = "http://localhost:3000/callback/offline-demo"

# Set by OfflineDemoExporter. Asserting on the value is what distinguishes
# "a job ran" from "the data arrived".
EXPECTED_PAYLOAD = "offline-demo data"

TRANSFER_TIMEOUT = 120.0


def test_api_advertises_the_credential_free_vertical(client):
    services = client.services_for(DATA_TYPE_ENUM)

    assert SERVICE in services["exportServices"]
    assert SERVICE in services["importServices"]


def test_transfer_completes_and_delivers_the_payload(client, server_log):
    encoded_job_id = client.create_job(
        export_service=SERVICE,
        import_service=SERVICE,
        data_type=DATA_TYPE_JSON,
        encryption_scheme=ENCRYPTION_SCHEME,
        callback_url=CALLBACK_URL,
    )
    assert encoded_job_id

    export_auth = client.generate_auth(encoded_job_id, "EXPORT", CALLBACK_URL)
    import_auth = client.generate_auth(encoded_job_id, "IMPORT", CALLBACK_URL)
    assert export_auth and import_auth

    client.reserve_worker(encoded_job_id)
    client.await_worker_claim(encoded_job_id)

    client.start_job(encoded_job_id, export_auth, import_auth)

    job_id = re.escape(decode_job_id(encoded_job_id))

    # Wait for the job to finish, and bail out the moment anything is logged at
    # SEVERE against this job id. A clean run emits no SEVERE lines at all, so
    # the pattern needs no list of known failures and stays adapter-agnostic.
    #
    # "with 0 error(s)" is necessary but NOT sufficient, which is worth being
    # explicit about because it is a trap. That count is the size of
    # copier.getErrors() -- the errors the idempotent import executor logged --
    # so a job that never copied anything also reports zero. Pointing the jar
    # at a different encryptionScheme than the driver posts reproduces it
    # exactly: JobProcessor logs "No auth decrypter found for scheme ...",
    # returns without transferring, and its finally block still prints
    # "Finished processing ... with 0 error(s)". That is why the delivered
    # payload is asserted separately below, and why failure is detected from
    # the SEVERE line rather than from the absence of a success line.
    server_log.wait_for(
        rf"Finished processing jobId: {job_id} with 0 error\(s\)\.",
        timeout=TRANSFER_TIMEOUT,
        fail_on=rf"SEVERE[^\n]*{job_id}",
        client=client,
    )

    # Asserted separately, and on purpose: a job can report success without
    # having moved anything. OfflineDemoImporter's println is the only artifact
    # a successful import leaves behind.
    #
    # Phrased through assert_contains rather than `in server_log.read()` so a
    # failure prints the tail of the log instead of all 30KB of it.
    server_log.assert_contains(
        "Received offline data:", "the importer never received anything"
    )
    server_log.assert_contains(
        EXPECTED_PAYLOAD, "the importer ran but the exported payload did not arrive"
    )
