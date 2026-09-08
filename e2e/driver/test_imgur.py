"""An Imgur -> Imgur transfer against a WireMock stand-in for api.imgur.com.

Where the offline-demo suite proves the machinery, this proves a data path: a
real, unmodified provider adapter paginating over HTTP, recursing into
sub-resources, downloading bytes into the temp store, and reading them back out
on the import side.

Unlike offline-demo, Imgur runs a real OAuth2 token exchange. That is diverted
to the mock rather than skipped -- see e2e/config/config/imgur.yaml -- so the
driver's request sequence stays identical for both adapters.
"""

import pathlib
import re

import pytest

from dtp import decode_job_id
from wiremock import decoded_image, form_params

# ImgurOAuthConfig.getServiceName() and ImgurTransferExtension.SERVICE_ID are
# both "Imgur", and TransferExtension.supportsService lowercases, so unlike
# offline-demo one spelling works for both the auth registry's exact-match
# lookup and the transfer registry.
SERVICE = "Imgur"

# PHOTOS is its own @JsonValue, so body and path spellings agree here too.
DATA_TYPE_JSON = "PHOTOS"
DATA_TYPE_ENUM = "PHOTOS"

ENCRYPTION_SCHEME = "cleartext"

# Never visited: the driver stands in for the browser leg of the OAuth flow.
CALLBACK_URL = "http://localhost:3000/callback/imgur"

# What the mock's token endpoint hands back. Asserting on it is what proves the
# divert worked rather than a real request having silently failed.
EXPECTED_ACCESS_TOKEN = "e2e-access-token"

TRANSFER_TIMEOUT = 180.0

FIXTURES = pathlib.Path(__file__).resolve().parents[1] / "mocks/imgur/__files/files"

# Seeded albums, and the id the mock hands back for each. Photos are expected to
# arrive carrying the *returned* id, not the original -- that round trip through
# IdempotentImportExecutor's cache is the thing being checked.
EXPECTED_ALBUMS = {
    "Album 1": "imported-album-1",
    "Album 2": "imported-album-2",
    "Album 3": "imported-album-3",
    "Non-album photos": "imported-album-default",
}

# Every photo the export should yield, and the album it belongs in. The two
# nonAlbum* entries are the ones the exporter has to *deduce*: the account-wide
# listing returns album photos too, and it keeps only the ids it has not already
# seen inside an album.
EXPECTED_PHOTOS = {
    "album1Photo1": "imported-album-1",
    "album1Photo2": "imported-album-1",
    "album2Photo1": "imported-album-2",
    "album3Photo1": "imported-album-3",
    "nonAlbumPhoto1": "imported-album-default",
    "nonAlbumPhoto2": "imported-album-default",
}


@pytest.mark.imgur
def test_auth_token_exchange_is_diverted_to_the_mock(client, imgur_mock):
    """S1: the OAuth2 token exchange reaches WireMock, not api.imgur.com.

    This is the step that has no offline-demo equivalent.
    OAuth2DataGenerator.generateAuthData POSTs to config.getTokenUrl() for
    real, so without the divert the run either hangs on a network call or
    fails against Imgur's actual API.

    It also covers the trap underneath: OAuth2ServiceExtension.initialize
    swallows a missing-credential IOException and returns *without* setting
    `initialized`, so absent IMGUR_KEY/IMGUR_SECRET this fails at
    "Cannot get OAuth2DataGenerator before initialization" -- on job creation,
    nowhere near the actual cause.
    """
    imgur_mock.reset_requests()

    encoded_job_id = client.create_job(
        export_service=SERVICE,
        import_service=SERVICE,
        data_type=DATA_TYPE_JSON,
        encryption_scheme=ENCRYPTION_SCHEME,
        callback_url=CALLBACK_URL,
    )
    assert encoded_job_id

    export_auth = client.generate_auth(encoded_job_id, "EXPORT", CALLBACK_URL)

    assert EXPECTED_ACCESS_TOKEN in export_auth, (
        "the token exchange did not come back with the mock's token; "
        f"got: {export_auth}"
    )
    assert imgur_mock.received("POST", "/oauth2/token"), (
        "WireMock never saw the token request -- the tokenUrl override in "
        "e2e/config/config/imgur.yaml did not take effect"
    )


@pytest.mark.imgur
def test_transfer_delivers_every_photo_to_the_right_album(client, server_log, imgur_mock):
    """S2: a complete Imgur -> Imgur transfer, asserted on what the mock received.

    Deliberately one test rather than several. The transfer is a single
    expensive act against shared state, and splitting the assertions across
    tests would either re-run it or make them order-dependent -- both worse
    than a long test body.
    """
    imgur_mock.reset_requests()

    encoded_job_id = client.create_job(
        export_service=SERVICE,
        import_service=SERVICE,
        data_type=DATA_TYPE_JSON,
        encryption_scheme=ENCRYPTION_SCHEME,
        callback_url=CALLBACK_URL,
    )
    export_auth = client.generate_auth(encoded_job_id, "EXPORT", CALLBACK_URL)
    import_auth = client.generate_auth(encoded_job_id, "IMPORT", CALLBACK_URL)

    client.reserve_worker(encoded_job_id)
    client.await_worker_claim(encoded_job_id)
    client.start_job(encoded_job_id, export_auth, import_auth)

    job_id = re.escape(decode_job_id(encoded_job_id))

    # Same completion signal and same fail-fast heuristic as offline-demo, and
    # for the same reason: "0 error(s)" is the size of copier.getErrors(), so a
    # job that copied nothing reports it too. Everything below is what actually
    # distinguishes a transfer from a no-op.
    server_log.wait_for(
        rf"Finished processing jobId: {job_id} with 0 error\(s\)\.",
        timeout=TRANSFER_TIMEOUT,
        fail_on=rf"SEVERE[^\n]*{job_id}",
        client=client,
    )

    # -- the copier actually recursed -------------------------------------
    #
    # Nine iterations are expected: albums pages 0/1/2, the three album image
    # sub-resources, and non-album pages 0/1/2. Asserting ">1" rather than "==9"
    # keeps this from breaking every time a fixture gains a row, while still
    # failing if pagination silently stops firing.
    iterations = server_log.count_matches(rf"Job {job_id}: Copy iteration: ")
    assert iterations > 1, (
        f"the copier ran {iterations} iteration(s) -- it never recursed, so "
        "neither pagination nor sub-resource traversal was exercised"
    )

    # Pagination specifically, on both axes.
    #
    # Note these check for page *2*, not page 1. The exporter derives "there is
    # more" from the current page being non-empty, so it always requests page 1
    # -- even when page 0 was the last page with data. Asking for page 2 is
    # therefore the first request that proves a second page actually had
    # content, which is the property worth asserting. Checking page 1 would
    # pass on single-page fixtures and quietly cover nothing.
    assert imgur_mock.received("GET", "/3/account/me/albums/2"), (
        "album pagination never got past the first page of data -- the fixture "
        "may have shrunk to a single page"
    )
    assert imgur_mock.received("GET", "/3/account/me/images/2"), (
        "non-album photo pagination never got past the first page of data"
    )

    # -- albums arrived ----------------------------------------------------
    album_posts = imgur_mock.requests_to("POST", "/3/album")
    created = {form_params(entry).get("title") for entry in album_posts}
    assert created == set(EXPECTED_ALBUMS), (
        f"wrong albums created.\n  expected: {sorted(EXPECTED_ALBUMS)}\n"
        f"  actual:   {sorted(created)}"
    )

    # Album 1's description is null in the fixture, and importAlbum omits the
    # field entirely rather than sending an empty one.
    by_title = {form_params(e).get("title"): form_params(e) for e in album_posts}
    assert "description" not in by_title["Album 1"]
    assert by_title["Album 2"]["description"] == "Description for Album 2"

    # -- photos arrived, byte for byte -------------------------------------
    image_posts = imgur_mock.requests_to("POST", "/3/image")

    # Map each upload back to its source fixture by content. This is the
    # assertion that covers the whole data path at once: the exporter's
    # HttpURLConnection download, the write into LocalTempFileStore, and the
    # importer reading the stream back out.
    fixtures = {p.stem: p.read_bytes() for p in FIXTURES.glob("*.jpg")}
    by_content = {v: k for k, v in fixtures.items()}
    assert len(by_content) == len(fixtures), "fixture images are not distinct"

    delivered = {}
    for entry in image_posts:
        payload = decoded_image(entry)
        assert payload in by_content, (
            "an uploaded image does not match any fixture byte for byte -- the "
            "temp store round trip corrupted it or served the wrong stream"
        )
        name = by_content[payload]
        assert name not in delivered, f"{name} was uploaded more than once"
        delivered[name] = form_params(entry).get("album")

    assert delivered == EXPECTED_PHOTOS, (
        f"wrong photos, or wrong album mapping.\n  expected: {EXPECTED_PHOTOS}\n"
        f"  actual:   {delivered}"
    )

    # -- ordering ----------------------------------------------------------
    # Every album is created before the first photo that references it; the
    # copier's contract is that parents are populated before children.
    journal = imgur_mock.requests()
    first_image = next(
        i for i, e in enumerate(journal)
        if e["method"] == "POST" and e["url"].startswith("/3/image")
    )
    last_needed_album = max(
        i for i, e in enumerate(journal)
        if e["method"] == "POST" and e["url"].startswith("/3/album")
        and form_params(e).get("title") in ("Album 1", "Album 2", "Album 3")
    )
    assert last_needed_album < first_image, (
        "a photo was uploaded before the album it belongs to was created"
    )
