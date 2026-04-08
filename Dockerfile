# ── Stage 1: 빌드 ──
# Gradle로 프로젝트를 빌드해서 JAR 파일을 만든다.
FROM eclipse-temurin:17-jdk AS build
WORKDIR /app

# Gradle 래퍼와 빌드 설정을 먼저 복사 (의존성 캐시 활용)
COPY gradle/ gradle/
COPY gradlew build.gradle settings.gradle ./
RUN chmod +x gradlew && ./gradlew dependencies --no-daemon

# 소스 코드를 복사하고 빌드 (테스트는 Docker에서 실행하지 않음)
COPY src/ src/
RUN ./gradlew bootJar --no-daemon -x test

# ── Stage 2: 실행 ──
# 빌드된 JAR만 가져와서 가벼운 이미지로 실행한다.
FROM eclipse-temurin:17-jre
WORKDIR /app
COPY --from=build /app/build/libs/*.jar app.jar
EXPOSE 8080
ENTRYPOINT ["java", "-jar", "app.jar"]
