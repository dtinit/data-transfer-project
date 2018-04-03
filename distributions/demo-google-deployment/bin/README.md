# Data Portability on Google Cloud Platform

## GKE
We run Kubernetes on Google Cloud Platform using GKE. This is scripted
at _setup_gke_environment.sh_. Our Kubernetes objects are stored in the
[k8s/](../../config/k8s/README.md) directory. We use Docker as our Kubernetes
container format.

## Project-specific settings
Project-specific settings, such as billing account, and project owners,
should be configured in a hidden file _init_project_vars.sh_ which is
gitignore'd. An example is provided at _init_project_vars_example.sh_.

## Application secrets
Application keys and secrets are stored in a GCS bucket,
_'app-data-\<Project Name\>'_.
Keys are stored raw in a _'keys/'_ directory in the bucket. Secrets are
encrypted with Cloud KMS and stored encrypted in an
_'encrypted_secrets/'_ directory.

#### Local development
For local development only, secrets may be stored in gradle properties.
Add any test secrets to your local _~/.gradle/gradle.properties_ file. A
sample is provided in _gradle_example.properties_.

We also provide functionality for local instances to point to a test GCP
project by specifying _cloud=GOOGLE_. The project this points to, as
well as credentials to access it, are configured in our Docker image
build [script](build_docker_image.sh). The project ID should be specified
in LOCAL_GCP_PROJECT in _init_common_vars.sh_. See 
_init_common_vars_example.sh_ for an example. Credentials
for that project's service account should be accessed from API >
Credentials > service account credentials in GoogleCloudPlatform and
copied into _service_account_creds.json_ locally.

For more information on running locally see
[Developer documentation](../../Documentation/Developer.md).

## Static content
We store static content in a GCS bucket that integrates with GCP's
CDN. We generate the content via _build_and_deploy_static_content.sh_,
which builds it using _ng build_. ng build versions its output files via
the bundle filenames which are hashes. Our index.html references these
versioned files and must be updated to include the new reference each
time a bundle changes. index.html itself is served from our backend.
This ensures that our app always fetches the newest static content.

## Docker
We use a [script](build_docker_image.sh) to build our Docker
images. Given an environment name, the script copies the appropriate
resources (index.html and settings yaml) and compiles a new jar. It then
generates a Dockerfile which sets our jar as the ENTRYPOINT. The script
can also optionally build a new Docker image and upload it to GCP Container
Registry.
