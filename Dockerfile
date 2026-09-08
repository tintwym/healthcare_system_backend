# syntax=docker/dockerfile:1
# Required on Render: Java/JVM is not a native runtime (only Docker).

FROM maven:3.9.9-eclipse-temurin-21 AS build
WORKDIR /app
ENV MAVEN_OPTS="-Xmx768m -XX:+UseSerialGC"
COPY pom.xml .
COPY src ./src
RUN mvn -B -DskipTests package \
 && cp target/medicore-backend-*.jar /app/app.jar

FROM eclipse-temurin:21-jre-alpine
WORKDIR /app
RUN addgroup -S medicore && adduser -S medicore -G medicore
COPY --from=build /app/app.jar /app/app.jar
USER medicore
EXPOSE 4110
ENV PORT=4110
ENTRYPOINT ["sh", "-c", "java -XX:MaxRAMPercentage=75.0 -jar /app/app.jar"]
