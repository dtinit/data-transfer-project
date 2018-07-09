# Running DTP from Docker locally

If you want to run a demo of DTP locally, but don't want the hassle of setting up a
development environment and building everything from source, you can take advantage of our
pre-packaged docker demo images.

## Install docker

See [docs.docker.com/install](https://docs.docker.com/install/) to install Docker for your OS.

## Get the docker image

Once you have docker install you need to pull the latest image from 
[our DockerHub repo](https://hub.docker.com/r/datatransferproject/demo/).

To do this run `docker pull datatransferproject/demo` this will download the latest docker image
onto your machine.

## Get App Credentials

To run DTP you needed to get App Credentials from the services you want to transfer data to
and from. This allows you go call their apis.  See our documentation about [Keys](Keys.md).

Once your have created your env.secrets file you are ready to run the docker image.

## Running the docker image

You'll want to run:

`docker run --rm -p 3000:443 -p 8080:8080 --env-file <path/to/env.secrets> datatransferproject/demo`

  - `<path/to/env.secrets>` is the path the `env.secrets` file you created in the previous step.
  - `--rm` tells docker to clean up the file system after it is done running
  - `-p 3000:443 -p 8080:8080` exposes some port from the docker system to your machine.
  - `datatransferproject/demo` is the name of the image you downloaded.
  
 If everything worked you'll start to see some log lines scrolling by like: 
 `2018-07-09 12:23:01,078 [JobPollingService RUNNING] DEBUG org.dataportabilityproject.transfer.JobPollingService - Polling for a job in state CREDS_AVAILABLE`
 
 That means things are working.
 
 ## Interactive with the DTP UI
 
 You can now point your browser at [https://localhost:3000](https://localhost:3000/). That will
 take you to the demo UI for DTP.  You can then select the type of data, and the services you want
 to move it to and from.
 
 **Note:** You will only be able to transfer data to services you have acquired a key for.