#!/bin/sh

# Script to deploy static content to our bucket in GCP
# Usage: ./deploy_static_content.sh <ENV_NAME> <PROJECT_ID_SUFFIX>

USAGE="Usage: ./deploy_static_content.sh <ENV_NAME> <PROJECT_ID_SUFFIX>"
print_and_exec() {
  echo -e "\n${1}"
  ${1}
}

if [[ $(pwd) != */gcp ]]; then
  echo -e "${USAGE}\nPlease run out of /gcp directory. Aborting."
  exit 1
fi

if [ -z $1 ]; then
  echo -e "${USAGE}\nERROR: Must provide an environment, e.g. 'qa', 'test', or 'prod'"
  exit 1
fi
if [ -z $2 ]; then
  echo -e "${USAGE}\nERROR: Must provide a project ID suffix,
  i.e. 'qa8' for project ID ${BASE_PROJECT_ID}-qa8"
  exit 1
fi
# script below sets env variables BASE_PROJECT_ID
source ./init_project_vars.sh

echo -e "Set hidden var:
BASE_PROJECT_ID: ${BASE_PROJECT_ID}"

ENV=$1
PROJECT_ID_SUFFIX=$2
PROJECT_ID="$BASE_PROJECT_ID-$PROJECT_ID_SUFFIX"
GCS_BUCKET="gs://static-${PROJECT_ID}/"
gcloud=$(which gcloud)|| { echo "Google Cloud SDK (gcloud) not found." >&2; exit 1; }
gsutil=$(which gsutil)|| { echo "Google Cloud Storage CLI (gsutil) not found." >&2; exit 1; }

GCP_DIR=$(pwd)

echo -e "\nCleaning up old resources"
print_and_exec "cd ../../../"
if [[ -e "resources/" ]]; then
  rm -rf resources/
fi
if [[ -e "static/" ]]; then
  rm -rf static/
fi
print_and_exec "cd googleplex-portability/client/"
print_and_exec "ng build --prod --env=${ENV}"
print_and_exec "cd ../../resources/"
# Reorganize everything in a top level static/ directory. This is a hack to keep static assets
# consistent between local and GCP environments"
print_and_exec "cp -r static/* ."
print_and_exec "rm -rf static/"
print_and_exec "cd .."
print_and_exec "mkdir static"
print_and_exec "cp -r resources/* static/"
print_and_exec "gsutil cp -r static ${GCS_BUCKET}"
echo -e "\nMaking folder public"
print_and_exec "gsutil iam ch allUsers:objectViewer ${GCS_BUCKET}"
echo -e "Done!
Please check ../portability-web/src/main/resources/static.index.html to see if any bundle references
need to be updated. If so, update index.html and build and deploy a new portability-api image"
