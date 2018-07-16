#!/bin/bash

#Script used to push new docker images to docker hug by travis.

./gradlew -PcloudType=local :distributions:demo-server:dockerize
echo "$DOCKER_PASSWORD" | docker login -u "$DOCKER_USERNAME" --password-stdin
docker push datatransferproject/demo
