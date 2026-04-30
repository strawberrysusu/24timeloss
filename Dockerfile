FROM node:20-alpine AS frontend-build
WORKDIR /app/frontend

COPY frontend/package.json frontend/package-lock.json ./
RUN npm ci

COPY frontend/ ./
COPY src/main/resources/static/style.css /app/src/main/resources/static/style.css
RUN npm run build

FROM eclipse-temurin:17-jdk AS build
WORKDIR /app

COPY gradle/ gradle/
COPY gradlew build.gradle settings.gradle ./
RUN chmod +x gradlew && ./gradlew dependencies --no-daemon

COPY src/ src/
COPY --from=frontend-build /app/src/main/resources/static/react/ src/main/resources/static/react/
RUN ./gradlew bootJar --no-daemon -x test -PskipFrontendBuild=true

FROM eclipse-temurin:17-jre
WORKDIR /app

# healthcheck용 curl 설치 + 보안: root로 앱을 실행하지 않도록 별도 비-root 사용자 생성
# (root로 실행되는 컨테이너가 컴프로마이즈되면 호스트로의 권한 상승 위험이 커진다)
RUN apt-get update \
    && apt-get install -y --no-install-recommends curl \
    && rm -rf /var/lib/apt/lists/* \
    && groupadd --system app \
    && useradd --system --gid app app

COPY --from=build /app/build/libs/*.jar app.jar
RUN chown app:app app.jar

USER app
EXPOSE 8080

# Spring Actuator의 /actuator/health 응답으로 컨테이너 상태를 판단한다.
# start-period: 부팅에 시간이 걸려도 그 동안의 실패는 unhealthy로 카운트하지 않게 60초 유예.
HEALTHCHECK --interval=30s --timeout=5s --start-period=60s --retries=3 \
    CMD curl -fsS http://localhost:8080/actuator/health || exit 1

ENTRYPOINT ["java", "-jar", "app.jar"]
