FROM maven:3.9.6-eclipse-temurin-21-alpine AS build
WORKDIR /app
# Maven necesita el pom.xml de TODOS los módulos que declara el pom padre, aunque solo se compile uno
COPY pom.xml .
COPY ms-config/pom.xml ms-config/
COPY ms-eureka/pom.xml ms-eureka/
COPY ms-gateway/pom.xml ms-gateway/
COPY ms-auth/pom.xml ms-auth/
COPY ms-productos/pom.xml ms-productos/
COPY ms-lotes/pom.xml ms-lotes/
COPY ms-cotizaciones/pom.xml ms-cotizaciones/
COPY ms-alertas/pom.xml ms-alertas/
COPY ms-reportes/pom.xml ms-reportes/
COPY ms-auth/src ms-auth/src/
# La caché de ~/.m2 se comparte entre servicios: las librerías se descargan una sola vez
RUN --mount=type=cache,target=/root/.m2,sharing=locked mvn -B -ntp -pl ms-auth -am clean package -DskipTests

FROM eclipse-temurin:21-jre-alpine
WORKDIR /app
COPY --from=build /app/ms-auth/target/*.jar app.jar
EXPOSE 8081
ENTRYPOINT ["java", "-jar", "app.jar"]
