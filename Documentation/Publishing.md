# Publishing

Artifacts are signed and published to Maven Central. This is automated through Gradle plugins.

## 1. Setting properties
 First you must set the necessary properties in [gradle.properties](../gradle.properties). These are:
 - `projectVersion` - this is the new version you wish to publish. We use [Semantic Versioning](https://semver.org/).
 - `ossrhUsername` & `ossrhPassword` - These are your Sonatype Jira credentials. Your account must have been granted publishing permissions. These are managed from a [JIRA ticket](https://issues.sonatype.org/browse/OSSRH-44189).
 - `signing.keyId` - The GPG key being used for signing the artifacts.
 - `signing.password` - The password for that GPG private key.
 - `signing.secretKeyRingFile` - The path to the file containing the GPG private key.

## 2. Sanity check
Make sure that the artifacts are building and running correctly. For example run the worker in the Docker container, see [Running Locally](RunningLocally.md) for instructions.

## 3. Sign and upload
To sign and publish the artifacts run the following Gradle command:

```
./gradlew sign uploadArchives --exclude-task :client-rest:uploadArchives
```

We exclude the client-rest archives as these are not a Java package.

## 4. Release
You can then release the deployment to the Central Repository following the steps on the Sonatype website  [here](https://central.sonatype.org/publish/release/). We are on the legacy host so make sure to use https://oss.sonatype.org/. Only attempt to Close the staging repository once the upload has finished.