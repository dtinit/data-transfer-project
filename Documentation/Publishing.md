# Publishing

Artifacts are signed and published to Maven Central. You can see the latest version at the maven central website https://search.maven.org/search?q=g:org.datatransferproject.

The publication process is mostly automated through Gradle plugins as documented below.

## 0. Versioning

We don't have strict rules on versioning just yet, but we are trying to stick 
to [Semantic Versioning](https://semver.org/). Until the moment we have
a clear guidance on versioning, please discuss future releases in the 
#maintainers chat.

## 1. Setting properties
 First you must set the necessary properties in [gradle.properties](../gradle.properties). These are:
 - `projectVersion` - this is the new version you wish to publish.
 - `ossrhUsername` & `ossrhPassword` - These are your Sonatype Jira credentials. Your account must have been granted publishing permissions. These are managed from a [JIRA ticket](https://issues.sonatype.org/browse/OSSRH-44189).
 - `signing.keyId` - The GPG key being used for signing the artifacts. (More information about setting up GPG keys can be found [here](https://central.sonatype.org/publish/requirements/gpg/))
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

 
## 5. Create github release and tag

1. Go to [/releases](https://github.com/google/data-transfer-project/releases);
2. Click 'Draft a new release';
3. Fill in the title and create a tag (the tag must have a `v` prefix, the title mustn't);
4. Click 'Generate release notes';
5. Publish release.

## 6. Update the project version
Go to `properties.gradle` and update the version, create a PR.
