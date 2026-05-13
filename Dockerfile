# --- Build stage ---
FROM eclipse-temurin:17-jdk-alpine AS build
WORKDIR /app

# Cache dependencies
COPY pom.xml mvnw* ./
# Bring in Maven wrapper if present; otherwise fall back to Maven from the image
RUN if [ ! -x ./mvnw ]; then \
        apk add --no-cache maven; \
    fi

COPY src ./src

# Use the wrapper if available, otherwise the apk-installed maven
RUN if [ -x ./mvnw ]; then \
        ./mvnw -q -DskipTests package; \
    else \
        mvn -q -DskipTests package; \
    fi

# --- Runtime stage ---
FROM eclipse-temurin:17-jre-alpine
WORKDIR /app

# Non-root user
RUN addgroup -S app && adduser -S app -G app
COPY --from=build /app/target/*.jar /app/app.jar
RUN chown -R app:app /app
USER app

# Render / Railway inject PORT; Spring Boot binds via server.port=${PORT:8080}
EXPOSE 8080
ENV JAVA_OPTS=""

ENTRYPOINT ["sh", "-c", "java $JAVA_OPTS -jar /app/app.jar"]
