# syntax=docker/dockerfile:1

# ---- build stage ----
FROM eclipse-temurin:21-jdk AS build
WORKDIR /app

# Resolve dependencies first so this layer stays cached until the build scripts change.
# (gradlew loses its exec bit when the build context comes from a non-POSIX checkout, so set it here.)
COPY gradlew settings.gradle.kts build.gradle.kts ./
COPY gradle ./gradle
RUN chmod +x gradlew && ./gradlew --no-daemon dependencies

# Then the sources, which change far more often than the dependency set.
COPY src ./src
RUN ./gradlew --no-daemon clean bootJar

# ---- runtime stage ----
FROM eclipse-temurin:21-jre AS runtime

# Cards are rendered with Batik through AWT/Java2D. Even headless this needs a native font stack;
# without fontconfig (which pulls in freetype) the app boots but card rendering fails at runtime.
RUN apt-get update \
    && apt-get install --no-install-recommends -y fontconfig \
    && rm -rf /var/lib/apt/lists/*

# Drop root.
RUN useradd --system --create-home --home-dir /app --shell /usr/sbin/nologin app
WORKDIR /app
COPY --from=build --chown=app:app /app/build/libs/*.jar /app/app.jar
USER app

# No EXPOSE: the bot uses long polling (outbound only). BOT_TOKEN is supplied at runtime, never baked in.
ENTRYPOINT ["java", "-Djava.awt.headless=true", "-jar", "/app/app.jar"]
