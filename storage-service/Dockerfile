FROM maven:3.9.6-eclipse-temurin-17 AS build
WORKDIR /app
COPY pom.xml .
COPY storage-service/pom.xml ./storage-service/
COPY storage-service/src ./storage-service/src
RUN mvn -f pom.xml clean package -pl storage-service -am -DskipTests

FROM eclipse-temurin:17-jre-alpine
WORKDIR /app
COPY --from=build /app/storage-service/target/storage-service-1.0.0.jar app.jar
EXPOSE 8081
ENTRYPOINT ["java", "-jar", "app.jar"]