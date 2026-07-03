# ---- Build stage ----
FROM maven:3.9-eclipse-temurin-21 AS build
WORKDIR /app

# Cache dependencies first (better layer caching on redeploys)
COPY pom.xml .
RUN mvn -B -q dependency:go-offline

# Build the executable jar
COPY src ./src
RUN mvn -B -q clean package -DskipTests

# ---- Runtime stage ----
FROM eclipse-temurin:21-jre
WORKDIR /app

# Copy the Spring Boot fat jar (excludes the *.jar.original)
COPY --from=build /app/target/dmme-backend-*.jar app.jar

# Render injects PORT; the app binds to ${PORT:8080}
ENV JAVA_OPTS=""
EXPOSE 8080

ENTRYPOINT ["sh", "-c", "java $JAVA_OPTS -jar app.jar"]
