"""Fixtures shared by the e2e tests.

Both the API base URL and the log path come from the environment so the driver
stays independent of how the server is deployed -- see docker-compose.yml.
"""

import os

import pytest

from dtp import DtpClient, ServerLog
from wiremock import WireMock

BASE_URL = os.environ.get("DTP_BASE_URL", "https://localhost:8080")
LOG_PATH = os.environ.get("DTP_LOG", "/var/log/dtp/dtp.log")
WIREMOCK_IMGUR_URL = os.environ.get("WIREMOCK_IMGUR_URL", "http://wiremock-imgur:8080")

# Boot covers a JVM start plus every unconfigured provider adapter logging
# "Did you set X_KEY and X_SECRET?" on the way past.
READY_TIMEOUT = float(os.environ.get("DTP_READY_TIMEOUT", "180"))


@pytest.fixture(scope="session")
def client() -> DtpClient:
    dtp = DtpClient(BASE_URL)
    dtp.await_ready(READY_TIMEOUT)
    return dtp


@pytest.fixture(scope="session")
def server_log() -> ServerLog:
    return ServerLog(LOG_PATH)


@pytest.fixture(scope="session")
def imgur_mock() -> WireMock:
    """Only started when run.sh brings up the `imgur` compose profile, which is
    also the only time an @pytest.mark.imgur test is selected."""
    return WireMock(WIREMOCK_IMGUR_URL)
