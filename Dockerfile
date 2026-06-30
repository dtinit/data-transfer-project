# Runs the Java/Gradle build and test suite with no local JDK install.
#
# Source is bind-mounted at `docker run` time -- this image is not rebuilt
# per code change. Gradle 6.9.2 (pulled by the wrapper regardless of this
# image's bundled Gradle version) can't parse Java 17 bytecode when
# compiling build.gradle/settings.gradle, so the JDK here is pinned to 11.
#
# The gradle:* image's default GRADLE_USER_HOME is /home/gradle/.gradle --
# mount a named volume there to cache resolved dependencies across runs:
#
#   docker build -t datatransferproject/dev .
#   docker run --rm -v "$PWD":/workspace -v gradle-cache:/home/gradle/.gradle datatransferproject/dev
FROM gradle:8.10.2-jdk11

WORKDIR /workspace

ENTRYPOINT ["./gradlew"]
CMD ["--no-daemon", "check"]
