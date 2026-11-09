# Etapa 1: Compilación
FROM maven:3.9.6-eclipse-temurin-17 AS build
WORKDIR /app
COPY . .

# Declaramos el argumento
ARG MODULE_NAME
# Usamos comillas para asegurar que el shell no lo interprete mal
RUN mvn clean package -DskipTests -pl "$MODULE_NAME" -am

# Etapa 2: Ejecución
FROM eclipse-temurin:17-jre-alpine
WORKDIR /app

# Volvemos a declarar el ARG para la segunda etapa
ARG MODULE_NAME
# Copiamos el jar generado usando el argumento
COPY --from=build "/app/${MODULE_NAME}/target/*.jar" app.jar

ENTRYPOINT ["java", "-jar", "app.jar"]