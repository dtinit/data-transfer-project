"""A complete offline-demo -> offline-demo transfer, driven over HTTP.

This is the credential-free end-to-end path: OfflineDemoAuthServiceExtension
bypasses OAuth entirely -- its "authorization URL" points straight back at the
local callback with a hardcoded code, and generateAuthData returns a fixed
token -- so no provider API keys are involved at any step.
"""

from dtp import run_transfer

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
    run_transfer(
        client,
        server_log,
        service=SERVICE,
        data_type=DATA_TYPE_JSON,
        encryption_scheme=ENCRYPTION_SCHEME,
        callback_url=CALLBACK_URL,
        timeout=TRANSFER_TIMEOUT,
    )

    # OfflineDemoImporter's println is the only artifact a successful import
    # leaves behind.
    server_log.assert_contains(
        "Received offline data:", "the importer never received anything"
    )
    server_log.assert_contains(
        EXPECTED_PAYLOAD, "the importer ran but the exported payload did not arrive"
    )
