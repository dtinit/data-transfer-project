# Apple
This folder contains the extension implementation for the [Apple](https://www.apple.com).

## Data Supported

 - Photos Import (JPEG, PNG, HEIC/HEIF, GIF, WebP, TIFF, RAW, and others)
 - Videos Import (MP4, MOV, and others)
 - Music Playlists Import

## Transfer Limits

| Parameter | Limit |
|---|---|
| Max file size | 50 GB (`ApplePhotosConstants.maxMediaTransferByteSize`) |
| Upload chunk size | 50 MB (`ApplePhotosConstants.contentRequestLength`) |
| Albums per API batch | 50 (`ApplePhotosConstants.maxNewAlbumRequests`) |
| Photos/videos per API batch | 50 (`ApplePhotosConstants.maxNewMediaRequests`) |

See [`Documentation/PhotoTransferExpectations.md`](../../../../Documentation/PhotoTransferExpectations.md)
for full guidance on expected file types, file sizes, and total job sizes.
 
## Current State

 - Photos:
   - Exporting is supported via https://privacy.apple.com/
   - Importing is supported via the exporting source platforms (Ex: Meta, Google etc).
 - Music:
   - Exporting is supported via https://privacy.apple.com/
   - Importing is supported via the exporting source platforms (Ex: Meta, Google etc).
 
## Keys & Auth
Apple uses OAuth2 for authorization.

### Create a developer account
Before getting started you will need a [Apple Developer account](https://developer.apple.com/help/account/get-started/about-your-developer-account).

### Create an OAuth App
After creating a developer account, you will need to [create/register your OAuth App](https://developer.apple.com/help/account/manage-identifiers/register-an-app-id).

## Maintained By

The Apple extension was created and maintained by the
[DTP maintainers](mailto:portability-maintainers@googlegroups.com)
(this includes developers from Apple).
