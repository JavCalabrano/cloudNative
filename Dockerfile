# Etapa 1: Compilación
FROM maven:3.9.6-eclipse-temurin-17 AS build
WORKDIR /app
COPY . .

# Declaramos el argumento en la etapa de construcción
ARG MODULE_NAME
# Compilamos el módulo específico
RUN mvn clean package -DskipTests -pl "$MODULE_NAME" -am

# Etapa 2: Ejecución
FROM eclipse-temurin:17-jre-alpine
WORKDIR /app

# ¡CRUCIAL! Volvemos a declarar el ARG en esta etapa para que lo reconozca
ARG MODULE_NAME
# Ahora sí, la variable tendrá valor y copiará el jar correcto
COPY --from=build /app/${MODULE_NAME}/target/*.jar app.jar

ENTRYPOINT ["java", "-jar", "app.jar"]