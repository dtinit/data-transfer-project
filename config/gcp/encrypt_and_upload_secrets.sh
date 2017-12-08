#!/bin/sh

# Script to encrypt application secrets, and then upload the encrypted secrets as well as the (raw)
# app keys to GCS.
# Usage: ./encrypt_and_upload_secrets.sh <env>
# Encrypts secrets in environments/<env>/raw_secrets and outputs encrypted values in
# environments/<env>/encrypted_secrets.
# Then uploads environments/<env>/encrypted_secrets and environments/<env>/keys to GCS.
# Keys and secrets in app_data should NOT be checked into source control (this is enforced with
# .gitignore) and only need to be encrypted/uploaded once per provider and GCP project.

if [[ $(pwd) != */gcp ]]; then
  echo "Please run out of /gcp directory. Aborting."
  exit 1
fi

if [ -z $1 ]; then
  echo "ERROR: Must provide an environment, e.g. local, qa, test, or prod"
  exit 1
fi

source ./init_project_vars.sh
ENV=$1

# Encrypts the key for ${1} - a given provider, e.g. "FLICKR", and saves it in encrypted_secrets/.
# Requires that a raw (unencrypted) secret exists in the user's local filesystem for the provider
# at the following location: environments/$ENV/app_data/raw_secrets/FLICKR_SECRET.txt.
# The secret is encrypted using the 'portability_secrets' keyring's key 'portability_secrets_key',
# which was created in setup_gke_environment.sh.
encrypt_secret() {
  gcloud kms encrypt \
    --location=global  \
    --keyring=portability_secrets \
    --key=portability_secrets_key \
    --plaintext-file=../environments/$ENV/app_data/raw_secrets/${1}_SECRET.txt \
    --ciphertext-file=../environments/$ENV/app_data/encrypted_secrets/${1}_SECRET.encrypted
}

BUCKET_NAME="app-data-portability"
GCS_BUCKET_NAME="gs://$BUCKET_NAME/"

PROJECT_ID=$(gcloud config get-value project)
echo -e "Default project for gcloud is ${PROJECT_ID}.
app-data bucket is: ${GCS_BUCKET_NAME}."
read -p "Continue (y/N)? " response
response=${response,,} # to lower
if [[ ${response} =~ ^(yes|y| ) ]]; then
  echo "Continuing"
else
  echo "Aborting"
  exit 0
fi

ENCRYPTED_SECRETS_DIR="../environments/$ENV/app_data/encrypted_secrets/"
if [[ ! -e $ENCRYPTED_SECRETS_DIR ]]; then
  echo -e "Directory $ENCRYPTED_SECRETS_DIR does not already exist... creating it"
  mkdir $ENCRYPTED_SECRETS_DIR
fi

echo -e "Encrypting secrets..."
encrypt_secret "FLICKR"
encrypt_secret "GOOGLE"
encrypt_secret "INSTAGRAM"
encrypt_secret "MICROSOFT"
encrypt_secret "RTM"
encrypt_secret "SHUTTERSTOCK"
encrypt_secret "SMUGMUG"

# TODO enforce one app key & secret per provider (e.g. FLICKR) in GCS to make less error prone.
echo -e "Uploading app keys and encrypted secrets to 'app-data' bucket"
gsutil cp -r ../environments/$ENV/app_data/keys ${GCS_BUCKET_NAME}
gsutil cp -r ../environments/$ENV/app_data/encrypted_secrets ${GCS_BUCKET_NAME}
