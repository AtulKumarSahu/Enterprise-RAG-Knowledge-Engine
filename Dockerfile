# Multi-stage Dockerfile for Enterprise Knowledge Assistant with RAG

# =====================================================================
# Stage 1: Build stage
# =====================================================================
FROM eclipse-temurin:21-jdk-jammy AS builder

WORKDIR /build

# Copy Maven wrapper and POM for dependency layer caching
COPY .mvn/ .mvn/
COPY mvnw pom.xml ./
RUN chmod +x mvnw
RUN ./mvnw dependency:go-offline -B

# Copy application source code
COPY src ./src

# Compile and package application jar
RUN ./mvnw clean package -DskipTests -B

# =====================================================================
# Stage 2: Production Runtime Stage
# =====================================================================
FROM eclipse-temurin:21-jre-jammy

LABEL maintainer="Enterprise AI Backend Team"
LABEL description="Enterprise Knowledge Assistant with RAG, Vector Store, and Role-Based Access Control"

# Run as non-root user
RUN useradd -m -u 1001 appuser
USER appuser

WORKDIR /app

# Copy executable jar from builder stage
COPY --from=builder /build/target/enterprise-knowledge-ai-*.jar app.jar

EXPOSE 8090

# Production JVM flags with container awareness
ENV JAVA_OPTS="-XX:+UseContainerSupport -XX:MaxRAMPercentage=75.0 -XX:+UseG1GC -Djava.security.egd=file:/dev/./urandom"

HEALTHCHECK --interval=30s --timeout=5s --start-period=40s --retries=3 \
  CMD curl -f http://localhost:8090/actuator/health || exit 1

ENTRYPOINT ["sh", "-c", "java $JAVA_OPTS -jar app.jar"]

