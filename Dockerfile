# Build stage
FROM gradle:8.6.0-jdk17 AS build
WORKDIR /app

# Copy gradle files for caching
COPY build.gradle settings.gradle ./
COPY gradle ./gradle
COPY gradlew ./
RUN ./gradlew dependencies --no-daemon

# Copy source and build
COPY src ./src
RUN ./gradlew bootJar --no-daemon -DskipTests

# Run stage
FROM eclipse-temurin:17-jre-jammy

# Create a non-root user
RUN addgroup --system appgroup && adduser --system appuser --ingroup appgroup

WORKDIR /app

# Copy the built jar
COPY --from=build /app/build/libs/*.jar app.jar

# Set ownership to the non-root user
RUN chown appuser:appgroup /app/app.jar

# Switch to the non-root user
USER appuser

EXPOSE 8080

ENV SPRING_PROFILES_ACTIVE=prod

ENTRYPOINT ["java", "-Djava.security.egd=file:/dev/./urandom", "-jar", "app.jar"]
