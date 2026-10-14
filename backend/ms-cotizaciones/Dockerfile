FROM maven:3.9.6-eclipse-temurin-21-alpine AS build
WORKDIR /app
COPY pom.xml .
COPY ms-cotizaciones/pom.xml ms-cotizaciones/
COPY ms-cotizaciones/src ms-cotizaciones/src/
RUN mvn -pl ms-cotizaciones -am clean package -DskipTests

FROM eclipse-temurin:21-jre-alpine
WORKDIR /app
COPY --from=build /app/ms-cotizaciones/target/*.jar app.jar
EXPOSE 8084
ENTRYPOINT ["java", "-jar", "app.jar"]
