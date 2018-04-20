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
# Script to encrypt application secrets, and then upload the encrypted secrets as well as the (raw)
# app keys to GCS.
#
# Usage: ./encrypt_and_upload_secrets.sh <PATH_TO_SECRETS_FILES>
#
# Ensure the correct project id is sets in your gcloud config, to see the current one, use:
#    gcloud config get-value project
#
# Ensure app-data-<project-id> bucket exists in the project <project id>
#
# This script expects the following structure to be found at the path provided
# <PATH_TO_SECRETS_FILES>/raw_secrets/
# <PATH_TO_SECRETS_FILES>/keys/
# <PATH_TO_SECRETS_FILES>/encrypted_secrets/
#
# Example:
# The following should exist:
#   /tmp/keys/FLICKR_KEY.txt
#   /tmp/raw_secrets/FLICKR_SECRET.txt
#   /tmp/encrypted_secrets/ #empty
#
# The gcloud project id should be set
#   gcloud config set project my-gcp-project
#
# Then running ./encrypt_and_upload_secrets.sh tmp
# will create:
#   /tmp/encrypted_secrets/FLICKR_SECRET.encrypted
#
# and upload them to the project my-gcp-project at
#   app-data-my-gcp-project/keys/FLICKR_KEY.txt
#   app-data-my-gcp-project/encrypted_secrets/FLICKR_SECRET.encrypted
#
#!/bin/sh

if [[ $(pwd) != */demo-google-deployment/bin ]]; then
  echo "Please run out of the /demo-google-deployment/bin directory. Aborting."
  exit 1
fi

if [ -z $1 ]; then
  echo "ERROR: Must provide a path to a directory containing key and secrets files"
  exit 1
fi

SECRETS_DIR=$1
echo -e "SECRETS_DIR is $SECRETS_DIR"

if [ ! -d "$SECRETS_DIR" ]; then
  echo "ERROR: $SECRETS_DIR does not exist"
  exit 1
fi

# Encrypts the key for ${1} - a given provider, e.g. "FLICKR", and saves it in encrypted_secrets/.
# Requires that a raw (unencrypted) secret exists in the user's local filesystem for the provider
# at the following location: environments/$ENV/app_data/raw_secrets/FLICKR_SECRET.txt.
# The secret is encrypted using the 'portability_secrets' keyring's key 'portability_secrets_key',
# which was created in setup_gke_environment.sh.
encrypt_secret() {
  echo -e "Encrypting secrets file: $SECRETS_DIR/raw_secrets/${1}_SECRET.txt"
  gcloud kms encrypt \
    --location=global  \
    --keyring=portability_secrets \
    --key=portability_secrets_key \
    --plaintext-file=$SECRETS_DIR/raw_secrets/${1}_SECRET.txt \
    --ciphertext-file=$SECRETS_DIR/encrypted_secrets/${1}_SECRET.encrypted
}


PROJECT_ID=$(gcloud config get-value project)
BUCKET_NAME="app-data-$PROJECT_ID"
GCS_BUCKET_NAME="gs://${BUCKET_NAME}/"
echo -e "Default project for gcloud is ${PROJECT_ID}.
app-data bucket is: ${GCS_BUCKET_NAME}."
echo -e "Current contents of bucket:"
gsutil ls ${GCS_BUCKET_NAME}
read -p "Continue (y/N)? " response
response=${response,,} # to lower
if [[ ${response} =~ ^(yes|y| ) ]]; then
  echo "Continuing"
else
  echo "Aborting"
  exit 0
fi

ENCRYPTED_SECRETS_DIR="$SECRETS_DIR/encrypted_secrets/"
if [[ ! -e $ENCRYPTED_SECRETS_DIR ]]; then
  echo -e "Directory $ENCRYPTED_SECRETS_DIR does not already exist... creating it"
  mkdir $ENCRYPTED_SECRETS_DIR
  echo -e "Encrypting secrets..."
  encrypt_secret "FLICKR"
  encrypt_secret "GOOGLE"
  #encrypt_secret "INSTAGRAM"
  #encrypt_secret "MICROSOFT"
  #encrypt_secret "RTM"
  #encrypt_secret "SHUTTERSTOCK"
  #encrypt_secret "SMUGMUG"
  #encrypt_secret "JWT"
fi


# TODO enforce one app key & secret per provider (e.g. FLICKR) in GCS to make less error prone.
# Note: gsutil cp doesn't let you specify a project, so hope that it uses the default in gcloud
# config... (https://stackoverflow.com/questions/45766055/gsutil-specify-project-on-copy). This has
# worked in practice but not sure if it's 100% reliable behavior.
echo -e "Uploading app keys and encrypted secrets to '${GCS_BUCKET_NAME}' bucket"
gsutil cp -r ${SECRETS_DIR}/keys ${GCS_BUCKET_NAME}
gsutil cp -r ${SECRETS_DIR}/encrypted_secrets ${GCS_BUCKET_NAME}
