# Generic Importers

## Implementing a Server

To consume data from a generic importer, create a HTTP service in the language of your choice.
The service should accept POST requests on a given base URL, and kebab-case sub-path specific to each data vertical (`https://example.com/import/blobs`, `https://example.com/import/social-posts`).

### HTTP Interface

Data will be POSTed to your endpoint with a `Content-Type` of either `application/json` for basic data-types, or `multipart/related` for file-based data-types. See below for how to interpret each of these.
Your endpoint should return a status 201 if the resource has been created, 40x for errors caused by the Importer, or 50x for service errors.

#### Basic data types

For basic data-types the Importer will send a POST request with a `Content-Type: application/json` header. The body of the POST request will be a UTF-8 encoded JSON payload conforming to the relevant data-type schema detailed in the [Schemas](#schemas) section.

For example, below is a full request of a SOCIAL_POST item (JSON formatted for readability)

```http
POST /import/social-posts HTTP/1.1
Content-Type: application/json
Content-Length: 510
Host: localhost:8080
Connection: Keep-Alive
Accept-Encoding: gzip
User-Agent: okhttp/3.9.1
Authorization: Bearer accessToken

{
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
Content-Length: 130

{"@type":"BlobbyFileData","folder":"/root/foo","document":{"name":"bar.mp4","dateModified":"2020-02-01","encodingFormat":"video/mp4"}}
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

### MEDIA

Endpoint: `/media`

The MEDIA vertical describes a user's photos and videos, and the albums that contain them.

Albums are guaranteed to be imported before the photos and videos contained in them.

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
        "favourite",
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

Folders are guaranteed to be imported before any of their containing data, which may include folders and/or files.

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

Calendars are guaranteed to be created before the events associated with them.

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

Only the SocialActivity data type is exposed by the SOCIAL-POSTS vertical, which is a basic data type.

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

The combination of the HTTP response code and `error` field can be used to encode for specific failure modes; see [Token Refresh](#token-refresh) below.

## Authentication and Authorization

Generic Importers support the OAuth v2 Authorization Code Flow; exporters will direct users to your OAuth authorization page, requesting an authorization code with OAuth scopes defined by your importer configuration, which will then be used to claim an access token.
The access token will be sent as a Bearer token in the HTTP Authorization header sent with all import requests.

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

The worker will request a token refresh through the standard OAuth refresh token flow, sending a form-encoded POST request to the configured refresh endpoint.

```http
POST /oauth/refresh HTTP/1.1
Accept: application/json
Content-Type: application/x-www-form-urlencoded
Content-Length: 92
Host: localhost:8080
Connection: Keep-Alive
Accept-Encoding: gzip
User-Agent: okhttp/3.9.1

refresh_token=refreshToken&client_id=appKey&client_secret=appSecret&grant_type=refresh_token
```

The token refresh endpoint should return a response containing the access token. If a new refresh_token is provided it will be used for future token refreshes.

```http
HTTP/1.1 200 OK
Content-Type: application/json

{
    "access_token": "newAccessToken",
    "refresh_token": "newRefreshToken", // optional
    "token_type": "Bearer"
}
```
