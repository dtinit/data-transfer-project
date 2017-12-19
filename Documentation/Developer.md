
# Development

## First Run/Setup
 * Install Git: `sudo apt-get install git-all`
 * Install Gerrit commit ID hook: ``curl -Lo `git rev-parse --git-dir`/hooks/commit-msg https://gerrit-review.googlesource.com/tools/hooks/commit-msg ; chmod +x `git rev-parse --git-dir`/hooks/commit-msg``
 * Install Maven: `sudo apt-get install maven`
 * Install NVM: `curl -o- https://raw.githubusercontent.com/creationix/nvm/v0.33.0/install.sh | bash`
 * Install Node: `nvm install node`
 * Install Angular: `npm install -g angular-cli`

## Building/Running CLI
 * Add a secrets_[environment].csv file to src/main/resources in portability-core and/or
   in portability-api, this should contain
   all your API keys, see secrets_template.csv for an example.
 * `mvn compile` will build the project.
 * `mvn exec:java -pl portability-core -Dexec.args="-cloud LOCAL -environment LOCAL"` will run the command line tool.

## Building/Running webapp locally

### Running angular in dev mode

The following commands will run the angular frontend in dev mode locally proxying requests to the local webserver.

* `cd client/`
* [optional] `npm install` #required first time through
* `ng serve --port 3000 --proxy-config proxy.conf.json`

### Running the API server in dev mode

The following builds and run the API server on port 8080

* `mvn clean install`
* `mvn exec:java -pl portability-api -Dexec.args="-cloud LOCAL -environment LOCAL -baseApiUrl http://localhost:8080 -baseUrl http://localhost:3000"`

For debugging, set this environment variable before running the mvn exec:java command above
* `export MAVEN_OPTS="-Xdebug -Xnoagent -Djava.compiler=NONE -Xrunjdwp:transport=dt_socket,address=5005,server=y,suspend=n"`

### Building/Running Worker App
 * Add a secrets_[environment].csv file to src/main/resources in portability-core and/or
   in portability-worker, this should contain
   all your API keys, see secrets_template.csv for an example.
 * `mvn compile` will build the project.
 * `mvn exec:java -pl portability-worker -Dexec.args="-cloud LOCAL -environment LOCAL"` will run the command line tool.

## Deploying in production

See (../config/README.md)

## This is not an official Google product
