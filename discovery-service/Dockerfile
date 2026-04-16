# ============================================
# Stage 1 : Build avec Maven
# ============================================
FROM maven:3.9-eclipse-temurin-17 AS build
WORKDIR /build

# Copier le pom parent et les poms des modules (pour le cache Maven)
COPY pom.xml .
COPY discovery-service/pom.xml ./discovery-service/
COPY config-service/pom.xml ./config-service/
COPY gateway-service/pom.xml ./gateway-service/
COPY user-service/pom.xml ./user-service/
COPY notification-service/pom.xml ./notification-service/

# Copier le code source du module
COPY discovery-service/src ./discovery-service/src

# Builder uniquement ce module et ses dependances
RUN mvn clean package -pl discovery-service -am -Dmaven.test.skip=true

# ============================================
# Stage 2 : Runtime JRE Alpine (image legere)
# ============================================
FROM eclipse-temurin:17-jre-alpine
WORKDIR /app
COPY --from=build /build/discovery-service/target/*.jar app.jar
EXPOSE 8761
ENTRYPOINT ["java", "-jar", "app.jar"]
