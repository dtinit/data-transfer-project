#!/bin/bash

#Script used by Travis to push new docker images to docker hub on PR merge to master.

./gradlew -PcloudType=local :distributions:demo-server:dockerize
# These environment variables are set via the Travis UI/CLI
echo "$DOCKER_PASSWORD" | docker login -u "$DOCKER_USERNAME" --password-stdin
docker push datatransferproject/demo
