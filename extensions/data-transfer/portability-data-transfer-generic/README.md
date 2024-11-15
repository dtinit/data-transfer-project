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

## Schemas

```json
{
    "TODO": "schema"
}
```
