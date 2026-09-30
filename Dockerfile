FROM maven:3.9-eclipse-temurin-21 AS build
WORKDIR /workspace
COPY pom.xml .
RUN mvn -q -Dmaven.test.skip=true dependency:go-offline
COPY src ./src
RUN mvn -q -Dmaven.test.skip=true package

FROM eclipse-temurin:21-jre-alpine
RUN addgroup -S app && adduser -S -G app app && mkdir -p /app/.local /app/logs && chown -R app:app /app
WORKDIR /app
COPY --from=build --chown=app:app /workspace/target/MyEnterpriseOpenSource-0.0.1-SNAPSHOT.jar app.jar
USER app
EXPOSE 8080
ENTRYPOINT ["java", "-jar", "app.jar"]
