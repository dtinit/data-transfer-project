#
# Copyright 2018 Google Inc.
#
# Licensed under the Apache License, Version 2.0 (the "License");
# you may not use this file except in compliance with the License.
# You may obtain a copy of the License at
#
#    https://www.apache.org/licenses/LICENSE-2.0
#
# Unless required by applicable law or agreed to in writing, software
# distributed under the License is distributed on an "AS IS" BASIS,
# WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
# See the License for the specific language governing permissions and
# limitations under the License.
#
# -----------------------------------------------------------------------
# Script to deploy static content to our bucket in GCP
# Usage: ./deploy_static_content.sh <ENV_NAME> <PROJECT_ID_SUFFIX>
#

#!/bin/sh

USAGE="Usage: ./distributions/demo-google-deployment/bin/build_and_deploy_static_content.sh <ENV_NAME> <PROJECT_ID_SUFFIX>"
print_and_exec() {
  echo -e "\n${1}"
  ${1}
}

ng=$(which ng)|| { echo "ng (Angular CLI) not found" >&2; exit 1; }


if [[ $(pwd) != */data-transfer-project ]]; then
  echo -e "${USAGE}\nPlease run out of the root data-transfer-project/ directory. Aborting."
  exit 1
fi

if [ -z $1 ]; then
  echo -e "${USAGE}\nERROR: Must provide an environment, e.g. 'qa', 'test', or 'prod'"
  exit 1
fi

# script below sets env variables BASE_PROJECT_ID
source ./distributions/demo-google-deployment/bin/init_project_vars.sh

echo -e "Set hidden var:
BASE_PROJECT_ID: ${BASE_PROJECT_ID}"
ENV=$1
PROJECT_ID_SUFFIX=$2
PROJECT_ID="$BASE_PROJECT_ID-$PROJECT_ID_SUFFIX"
BUCKET_NAME="$WEBSITE"
GCS_BUCKET_NAME="gs://$BUCKET_NAME/"
gcloud=$(which gcloud)|| { echo "Google Cloud SDK (gcloud) not found." >&2; exit 1; }
gsutil=$(which gsutil)|| { echo "Google Cloud Storage CLI (gsutil) not found." >&2; exit 1; }

echo -e "PROJECT_ID: ${PROJECT_ID}"

print_and_exec "cd client-rest/"
if [ -d "static" ]; then
  rm -rf static
fi
if [ -d "dist/portability-demo/static" ]; then
  rm -rf dist/portability-demo/static
fi
print_and_exec "ng build --prod"
print_and_exec "mkdir static"
print_and_exec "mv dist/portability-demo/* static/"
print_and_exec "mv static dist/portability-demo"
print_and_exec "mv dist/portability-demo/static/index.html dist/portability-demo"
print_and_exec "mv dist/portability-demo/static/favicon.ico dist/portability-demo"
cd dist/portability-demo

echo -e "new index.html\n"
cat index.html

cd static
main_new=$(ls | grep main.*.js)
styles_new=$(ls | grep styles.*.css)
runtime_new=$(ls | grep runtime.*.js)
polyfills_new=$(ls | grep polyfills.*.js)

print_and_exec "cd .."
print_and_exec "pwd"
echo -e "\nnew artifacts:\n$main_new\n$styles_new\n$runtime_new\n$polyfills_new"
# Prepend artifact references in index.html with static/.
sed -i "s|$main_new|static/$main_new|g" "index.html"
sed -i "s|$styles_new|static/$styles_new|g" "index.html"
sed -i "s|$runtime_new|static/$runtime_new|g" "index.html"
sed -i "s|$polyfills_new|static/$polyfills_new|g" "index.html"

echo -e "\nindex.html after referencing artifacts in static/\n"
cat index.html

print_and_exec "gsutil cp -r . ${GCS_BUCKET_NAME}"

