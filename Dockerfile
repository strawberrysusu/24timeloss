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

COPY --from=build /app/build/libs/*.jar app.jar

EXPOSE 8080
ENTRYPOINT ["java", "-jar", "app.jar"]
