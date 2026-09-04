FROM gradle:8.14.4-jdk17 AS build
WORKDIR /workspace
COPY . .
ARG MODULE=login-web
RUN gradle --no-daemon :${MODULE}:bootJar && cp ${MODULE}/build/libs/*.jar /app.jar
FROM eclipse-temurin:17-jre
WORKDIR /app
COPY --from=build /app.jar app.jar
USER 10001
ENTRYPOINT ["java", "-jar", "app.jar"]
