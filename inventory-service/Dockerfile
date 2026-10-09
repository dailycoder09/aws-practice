# Multi-stage Docker build for Inventory Service
# Stage 1: Build stage
FROM docker.io/library/maven:3.9.6-eclipse-temurin-17 AS build

# Set working directory
WORKDIR /app

# Behind a company proxy (HTTPS_PROXY set) Maven needs a proxy setting, because it ignores that variable.
# Each Maven step below writes the setting fresh, or removes it when no proxy is set, so a cached layer
# can never carry a stale proxy into a build made on a different network (CI and AWS have no proxy).

# Copy pom.xml and download dependencies (cached layer)
COPY pom.xml .
RUN if [ -n "$HTTPS_PROXY" ]; then \
      hp="${HTTPS_PROXY#*://}"; hp="${hp%%/*}"; host="${hp%%:*}"; port="${hp##*:}"; \
      mkdir -p /root/.m2; \
      printf '<settings><proxies><proxy><id>https</id><active>true</active><protocol>https</protocol><host>%s</host><port>%s</port></proxy><proxy><id>http</id><active>true</active><protocol>http</protocol><host>%s</host><port>%s</port></proxy></proxies></settings>' "$host" "$port" "$host" "$port" > /root/.m2/settings.xml; \
    else \
      rm -f /root/.m2/settings.xml; \
    fi \
    && mvn dependency:go-offline -B

# Copy source code
COPY src ./src

RUN mkdir -p /app/logs
# Build the application (skip tests for faster builds)
RUN if [ -n "$HTTPS_PROXY" ]; then \
      hp="${HTTPS_PROXY#*://}"; hp="${hp%%/*}"; host="${hp%%:*}"; port="${hp##*:}"; \
      mkdir -p /root/.m2; \
      printf '<settings><proxies><proxy><id>https</id><active>true</active><protocol>https</protocol><host>%s</host><port>%s</port></proxy><proxy><id>http</id><active>true</active><protocol>http</protocol><host>%s</host><port>%s</port></proxy></proxies></settings>' "$host" "$port" "$host" "$port" > /root/.m2/settings.xml; \
    else \
      rm -f /root/.m2/settings.xml; \
    fi \
    && mvn clean package -DskipTests

# Stage 2: Runtime stage
FROM docker.io/library/eclipse-temurin:17-jre-alpine

# Set working directory
WORKDIR /app

# Create non-root user for security
RUN addgroup -S spring && adduser -S spring -G spring

# Copy JAR from build stage
COPY --from=build /app/target/*.jar app.jar

# Give spring user ownership of everything under /app
RUN chown -R spring:spring /app

# Switch to non-root user
USER spring:spring

# Expose port
EXPOSE 8082

# Health check
HEALTHCHECK --interval=30s --timeout=3s --start-period=40s --retries=3 \
    CMD wget --no-verbose --tries=1 --spider http://localhost:8082/actuator/health || exit 1

# Set JVM options for containerized environment
ENV JAVA_OPTS="-Xms256m -Xmx512m -XX:+UseG1GC -XX:MaxGCPauseMillis=200 -XX:+UseStringDeduplication"

# Run the application
ENTRYPOINT ["sh", "-c", "java $JAVA_OPTS -jar app.jar"]
