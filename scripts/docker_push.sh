#!/bin/bash

#Script used to push new docker images to docker hub by travis.

# These environment variables are set via the Travis UI/CLI
echo "$DOCKER_PASSWORD" | docker login -u "$DOCKER_USERNAME" --password-stdin
docker push datatransferproject/demo
