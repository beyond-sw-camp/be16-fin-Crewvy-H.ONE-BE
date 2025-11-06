FROM openjdk:17-jdk-alpine AS stage1

WORKDIR /app

COPY gradle gradle
COPY src src
COPY build.gradle .
COPY settings.gradle .
COPY gradlew .
RUN chmod +x gradlew
RUN ./gradlew bootJar

FROM openjdk:17-jdk-alpine
WORKDIR /app
COPY --from=stage1 /app/build/libs/*.jar app.jar

ENTRYPOINT ["java", "-Dspring.profiles.active=prod", "-jar", "app.jar"]
