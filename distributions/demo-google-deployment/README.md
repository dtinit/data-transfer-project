# Demo Google deployment

This distribution shows one way Data Transfer Project may be deployed on Google
Cloud Platform.

## GKE
We run Kubernetes on Google Cloud Platform using GKE. This is scripted
at `bin/setup_gke_environment.sh`. Our Kubernetes objects are stored in the
[k8s/](../common/k8s/README.md) directory. We use Docker as our Kubernetes
container format.

## Project-specific settings
Project-specific settings, such as billing account, and project owners,
should be configured in a hidden file `bin/init_project_vars.sh` which is
gitignore'd. An example is provided at `bin/init_project_vars_example.sh`.

## Application secrets
Application keys and secrets are stored in a GCS bucket,
`'app-data-\<Project Name\>'`.
Keys are stored raw in a `'keys/'` directory in the bucket. Secrets are
encrypted with Cloud KMS and stored encrypted in an
`'encrypted_secrets/'` directory.

#### Local development
When using the LOCAL environment, local instances point to a test GCP
project. The project this points to, as well as credentials to access it,
are configured in our Docker image build [script](bin/build_docker_image.sh).
The project ID should be specified in LOCAL_GCP_PROJECT in `bin/init_common_vars.sh`.
See `bin/init_common_vars_example.sh` for an example. Credentials
for that project's service account should be accessed from API >
Credentials > service account credentials in your GCP project and
copied into `service_account_creds.json` locally.

For more information on running locally see
[Developer documentation](../../Documentation/Developer.md).

## Static content
We store static content in a GCS bucket that integrates with GCP's
CDN. We generate the content via `bin/build_and_deploy_static_content.sh`,
which builds it using `ng build`. ng build versions its output files via
the bundle filenames which are hashes. Our `index.html` references these
versioned files and must be updated to include the new reference each
time a bundle changes. `index.html` itself is served from our backend 
(the API server). This ensures that our app always fetches the newest
static content.

## Docker
We use a [script](bin/build_docker_image.sh) to build our Docker
images. Given an environment name, the script copies the appropriate
resources (index.html and settings yaml) and compiles a new jar. It then
generates a Dockerfile which sets our jar as the ENTRYPOINT. The script
can also optionally build a new Docker image and upload it to GCP Container
Registry.
