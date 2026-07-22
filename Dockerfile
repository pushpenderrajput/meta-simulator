# Step 1: Build the JAR with Maven & Java 21
FROM maven:3.9.6-eclipse-temurin-21 AS build
WORKDIR /app
COPY pom.xml .
COPY src ./src
RUN mvn clean package -DskipTests

# Step 2: Run on lightweight Java 21 JRE
FROM eclipse-temurin:21-jre-alpine
WORKDIR /app
COPY --from=build /app/target/*.jar app.jar

EXPOSE 8081
ENV PORT=8081

# Optimized JVM memory & GC settings for High TPS on Oracle Cloud (4 vCPU / 24GB RAM)
ENTRYPOINT ["java", \
            "-server", \
            "-Xms4g", \
            "-Xmx12g", \
            "-XX:+UseG1GC", \
            "-XX:MaxGCPauseMillis=20", \
            "-XX:+UseStringDeduplication", \
            "-jar", "app.jar"]