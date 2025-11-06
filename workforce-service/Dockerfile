# 1단계: Gradle을 사용하여 애플리케이션 빌드
# Gradle 빌드 도구가 포함된 이미지를 사용합니다.
FROM gradle:8.4-jdk17-alpine AS build

# 컨테이너 내 작업 디렉터리 설정
WORKDIR /home/gradle/src

# 멀티 프로젝트 빌드를 위해 전체 프로젝트 컨텍스트를 복사합니다.
# 이는 루트의 build.gradle, settings.gradle 및 모든 서브 프로젝트를 포함합니다.
COPY --chown=gradle:gradle . .

# 'workforce-service' 하위 프로젝트에 대해서만 bootJar 태스크를 실행합니다.
# '-x test' 플래그는 이미지 빌드 중 테스트를 생략하여 속도를 높입니다.
RUN gradle :workforce-service:bootJar -x test


# 2단계: 실제 실행을 위한 가벼운 최종 이미지 생성
# JRE(Java Runtime Environment)만 포함된 이미지를 사용하여 이미지 크기를 최소화합니다.
FROM openjdk:17-jdk-alpine

# 애플리케이션 작업 디렉터리 설정
WORKDIR /app

# 빌드 단계(build stage)에서 생성된 JAR 파일을 최종 이미지로 복사합니다.
# 'workforce-service' 서브 프로젝트의 빌드 결과물 경로를 지정합니다.
COPY --from=build /home/gradle/src/workforce-service/build/libs/*.jar app.jar

# 컨테이너가 시작될 때 실행될 명령어 설정
# '--spring.profiles.active=prod' 인자를 추가하여 'prod' 프로파일로 애플리케이션을 실행합니다.
ENTRYPOINT ["java", "-jar", "app.jar", "--spring.profiles.active=prod"]