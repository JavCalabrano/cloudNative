# Etapa 1: Compilación (se construye desde la raíz)
FROM maven:3.9.6-eclipse-temurin-17 AS build
WORKDIR /app
# Copiamos todo el proyecto
COPY . .
# Compilamos solo el módulo que necesitamos (usando -pl y -am)
ARG MODULE_NAME
RUN mvn clean package -DskipTests -pl ${MODULE_NAME} -am

# Etapa 2: Ejecución
FROM eclipse-temurin:17-jre-alpine
WORKDIR /app
ARG MODULE_NAME
# Copiamos el jar generado
COPY --from=build /app/${MODULE_NAME}/target/*.jar app.jar
ENTRYPOINT ["java", "-jar", "app.jar"]