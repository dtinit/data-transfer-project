# Google
This folder contains the extension implementation for the
[Google](https://www.google.com).

## Data Supported

 - Blogger Import of Social Posts.
 - Calendar Import & Export
 - Contacts Import & Export
 - G+ export of posts
 - Mail Import & Export
 - Photos Import & Export
 - Tasks Import & Export
 - Social Posts Export

## Current State

 - Calendar doesn't support recurring events yet
 - All services still need some final testing
 - G+ exports are limited by what is avalible in their API:
   - Export is only for public posts
   - For posts with multiple albums only the thumbnails are ported
 - Blogger import:
    - Imports into your first blog, doesn't create a new blog for the imported content
    - Stores attached photos in Drive

## Keys & Auth

Sign up for a [Google Cloud developer account](https://console.developers.google.com/start)
and then enable the appropriate APIs in the console.

Google uses OAuth 2 for authorization.

## Maintained By

The Google extension was created and maintained by the
[DTP maintainers](mailto:portability-maintainers@googlegroups.com)
(this includes developers from Google).