FROM gradle:8.6.0-jdk21-alpine AS build

WORKDIR /app

COPY build.gradle settings.gradle ./
RUN gradle dependencies

COPY src/ ./src
RUN gradle bootJar

FROM eclipse-temurin:21-jdk AS final

COPY --from=build /app/build/libs/*.jar /app/app.jar
ENTRYPOINT ["java", "-jar", "/app/app.jar"]
