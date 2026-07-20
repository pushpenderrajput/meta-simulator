# Step 1: Build the JAR with Maven & Java 21
FROM maven:3.9.6-eclipse-temurin-21 AS build
WORKDIR /app
COPY pom.xml .
COPY src ./src
RUN mvn clean package -DskipTests

# Step 2: Run the lightweight Java 21 image
FROM eclipse-temurin:21-jre-alpine
WORKDIR /app
COPY --from=build /app/target/*.jar app.jar

EXPOSE 8081
ENV PORT=8081

# Optimize JVM memory footprint for 512MB RAM
ENTRYPOINT ["java", "-Xmx384m", "-Xms128m", "-XX:+UseG1GC", "-jar", "app.jar"]