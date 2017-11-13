FROM gcr.io/google-appengine/openjdk:8
COPY portability-web/target/portability-web-1.0-SNAPSHOT.jar /app.jar
EXPOSE 8080/tcp
ENTRYPOINT ["java", "-jar", "/app.jar", "-cloud", "GOOGLE", "-environment", "TEST"]
