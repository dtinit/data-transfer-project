# Distributions

Data Transfer Project is platform agnostic.

Currently supported deployments are:
 - Local from source
 - [Local from docker](demo-server)
 - [Google (GCP)](demo-google-deployment)
 - Microsoft (Azure) [in progress].

We welcome contributors to add configuration for additional platforms.

## Local development

For local development (only), secrets may be stored in environment variables.

### For running from a jar file

Set environment variables directly in your OS (e.g. `.bashrc` file, or 
`System > Control Panel > Advanced system settings > Environment Variables` on Windows).

### For running via docker
Add any secrets to your local `distributions/demo-server/env.secrets` file. A
sample is provided in `distributions/demo-server/env.secrets.template`.
