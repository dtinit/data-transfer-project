#!/bin/sh

# Script to set up a new GCP environment based on the passed in name
# Must run from gcp/ directory
# Usage: ./setup_environment <ENV_NAME>

# Constants
GOOGLE_BILLING_ACCOUNT=0072FE-A63213-1B46B7
GOOGLE_ORGANIZATION=433637338589 #ID for google.com organization in GCP project
GCE_ENFORCER_REASON=pre-launch
INSTANCE_GROUP_SIZE=2 #Number of VMs to run for our GKE jobs
ZONE=us-central1-a
STATIC_BUCKET_NAME=static-bucket
NODE_PORT=30580 #If this changes, change nodePort in api-service.yaml too
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
PROJECT_ID="dataliberation-portability-$ENV"
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
gcloud projects create ${PROJECT_ID} --name=${PROJECT_ID} --organization=$GOOGLE_ORGANIZATION

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
gcloud alpha billing projects link ${PROJECT_ID} --billing-account=$GOOGLE_BILLING_ACCOUNT

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

# Setting named port on instance group. First, have to get the name of the instance group. This
# is auto generated by GKE cluster creation above and we can't change it. :(
INSTANCE_GROUPS=$(gcloud compute instance-groups list)
echo -e "Instance groups: \n${INSTANCE_GROUPS}"
if [[ -z ${INSTANCE_GROUPS} ]] ; then
  echo "Cluster did not create instance group as expected"
  exit 1
else
  IFS=$' \t\n'
  ARRAY=(${INSTANCE_GROUPS})
  INSTANCE_GROUP_NAME=${ARRAY[6]}
fi

print_step "Setting named port 'http' on instance group ${INSTANCE_GROUP_NAME}"
gcloud compute instance-groups set-named-ports ${INSTANCE_GROUP_NAME} \
--named-ports=http:${NODE_PORT} --zone=${ZONE}

print_step "Creating Kubernetes service portability.api"
kubectl create -f api-service.yaml

print_step "Creating health check for backend service"
gcloud compute http-health-checks create portability-health-check --port=${NODE_PORT}

print_step "Creating GCP backend service '${BACKEND_SERVICE_NAME}'"
gcloud compute backend-services create ${BACKEND_SERVICE_NAME} \
--port=80 --port-name=http --protocol=HTTP --global --http-health-checks=portability-health-check

print_step "Adding instance group ${INSTANCE_GROUP_NAME} as a backend to ${BACKEND_SERVICE_NAME}"
gcloud compute backend-services add-backend ${BACKEND_SERVICE_NAME} \
--instance-group=${INSTANCE_GROUP_NAME} --balancing-mode=UTILIZATION --global \
--instance-group-zone=${ZONE}

print_step "Creating load balancer"
gcloud compute url-maps create ${LB_NAME} \
--default-service ${BACKEND_SERVICE_NAME}
gcloud compute url-maps add-path-matcher ${LB_NAME} \
--default-service ${BACKEND_SERVICE_NAME} --path-matcher-name "static-bucket-mapping" \
--backend-bucket-path-rules "/static/*=${STATIC_BUCKET_NAME}"

print_step "Reserving a static external IP"
gcloud compute addresses create ${LB_EXTERNAL_IP_NAME} --global
EXTERNAL_IPS=$(gcloud compute addresses list)
if [[ -z ${EXTERNAL_IPS} ]] ; then
  echo "Could not reserve external IP"
  exit 1
else
  IFS=$' \t\n'
  ARRAY=(${EXTERNAL_IPS})
  EXTERNAL_IP_ADDRESS=${ARRAY[5]}
  echo -e "\nReserved external IP address: ${EXTERNAL_IP_ADDRESS}"
fi

print_step "Creating HTTPS proxy to our load balancer"
gcloud compute target-https-proxies create ${LB_HTTPS_PROXY_NAME} --url-map=${LB_NAME} \
--ssl-certificates=${SSL_CERT_NAME}

print_step "Creating global forwarding rule, i.e. load balancer 'frontend'"
gcloud compute forwarding-rules create ${LB_FORWARDING_RULE_NAME} \
    --address ${EXTERNAL_IP_ADDRESS} --ip-protocol TCP --ports=443 \
    --global --target-https-proxy ${LB_HTTPS_PROXY_NAME}

# TODO:
# - IAP on backend-service (whitelist select users). Might make more sense to do manually.
# - Firewall

echo -e "\nDone creating project ${PROJECT_ID}!
Next steps, not done by this script, are:
1. Point the domain to our external IP ${EXTERNAL_IP_ADDRESS}
2. Deploy the latest docker image to our GKE cluster
3. Upload the latest static content to our bucket"

