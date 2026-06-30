# ============================================================
# Etapa 1: compilar el proyecto con Maven y Java 17
# ============================================================
FROM maven:3.9.9-eclipse-temurin-17 AS build

WORKDIR /app

COPY pom.xml .
COPY src ./src

RUN mvn clean package -DskipTests


# ============================================================
# Etapa 2: ejecutar la aplicación (JRE liviano)
# ============================================================
FROM eclipse-temurin:17-jre

WORKDIR /app

COPY --from=build /app/target/*.jar app.jar

# El puerto real lo inyecta el entorno con ${PORT}; este EXPOSE es informativo.
EXPOSE 8084

ENTRYPOINT ["java", "-jar", "app.jar"]
