FROM gcr.io/google-appengine/openjdk:8
COPY portability-api/target/portability-api-1.0-SNAPSHOT.jar /api.jar
EXPOSE 5005/tcp
COPY config/gcp/service_acct_creds.json /service_acct_creds.json
ENV GOOGLE_PROJECT_ID=world-takeout-qa
ENV GOOGLE_APPLICATION_CREDENTIALS=/service_acct_creds.json
EXPOSE 5005/tcp
ENTRYPOINT ["java", "-Xdebug", "-Xrunjdwp:transport=dt_socket,server=y,suspend=n,address=5005", "-jar", "/api.jar"]
EXPOSE 8080/tcp
LABEL git_commit=d3eeffb
