# portability-api

This folder contains the binary for the API server used by DTP.

DTP has two servers: the API, and the Worker.  The API server exposes DTP's
API (hence the name).  This allows clients to submit jobs, and check job
statuses.  The [Worker](../portability-transfer) does the actual transfer
of the data.

