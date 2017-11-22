# Data Portability on Google Cloud Platform

## GKE
We run Kubernetes on Google Cloud Platform using GKE. This is scripted
at scripts/setup_gke_environment.sh. Our Kubernetes objects are stored
in the 'k8s' directory. We use Docker as our Kubernetes container
format.

## Application secrets
We currently compile our OAuth app secrets into our binary at
portability-web/src/main/resources/secrets.csv.

## Environment-specific settings
Environment-specific settings (values for PortabilityFlags) as well
as app secrets are stored per environment in the environments/
directory. There is a subdirectory for each environment: local/,
qa/, and test/. Each environment subdirectory contains a
settings.yaml for PortabilityFlags values and secrets.csv for
application secrets.

## Docker
We use the scripts/build_and_deploy_api.sh to build our Docker images.
Given an environment name, the script copies the appropriate
secrets.csv to portability-web/src/main/resources/secrets.csv and
compiles a new jar. It then generates a Dockerfile which passes
settings from the appropriate settings.yaml as flags to our jar
via ENTRYPOINT in our Dockerfile. The script can also optionally
build a new Docker image and upload it to GKE.

## Static content
We store static content in a GCS bucket that integrates with GCP's
CDN. We generate the content via
scripts/build_and_deploy_static_content.sh, which builds it using
'ng build'. ng build versions its output files via the bundle
filenames which are hashes. Our index.html references these versioned
files and must be updated to include the new reference each time a
bundle changes. index.html itself is served from our backend. This
ensures that our app always fetches the newest static content.