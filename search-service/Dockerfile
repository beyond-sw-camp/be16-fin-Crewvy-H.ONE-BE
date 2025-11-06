# 1단계: Gradle을 사용하여 애플리케이션 빌드
FROM gradle:8.4-jdk17-alpine AS build

WORKDIR /home/gradle/src

# 멀티 프로젝트 빌드를 위해 전체 프로젝트 소스를 복사
COPY --chown=gradle:gradle . .

# 'search-service' 하위 프로젝트에 대해서만 bootJar 태스크를 실행
RUN gradle :search-service:bootJar -x test


# 2단계: 실제 실행을 위한 가벼운 최종 이미지 생성
FROM openjdk:17-jdk-alpine

WORKDIR /app

# 빌드 단계에서 생성된 JAR 파일을 최종 이미지로 복사
COPY --from=build /home/gradle/src/search-service/build/libs/*.jar app.jar

# 'prod' 프로파일로 애플리케이션을 실행
ENTRYPOINT ["java", "-jar", "app.jar", "--spring.profiles.active=prod"]
