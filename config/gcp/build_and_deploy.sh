#!/bin/sh

# Interactive script to generate a Dockerfile for the given environment.
# Can optionally build a new jar and docker image based on the command prompts.
#
# Usage: ./config/gcp/build_and_deploy.sh <binary> <env> [project-suffix]
# - binary is required and specifies which server to build.
#     This should be one of: api, web, worker
#     ex: api will build the portability-api binary
# - env is the environment you would like to build in. This should correspond to an environment dir
#     in config/environments.
# - project-suffix is required except for env=local
#
# Must be run from the root source directory googleplex-portability/
#
# ex: build_and_deploy.sh web qa qa2
# Will package and deploy portability-web using config/environments/qa/settings.yaml and
# project BASE_PROJECT_ID-qa2

if [[ $(pwd) != */googleplex-portability ]]; then
  echo "Please run out of /googleplex-portability directory. Aborting."
  exit 1
fi

# Constants
# script below sets env variables BASE_PROJECT_ID
source config/gcp/init_project_vars.sh

echo -e "Set hidden var:
BASE_PROJECT_ID: ${BASE_PROJECT_ID}"

if [ -z $1 ]; then
  echo "ERROR: Must provide a binary, e.g. 'api', 'web', 'worker'"
  exit 1
fi

if [ -z $2 ]; then
  echo "ERROR: Must provide an environment, e.g. 'local', 'test', 'qa', or 'prod'"
  exit 1
fi

BINARY=$1
ENV=$2
PROJECT_ID_SUFFIX=$3
SRC_DIR="portability-$BINARY"

# Parse settings file
SETTINGS_FILE="config/environments/$ENV/settings.yaml"
echo -e "\nParsing settings file $SETTINGS_FILE"
if [[ ! -e ${SETTINGS_FILE} ]]; then
  echo "Invalid environment $ENV entered. No settings file found at $SETTINGS_FILE. Aborting."
  exit 1
fi
PARSED_ALL_FLAGS=true
FLAG_ENV=$(grep -o 'env: [^, }]*' ${SETTINGS_FILE} | sed 's/^.*: //')
FLAG_CLOUD=$(grep -o 'cloud: [^, }]*' ${SETTINGS_FILE} | sed 's/^.*: //')
FLAG_BASE_URL=$(grep -o 'baseUrl: [^, }]*' ${SETTINGS_FILE} | sed 's/^.*: //')
FLAG_BASE_API_URL=$(grep -o 'baseApiUrl: [^, }]*' ${SETTINGS_FILE} | sed 's/^.*: //')
if [[ -z ${FLAG_ENV} ]]; then
  PARSED_ALL_FLAGS=false
  echo "Could not parse setting 'env' in $SETTINGS_FILE"
else
  echo -e "env: $FLAG_ENV"
fi
if [[ -z ${FLAG_CLOUD} ]]; then
  PARSED_ALL_FLAGS=false
  echo "Could not parse setting 'cloud' in $SETTINGS_FILE"
else
  echo -e "cloud: $FLAG_CLOUD"
fi
if [[ -z ${FLAG_BASE_URL} ]]; then
  PARSED_ALL_FLAGS=false
  echo "Could not parse setting 'baseUrl' in $SETTINGS_FILE"
else
  echo -e "baseUrl: $FLAG_BASE_URL"
fi
if [[ -z ${FLAG_BASE_API_URL} ]]; then
  PARSED_ALL_FLAGS=false
  echo "Could not parse setting 'baseApiUrl' in FLAG_BASE_API_URL"
else
  echo -e "baseApiUrl: $FLAG_BASE_API_URL"
fi
if !(${PARSED_ALL_FLAGS}) ; then
  exit 1
fi

echo -e "\nChecking that you have Maven installed"
mvn=$(which mvn)|| { echo "Maven (mvn) not found. Please install it and try again." >&2; exit 1; }
echo -e "Checking that you have docker installed"
docker=$(which docker)|| { echo "docker not found. Please install it and try again." >&2; exit 1; }
echo -e "Checking that you have gcloud installed"
gcloud=$(which gcloud)|| { echo "gcloud not found. Please install it and try again." >&2; exit 1; }

read -p "You should compile a new jar if there are java or index.html changes.
Compile and package jar at this time? (Y/n): " response
if [[ ! ${response} =~ ^(no|n| ) ]]; then
  # secrets.csv is deprecated except for local development. Delete any old versions of this file
  # so it doesn't make its way into our jar, even though our binary is configured to ignore it
  # for non-local environments.
  SECRETS_CSV_DEST_PATH="$SRC_DIR/src/main/resources/secrets.csv"
  if [[ -e ${SECRETS_CSV_DEST_PATH} ]]; then
    echo -e "\nRemoving old secrets.csv"
    rm ${SECRETS_CSV_DEST_PATH}
    if [[ -e ${SECRETS_CSV_DEST_PATH} ]]; then
      echo "Problem removing old secrets.csv. Aborting."
      exit 1
    fi
  fi

  # Local uses index from ng serve, everything else uses index built from
  # build_and_deploy_static_content.sh
  if [[ ${ENV} != "local" ]]; then
    # Copy index.html from local/ or test/ into $SRC_DIR/src/main/resources/static/index.html
    INDEX_HTML_DEST_PATH="$SRC_DIR/src/main/resources/static/index.html"
    if [[ -e ${INDEX_HTML_DEST_PATH} ]]; then
      echo -e "\nRemoving old index.html"
      rm ${INDEX_HTML_DEST_PATH}
      if [[ -e ${INDEX_HTML_DEST_PATH} ]]; then
        echo "Problem removing old index.html. Aborting."
        exit 1
      fi
    fi
    INDEX_HTML_SRC_PATH="config/environments/$ENV/index.html"
    echo -e "Copying index.html from $INDEX_HTML_SRC_PATH to $INDEX_HTML_DEST_PATH"
    cp $INDEX_HTML_SRC_PATH $INDEX_HTML_DEST_PATH
    if [[ ! -e ${INDEX_HTML_DEST_PATH} ]]; then
      echo "Problem copying index.html. Aborting."
      exit 1
    fi
    echo -e "Copied index.html"
  else
    # secrets.csv in our binary is only used for local development. For prod, app keys & secrets
    # are stored in GCS and secrets are encrypted with KMS.
    SECRETS_CSV_SRC_PATH="config/environments/$ENV/secrets.csv"
    echo -e "Copying secrets.csv from $SECRETS_CSV_SRC_PATH to $SECRETS_CSV_DEST_PATH"
    cp $SECRETS_CSV_SRC_PATH $SECRETS_CSV_DEST_PATH
    if [[ ! -e ${SECRETS_CSV_DEST_PATH} ]]; then
      echo "Problem copying secrets.csv. Aborting."
      exit 1
    fi
    echo -e "Copied secrets\n"
  fi

  # Compile jar with maven.
  echo -e "\nCompiling and installing...\n"
  mvn clean install
  echo -e "Packaging...\n"

  # TODO: Remove when spring is replaced
  if [[ $BINARY == "web" ]]; then
    mvn package -e spring-boot:repackage -pl portability-web
  else
    mvn package -pl $SRC_DIR
  fi
fi

read -p "Would you like to run the app jar at this time? (Y/n): " response
if [[ ! ${response} =~ ^(no|n| ) ]]; then
  COMMAND="java -jar $SRC_DIR/target/$SRC_DIR-1.0-SNAPSHOT.jar -cloud $FLAG_CLOUD -environment $FLAG_ENV -baseUrl $FLAG_BASE_URL -baseApiUrl $FLAG_BASE_API_URL"
  echo -e "running $COMMAND"
  $COMMAND
fi

# Generate Dockerfile based on env/settings.yaml.
# For now all flags will be passed to binary at run time via ENTRYPOINT but still in "image compile
# time". In the future, should look into ways of compiling these settings into the binary itself
# rather than the image. Also, it would be cleaner to pass settings.yaml directly rather than
# individual flags.

# LOCAL_DEBUG_SETTINGS are only set for env=LOCAL. They should not be used for prod environments.
LOCAL_DEBUG_SETTINGS=""
OPTIONAL_DEBUG_FLAG=""
if [[ ${FLAG_ENV} == "LOCAL" ]]; then
  LOCAL_DEBUG_SETTINGS="EXPOSE 5005/tcp"
  OPTIONAL_DEBUG_FLAG="\"-Xdebug\", \"-Xrunjdwp:transport=dt_socket,server=y,suspend=n,address=5005\","
  # Setup for allowing local instance to talk to GCP. This is useful for debugging as well as
  # avoiding the need for a local impl for each service we use.
  if [[ ${FLAG_CLOUD} == "GOOGLE" ]]; then
    CREDS_FILE=config/gcp/service_acct_creds.json
    read -p "
Caution!

You are using --cloud=GOOGLE which is not the default for env=LOCAL. This will connect to GCP, and
requires you download service account credentials to the following location:
$CREDS_FILE.

Continue? (Y/n): " response
    if [[ ${response} =~ ^(no|n| ) ]]; then
      echo "Aborting"
      exit 0
    fi
    # This is a test project. Only use test projects here!
    LOCAL_GCP_PROJECT="world-takeout-test"
    LOCAL_DEBUG_SETTINGS=$LOCAL_DEBUG_SETTINGS"
COPY $CREDS_FILE /service_acct_creds.json
ENV GOOGLE_PROJECT_ID=$LOCAL_GCP_PROJECT
ENV GOOGLE_APPLICATION_CREDENTIALS=/service_acct_creds.json
EXPOSE 5005/tcp"
  fi
fi

if [[ ${BINARY} == "web" || ${BINARY} == "api" ]]; then
  ENTRYPOINT_COMMAND="ENTRYPOINT [\"java\", $OPTIONAL_DEBUG_FLAG \"-jar\", \"/$BINARY.jar\", \"-cloud\", \"$FLAG_CLOUD\", \"-environment\", \"$FLAG_ENV\", \"-baseUrl\", \"$FLAG_BASE_URL\", \"-baseApiUrl\", \"$FLAG_BASE_API_URL\"]"
elif [[ ${BINARY} == worker ]]; then
  ENTRYPOINT_COMMAND="ENTRYPOINT [\"java\", $OPTIONAL_DEBUG_FLAG \"-jar\", \"/$BINARY.jar\", \"-cloud\", \"$FLAG_CLOUD\", \"-environment\", \"$FLAG_ENV\"]"
fi

# And onto generating the dockerfile...
cat >Dockerfile <<EOF
FROM gcr.io/google-appengine/openjdk:8
COPY $SRC_DIR/target/$SRC_DIR-1.0-SNAPSHOT.jar /$BINARY.jar
$LOCAL_DEBUG_SETTINGS
$ENTRYPOINT_COMMAND
EXPOSE 8080/tcp
EOF

echo -e "\nGenerated Dockerfile:\n"
cat Dockerfile
echo -e ""

if [[  ${FLAG_ENV} != "LOCAL" ]]; then
  if grep -q GOOGLE_APPLICATION_CREDENTIALS Dockerfile; then
  echo -e "\nProblem found in Dockerfile. Did you edit LOCAL_DEBUG_SETTINGS above?

You are setting GOOGLE_APPLICATION_CREDENTIALS in a non-local environment. This should be done for
local development only. Please fix this and try again."
  exit 1
  fi
fi

# Option to build a docker image. For non-local, the script will find the latest version tag for
# you or allow you to enter one manually.
read -p "Build a docker image this time? (Y/n): " response
if [[ ${response} =~ ^(no|n| ) ]]; then
  echo "Exiting"
  exit 0
else
  # we only have 2 gcp services: the api-server and the worker-server. Both the portability-api and
  # portability-web binary map to the api-server (this will be removed once we deprecate
  # portability-web) and the portability-worker maps to the worker-server.
  # TODO: remove this and use $SRC_DIR once portability-web is deprecated.
  if [[ $BINARY == "worker" ]]; then
    IMAGE_SUFFIX="portability-worker"
  else
    IMAGE_SUFFIX="portability-api"
  fi

  if [[ ${FLAG_ENV} == "LOCAL" ]]; then
    IMAGE_NAME="gcr.io/$PROJECT_ID-$ENV/$IMAGE_SUFFIX"
    read -p "Using local version tag v1. OK? (Y/n): " response
    if [[ ${response} =~ ^(no|n| ) ]]; then
      echo "Exiting"
      exit 0
    else
      VERSION_TAG=v1
      echo "Using version tag v1"
    fi
    PROJECT_ID="${BASE_PROJECT_ID}-${ENV}"
    IMAGE_NAME="gcr.io/$PROJECT_ID/$IMAGE_SUFFIX"
  else
    if [ -z $PROJECT_ID_SUFFIX ]; then
      echo -e "ERROR: Since env=${ENV} (!= local), you must provide a project ID suffix, i.e. 'qa8'
      for project ID ${BASE_PROJECT_ID}-qa8"
      exit 1
    fi
    PROJECT_ID="${BASE_PROJECT_ID}-${PROJECT_ID_SUFFIX}"
    IMAGE_NAME="gcr.io/$PROJECT_ID/$IMAGE_SUFFIX"
    read -p "Changing your default project to ${PROJECT_ID}. OK? (Y/n) " response
    if [[ ${response} =~ ^(no|n| ) ]]; then
      echo "Aborting"
      exit 0
    else
      gcloud config set project ${PROJECT_ID}
    fi
    echo -e "\nChoosing a version tag. Trying first to find latest image from GCP."
    IMAGE_TAGS=$(gcloud container images list-tags --project $PROJECT_ID $IMAGE_NAME)
    # Sample response:
    # gcloud container images list-tags gcr.io/foo/portability-api
    # DIGEST        TAGS                    TIMESTAMP
    # bar           baz                      2017-11-26T19:50:06
    # bar           v9                      2017-11-26T19:50:06
    # TODO: Consider using HTTP API for a cleaner response than parsing from gcloud. However this
    # would require prompting for an API key which might be too much hassle.
    if [[ -z ${IMAGE_TAGS} ]] ; then
      echo "Couldn't find images on GCP"
    else
      echo -e "Found images:\n$IMAGE_TAGS"
      IFS=" "
      IMAGE_TAGS_ARRAY=(${IMAGE_TAGS})
      LATEST_IMAGE_TAGS=${IMAGE_TAGS_ARRAY[3]}
      if [[ ! -z ${LATEST_IMAGE_TAGS} ]] ; then
        # Split image tags response. The array is evaluated using the delimiters stored in IFS.
        # Restore IFS to its original state when done.
        OIFS=$IFS
        IFS=","
        LATEST_IMAGE_TAGS_ARRAY=($LATEST_IMAGE_TAGS)
        IFS=${OIFS}
        for key in "${!LATEST_IMAGE_TAGS_ARRAY[@]}"; do
          if [[ ${LATEST_IMAGE_TAGS_ARRAY[$key]} =~ v[0-9] ]] ; then
            LATEST_VERSION_TAG=${LATEST_IMAGE_TAGS_ARRAY[$key]} # Finds 'v9' in sample response
          fi
        done
        if [[ ! -z ${LATEST_VERSION_TAG} ]] ; then
          echo -e "Latest version tag is $LATEST_VERSION_TAG\n"
          VERSION_NUMBER=${LATEST_VERSION_TAG#v}
          NEW_VERSION_NUMBER=$((VERSION_NUMBER + 1))
          NEW_VERSION_TAG_PENDING="v$NEW_VERSION_NUMBER"
          read -p "Next version tag is: $NEW_VERSION_TAG_PENDING. Would you like to use tag $NEW_VERSION_TAG_PENDING? (Y/n): " response
          if [[ ! ${response} =~ ^(no|n| ) ]]; then
            VERSION_TAG=${NEW_VERSION_TAG_PENDING}
          fi
        fi
      fi
    fi
    if [[ -z ${VERSION_TAG} ]] ; then
      read -p "Please enter a version tag of the form 'v' followed by a number, like v25, or 'abort': " response
      if [[ ${response} == "abort" ]]; then
        echo "Aborting"
        exit 1
      else
        if [[ ${response} =~ v[0-9] ]] ; then
          VERSION_TAG=${response}
        else
          echo -e "Invalid version tag $response. Must be of the form 'v' followed by a number, like v25.\nAborting."
          exit 1
        fi
      fi
    fi
  fi
fi

IMAGE="$IMAGE_NAME:$VERSION_TAG"
echo ""
read -p "Build docker image $IMAGE? (Y/n): " response
if [[ ${response} =~ ^(no|n| ) ]]; then
  echo "Exiting"
  exit 0
else
  docker build -t $IMAGE .
fi

echo ""
if [[ ${ENV} == "local" ]]; then
  read -p "Run docker image $IMAGE locally? (Y/n): " response
  if [[ ${response} =~ ^(no|n| ) ]]; then
    echo "Continuing"
  else
    docker run -ti --rm -p 8080:8080 -p 5005:5005 $IMAGE
  fi
fi

if [[ ${ENV} != "local" ]]; then
  echo ""
  read -p "Push docker image $IMAGE to cloud? (Y/n): " response
  if [[ ${response} =~ ^(no|n| ) ]]; then
    echo "Exiting"
    exit 0
  else
    gcloud docker -- push $IMAGE
  fi
fi

echo "Done"