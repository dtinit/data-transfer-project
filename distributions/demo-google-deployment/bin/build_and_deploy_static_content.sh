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

if [[ $(pwd) != */data-transfer-project ]]; then
  echo -e "${USAGE}\nPlease run out of the root data-transfer-project/ directory. Aborting."
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
source ./distributions/demo-google-deployment/bin/init_project_vars.sh

echo -e "Set hidden var:
BASE_PROJECT_ID: ${BASE_PROJECT_ID}"

ENV=$1
PROJECT_ID_SUFFIX=$2
PROJECT_ID="$BASE_PROJECT_ID-$PROJECT_ID_SUFFIX"
GCS_BUCKET="gs://static-${PROJECT_ID}/"
gcloud=$(which gcloud)|| { echo "Google Cloud SDK (gcloud) not found." >&2; exit 1; }
gsutil=$(which gsutil)|| { echo "Google Cloud Storage CLI (gsutil) not found." >&2; exit 1; }

GCP_DIR=$(pwd)
print_and_exec "cd client/"
echo -e "\nCleaning up old resources"
if [[ -e "../../static/" ]]; then
  rm -rf ../../static/
fi
print_and_exec "ng build --prod --env=${ENV}"
print_and_exec "mkdir ../../static/"
# Reorganize everything in a top level static/ directory. This is a hack to keep static assets
# consistent between local and GCP environments"
print_and_exec "cp -r build/resources/static/* ../../static"
print_and_exec "rm -rf build/resources/static/"
print_and_exec "cp -r build/resources/* ../../static/"
print_and_exec "gsutil cp -r ../../static ${GCS_BUCKET}"
echo -e "\nMaking folder public"
print_and_exec "gsutil iam ch allUsers:objectViewer ${GCS_BUCKET}"
cd ../../static

echo -e "new index.html\n"
cat index.html
main_new=$(ls | grep main.*.bundle.js)
styles_new=$(ls | grep styles.*.bundle.css)
inline_new=$(ls | grep inline.*.bundle.js)
vendor_new=$(ls | grep vendor.*.bundle.js)
polyfills_new=$(ls | grep polyfills.*.bundle.js)
echo -e "\nnew bundles:\n$main_new\n$styles_new\n$inline_new\n$vendor_new\n$polyfills_new"
# Prepend bundle references in index.html with static/.
sed -i "s|$main_new|static/$main_new|g" "index.html"
sed -i "s|$styles_new|static/$styles_new|g" "index.html"
sed -i "s|$inline_new|static/$inline_new|g" "index.html"
sed -i "s|$vendor_new|static/$vendor_new|g" "index.html"
sed -i "s|$polyfills_new|static/$polyfills_new|g" "index.html"
echo -e "\nindex.html after referencing bundles in static/\n"
cat index.html

INDEX_HTML_LOCATION="../data-transfer-project/distributions/demo-google-deployment/resources/config/environments/$ENV/index.html"
if [[ -e ${INDEX_HTML_LOCATION} ]]; then
  print_and_exec "mkdir -p /tmp/${PROJECT_ID}/"
  print_and_exec "cp ${INDEX_HTML_LOCATION} /tmp/${PROJECT_ID}/index.html"
  echo -e "\nSaving old index html at /tmp/${PROJECT_ID}/index.html. Was:\n"
  cat /tmp/${PROJECT_ID}/index.html
fi
print_and_exec "cp index.html ${INDEX_HTML_LOCATION}"

