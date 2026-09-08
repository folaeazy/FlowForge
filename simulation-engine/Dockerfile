# ── Stage 1: Build ────────────────────────────────────────────────────────────
FROM eclipse-temurin:21-jdk-alpine AS builder

WORKDIR /build

COPY .mvn/ .mvn/
COPY mvnw pom.xml ./

RUN chmod +x mvnw

RUN ./mvnw dependency:go-offline -q

COPY src/ src/

RUN ./mvnw clean package -DskipTests -q


# ── Stage 2: Runtime ──────────────────────────────────────────────────────────
FROM eclipse-temurin:21-jre-alpine AS runtime

WORKDIR /app

RUN addgroup -S flowforge && adduser -S flowforge -G flowforge

COPY --from=builder /build/target/simulation-engine-*.jar app.jar

RUN chown flowforge:flowforge app.jar

USER flowforge

ENTRYPOINT ["java", \
    "-XX:+UseZGC", \
    "-XX:MaxRAMPercentage=75.0", \
    "-XX:+ExitOnOutOfMemoryError", \
    "-Dfile.encoding=UTF-8", \
    "-jar", "app.jar"]