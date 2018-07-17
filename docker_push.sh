#!/bin/bash

#Script used to push new docker images to docker hug by travis.

./gradlew -PcloudType=local :distributions:demo-server:dockerize
# These envionment variable's are set via the Travis UI/CLI
echo "$DOCKER_PASSWORD" | docker login -u "$DOCKER_USERNAME" --password-stdin
docker push datatransferproject/demo
