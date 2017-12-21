# Data Portability on Google Cloud Platform

## GKE
We run Kubernetes on Google Cloud Platform using GKE. This is scripted
at setup_gke_environment.sh. Our Kubernetes objects are stored in the
'k8s' directory. We use Docker as our Kubernetes container format.

## Environment-specific settings
Environment-specific settings (values for PortabilityFlags) are stored
per environment in the environments/ directory. There is a subdirectory
for each environment: local/, qa/, test/, and prod/. Each environment
subdirectory contains a settings.yaml for PortabilityFlags values.

## Project-specific settings
Project-specific settings, such as billing account, and project owners,
should be configured in a hidden file init_project_vars.sh which is
gitignore'd. An example is provided at init_project_vars_example.sh.

## Application secrets
Application keys and secrets are stored in a GCS bucket, 'app-data-portability'.
Keys are stored raw in a 'keys/' directory in the bucket. Secrets are
encrypted with Cloud KMS and stored encrypted in an 'encrypted_secrets/'
directory.

#### Local development
For local development only, secrets may be stored in a local file which
is compiled into our binary, at environments/local/secrets.csv. This is
controlled with the cloud=LOCAL flag.

We also provide functionality for local instances to point to a test GCP
project by specifying cloud=GOOGLE. The project this points to, as well
as credentials to access it, are configured in build_and_upload_docker_image.sh.

## Static content
We store static content in a GCS bucket that integrates with GCP's
CDN. We generate the content via build_and_deploy_static_content.sh,
which builds it using 'ng build'. ng build versions its output files via
the bundle filenames which are hashes. Our index.html references these
versioned files and must be updated to include the new reference each
time a bundle changes. index.html itself is served from our backend.
This ensures that our app always fetches the newest static content.

## Docker
We use the build_and_deploy_api.sh script to build our Docker images.
Given an environment name, the script copies the appropriate
resources (index.html and secrets.csv) and compiles a new jar.
It then generates a Dockerfile which passes settings from the
appropriate settings.yaml as flags to our jar via ENTRYPOINT in our
Dockerfile. The script can also optionally build a new Docker image and
upload it to GKE.