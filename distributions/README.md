# Distributions

Data Transfer Project is platform agnostic.

We offer demo local and Google (GCP) deployments, with a
Microsoft (Azure) one in progress.

We welcome contributors to add configuration for additional platforms.

## Local development

For local development (only), secrets may be stored in environment variables.

### For running from a jar file

Set environment variables directly in your OS (e.g. `.bashrc` file, or 
`System > Control Panel > Advanced system settings > Environment Variables` on Windows).

### For running via docker
Add any secrets to your local `distributions/demo-server/env.secrets` file. A
sample is provided in `distributions/demo-server/env.secrets.template`.
