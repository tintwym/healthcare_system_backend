# syntax=docker/dockerfile:1

FROM maven:3.9.9-eclipse-temurin-21 AS build
WORKDIR /app
COPY pom.xml .
COPY src ./src
RUN mvn -q -DskipTests package

FROM eclipse-temurin:21-jre-alpine
WORKDIR /app
RUN addgroup -S medicore && adduser -S medicore -G medicore
COPY --from=build /app/target/medicore-backend-*.jar /app/app.jar
USER medicore
EXPOSE 4110
ENV PORT=4110
# Render injects PORT; Spring maps server.port from ${PORT}
ENTRYPOINT ["sh", "-c", "java -jar /app/app.jar"]
