# Running DTP Locally

This document outlines how to run our [demo server](https://github.com/google/data-transfer-project/tree/master/distributions/demo-server)
locally. The demo consists of all three DTP components (an API server, a
Transfer Worker server, and a frontend) all packaged into one Docker image.
Note that in production, the API and Transfer Workers run separately,
but they are combined into the same binary here for demo purposes.

## Acquire keys
To run the Data Transfer Project, each hosted instance (including your
local one) needs its own API keys for each service it will facilitate
transfers with.

Please see the [API keys](Keys.md) document for instructions on acquiring keys.

To be clear, **you will only be able to transfer data to/from services you have acquired a key for.** This is by design, as explained in [API keys](Keys.md).

## Setup keys

The first time you run the demo image, you need to configure your
credentials by copying `distributions/demo-server/env.secrets.template` to
`env.secrets` and inserting the API keys and secrets you acquired above.

## Install Docker

See [docs.docker.com/install](https://docs.docker.com/install/) to install Docker for your OS.

## Start Docker

You only need to do this once:
`sudo service docker start`

## Obtain a Docker image

You may run our latest [demo image](https://hub.docker.com/r/datatransferproject/demo/) from dockerhub, or build it yourself.

To download our latest demo image, run:

`docker pull datatransferproject/demo`

Or, to build it yourself, run:

1. `cd client-rest; npm install` (see [First Run/Setup](Developer.md#first-runsetup) for more details)
2. `./gradlew :distributions:demo-server:dockerize`

This will generate the dockerfile and build an image, copying over local settings (configured in `.gradle/properties.gradle`) and using the local "cloud hosting" extension.

## Run the Docker image

`docker run --rm -p 3000:443 -p 5005:5005 -p 8080:8080 --env-file env.secrets --name dtp-demo datatransferproject/demo`

will run the demo server image from above on `localhost:8080`.

  - `--rm` tells docker to clean up the file system after it is done running
  - `-p 3000:443 -p 8080:8080` exposes some port from the docker system to your machine
  - `datatransferproject/demo` is the name of the image you obtained

If everything worked, you'll start to see some log lines scrolling by, like:
 `2018-07-09 12:23:01,078 [JobPollingService RUNNING] DEBUG org.datatransferproject.transfer.JobPollingService - Polling for a job in state CREDS_AVAILABLE`

## Interact with the application

You should now be able to access the web application at https://localhost:3000.

The API is accessible via https://localhost:3000/api/datatypes. A java debugger can be connected via `port 5005`.

You can interact with the docker image via `docker exec -it dtp-demo <command>`
