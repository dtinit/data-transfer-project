#!/bin/sh

# Interactive script to generate a Dockerfile for the given environment.
# Can optionally build a new jar and docker image based on the command prompts.
# Usage: ./build.sh <env> [project-suffix]
# project-suffix is required except for env=local
# ex: ./build.sh qa qa8 # Will use config/qa/settings.yaml & project dataliberation-portability-qa8

# Constants
BASE_PROJECT_ID="dataliberation-portability"

if [ -z $1 ]; then
  echo "ERROR: Must provide an environment, e.g. 'local', 'test', 'qa', or 'prod'"
  exit 1
fi
ENV=$1
PROJECT_ID_SUFFIX=$2

# Parse settings file
SETTINGS_FILE="config/$ENV/settings.yaml"
echo -e "\nParsing settings file $SETTINGS_FILE"
if [[ ! -e ${SETTINGS_FILE} ]]; then
  echo "Invalid environment $ENV entered. No settings file found at $SETTINGS_FILE. Aborting."
  exit 1
fi
PARSED_ALL_FLAGS=true
FLAG_ENV=$(grep -o 'env: [^, }]*' ${SETTINGS_FILE} | sed 's/^.*: //')
FLAG_CLOUD=$(grep -o 'cloud: [^, }]*' ${SETTINGS_FILE} | sed 's/^.*: //')
if [[ -z ${FLAG_ENV} ]]; then
  PARSED_ALL_FLAGS=false
  echo "Could not parse setting 'env' in $SETTINGS_FILE"
else
  echo -e "FLAG_ENV: $FLAG_ENV"
fi
if [[ -z ${FLAG_CLOUD} ]]; then
  PARSED_ALL_FLAGS=false
  echo "Could not parse setting 'cloud' in $SETTINGS_FILE"
else
  echo -e "FLAG_CLOUD: $FLAG_CLOUD"
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

read -p "Compile and package jar at this time? (Y/n): " response
if [[ ! ${response} =~ ^(no|n| ) ]]; then
  # Note: WT engineers should store copies of secrets.csv locally in each environment's directory,
  # e.g. local/secrets.csv and test/secrets.csv. See secrets_template.csv for more info on secrets.

  # Copy secrets.csv from local/ or test/ into portability-web/src/main/resources/secrets.csv
  SECRETS_CSV_DEST_PATH="portability-web/src/main/resources/secrets.csv"
  if [[ -e ${SECRETS_CSV_DEST_PATH} ]]; then
    echo -e "\nRemoving old secrets.csv"
    rm ${SECRETS_CSV_DEST_PATH}
    if [[ -e ${SECRETS_CSV_DEST_PATH} ]]; then
      echo "Problem removing old secrets.csv. Aborting."
      exit 1
    fi
  fi
  SECRETS_CSV_SRC_PATH="config/$ENV/secrets.csv"
  echo -e "Copying secrets.csv from $SECRETS_CSV_SRC_PATH to $SECRETS_CSV_DEST_PATH"
  cp $SECRETS_CSV_SRC_PATH $SECRETS_CSV_DEST_PATH
  if [[ ! -e ${SECRETS_CSV_DEST_PATH} ]]; then
    echo "Problem copying secrets.csv. Aborting."
    exit 1
  fi
  echo -e "Copied secrets\n"

  # Compile jar with maven.
  echo -e "\nCompiling and installing...\n"
  mvn clean install
  echo -e "Packaging...\n"
  # TODO: Remove when spring is replaced
  mvn package -e spring-boot:repackage -pl portability-web
fi

# Generate Dockerfile based on env/settings.yaml.
# For now all flags will be passed to binary at run time via ENTRYPOINT but still in "image compile
# time". In the future, should look into ways of compiling these settings into the binary itself
# rather than the image.
cat >Dockerfile <<EOF
FROM gcr.io/google-appengine/openjdk:8
COPY portability-web/target/portability-web-1.0-SNAPSHOT.jar /app.jar
EXPOSE 8080/tcp
ENTRYPOINT ["java", "-jar", "/app.jar", "-cloud", "$FLAG_CLOUD", "-environment", "$FLAG_ENV"]
EOF
echo -e "\nGenerated Dockerfile:\n"
cat Dockerfile
echo -e ""

# Option to build a docker image. For non-local, the script will find the latest version tag for
# you or allow you to enter one manually.
read -p "Build a docker image this time? (Y/n): " response
if [[ ${response} =~ ^(no|n| ) ]]; then
  echo "Exiting"
  exit 0
else
  if [[ ${ENV} == "local" ]]; then
    read -p "Using local version tag v1. OK? (Y/n): " response
    if [[ ${response} =~ ^(no|n| ) ]]; then
      echo "Exiting"
      exit 0
    else
      VERSION_TAG=v1
      echo "Using version tag v1"
    fi
    PROJECT_ID="${BASE_PROJECT_ID}-${ENV}"
  else
    if [ -z $PROJECT_ID_SUFFIX ]; then
      echo -e "ERROR: Since env=${ENV} (!= local), you must provide a project ID suffix, i.e. 'qa8'
      for project ID dataliberation-portability-qa8"
      exit 1
    fi
    PROJECT_ID="${BASE_PROJECT_ID}-${PROJECT_ID_SUFFIX}"
    read -p "Changing your default project to ${PROJECT_ID}. OK? (Y/n) " response
    if [[ ${response} =~ ^(no|n| ) ]]; then
      echo "Aborting"
      exit 0
    else
      gcloud config set project ${PROJECT_ID}
    fi
    echo -e "\nChoosing a version tag. Trying first to find latest image from GCP."
    IMAGE_TAGS=$(gcloud container images list-tags --project $PROJECT_ID gcr.io/$PROJECT_ID/portability-api)
    if [[ -z ${IMAGE_TAGS} ]] ; then
      echo "Couldn't find images on GCP"
    else
      echo -e "Found images:\n$IMAGE_TAGS"
      IFS=" "
      IMAGE_TAGS_ARRAY=(${IMAGE_TAGS})
      LATEST_IMAGE_TAGS=${IMAGE_TAGS_ARRAY[3]}
      echo -e "\nLatest image tags: $LATEST_IMAGE_TAGS"
      if [[ ! -z ${LATEST_IMAGE_TAGS} ]] ; then
        IFS=","
        LATEST_IMAGE_TAGS_ARRAY=($LATEST_IMAGE_TAGS)
        for key in "${!LATEST_IMAGE_TAGS_ARRAY[@]}"; do
          if [[ ${LATEST_IMAGE_TAGS_ARRAY[$key]} =~ v[0-9] ]] ; then
            LATEST_VERSION_TAG=${LATEST_IMAGE_TAGS_ARRAY[$key]}
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

DOCKER_IMAGE="gcr.io/$PROJECT_ID/portability-api:$VERSION_TAG"
echo ""
read -p "Build docker image $DOCKER_IMAGE? (Y/n): " response
if [[ ${response} =~ ^(no|n| ) ]]; then
  echo "Exiting"
  exit 0
else
  docker build -t gcr.io/$PROJECT_ID/portability-api:$VERSION_TAG .
fi

if [[ ${ENV} != "local" ]]; then
  echo ""
  read -p "Push docker image to cloud? (Y/n): " response
  if [[ ${response} =~ ^(no|n| ) ]]; then
    echo "Exiting"
    exit 0
  else
    gcloud docker -- push gcr.io/$PROJECT_ID/portability-api:$VERSION_TAG
  fi
fi

echo "Done"