# Distributions

Data Transfer Project is platform agnostic.

We offer demo local and Google (GCP) deployments, with a
Microsoft (Azure) one in progress.

We welcome contributors to add configuration for additional platforms.

## Local development

For local development (only), secrets may be stored in gradle properties.
Add any test secrets to your local `~/.gradle/dataportability.secrets.properties` file. A
sample is provided in `common/dataportability.secrets.example.properties`.

This is supported in the local distributions -- `demo-server`, `gateway-default`, and
`worker-default`.
