#!/bin/sh

# Interactive script to build a new jar.
#
# Usage: ./config/gcp/build_jar.sh <binary> <env>
# - binary is required and specifies which server to build.
#     This should be one of: api, worker
#     ex: api will build the portability-api binary
# - env is the environment you would like to build in. This should correspond to an environment dir
#     in config/environments. Settings for this environment are copied into the binary.
#
# Must be run from the root source directory googleplex-portability/

if [[ $(pwd) != */googleplex-portability ]]; then
  echo "Please run out of /googleplex-portability directory. Aborting."
  exit 1
fi

if [ -z $1 ]; then
  echo "ERROR: Must provide a binary: 'api' or 'worker'"
  exit 1
fi

if [ -z $2 ]; then
  echo "ERROR: Must provide an environment, e.g. 'local', 'test', 'qa', or 'prod'"
  exit 1
fi

BINARY=$1
ENV=$2
SRC_DIR="portability-$BINARY"

mvn=$(which mvn)|| { echo "Maven (mvn) not found. Please install it and try again." >&2; exit 1; }

# Copy settings yaml files from ENV/settings/ into $SRC_DIR/src/main/resources/
SETTINGS_DEST_PATH="$SRC_DIR/src/main/resources/settings/"
if [[ -e ${SETTINGS_DEST_PATH} ]]; then
  echo -e "\nRemoving old settings folder"
  rm -rf ${SETTINGS_DEST_PATH}
  if [[ -e ${SETTINGS_DEST_PATH} ]]; then
    echo "Problem removing old settings.yaml. Aborting."
    exit 1
  fi
fi
mkdir $SETTINGS_DEST_PATH
SETTINGS_SRC_PATH="config/environments/$ENV/settings/"
echo -e "Copying common.yaml from $SETTINGS_SRC_PATH to $SETTINGS_DEST_PATH"
cp "${SETTINGS_SRC_PATH}common.yaml" "${SETTINGS_DEST_PATH}common.yaml"
if [[ ! -e "${SETTINGS_DEST_PATH}common.yaml" ]]; then
  echo "Problem copying settings/common.yaml. Aborting."
  exit 1
fi
if [[ $BINARY == "web" || $BINARY == "api" ]]; then
  echo -e "Copying api.yaml from $SETTINGS_SRC_PATH to $SETTINGS_DEST_PATH"
  cp "${SETTINGS_SRC_PATH}api.yaml" "${SETTINGS_DEST_PATH}api.yaml"
  if [[ ! -e "${SETTINGS_DEST_PATH}api.yaml" ]]; then
    echo "Problem copying settings/api.yaml. Aborting."
    exit 1
  fi
fi
echo -e "Copied settings/"

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
  INDEX_HTML_DEST_PATH_DIR="$SRC_DIR/src/main/resources/static/"
  INDEX_HTML_DEST_PATH="$INDEX_HTML_DEST_PATH_DIR/index.html"
  if [[ ! -e $INDEX_HTML_DEST_PATH_DIR ]]; then
    mkdir $INDEX_HTML_DEST_PATH_DIR
  fi
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

mvn package -pl $SRC_DIR

read -p "Would you like to run the app jar at this time? (Y/n): " response
if [[ ! ${response} =~ ^(no|n| ) ]]; then
  COMMAND="java -jar -Xdebug -Xrunjdwp:transport=dt_socket,server=y,suspend=n,address=5005 $SRC_DIR/target/$SRC_DIR-1.0-SNAPSHOT.jar"
  echo -e "running $COMMAND"
  $COMMAND
fi