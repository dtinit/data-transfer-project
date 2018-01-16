#!/bin/sh

# Sample init_project_vars.sh for the Acme Corporation

ORGANIZATION_ID=123456 #ID for your organization in GCP project
BILLING_ACCOUNT_ID=123-456
BASE_PROJECT_ID="acme-portability"
OWNERS="\"user:alice@acme.com\",\"user:bob@acme.com\""
# Optional: Test project when running locally and pointing to a GCP project.
# Only use test projects here! Only used when cloud=GOOGLE in local/common.yaml.
LOCAL_GCP_PROJECT="acme-portability-qa"