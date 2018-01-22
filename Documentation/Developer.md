
# Development

## Communication

 * General questions and discussion: [portability-discuss@googlegroups.com](mailto:portability-discuss@googlegroups.com)
 * [Slack channel](https://portability.slack.com), email 
   [portability-maintainers@googlegroups.com](mailto:portability-maintainers@googlegroups.com) for invitation
 * Weekly Skype call, email [portability-maintainers@googlegroups.com](mailto:portability-maintainers@googlegroups.com)
   for invitation

## First Run/Setup

###  From top level directory
* Install Git: `sudo apt-get install git-all`
* Install Maven: `sudo apt-get install maven`

### From /client directory
 * Install NVM: `curl -o- https://raw.githubusercontent.com/creationix/nvm/v0.33.0/install.sh | bash`
 * Install Node: `nvm install node`
 * Install Angular: `npm install -g @angular/cli`
 * Install modules: `npm install`
 * Test `ng serve --port 3000 --proxy-config proxy.conf.json --environment local`

## Environment-specific settings
Environment-specific settings (values for PortabilityFlags) are stored
per environment in the environments/ directory. There is a subdirectory
for each environment: local/, qa/, test/, and prod/. Each environment
subdirectory contains a settings.yaml and common.yaml for PortabilityFlags values.

For local development and testing, a secrets.csv file should be included in config/environments/local.
This should contain all your API keys, see portability-core/src/resources/secrets_template.csv for an example.

## Building/Running portability-core CLI locally
The following builds and optionally runs the core CLI

 * `bin/build_and_run_jar.sh core local`
   * This copies over LOCAL secrets and settings and compiles the core cli jar.
   * This will also prompt you to run the jar.

## Building/Running the API server locally
The following builds and optionally runs the API server on port 8080

 * `bin/build_and_run_jar.sh api local`
   * This copies over LOCAL secrets and settings and compiles the api jar.
   * This will also prompt you to run the jar.

## Building/Running the Worker locally
The following builds and optionally runs the worker binary

 * `bin/build_and_run_jar.sh worker local`
   * This copies over LOCAL secrets and settings and compiles the worker jar.
   * This will also prompt you to run the jar.

## Running angular in dev mode

The following commands will run the angular frontend in dev mode locally proxying requests to the local webserver.

* `cd client/`
* [optional] `npm install` #required first time through
* `ng serve --port 3000 --proxy-config proxy.conf.json --environment local`

## Running angular tests

The following commands will run the angular frontend in dev mode locally proxying requests to the local webserver.

* `cd client/`
* `ng test --env=local -sm=false`
  * Flag for environment set to local
  * Flag for sourcemaps set to false gives better debugging messages

## Deploying in production

[See config/README.md](../config/README.md)

## This is not an official Google product
