# Photo Transfer: File Types, Sizes, and Job Scale

Reference for implementors building photo import/export adapters.

---

## File Types

Adapters receive the MIME type via `PhotoModel.getMimeType()` and filename via `PhotoModel.getTitle()`. Handle at minimum:

| MIME type | Extension | Notes |
|---|---|---|
| `image/jpeg` | `.jpg` | Most common across all platforms |
| `image/png` | `.png` | Screenshots, edited images |
| `image/heic` / `image/heif` | `.heic` | Default on iOS since 2017; may be majority of Apple libraries |
| `image/gif` | `.gif` | Animated GIFs from social platforms |
| `image/webp` | `.webp` | Common from Android / Google Photos |
| `image/tiff` | `.tiff` | Lossless; DSLR and professional libraries |
| `image/x-raw` (various) | `.dng`, `.cr2`, `.nef`, `.arw` | RAW from DSLRs; 15–80 MB each |
| `video/mp4` | `.mp4` | Most common video format |
| `video/quicktime` | `.mov` | Default iOS video |

> Some exporters omit or genericise `mediaType`. Use the file extension as a fallback and accept unknown types rather than rejecting them.

---

## File Sizes and Limits

| Format | Typical size |
|---|---|
| JPEG (phone) | 2–8 MB |
| HEIC (phone) | 1–5 MB |
| TIFF / RAW (DSLR) | 10–80 MB |
| Video (short clip) | 50–500 MB |
| Video (4K, 1 min) | ~350 MB |

**Enforced limits:**
- **Apple:** 50 GB per file (`ApplePhotosConstants.maxMediaTransferByteSize`). Exceeded files throw `AppleContentException` — handle per-item, not as a job failure.
- **Amazon:** No hard per-file limit. Storage exhaustion is caught as `DestinationMemoryFullException` — this is terminal.
- **Amazon field lengths:** Album names and photo titles truncated to 200 characters via `AmazonPhotosTransmogrificationConfig`. Note the ` - Imported from <ExportService>` suffix is appended after truncation, so effective usable length is ~170 characters.

---

## Batching (Apple)

All constants in `ApplePhotosConstants`:

| Parameter | Value |
|---|---|
| Albums per API call | 50 |
| Photos/videos per API call | 50 |
| Upload chunk size | 50 MB |

Amazon uploads each file whole (no chunking); temp files are deleted after each upload.

---

## Typical Job Sizes

| Library | Albums | Photos | Total |
|---|---|---|---|
| Small | 5–20 | 500–2k | 1–10 GB |
| Medium | 20–100 | 2k–20k | 10–100 GB |
| Large | 100–500 | 20k–100k | 100 GB–1 TB |

Design against the **medium** baseline (50 GB, ~10,000 photos). At 50-item batches that's 200+ API calls to Apple; at 50 MB chunks, 1,000+ upload chunks.

---

## Key Implementor Notes

- **HEIC** may dominate Apple libraries — handle it explicitly or transcode; don't silently skip.
- **Resume support** is essential for large jobs. Participate in `IdempotentImportExecutor` by recording item IDs after each successful upload.
- **Duplicates** should be silently skipped (both Apple and Amazon do this). Don't treat them as failures.
- **Null MIME types / missing titles** do occur. Fall back to file extension and `"untitled"` respectively.
