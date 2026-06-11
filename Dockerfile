# ── Stage 1: Build ────────────────────────────────────────────────────────────
FROM eclipse-temurin:21-jdk-alpine AS builder
WORKDIR /build

# Copy Maven wrapper + pom first — separate Docker layer.
# Docker caches this layer. If only src/ changes (not pom.xml),
# Maven skips dependency downloads entirely on rebuild.
# This turns a 3-minute rebuild into a 20-second one.
COPY .mvn/ .mvn/
COPY mvnw pom.xml ./
RUN chmod +x mvnw

# Download all dependencies into the image layer cache.
# -q = quiet output, go-offline = no network calls during build
RUN ./mvnw dependency:go-offline -q

# Now copy source — this layer only invalidates when code changes
COPY src/ src/

# Build the JAR, skip tests (tests run in CI, not in Docker build)
RUN ./mvnw clean package -DskipTests -q

# ── Stage 2: Runtime ──────────────────────────────────────────────────────────
# JRE only — strips compiler, Maven, debug tools from final image.
# eclipse-temurin:21-jre-alpine ≈ 85MB vs 400MB+ for full JDK image
FROM eclipse-temurin:21-jre-alpine AS runtime
WORKDIR /app

# Create non-root user .
RUN addgroup -S flowforge && adduser -S flowforge -G flowforge

# Copy built JAR from build stage — nothing else comes over
COPY --from=builder /build/target/flowforge-*.jar app.jar

# Set file ownership before switching user
RUN chown flowforge:flowforge app.jar

USER flowforge

# JVM flags:
# -XX:+UseZGC                → low-pause GC, critical for job processing latency
# -XX:MaxRAMPercentage=75.0  → respects Docker memory limit, uses 75% of it
# -XX:+ExitOnOutOfMemoryError→ fail fast on OOM instead of limping along
# -Dfile.encoding=UTF-8      → explicit, avoids platform-dependent surprises
ENTRYPOINT ["java", \
  "-XX:+UseZGC", \
  "-XX:MaxRAMPercentage=75.0", \
  "-XX:+ExitOnOutOfMemoryError", \
  "-Dfile.encoding=UTF-8", \
  "-jar", "app.jar"]

EXPOSE 8080