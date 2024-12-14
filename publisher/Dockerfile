FROM openjdk:11-jre-slim
WORKDIR /app
COPY target/dapr-pulsar-publisher-1.0-SNAPSHOT.jar app.jar
ENTRYPOINT ["java", "-jar", "app.jar"]