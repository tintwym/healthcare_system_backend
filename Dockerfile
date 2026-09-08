# syntax=docker/dockerfile:1

FROM maven:3.9.9-eclipse-temurin-21 AS build
WORKDIR /app
COPY pom.xml .
# Cache dependencies before copying sources (faster / more reliable on Render)
RUN mvn -q -B -DskipTests dependency:go-offline || true
COPY src ./src
RUN mvn -q -B -DskipTests clean package \
 && mv target/medicore-backend-*.jar /app/app.jar

FROM eclipse-temurin:21-jre-alpine
WORKDIR /app
RUN addgroup -S medicore && adduser -S medicore -G medicore
COPY --from=build /app/app.jar /app/app.jar
USER medicore
EXPOSE 4110
ENV PORT=4110
ENTRYPOINT ["sh", "-c", "java -XX:MaxRAMPercentage=75.0 -jar /app/app.jar"]
