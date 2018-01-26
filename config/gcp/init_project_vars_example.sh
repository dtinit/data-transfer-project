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
# Sample init_project_vars.sh for the Acme Corporation
#
#!/bin/sh


ORGANIZATION_ID=123456 #ID for your organization in GCP project
BILLING_ACCOUNT_ID=123-456
BASE_PROJECT_ID="acme-portability"
OWNERS="\"user:alice@acme.com\",\"user:bob@acme.com\""
# Optional: Test project when running locally and pointing to a GCP project.
# Only use test projects here! Only used when cloud=GOOGLE in local/common.yaml.
LOCAL_GCP_PROJECT="acme-portability-qa"