# 1단계: eclipse-temurin과 Gradle Wrapper를 사용하여 애플리케이션 빌드
FROM eclipse-temurin:17-jdk-alpine AS build

# 컨테이너 내 작업 디렉터리 설정
WORKDIR /app

# 프로젝트 전체 소스를 컨테이너의 /app 디렉터리로 복사합니다.
COPY . .

# gradlew 스크립트에 실행 권한을 부여합니다.
RUN chmod +x ./gradlew

# Gradle Wrapper를 사용하여 'search-service' 하위 프로젝트를 빌드합니다.
RUN ./gradlew :search-service:bootJar -x test

# 2단계: 실제 실행을 위한 가벼운 최종 이미지 생성
FROM eclipse-temurin:17-jre-alpine

# 애플리케이션 작업 디렉터리 설정
WORKDIR /app

# 빌드 단계(build stage)에서 생성된 JAR 파일을 최종 이미지로 복사합니다.
COPY --from=build /app/search-service/build/libs/*.jar app.jar

# 'prod' 프로파일로 애플리케이션을 실행합니다.
ENTRYPOINT ["java", "-Duser.timezone=Asia/Seoul", "-jar", "app.jar", "--spring.profiles.active=prod"]