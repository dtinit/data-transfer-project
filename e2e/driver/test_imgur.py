"""Imgur -> Imgur against WireMock (e2e/mocks/imgur). See e2e/README.md."""

import base64
import pathlib

import pytest

from dtp import run_transfer
from wiremock import WireMock, form_params

SERVICE = "Imgur"
DATA_TYPE = "PHOTOS"
ENCRYPTION_SCHEME = "cleartext"

# Sent as redirect_uri; never visited.
CALLBACK_URL = "http://localhost:3000/callback/imgur"

TRANSFER_TIMEOUT = 180.0

# What the mock's token endpoint hands out (e2e/mocks/imgur/mappings/oauth-token.json).
ACCESS_TOKEN = "e2e-access-token"

FIXTURES = pathlib.Path(__file__).resolve().parents[1] / "mocks/imgur/__files/files"

EXPECTED_ALBUMS = {"Album 1", "Album 2", "Album 3", "Non-album photos"}

# Photo -> the album id the mock returned when that album was created, resolved
# through IdempotentImportExecutor's cache across copy iterations.
EXPECTED_PHOTOS = {
    "album1Photo1": "imported-Album 1",
    "album1Photo2": "imported-Album 1",
    "album2Photo1": "imported-Album 2",
    "album3Photo1": "imported-Album 3",
    "nonAlbumPhoto1": "imported-Non-album photos",
    "nonAlbumPhoto2": "imported-Non-album photos",
}


@pytest.fixture(scope="module")
def imgur_mock() -> WireMock:
    return WireMock("http://wiremock-imgur:8080")


def test_transfer_delivers_every_photo_to_the_right_album(client, server_log, imgur_mock):
    """One test: the transfer is one expensive act; split assertions would need
    re-runs or ordering."""
    imgur_mock.reset_journal()

    run_transfer(
        client,
        server_log,
        service=SERVICE,
        data_type=DATA_TYPE,
        encryption_scheme=ENCRYPTION_SCHEME,
        callback_url=CALLBACK_URL,
        timeout=TRANSFER_TIMEOUT,
    )

    # The token the mock issued reached both the exporter and the importer.
    api_calls = imgur_mock.requests_to("GET", "/3/") + imgur_mock.requests_to("POST", "/3/")
    tokens = {call["headers"].get("Authorization") for call in api_calls}
    assert tokens == {f"Bearer {ACCESS_TOKEN}"}, f"unexpected Authorization headers: {tokens}"

    # -- albums ---------------------------------------------------------------
    album_posts = [form_params(e) for e in imgur_mock.requests_to("POST", "/3/album")]
    by_title = {album.get("title"): album for album in album_posts}
    assert set(by_title) == EXPECTED_ALBUMS, f"wrong albums created: {sorted(by_title)}"
    # A null description is omitted rather than sent empty.
    assert "description" not in by_title["Album 1"]
    assert by_title["Album 2"]["description"] == "Description for Album 2"

    # -- photos ---------------------------------------------------------------
    fixtures = {p.stem: p.read_bytes() for p in FIXTURES.glob("*.jpg")}
    by_content = {v: k for k, v in fixtures.items()}
    assert len(by_content) == len(fixtures), "fixture images are not distinct"

    # Each photo downloaded once: the account-wide listing repeats every album
    # photo, and the exporter has to skip those. The importer's id cache would
    # hide a second download from the upload checks below.
    downloads = sorted(e["url"] for e in imgur_mock.requests_to("GET", "/files/"))
    assert downloads == sorted(f"/files/{name}.jpg" for name in fixtures), (
        f"each fixture should be downloaded exactly once; got {downloads}"
    )

    # Matching uploads to fixtures by content covers the export download,
    # LocalTempFileStore and the import read.
    delivered = {}
    for entry in imgur_mock.requests_to("POST", "/3/image"):
        params = form_params(entry)
        payload = base64.b64decode(params["image"])
        assert payload in by_content, "an uploaded image matches no fixture byte for byte"
        name = by_content[payload]
        assert name not in delivered, f"{name} was uploaded more than once"
        delivered[name] = params.get("album")

    assert delivered == EXPECTED_PHOTOS, (
        f"wrong photos, or wrong album mapping.\n  expected: {EXPECTED_PHOTOS}\n"
        f"  actual:   {delivered}"
    )
