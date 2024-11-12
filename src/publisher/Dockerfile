FROM --platform=linux/amd64 eclipse-temurin:11-jre
WORKDIR /app
COPY target/dapr-pulsar-publisher-1.0-SNAPSHOT.jar app.jar
ENTRYPOINT ["java", "-jar", "app.jar"]