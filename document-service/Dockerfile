FROM maven:3.9.6-eclipse-temurin-17 AS build
WORKDIR /app
# Para resolver el pom padre, copiamos desde la raíz (el context será la raíz)
COPY pom.xml .
COPY document-service/pom.xml ./document-service/
COPY document-service/src ./document-service/src
# Compilamos el módulo
RUN mvn -f pom.xml clean package -pl document-service -am -DskipTests

FROM eclipse-temurin:17-jre-alpine
WORKDIR /app
COPY --from=build /app/document-service/target/document-service-1.0.0.jar app.jar
EXPOSE 8080
ENTRYPOINT ["java", "-jar", "app.jar"]