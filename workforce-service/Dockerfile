FROM eclipse-temurin:17-jdk-jammy AS builder

WORKDIR /app

# 루트 기준으로 모든 모듈 복사
COPY . .

# 빌드 (apigateway만 빌드)
RUN ./gradlew :workforce-service:bootJar

# 실행용 이미지
FROM eclipse-temurin:17-jdk-jammy
WORKDIR /app

# 빌드 결과만 복사
COPY --from=builder /app/workforce-service/build/libs/*.jar app.jar

ENTRYPOINT ["java", "-Dspring.profiles.active=prod", "-jar", "app.jar"]
