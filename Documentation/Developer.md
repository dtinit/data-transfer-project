
# Development

## Communication

 * General questions and discussion: [dtp-discuss@googlegroups.com](mailto:dtp-discuss@googlegroups.com)
 * [Slack channel](https://portability.slack.com), email
   [portability-maintainers@googlegroups.com](mailto:portability-maintainers@googlegroups.com) for invitation
 * Weekly Skype call, email [portability-maintainers@googlegroups.com](mailto:portability-maintainers@googlegroups.com)
   for invitation

## First Run/Setup

###  From top level directory
* Install Git: `sudo apt-get install git-all`
* Install Gradle: `sudo apt-get install gradle`
 * Install NVM: `curl -o- https://raw.githubusercontent.com/creationix/nvm/v0.33.0/install.sh | bash`
 * Install Node: `nvm install node`
 * Install Angular: `npm install -g @angular/cli`
 * [Install Docker](https://docs.docker.com/install/)
 * Install the Angular CLI `./gradlew client:installLocalAngularCli`

## IntelliJ setup
The following instructions work for IntelliJ IDEA version 2017.2.6.

#### Import the project
 * Open IntelliJ -> Import Project
 * Navigate to the 'data-transfer-project' directory where you have git cloned the repo
 * Import project from external model (Gradle) -> uncheck 'Create separate module per source set' -> Finish

#### Enable annotation processing
 * Go to File -> Settings -> Build, Execution, Deployment -> Compiler -> Annotation Processors
 * Check 'Enable annotation processing'
 * Store generated sources relative to: 'Module content root' (not the default)
 * Production sources directory: `build/classes/java/`

#### Setup formatting
* File -> Settings -> Editor -> General -> Ensure line feed at file end on Save
* Install the 'google-java-format' plugin, and enable it in Settings

#### Setup automatic license header
* Go to File -> Settings -> Editor -> Copyright -> Copyright Profiles
* Click the green + button, add a new profile called "Data-Transfer-Project-Authors". Use this as the text:

```
Copyright $today.year The Data Transfer Project Authors.

Licensed under the Apache License, Version 2.0 (the "License");
you may not use this file except in compliance with the License.
You may obtain a copy of the License at

https://www.apache.org/licenses/LICENSE-2.0

Unless required by applicable law or agreed to in writing, software
distributed under the License is distributed on an "AS IS" BASIS,
WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
See the License for the specific language governing permissions and
limitations under the License.
```
* Create a new temp file (e.g. Test.java) in Intellij.
* Inside the file, enter Alt-Insert -> Copyright
* It should prompt you to select the new Copyright profile
* The copyright should be added to all new files from now on (note: it might be collapsed so not immediately obvious)

## Acquire Keys
To run an instance of DTP you need to have API keys from the services you wish to interact with.
Please see the [acquire keys](Keys.md) document for points on how to do this.

## Starting Docker
You only need to do this once. 
* `sudo service docker start`

## Building/Running locally
The following builds and optionally runs the demo server (including Worker, API and UI) locally

 * NOTE: The first time you run you need to configure your credentials by copying
   distributions/demo-server/env.secrets.template to env.secrets and inserting the API keys
   and secrets for the services you with to interact with.

 * `./gradlew -PcloudType=local :distributions:demo-server:dockerize`
   * This copies over LOCAL settings (configured in .gradle/properties.gradle) using
   the LocalCloud implementation
   * This will also build the docker image.

 * `docker run --rm -p 3000:443 -p 5005:5005 -p 8080:8080 --env-file distributions/demo-server/env.secrets --name dtp-demo datatransferproject/demo`
   * This will run the demo server image that was just created on localhost:8080
   

 * You should now be able to access the web application at https://localhost:3000
 * The API is accessible via https://localhost:8080/_/listDataTypes
 * A java debugger can be connected via port 8080
 * You can interact with the docker image via `docker exec -it dtp-demo <command>`

## Deploying in production

A demo distribution for Google Cloud Platform is available at
`distributions/demo-google-deployment`.

A demo Azure distribution is also in development.

## Build Problem F.A.Q

### AutoValue errors
If you get an error `error: duplicate class... final class AutoValue_...` it is indicative of your IDE and gradle clashing.  To fix it you want to delete the referenced build/classes directory.

## Misc Updates
There is a rest version of the client and demo-server in progress. While it is not complete yet and lacks some of the
features in the http based client, it is still runnable and works for all services that implement oauth2.

The following runs the client-rest api
  * `cd client-rest`
  * `ng serve --ssl --port 3000 --proxy-config proxy.conf.json`

The following builds and runs the demo-server (which contains the worker and the api) with the jettyrest transport to be
used with the client-rest UI.
  * `docker network create dataportability`
  * `./gradlew -PtransportType=jettyrest -PapiPort=3000  -PcloudType=local clean check :distributions:demo-server:dockerize`
  * `docker run --rm -p 8080:8080 -p 5005:5005 -p 3000:3000 --env-file distributions/demo-server/env.secrets --name demoserver --network dataportability dataportability/demo`

