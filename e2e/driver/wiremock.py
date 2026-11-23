"""Reader for a WireMock instance's /__admin/requests journal."""

from __future__ import annotations

import urllib.parse

import requests


class WireMock:
    def __init__(self, base_url: str, timeout: float = 30.0):
        self.base_url = base_url.rstrip("/")
        self.timeout = timeout

    def reset_journal(self) -> None:
        """Clear the journal; matters only for reruns against a long-lived mock."""
        response = requests.delete(
            f"{self.base_url}/__admin/requests", timeout=self.timeout
        )
        response.raise_for_status()

    def journal(self) -> list[dict]:
        """Every request received since the last reset."""
        response = requests.get(
            f"{self.base_url}/__admin/requests", timeout=self.timeout
        )
        response.raise_for_status()
        return [entry["request"] for entry in response.json().get("requests", [])]

    def requests_to(self, method: str, url_prefix: str) -> list[dict]:
        return [
            entry
            for entry in self.journal()
            if entry["method"] == method and entry["url"].startswith(url_prefix)
        ]


def form_params(entry: dict) -> dict[str, str]:
    """Decode an x-www-form-urlencoded request body."""
    return dict(urllib.parse.parse_qsl(entry.get("body", ""), keep_blank_values=True))
