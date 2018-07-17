# portability-transfer

This folder contains the binary for the Worker server used by DTP.

DTP has two servers: the API, and the Worker.  The [API server](../portability-api)
exposes DTP's API (hence the name).  This allows clients to submit jobs,
and check job statuses.  The Worker does the actual transfer
of the data.
