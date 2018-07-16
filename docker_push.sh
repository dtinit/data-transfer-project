#!/bin/bash

#Script used to push new docker images to docker hug by travis.

echo "$DOCKER_PASSWORD" | docker login -u "$DOCKER_USERNAME" --password-stdin
docker push datatransferproject/demo
