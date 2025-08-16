# Dockerfile
FROM amazoncorretto:21-alpine as builder

WORKDIR /app
COPY gradle gradle
COPY build.gradle.kts settings.gradle.kts gradlew ./
COPY src src

RUN chmod +x ./gradlew
RUN ./gradlew build -x test

FROM amazoncorretto:21-alpine

WORKDIR /app
COPY --from=builder /app/build/libs/*.jar app.jar

EXPOSE 8080

ENTRYPOINT ["java", "-jar", "/app/app.jar"]