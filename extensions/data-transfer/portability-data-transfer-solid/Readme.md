# Solid/Inrupt
This folder contains the extension implementation for the
[Solid](https://solid.mit.edu/) service.

It has been developed and tested against the [Inrupt](https://solid.inrupt.com/) implementation
and so might not be compatible with other implementations. 

## Data Supported

 - Contacts import and export.

## Current State
This is Alpha, it mostly works for the most common contacts features, but is not feature complete
in terms of all the features of contacts.

## Keys

Solid uses [WebID-TLS](https://dvcs.w3.org/hg/WebID/raw-file/tip/spec/tls-respec.html) to
authenticate. So there is no need to apply for keys.  See 
[SslHelper](src/main/java/org/datatransferproject/transfer/solid/SslHelper.java) for the code
that handles the WebID-TLS handshake and acquires a cookie.

See [Solid Auth Setup](SolidAuthSetup.md) for more details on how to configure your pod.

## Maintained By

The Solid extension was created by the
[DTP maintainers](mailto:portability-maintainers@googlegroups.com)
and is not an official product of Solid/Inrupt.
