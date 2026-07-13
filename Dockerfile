# Runs the Java/Gradle build and test suite with no local JDK install.
#
# Source is bind-mounted at `docker run` time -- this image is not rebuilt
# per code change. build.gradle uses the legacy `maven` plugin, removed in
# Gradle 7+, so the build only works under the wrapper-pinned Gradle 6.9.2
# (see gradle/wrapper/gradle-wrapper.properties) -- not whatever `gradle`
# version a base image happens to bundle. The JDK here is pinned to 11
# because that 6.9.2/Groovy-DSL combo can't parse newer bytecode when
# compiling build.gradle/settings.gradle.
#
# The wrapper distribution is resolved and cached into an image layer at
# build time (`./gradlew --version` below) rather than left to download on
# first `docker run` -- same version pin, but the image is self-contained
# and works offline. GRADLE_USER_HOME matches the default the gradle:*
# image used, so existing gradle-cache volumes from that image still work.
#
#   docker build -t datatransferproject/dev .
#   docker run --rm -v "$PWD":/workspace -v gradle-cache:/home/gradle/.gradle datatransferproject/dev
FROM eclipse-temurin:11-jdk-jammy

ENV GRADLE_USER_HOME=/home/gradle/.gradle
WORKDIR /workspace

COPY gradlew gradlew
COPY gradle/wrapper gradle/wrapper
RUN ./gradlew --version --no-daemon

ENTRYPOINT ["./gradlew"]
CMD ["--no-daemon", "check"]
