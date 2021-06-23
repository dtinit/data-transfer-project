# Photobucket

This folder contains the extension implementation for [Photobucket](https://photobucket.com).

## Data Supported

- Photos import

## Current State

- Does not support exports.

## Keys & Auth

- Photobucket uses OAuth 2 for authorization.

## Maintained By

The Photobucket extension was created and is maintained by
[Photobucket developers](https://photobucket.com).

## Errors and exceptions

| Exception     | Message       | Explanation   | Notes        |
| ------------- | ------------- | ------------- |------------- |
| OverlimitException                | User reached his limits, unable to proceed with data import.  | Critical, unable to migrate content as user reached its limits on PB side| |
| IllegalStateException             | Unable to get input stream for image %image_title%  | No valid stream was provided, unable to import particular image file | Need to consider whether we need to migrate rest of content or not. For now import will be stopped |
| GraphQLException                  | Empty response body was provided by GQL server | Back end issue, no valid response was provided. Retry can be applied. | |
| MethodNotImplementedException     | PhotobucketPhotosExporter is not implemented yet, unable to export data. | Data export if not supported | |
| WrongStatusCodeRetriableException | GQL server provided response code >= 500. | Wrong response code provided by back end. Retry can be applied. | |
| WrongStatusCodeException          | Wrong status code=[%code%] provided by GQL server for jobId=[%jobId%] | Wrong response code, most probably retry won't help, as with the same data result is idempotent |
| WrongStatusCodeRetriableException | REST server provided response code >= 500. | Wrong response code provided by back end. Retry can be applied. | |
| WrongStatusCodeException          | Wrong status code=[%code%] provided by REST server for jobId=[%jobId%] | Wrong response code, most probably retry won't help, as with the same data result is idempotent |
| ResponseParsingException          | Unable to process GQL response to get root album id | Parsing error, retry won't help | |
| ResponseParsingException          | Unable to process REST response to get user stats | Parsing error, retry won't help | |
| AlbumImportException              | Album was not created | Thrown in case of BE failure when unable to create album | Critical only while top level album creation. If any other album was not created, images will migrate to top level album |
