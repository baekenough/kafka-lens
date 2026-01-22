# Kafka Lens Backend - Dockerfile
# Multi-stage build for optimized image size

# Build stage
FROM eclipse-temurin:21-jdk-alpine AS builder

WORKDIR /app

# Copy Maven wrapper and pom.xml first for dependency caching
COPY kafka-lens-backend/mvnw .
COPY kafka-lens-backend/.mvn .mvn
COPY kafka-lens-backend/pom.xml .

# Download dependencies (cached layer)
RUN chmod +x mvnw && ./mvnw dependency:go-offline -B

# Copy source and build
COPY kafka-lens-backend/src src
RUN ./mvnw package -DskipTests -B

# Runtime stage
FROM eclipse-temurin:21-jre-alpine

WORKDIR /app

# Create non-root user for security
RUN addgroup -S kafkalens && adduser -S kafkalens -G kafkalens

# Copy JAR from builder
COPY --from=builder /app/target/*.jar app.jar

# Create config directory
RUN mkdir -p /app/config && chown -R kafkalens:kafkalens /app

# Switch to non-root user
USER kafkalens

# Environment variables
ENV JAVA_OPTS="-Xms256m -Xmx512m" \
    ADMIN_PASSWORD="" \
    KAFKA_USER="" \
    KAFKA_PASSWORD="" \
    CLUSTERS_CONFIG_PATH="/app/config/clusters.yml"

# Health check
HEALTHCHECK --interval=30s --timeout=10s --start-period=60s --retries=3 \
    CMD wget --no-verbose --tries=1 --spider http://localhost:8080/actuator/health || exit 1

# Expose port
EXPOSE 8080

# Run application
ENTRYPOINT ["sh", "-c", "java $JAVA_OPTS -jar app.jar"]
