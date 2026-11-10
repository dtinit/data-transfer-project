"""Reader for a WireMock standalone instance's admin API.

Nothing here is adapter-specific -- it is the generic "what did the mock
actually receive" surface. WireMock records every request it served, bodies
intact, at ``/__admin/requests``, which is what makes assertions about the
delivered payload possible at all; the offline-demo suite has to settle for
grepping a log line.
"""

from __future__ import annotations

import base64
import urllib.parse

import requests


class WireMock:
    def __init__(self, base_url: str, timeout: float = 30.0):
        self.base_url = base_url.rstrip("/")
        self.timeout = timeout

    # -- admin ------------------------------------------------------------

    def reset_requests(self) -> None:
        """Clear the request journal, so one test cannot see another's traffic."""
        response = requests.delete(
            f"{self.base_url}/__admin/requests", timeout=self.timeout
        )
        response.raise_for_status()

    def requests(self) -> list[dict]:
        """Every request served since the last reset, oldest first.

        WireMock returns them newest-first; reversing here means callers can
        assert on ordering (albums before photos, say) by list position, which
        is the obvious reading.
        """
        response = requests.get(
            f"{self.base_url}/__admin/requests", timeout=self.timeout
        )
        response.raise_for_status()
        entries = [entry["request"] for entry in response.json().get("requests", [])]
        return list(reversed(entries))

    # -- querying ---------------------------------------------------------

    def requests_to(self, method: str, url_prefix: str) -> list[dict]:
        return [
            entry
            for entry in self.requests()
            if entry["method"] == method and entry["url"].startswith(url_prefix)
        ]

    def received(self, method: str, url_prefix: str) -> bool:
        return bool(self.requests_to(method, url_prefix))


def form_params(entry: dict) -> dict[str, str]:
    """Decode an ``application/x-www-form-urlencoded`` body.

    Both Imgur import calls post form bodies rather than JSON --
    ``FormBody.Builder`` in ImgurPhotosImporter -- so this is how the delivered
    album titles and image bytes are read back out.
    """
    return dict(urllib.parse.parse_qsl(entry.get("body", ""), keep_blank_values=True))


def decoded_image(entry: dict) -> bytes:
    """The raw bytes behind a POST /image call's base64 ``image`` parameter."""
    return base64.b64decode(form_params(entry)["image"])
