# ============================================
# Stage 1 : Build avec Maven
# ============================================
FROM maven:3.9-eclipse-temurin-17 AS build
WORKDIR /build

COPY pom.xml .
COPY discovery-service/pom.xml ./discovery-service/
COPY config-service/pom.xml ./config-service/
COPY gateway-service/pom.xml ./gateway-service/
COPY user-service/pom.xml ./user-service/
COPY notification-service/pom.xml ./notification-service/

COPY notification-service/src ./notification-service/src

RUN mvn clean package -pl notification-service -am -Dmaven.test.skip=true

# ============================================
# Stage 2 : Runtime JRE Alpine
# ============================================
FROM eclipse-temurin:17-jre-alpine
WORKDIR /app
COPY --from=build /build/notification-service/target/*.jar app.jar
EXPOSE 8083
ENTRYPOINT ["java", "-jar", "app.jar"]
