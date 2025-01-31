# Generic Importers

## Overview

The Data Transfer Project transfer worker manages the transfer of data between data exporter and data importer services. \
The transfer of some set of user data is encapsulated by a transfer job. Each job consists of many data items from a particular data vertical (CALENDAR, PHOTOS, etc.). See the [Schemas](#schemas) section for the list of data verticals supported by Generic Importers.

Transfer jobs are initiated by a user, generally on the platform owned by the exporter service. When processing a transfer job the transfer worker pulls data items from the exporter service and pushes those data items to the importer service. \
The interface for pulling data from exporters and pushing to importers are implemented in DTP by `Exporter` modules and `Importer` modules respectively. This allows the DTP worker to transfer data between services agnostic of protocol and encoding, and without assumptions about the storage or organisation of data at the destination.

The typical way to integrate with DTP as a data importer is to implement an `Importer` module through an extension that pushes data to a new or existing API. This requires writing and maintaining a Java extension, but gives complete control over how the module interacts with your API. \
As an alternative path for integration, the Generic Importers extension provides an `Importer` module implementation that can be configured to call any endpoint conforming to the API described in the [Generic Importer API](#generic-importer-api) section. This allows developers to focus on languages and frameworks they are familiar with, at the expense of being able to customise the behaviour of the importer or reusing existing APIs.

## Generic Importer API

The Generic Importer API uses HTTP as the application layer protocol and encodes most request data as `application/json`. Data items are sent one-per-request 

The service should accept POST requests on a given base URL, and kebab-case sub-path specific to each data vertical (`https://example.com/import/blobs`, `https://example.com/import/social-posts`). \
See the [HTTP Interface](#http-interface) section for details on the API.

When a transfer job is created, credentials to call your service endpoint are obtained using OAuth 2.0 (see [Authentication and Authorization](#authentication-and-authorization) below).
Your service should therefore support OAuth 2.0 or integrate with another service which provides these features.

### DTP Behaviour

This section contains information on how the DTP platform behaves, and in particular how the Generic `Importer` module works. This should help to guide decisions in how you implement your importer service.

#### Job Life-cycle

Transfer jobs get created by users on the platform hosting the DTP transfer worker, which is generally owned by the exporter. \
During job creation the platform needs to obtain credentials to call the exporting service and importing service on behalf of the user, which for importer services using Generic Importers means using the OAuth 2.0 Authorization Code flow discussed in [Authentication and Authorization](#authentication-and-authorization). \
Once a DTP transfer worker begins working on the transfer job it will begin sending POST requests to your endpoint containing user data items to associate with their account, authenticated and authorized using the access code obtained at job creation. \
The job continues until all items have been sent.

> [!NOTE]
> The Generic Importers extension currently provides no mechanism for informing an importer service of the beginning or end of a transfer job.

#### Ordering

Data items are sent to your importer service in the same order that they are exported from the exporter service. This makes the order of data items transferred in a job an implementation detail of the `Exporter` module and corresponding exporter service, however there are is a helpful convention that Exporters tend to follow; for data items that are containers of other items, such as photo albums, these items are conventionally exported before the data items contained inside them (or at most in the same page). \
The Generic `Importer` module also follows this convention; exporters export data in pages (`ContainerResource`s, for those reading the code), and the Generic `Importer` module will export albums in a page before the photos in that page.

##### Failures on dependent items

In some circumstances the transfer of a data item that is a container of other data items may fail. \
The DTP transfer worker makes a best-effort to transfer all data, and does not consider the dependencies between data items; importer services may not respect this dependency, or have their own way of recovering from such failures.
For this reason if a container data item fails to transfer then the data items contained within that container will still be transferred. It is the responsibility of the importer service to decide how to handle this case.

#### Concurrency

To keep the API simple, data items in a transfer job are sent by the `Importer` module to your HTTP service sequentially and as one data item per request. \
Your API may receive multiple concurrent requests, even for the same user, if there are multiple concurrent transfer jobs to your service.

#### Data Rates

The limiting factors for data import rate are the speeds of the export service and your HTTP service. Since export services batch their exports to the DTP transfer worker and are typically hosted in or near the same data-center as the transfer worker, this practically means data will be sent as fast as your HTTP service can accept it. \
If your service cannot accept data at the rate sent by the DTP transfer worker some strategies for managing this include throttling and rate-limiting.

Your service can throttle the DTP transfer worker by setting a maximum request rate per unit of time and queueing requests that exceeds that quota until the next time period. Since the Generic Importer module sends each data item sequentially the next data item will not be sent until your service has completed a HTTP response for the current item, making this an effective strategy to slow the transfer worker down. \
Note that throttling requests for too long may cause the HTTP request to time-out.

Alternatively or additionally, your service can rate limit the DTP worker by responding with a 429 Too Many Requests status code when the transfer worker is sending data too quickly. The request will be retried with a delay as described in the [Retries](#retries) section. \
Note that the transfer worker will not retry sending a given data item indefinitely, so this may result in lost data.

#### Retries

If a request sent to your HTTP service fails it will usually be retried. The retry and back-off logic is based on a configuration set for the instance of the DTP transfer worker that processes the transfer job. The retry logic is therefore typically configured by the owners of each exporter service.

The DTP transfer worker's default retry strategy is defined in [distributions/demo-server/src/main/resources/config/retry/default.yaml]. At time of writing, the default strategy is an exponential back-off with 1.5x multiplier, starting at 1000ms, with up to 5 retry attempts. \
If the default retry behaviour is not sufficient for your service, please contact the owner of the exporter service to configure a more specific retry strategy for your service, or create an issue or pull request to make this configurable per-import service.

### HTTP Interface

The Generic `Importer` module maps each data item in a job to one HTTP request made to your HTTP service. \
Data will be POSTed to your endpoint with a `Content-Type` of either `application/json` for basic data-types, or `multipart/related` for file-based data-types. See below for how to interpret each of these. \
Your endpoint should return a 20x status code if the resource has been created, 40x for errors caused by the Importer, or 50x for service errors. See [Endpoint Errors](#endpoint-errors) below for details on returning errors.

#### Basic data types

For basic data-types the Importer will send a POST request with a `Content-Type: application/json` header. The body of the POST request will be a UTF-8 encoded JSON payload conforming to the relevant data-type schema detailed in the [Schemas](#schemas) section.

For example, below is a full request of a SOCIAL-POSTS item (JSON formatted for readability)

```http
POST /import/social-posts HTTP/1.1
Content-Type: application/json
Content-Length: 660
Host: localhost:8080
Connection: Keep-Alive
Accept-Encoding: gzip
User-Agent: okhttp/3.9.1
Authorization: Bearer accessToken

{
  "@type": "GenericPayload",
  "schemaSource": ".../SocialPostsSerializer.java",
  "apiVersion": "0.1.0",
  "payload": {
    "@type": "SocialActivityData",
    "metadata": {
      "@type": "SocialActivityMetadata",
      "actor": {
        "@type": "SocialActivityActor",
        "id": "321",
        "name": "Steve",
        "url": null
      }
    },
    "activity": {
      "@type": "SocialActivityModel",
      "id": "456",
      "published": 1731604863.845677,
      "type": "NOTE",
      "attachments": [
        {
          "@type": "SocialActivityAttachment",
          "type": "IMAGE",
          "url": "foo.com",
          "name": "Foo",
          "content": null
        }
      ],
      "location": {
        "@type": "SocialActivityLocation",
        "name": "foo",
        "longitude": 10.0,
        "latitude": 10.0
      },
      "title": "Hello world!",
      "content": "Hi there",
      "url": null
    }
  }
}
```

#### File-based data types

To support large files as part of MEDIA, PHOTOS, VIDEOS and BLOBS imports, the request for these data-types is a `multipart/related` request with a `boundary`. \
The first part will be an `application/json` payload containing metadata related to the file being importer. \
The second part will have its `Content-Type` set to the MIME type of the file, and will be a multi-part stream of the bytes in the file with `boundary` separators.

```http
POST /import/blobs HTTP/1.1
Content-Type: multipart/related; boundary=1581c5eb-05d0-42fe-bfb3-472151f366cd
Content-Length: 524288384
Host: localhost:8080
Connection: Keep-Alive
Accept-Encoding: gzip
User-Agent: okhttp/3.9.1
Authorization: Bearer accessToken

--1581c5eb-05d0-42fe-bfb3-472151f366cd
Content-Type: application/json
Content-Length: 236

{"@type":"GenericPayload","schemaSource":".../BlobbySerializer.java","apiVersion": "0.1.0","payload":{"@type":"BlobbyFileData","folder":"/root/foo","document":{"name":"bar.mp4","dateModified":"2020-02-01","encodingFormat":"video/mp4"}}}
--1581c5eb-05d0-42fe-bfb3-472151f366cd
Content-Type: video/mp4
Content-Length: 524288000

...524288000 bytes follow
--1581c5eb-05d0-42fe-bfb3-472151f366cd--
```

> [!NOTE]
> Endpoints supporting file-based data-types may also receive basic data-types, for example the `/blobs` endpoint should
> support receiving folders encoded as `application/json` POST data.

### Configuration

To configure an importer, create a YAML configuration file in the `extensions/data-transfer/portability-data-transfer-generic/src/main/resources/config/` directory of this repository.

```yaml
# example.yaml
serviceConfig:
  serviceId: "Example"
  endpoint: "https://example.com/dtp/"
  verticals:
    - vertical: SOCIAL-POSTS
    - vertical: BLOBS
    - vertical: MEDIA
    - vertical: CALENDAR
```

## Schemas

Below are the [JSON schemas](https://json-schema.org/specification) for each supported vertical.

All POST requests containing data to be imported are wrapped in a top-level `GenericPayload` wrapper.

```json
{
  "$schema": "https://json-schema.org/draft/2020-12/schema",
  "type": "object",
  "properties": {
    "apiVersion": {
      "type": "string"
    },
    "payload": {
      "type": "object",
      "description": "The inner payload, which contains data from one of the described data verticals"
    },
    "schemaSource": {
      "type": "string",
      "description": "The source file containing the schema definition"
    },
    "@type": {
      "const": "GenericPayload"
    }
  },
  "required": [
    "@type",
    "schemaSource",
    "apiVersion",
    "payload"
  ]
}
```

### MEDIA

Endpoint: `/media`

The MEDIA vertical describes a user's photos and videos, and the albums that contain them.

Albums are conventionally imported before the photos and videos contained in them.

#### Basic Data Types

The only basic data type exposed for MEDIA data is `Album`, which contains metadata about an album.

```json
{
  "$schema" : "https://json-schema.org/draft/2020-12/schema",
  "type": "object",
  "properties": {
    "description": {
      "type": "string"
    },
    "id": {
      "type": "string"
    },
    "name": {
      "type": "string"
    },
    "@type": {
      "const": "Album"
    }
  },
  "required": [
    "@type",
    "id",
    "name"
  ]
}
```

#### File-based Data Types

The `Photo` and `Video` data types are sent as file-based data containing metadata described below in the JSON part of the payload.

```json
{
  "$schema" : "https://json-schema.org/draft/2020-12/schema",
  "$defs": {
    "FavoriteInfo": {
      "type": "object",
      "properties": {
        "favorite": {
          "type": "boolean"
        },
        "lastUpdateTime": {
          "type": "string",
          "format": "date-time"
        },
        "@type": {
          "const": "FavoriteInfo"
        }
      },
      "required": [
        "@type",
        "favorite",
        "lastUpdateTime"
      ]
    }
  },
  "anyOf": [
    {
      "type": "object",
      "properties": {
        "albumId": {
          "type": "string"
        },
        "description": {
          "type": "string"
        },
        "favoriteInfo": {
          "$ref": "#/$defs/FavoriteInfo"
        },
        "name": {
          "type": "string"
        },
        "uploadedTime": {
          "type": "string",
          "format": "date-time"
        },
        "@type": {
          "const": "Video"
        }
      },
      "required": [
        "@type",
        "albumId",
        "name"
      ]
    },
    {
      "type": "object",
      "properties": {
        "albumId": {
          "type": "string"
        },
        "description": {
          "type": "string"
        },
        "favoriteInfo": {
          "$ref": "#/$defs/FavoriteInfo"
        },
        "name": {
          "type": "string"
        },
        "uploadedTime": {
          "type": "string",
          "format": "date-time"
        },
        "@type": {
          "const": "Photo"
        }
      },
      "required": [
        "@type",
        "albumId",
        "name"
      ]
    }
  ]
}
```

#### PHOTOS and VIDEOS

The PHOTOS and VIDEOS verticals are implemented as a subset of the MEDIA data type. If your importer is configured to support only PHOTOS, for example, photo data will be imported using the MEDIA schemas, but containing only `Album` and `Photo` payloads.

### BLOBS

Endpoint: `/blobs`

The BLOBS vertical represents arbitrary file data and folder structures.

Folders are conventionally imported before any of their containing data, which may include folders and/or files.

#### Basic Data Types

The only basic data type exposed for BLOBS data is `Folder`, which describes a folder containing other folders and/or files.

```json
{
  "$schema": "https://json-schema.org/draft/2020-12/schema",
  "type": "object",
  "properties": {
    "path": {
      "type": "string",
      "description": "Full path of the folder to be created"
    },
    "@type": {
      "const": "Folder"
    }
  },
  "required": [
    "@type",
    "path"
  ]
}
```

#### File-based Data Types

The only file-based data type exposed for BLOBS data is `File`, which describes an arbitrary file in a folder. The file's metadata will be describes in the JSON part of the payload, with the below schema.

```json
{
  "$schema": "https://json-schema.org/draft/2020-12/schema",
  "type": "object",
  "properties": {
    "dateModified": {
      "type": [
        "string",
        "null"
      ],
      "format": "date-time"
    },
    "folder": {
      "type": "string"
    },
    "name": {
      "type": "string"
    },
    "@type": {
      "const": "File"
    }
  },
  "required": [
    "@type",
    "name",
    "folder"
  ]
}
```

### CALENDAR

Endpoint: `/calendar`

The CALENDAR vertical describes calendars and events on those calendars.

Calendars are conventionally imported before the events associated with them.

All data exposed by the CALENDER vertical is encoded as a basic data type.

```json
{
  "$schema": "https://json-schema.org/draft/2020-12/schema",
  "$defs": {
    "CalendarEventTime": {
      "type": "object",
      "properties": {
        "dateOnly": {
          "type": "boolean"
        },
        "dateTime": {
          "type": "string",
          "format": "date-time"
        },
        "@type": {
          "const": "CalendarEventModel$CalendarEventTime"
        }
      },
      "required": [
        "@type",
        "dateTime"
      ]
    }
  },
  "anyOf": [
    {
      "type": "object",
      "properties": {
        "description": {
          "type": "string"
        },
        "id": {
          "type": "string"
        },
        "name": {
          "type": "string"
        },
        "@type": {
          "const": "Calendar"
        }
      },
      "required": [
        "@type",
        "name",
        "id"
      ]
    },
    {
      "type": "object",
      "properties": {
        "attendees": {
          "type": "array",
          "items": {
            "type": "object",
            "properties": {
              "displayName": {
                "type": "string"
              },
              "email": {
                "type": "string"
              },
              "optional": {
                "type": "boolean"
              },
              "@type": {
                "const": "CalendarAttendeeModel"
              }
            },
            "required": [
              "@type"
            ]
          }
        },
        "calendarId": {
          "type": "string"
        },
        "endTime": {
          "$ref": "#/$defs/CalendarEventTime"
        },
        "location": {
          "type": "string"
        },
        "notes": {
          "type": "string"
        },
        "recurrenceRule": {
          "type": "object",
          "properties": {
            "exDate": {
              "type": "object"
            }
          }
        },
        "startTime": {
          "$ref": "#/$defs/CalendarEventTime"
        },
        "title": {
          "type": "string"
        },
        "@type": {
          "const": "CalendarEvent"
        }
      },
      "required": [
        "@type",
        "calendarId",
        "title"
      ]
    }
  ]
}
```

### SOCIAL-POSTS

Endpoint: `/social-posts`

The SOCIAL-POSTS vertical represents posts made by the user on a social media platform.

Only the `SocialActivity` data type is exposed by the SOCIAL-POSTS vertical, which is a basic data type.

```json
{
  "$schema": "https://json-schema.org/draft/2020-12/schema",
  "type": "object",
  "properties": {
    "activity": {
      "type": "object",
      "properties": {
        "attachments": {
          "type": "array",
          "items": {
            "type": "object",
            "properties": {
              "content": {
                "type": "string"
              },
              "name": {
                "type": "string"
              },
              "type": {
                "type": "string",
                "enum": [
                  "LINK",
                  "IMAGE",
                  "VIDEO"
                ]
              },
              "url": {
                "type": "string"
              },
              "@type": {
                "const": "SocialActivityAttachment"
              }
            },
            "required": [
              "@type",
              "name"
            ]
          }
        },
        "content": {
          "type": "string"
        },
        "id": {
          "type": "string"
        },
        "location": {
          "type": "object",
          "properties": {
            "latitude": {
              "type": "number"
            },
            "longitude": {
              "type": "number"
            },
            "name": {
              "type": "string"
            },
            "@type": {
              "const": "SocialActivityLocation"
            }
          },
          "required": [
            "@type",
            "latitude",
            "longitude"
          ]
        },
        "published": {
          "type": "string",
          "format": "date-time"
        },
        "title": {
          "type": "string"
        },
        "type": {
          "type": "string",
          "enum": [
            "CHECKIN",
            "POST",
            "NOTE"
          ]
        },
        "url": {
          "type": "string"
        },
        "@type": {
          "const": "SocialActivityModel"
        }
      },
      "required": [
        "@type",
        "id",
        "content"
      ]
    },
    "metadata": {
      "type": "object",
      "properties": {
        "actor": {
          "type": "object",
          "properties": {
            "id": {
              "type": "string"
            },
            "name": {
              "type": "string"
            },
            "url": {
              "type": "string"
            },
            "@type": {
              "const": "SocialActivityActor"
            }
          },
          "required": [
            "@type",
            "id"
          ]
        },
        "@type": {
          "const": "SocialActivityMetadata"
        }
      },
      "required": [
        "@type"
      ]
    },
    "@type": {
      "const": "SocialActivity"
    }
  },
  "required": [
    "@type",
    "activity"
  ]
}
```

### Endpoint Errors

If there is an error importing an item on any endpoint, the relevant HTTP response code (40x, 50x) should be set, and a JSON encoded response body with the following schema should be sent.

```json
{
  "type": "object",
  "properties": {
    "error": {
      "type": "string"
    },
    "error_description": {
      "type": "string"
    }
  },
  "required": [
    "error"
  ]
}
```

The combination of the HTTP response code and `error` field can be used to encode for specific failure modes; see [Token Refresh](#token-refresh) & [Destination Full](#destination-full) below.

## Authentication and Authorization

Generic Importers support the [OAuth 2.0 Authorization Code Flow](https://datatracker.ietf.org/doc/html/rfc6749#section-1.3.1); platforms will direct users to your OAuth authorization page, requesting an authorization code with OAuth scopes defined by your importer configuration, which will then be used to claim an access token.
The access token will be sent as a Bearer token in the HTTP Authorization header sent with all import requests made by the DTP transfer worker.

The authorization server may be the same as your HTTP service, or can be a separate server that your service integrates with. \
When implementing your importer service there is likely an existing OAuth library that integrates with your chosen framework, or there are third-party authorization services you can integrate with.

### Token Refresh

If your configuration specifies a token refresh endpoint and a refresh token is provided alongside the access token, the transfer worker will attempt to refresh the access token in the event your service endpoint returns a HTTP 401 (Unauthorized) error with a JSON payload containing an `invalid_token` error message.

```http
HTTP/1.1 401 Unauthorized
Content-Type: application/json

{
  "error": "invalid_token",
  "error_description": "The access token expired"
}
```

The worker will request a token refresh through the standard OAuth refresh token flow.

### Destination Full

If the destination is unable to accept additional data for user due to capacity constrain or other limitations,
the POST APIs should throw `413 Payload Too Large` error to avoid unnecessary data transfer attempts.

`error` field should strictly be set to `destination_full` to indicate that the destination storage is full.

Example error response:

```
HTTP/1.1 413 Payload Too Large
Content-Type: application/json
{
  "error": "destination_full",
  "error_description": "The destination storage is full"
}
```
The worker will pause the job and won't retry in such scenario.
Some exporters also support the ability to retry transfers after user frees up space in the destination.