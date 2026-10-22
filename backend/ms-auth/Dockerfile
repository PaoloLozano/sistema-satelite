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
# Espera a que ms-config responda antes de arrancar: tras encender la VM Docker inicia todo a la vez
# y, sin configuración, el servicio arrancaría vacío (el gateway sin rutas). CONFIG_HOST lo define el compose.
ENTRYPOINT ["sh", "-c", "if [ -n \"$CONFIG_HOST\" ]; then until wget -q -O /dev/null \"http://$CONFIG_HOST:8888/actuator/health\"; do echo 'Esperando a ms-config...'; sleep 3; done; fi; exec java -jar app.jar"]
