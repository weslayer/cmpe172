# Builds the React SPA, bundles it into the Spring Boot jar, and runs the jar on a JRE as a non-root user.

FROM node:22-alpine AS web
WORKDIR /web
COPY frontend/package.json frontend/package-lock.json ./
RUN npm ci
COPY frontend/ ./
RUN npm run build

FROM maven:3.9-eclipse-temurin-22 AS build
WORKDIR /app
COPY pom.xml .
RUN mvn -B -q dependency:go-offline
COPY src ./src
COPY --from=web /web/dist ./src/main/resources/static
# The tests need Postgres; CI runs them before it builds this image.
RUN mvn -B -q -DskipTests package

FROM eclipse-temurin:22-jre
RUN useradd --system --no-create-home app
USER app
WORKDIR /app
COPY --from=build /app/target/equipment-rental-0.0.1-SNAPSHOT.jar app.jar
EXPOSE 8080
ENTRYPOINT ["java", "-jar", "app.jar"]
