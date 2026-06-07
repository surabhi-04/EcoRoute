# Stage 1: Distributed Build Container Environment
FROM maven:3.8.5-openjdk-17 AS build-engine
WORKDIR /app
COPY pom.xml .
COPY src ./src
RUN mvn clean package -DskipTests

# Stage 2: Hardened Runtime Environment
FROM openjdk:17-slim
WORKDIR /runtime
COPY --from=build-engine /app/target/ecoroute-0.0.1-SNAPSHOT.jar ecoroute.jar
EXPOSE 8080
ENTRYPOINT ["java", "-jar", "ecoroute.jar"]
