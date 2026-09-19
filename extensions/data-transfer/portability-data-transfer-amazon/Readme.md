# Amazon Photos

This folder contains the extension implementation for [Amazon Photos](https://www.amazon.com/photos).

## Data Supported

 - Photos Import (JPEG, PNG, HEIC/HEIF, GIF, WebP, TIFF, RAW, and others)
 - Videos Import (MP4, MOV, and others)

> **Note:** This extension is import-only. Amazon Photos export is not currently supported by DTP;
> users can export their data via Amazon's own data export tools.

## Transfer Behaviour

- Albums are created with a ` - Imported from <ExportService>` suffix to distinguish them from
  pre-existing Amazon Photos albums.
- Duplicate files (detected by Amazon's API) are silently skipped — they do not count as failures.
- If the destination runs out of storage, the job terminates with `DestinationMemoryFullException`.
  This is a terminal condition; the user must free space and restart the transfer.

## Field Limits

Amazon Photos enforces maximum lengths on string fields. DTP applies these automatically via
`AmazonPhotosTransmogrificationConfig` before each import:

| Field | Max length |
|---|---|
| Album name | 200 characters |
| Photo title | 200 characters |

Note that the ` - Imported from <ExportService>` suffix (up to ~30 characters) is appended
**after** transmogrification, so effective album-name space from the source is ~170 characters.

## Keys & Auth

Amazon Photos uses OAuth2. You will need an Amazon developer account and a registered OAuth
application to obtain a client ID and client secret.

Set the following environment variables (or populate `env.secrets`):

```
AMAZON_KEY=<your client id>
AMAZON_SECRET=<your client secret>
```

## File Types and Job Size Expectations

See [`Documentation/PhotoTransferExpectations.md`](../../../../Documentation/PhotoTransferExpectations.md)
for full guidance on expected file types, individual file sizes, and total job scale.

## Maintained By

The Amazon extension was created and maintained by the
[DTP maintainers](mailto:portability-maintainers@googlegroups.com).
