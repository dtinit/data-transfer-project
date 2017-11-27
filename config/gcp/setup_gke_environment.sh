#!/bin/sh

# Script to set up a new GKE (Kubernetes on Google Cloud Platform) environment based on the passed
# in ENV_NAME. Sets up associated GCP project as well. Must run from scripts/ directory.
# Usage: ./setup_gke_environment <ENV_NAME>

source ./init_hidden_vars.sh
echo -e "Set hidden vars:
BASE_PROJECT_ID: ${BASE_PROJECT_ID}
ORGANIZATION_ID: ${ORGANIZATION_ID}
BILLING_ACCOUNT_ID: ${BILLING_ACCOUNT_ID}"

# Constants
GCE_ENFORCER_REASON=pre-launch
INSTANCE_GROUP_SIZE=2 #Number of VMs to run for our GKE jobs
ZONE=us-central1-a
STATIC_BUCKET_NAME=static-bucket
NODE_PORT=30580 #If this changes, change nodePort in api-service.yaml too
HEALTH_CHECK_PORT=10256
BACKEND_SERVICE_NAME=api-backend-service
LB_NAME=portability-load-balancer
LB_EXTERNAL_IP_NAME=load-balancer-external-ip
LB_HTTPS_PROXY_NAME=load-balancer-https-proxy
LB_FORWARDING_RULE_NAME=portability-forwarding-rule
SSL_CERT_NAME=portability-cert
NUM_STEPS=20
CURR_STEP=0

print_step() {
  echo -e "\n$((++CURR_STEP))/${NUM_STEPS}. $1"
}

if [[ $(pwd) != */gcp ]]; then
  echo "Please run out of /gcp directory. Aborting."
  exit 1
fi

if [ -z $1 ]; then
  echo "ERROR: Must provide an environment, e.g. 'qa', 'test', or 'prod'"
  exit 1
fi

ENV=$1
PROJECT_ID="${BASE_PROJECT_ID}-$ENV"
gcloud=$(which gcloud)|| { echo "Google Cloud SDK (gcloud) not found." >&2; exit 1; }
gsutil=$(which gsutil)|| { echo "Google Cloud Storage CLI (gsutil) not found." >&2; exit 1; }
kubectl=$(which kubectl)|| { echo "Kubernetes CLI (kubectl) not found." >&2; exit 1; }

read -p "This script will install an SSL certificate on the project from your local filesystem soon.
You should get the cert ready now. It takes about 5 minutes. See this script for instructions.
Continue (y/N)? " response
response=${response,,} # to lower
if [[ ${response} =~ ^(yes|y| ) ]]; then
  echo "Continuing"
else
  echo "Aborting"
  exit 0
fi

# Instructions to obtain a free Letsencrypt SSL cert (5 mins):
# wget https://dl.eff.org/certbot-auto
# chmod a+x certbot-auto
# ./certbot-auto certonly --agree-tos --renew-by-default --manual --preferred-challenges=dns \
# -d gardenswithoutwalls-qa.net,www.gardenswithoutwalls-qa.net
# Enter the text records and wait 1-2 minutes and confirm
# It should save cert as follows:
# Your certificate and chain have been saved at:
# /etc/letsencrypt/live/gardenswithoutwalls-qa.net/fullchain.pem
# Your key file has been saved at:
# /etc/letsencrypt/live/gardenswithoutwalls-qa.net/privkey.pem
# Note: it is easiest to then sudo cp these files to a temporary location since the default
# file permissions are difficult.

read -p "Please enter the path to the certificate file (.crt or .pem): " CRT_FILE_PATH
if [[ ! -e ${CRT_FILE_PATH} ]]; then
  echo -e "No file found at ${CRT_FILE_PATH}. Aborting."
  exit 1
fi

read -p "Please enter the path to the key file (.key or .pem): " KEY_FILE_PATH
if [[ ! -e ${KEY_FILE_PATH} ]]; then
  echo -e "No file found at ${KEY_FILE_PATH}. Aborting."
  exit 1
fi

print_step
read -p "Creating project ${PROJECT_ID}. Continue (y/N)? " response
response=${response,,} # to lower
if [[ ${response} =~ ^(yes|y| ) ]]; then
  echo "Continuing"
else
  echo "Aborting"
  exit 0
fi
gcloud projects create ${PROJECT_ID} --name=${PROJECT_ID} --organization=$ORGANIZATION_ID

print_step
read -p "Changing your default project for gcloud to ${PROJECT_ID}. Continue (y/N)? " response
response=${response,,} # to lower
if [[ ${response} =~ ^(yes|y| ) ]]; then
# Need to set default project or you can't make a GCS bucket using gsutil later on. gsutil goes off
# of gcloud's default project and doesn't accept a project flag.
gcloud config set project ${PROJECT_ID}
else
  echo "Aborting"
  exit 0
fi

print_step "Creating a service account for IAM"
gcloud iam --project ${PROJECT_ID} service-accounts create ${PROJECT_ID} --display-name "${PROJECT_ID} service account"
SERVICE_ACCOUNT="${PROJECT_ID}@${PROJECT_ID}.iam.gserviceaccount.com"
echo -e "\nCreated service account:"
gcloud iam --project ${PROJECT_ID} service-accounts describe ${SERVICE_ACCOUNT}

print_step "Granting permissions to service account and team members"
# Note: Don't see an easy way to pass multiple members in one command. Can use a json file but
# would have to do some manipulation to get variables in it.
# TODO: move members to hidden file/vars
gcloud projects add-iam-policy-binding ${PROJECT_ID} --member serviceAccount:${SERVICE_ACCOUNT} --role roles/editor
gcloud projects add-iam-policy-binding ${PROJECT_ID} --member user:chuy@google.com --role roles/owner
gcloud projects add-iam-policy-binding ${PROJECT_ID} --member user:rtannenbaum@google.com --role roles/owner
gcloud projects add-iam-policy-binding ${PROJECT_ID} --member user:sihamh@google.com --role roles/owner
gcloud projects add-iam-policy-binding ${PROJECT_ID} --member user:willard@google.com --role roles/owner

print_step "Enabling APIs"
# Needed for 'gcloud compute'
gcloud services --project ${PROJECT_ID} enable compute.googleapis.com
# Needed for managing container images
gcloud services --project ${PROJECT_ID} enable containerregistry.googleapis.com

print_step "Enabling billing" # Needed for installing SSL cert
gcloud alpha billing projects link ${PROJECT_ID} --billing-account=$BILLING_ACCOUNT_ID

print_step "Installing SSL certificate"
gcloud compute ssl-certificates create ${SSL_CERT_NAME} \
    --certificate ${CRT_FILE_PATH} --private-key ${KEY_FILE_PATH}

print_step "Enabling GCE Enforcer"
gcloud alpha projects update ${PROJECT_ID}  --update-labels="gce-enforcer-fw-opt-in=$GCE_ENFORCER_REASON"

print_step "Creating GCS bucket"
BUCKET_NAME="static-$PROJECT_ID"
GCS_BUCKET_NAME="gs://$BUCKET_NAME/"
gsutil mb ${GCS_BUCKET_NAME}
echo "Created GCS bucket $GCS_BUCKET_NAME"

print_step "Creating backend bucket"
gcloud compute --project ${PROJECT_ID} backend-buckets create ${STATIC_BUCKET_NAME} --gcs-bucket-name=${BUCKET_NAME}
# TODO use --enable-cdn flag when we are ready to use CDN

# Note: May want to enable autoscaling at some point
print_step "Creating GKE cluster. This will create a VM instance group automatically."
gcloud container clusters create portability-api-cluster --zone ${ZONE} \
--num-nodes=${INSTANCE_GROUP_SIZE} --image-type=COS \
--cluster-ipv4-cidr=10.4.0.0/14

print_step "Creating health check for backend service and instance group"
gcloud compute http-health-checks create portability-health-check --port=${NODE_PORT} \
--request-path=/healthz --port=${HEALTH_CHECK_PORT}

# Setting named port on instance group. First, have to get the name of the instance group. This
# is auto generated by GKE cluster creation above and we can't change it. :(
INSTANCE_GROUPS=$(gcloud compute instance-groups list)
# Sample response:
# gcloud compute instance-groups list
# NAME                           LOCATION       SCOPE  NETWORK  MANAGED  INSTANCES
# foo-clus-default-pool-bar-grp  us-central1-a  zone   default  Yes      2
# TODO: Consider using HTTP API for a cleaner response than parsing from gcloud
echo -e "Instance groups: \n${INSTANCE_GROUPS}"
if [[ -z ${INSTANCE_GROUPS} ]] ; then
  echo "Cluster did not create instance group as expected"
  exit 1
else
  # Split instance groups response. The array is evaluated using the delimiters stored in IFS.
  # Restore IFS to its original state when done.
  OIFS=$IFS
  IFS=$' \t\n'
  ARRAY=(${INSTANCE_GROUPS})
  INSTANCE_GROUP_NAME=${ARRAY[6]} # Grabs 'foo-clus-default-pool-bar-grp' from sample response
  IFS=${OIFS}
fi

print_step "Setting named port 'http' on instance group ${INSTANCE_GROUP_NAME}"
gcloud compute instance-groups set-named-ports ${INSTANCE_GROUP_NAME} \
--named-ports=http:${NODE_PORT} --zone=${ZONE}

# TODO: Uncomment as soon as 'gcloud compute instance-groups managed set-autohealing' is GA. It is
# currently in alpha (gcloud alpha compute) which requires project to be whitelisted. For now, have
# to do this step manually. There is an instruction for this at the end.
# print_step "Set health check on instance group ${INSTANCE_GROUP_NAME}"
# gcloud compute instance-groups managed set-autohealing portability-auto-healing \
# --http-health-check=portability-health-check --zone=${ZONE}

print_step "Creating GCP backend service '${BACKEND_SERVICE_NAME}'"
gcloud compute backend-services create ${BACKEND_SERVICE_NAME} \
--port=80 --port-name=http --protocol=HTTP --global --http-health-checks=portability-health-check

print_step "Adding instance group ${INSTANCE_GROUP_NAME} as a backend to ${BACKEND_SERVICE_NAME}"
gcloud compute backend-services add-backend ${BACKEND_SERVICE_NAME} \
--instance-group=${INSTANCE_GROUP_NAME} --balancing-mode=UTILIZATION --global \
--instance-group-zone=${ZONE}

print_step "Creating Kubernetes service portability.api"
kubectl create -f ../k8s/api-service.yaml

print_step "Creating load balancer"
gcloud compute url-maps create ${LB_NAME} \
--default-service ${BACKEND_SERVICE_NAME}
gcloud compute url-maps add-path-matcher ${LB_NAME} \
--default-service ${BACKEND_SERVICE_NAME} --path-matcher-name "static-bucket-mapping" \
--backend-bucket-path-rules "/static/*=${STATIC_BUCKET_NAME}"

print_step "Reserving a static external IP"
gcloud compute addresses create ${LB_EXTERNAL_IP_NAME} --global
EXTERNAL_IPS=$(gcloud compute addresses list)
# Sample response:
# gcloud compute addresses list
# NAME                       REGION  ADDRESS         STATUS
# load-balancer-external-ip          35.201.127.254  IN_USE
# TODO: Consider using HTTP API for a cleaner response than parsing from gcloud
if [[ -z ${EXTERNAL_IPS} ]] ; then
  echo "Could not reserve external IP"
  exit 1
else
  # Split external IP response. The array is evaluated using the delimiters stored in IFS.
  # Restore IFS to its original state when done.
  OIFS=$IFS
  IFS=$' \t\n'
  ARRAY=(${EXTERNAL_IPS})
  EXTERNAL_IP_ADDRESS=${ARRAY[5]} # Grabs '35.201.127.254' from sample response
  IFS=${OIFS}
  echo -e "\nReserved external IP address: ${EXTERNAL_IP_ADDRESS}"
fi

print_step "Creating HTTPS proxy to our load balancer"
gcloud compute target-https-proxies create ${LB_HTTPS_PROXY_NAME} --url-map=${LB_NAME} \
--ssl-certificates=${SSL_CERT_NAME}

print_step "Creating global forwarding rule, i.e. load balancer 'frontend'"
gcloud compute forwarding-rules create ${LB_FORWARDING_RULE_NAME} \
    --address ${EXTERNAL_IP_ADDRESS} --ip-protocol TCP --ports=443 \
    --global --target-https-proxy ${LB_HTTPS_PROXY_NAME}

print_step "Creating a Kubernetes deployment"
echo -e "\nChoose a version tag. The latest image versions deployed to GCP are as follows."
IMAGE_NAME="gcr.io/$PROJECT_ID/portability-api"
IMAGE_TAGS=$(gcloud container images list-tags --project $PROJECT_ID $IMAGE_NAME)
echo $IMAGE_TAGS
read -p "Please enter the version tag to use (e.g. v1): " VERSION_TAG
IMAGE="$IMAGE_NAME:$VERSION_TAG"
# Substitute in the current image to our deployment yaml
sed -i "s|IMAGE|$IMAGE|g" "api-deployment.yaml"
kubectl create -f ../k8s/api-deployment.yaml
# Restore deployment yaml file to previous state so we don't check in an image
# that will become stale, and so the substitution works again next time
sed -i "s|$IMAGE|IMAGE|g" "api-deployment.yaml"

print_step "Opening up VM firewall rule to allow requests from load balancer and health checkers"
# This is actually easier than creating a new firewall rule because it's difficult to get the
# network tag applied to the VMs. We need to use the network tag for this firewall rule or else
# GCE Enforcer will remove the rule. We can't apply a custom network tag to our instance group.
# It's not feasible to apply one to individual VMs since it won't necessarily automatically
# apply to any new autoscale VMs.
HEALTH_CHECKER_IP_RANGES=209.85.152.0/22,209.85.204.0/22,35.191.0.0/16
LB_IP_RANGE=130.211.0.0/22
NETWORK_IP_RANGE=10.128.0.0/9
FIREWALL_RULES=$(gcloud compute firewall-rules list)
IFS=$'\n'
FIREWALL_RULES_ARRAY=($FIREWALL_RULES)
UPDATED_FIREWALL_RULE=false
for key in "${!FIREWALL_RULES_ARRAY[@]}"; do
  FIREWALL_RULE=${FIREWALL_RULES_ARRAY[$key]}
  IFS=$' \t'
  FIREWALL_RULE_ARRAY=($FIREWALL_RULE)
  FIREWALL_RULE_NAME=${FIREWALL_RULE_ARRAY[0]}
  if [[ $FIREWALL_RULE_NAME == *-vms ]]; then
    echo -e "Found vms firewall rule: $FIREWALL_RULE_NAME"
    EXISTING_ALLOWED_PROTOCOLS_PORTS=${FIREWALL_RULE_ARRAY[4]}
    UPDATE_FIREWALL_CMD="gcloud compute firewall-rules update $FIREWALL_RULE_NAME \
    --allow=tcp:${HEALTH_CHECK_PORT},tcp:${NODE_PORT},$EXISTING_ALLOWED_PROTOCOLS_PORTS \
    --source-ranges=${NETWORK_IP_RANGE},${HEALTH_CHECKER_IP_RANGES},${LB_IP_RANGE}"
    echo $UPDATE_FIREWALL_CMD
    ${UPDATE_FIREWALL_CMD}
    UPDATED_FIREWALL_RULE=true
  fi
done
if !(${UPDATED_FIREWALL_RULE}); then
  echo "Could not update firewall rule. Aborting"
  exit 1
fi

# TODO:
# - IAP on backend-service (whitelist select users). Might make more sense to do manually.

echo -e "\nDone creating project ${PROJECT_ID}!
Next steps, not done by this script, are:
1. Set the health check on the instance group. This can't be scripted yet!
   See note in this script. :(
2. Point the domain to our external IP ${EXTERNAL_IP_ADDRESS}
3. Deploy the latest docker image to our GKE cluster
4. Upload the latest static content to our bucket"

