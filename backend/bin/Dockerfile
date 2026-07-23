# syntax=docker/dockerfile:1

FROM maven:3.9.9-eclipse-temurin-25 AS builder
WORKDIR /workspace

COPY backend/pom.xml ./backend/pom.xml
RUN mvn -f backend/pom.xml dependency:go-offline

COPY backend/src ./backend/src
COPY backend/src/main/resources/application.yml ./backend/src/main/resources/application.yml
COPY backend/src/main/resources/db/migration ./backend/src/main/resources/db/migration

RUN mvn -f backend/pom.xml clean package -DskipTests

FROM eclipse-temurin:25-jre-jammy
WORKDIR /app

RUN apt-get update && apt-get install -y --no-install-recommends curl && rm -rf /var/lib/apt/lists/*

COPY --from=builder /workspace/backend/target/*.jar /app/app.jar

EXPOSE 8080

HEALTHCHECK --interval=30s --timeout=10s --start-period=40s --retries=3 \
  CMD curl -f http://localhost:8080/api/v1/health || exit 1

ENTRYPOINT ["java", "-jar", "/app/app.jar"]
