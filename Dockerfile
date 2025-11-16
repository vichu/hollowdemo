# Stage 1: Build the application
FROM gradle:8.5-jdk21 AS build
WORKDIR /app

# Copy gradle files first for better caching
COPY build.gradle settings.gradle ./
COPY gradle ./gradle

# Download dependencies (this layer will be cached)
RUN gradle dependencies --no-daemon || true

# Copy source code
COPY src ./src

# Build the application
RUN gradle bootJar --no-daemon

# Stage 2: Runtime image
FROM eclipse-temurin:21-jre-alpine
WORKDIR /app

# Create directory for hollow data
RUN mkdir -p /app/hollow-data

# Copy the built JAR from build stage
COPY --from=build /app/build/libs/*.jar app.jar

# Set default environment variables
ENV SPRING_PROFILES_ACTIVE=consumer
ENV HOLLOW_DATA_PATH=/app/hollow-data

# Expose default Spring Boot port
EXPOSE 8080

# Run the application
ENTRYPOINT ["java", "-jar", "app.jar"]