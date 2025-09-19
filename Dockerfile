FROM gradle:8.6.0-jdk21-alpine AS build

WORKDIR /app

COPY build.gradle settings.gradle  ./
RUN gradle dependencies

COPY src/ ./src
RUN gradle bootJar

FROM openjdk:21-jdk-slim AS final

COPY --from=build /app/build/libs/property-api.jar /app/app.jar
ENTRYPOINT ["java", "-jar", "/app/app.jar"]
