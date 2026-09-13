# Étape de build
FROM maven:3.9.9-eclipse-temurin-17 AS builder
WORKDIR /workspace

# Copier le pom.xml pour mettre en cache les dépendances
COPY pom.xml ./pom.xml
RUN mvn dependency:go-offline -B

# Copier le code source
COPY src ./src

# Builder le jar
RUN mvn clean package -DskipTests

# Étape d'exécution
FROM eclipse-temurin:17-jre
WORKDIR /app

RUN apt-get update && apt-get install -y --no-install-recommends curl && rm -rf /var/lib/apt/lists/*

COPY --from=builder /workspace/target/*.jar app.jar

EXPOSE 8082

HEALTHCHECK --interval=30s --timeout=10s --start-period=40s --retries=3 \
  CMD curl -f http://localhost:8082/api/health || exit 1

ENTRYPOINT ["java", "-jar", "app.jar"]