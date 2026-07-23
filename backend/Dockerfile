FROM maven:3.9.11-eclipse-temurin-17 AS build
WORKDIR /workspace
COPY pom.xml /workspace/pom.xml
COPY backend/pom.xml /workspace/backend/pom.xml
COPY backend/src /workspace/backend/src
RUN mvn --batch-mode -f /workspace/pom.xml -pl backend -am package -DskipTests

FROM eclipse-temurin:17-jre-alpine
RUN addgroup -S app && adduser -S app -G app
WORKDIR /app
COPY --from=build /workspace/backend/target/backend-0.1.0.jar app.jar
USER app
EXPOSE 8080
ENTRYPOINT ["java", "-XX:MaxRAMPercentage=75", "-jar", "/app/app.jar"]

