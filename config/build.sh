#!/bin/sh

if [ -z $1 ]; then
  echo "ERROR: Must provide an environment, e.g. 'test' or 'prod'"
  exit 1
fi
ENV=$1
SETTINGS_FILE="config/$ENV/settings.yaml"

if [[ ! -e ${SETTINGS_FILE} ]]; then
  echo "Invalid environment $ENV entered. No settings file found at $SETTINGS_FILE. Aborting."
  exit 1
fi

echo -e "\nChecking that you have Maven installed"
mvn=$(which mvn)|| { echo "Maven (mvn) not found. Please install it and try again." >&2; exit 1; }
# param tag = tag for docker image, e.g. v2

# Note: WT engineers should store copies of secrets.csv locally in each environment's directory,
# e.g. local/secrets.csv and test/secrets.csv. See secrets_template.csv for more info on secrets.

# Copy secrets.csv from local/ or test/ into portability-web/src/main/resources/secrets.csv
SECRETS_CSV_DEST_PATH="portability-web/src/main/resources/secrets.csv"
if [[ -e ${SECRETS_CSV_DEST_PATH} ]]; then
  echo "Removing old secrets.csv"
  rm ${SECRETS_CSV_DEST_PATH}
  if [[ -e ${SECRETS_CSV_DEST_PATH} ]]; then
    echo "Problem removing old secrets.csv. Aborting."
    exit 1
  fi
fi
SECRETS_CSV_SRC_PATH="config/$ENV/secrets.csv"
echo -e "\nCopying secrets.csv from $SECRETS_CSV_SRC_PATH to $SECRETS_CSV_DEST_PATH"
cp $SECRETS_CSV_SRC_PATH $SECRETS_CSV_DEST_PATH
if [[ ! -e ${SECRETS_CSV_DEST_PATH} ]]; then
  echo "Problem copying secrets.csv. Aborting."
  exit 1
fi
echo "Copied secrets"

# Compile jar with maven.
echo -e "\nCompiling and installing...\n"
mvn clean install
echo -e "\nPackaging...\n"
# TODO: Remove when spring is replaced
mvn package -e spring-boot:repackage -pl portability-web

# TODO: parse settings file
FLAG_ENV="LOCAL"
FLAG_CLOUD="GOOGLE"

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

# TODO: Build image with 'docker build'

