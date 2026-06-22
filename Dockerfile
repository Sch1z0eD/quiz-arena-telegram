# syntax=docker/dockerfile:1

# ---- frontend build stage ----
# Builds the admin UI to static assets. VITE_BOT_USERNAME is compiled into the bundle here (vite build runs
# in production mode and ignores .env.development), so it must arrive as a build arg. Same-origin: VITE_API_BASE
# is empty, so the panel calls /api/... on whatever host serves it.
FROM node:22 AS web
WORKDIR /web
COPY admin-ui/package.json admin-ui/package-lock.json ./
RUN npm ci
COPY admin-ui/ ./
ARG VITE_BOT_USERNAME=""
ENV VITE_API_BASE=""
RUN VITE_BOT_USERNAME="$VITE_BOT_USERNAME" npm run build

# ---- backend build stage ----
FROM eclipse-temurin:21-jdk AS build
WORKDIR /app

# Resolve dependencies first so this layer stays cached until the build scripts change.
# (gradlew loses its exec bit when the build context comes from a non-POSIX checkout, so set it here.)
COPY gradlew settings.gradle.kts build.gradle.kts ./
COPY gradle ./gradle
RUN chmod +x gradlew && ./gradlew --no-daemon dependencies

# Then the sources, which change far more often than the dependency set.
COPY src ./src
# Bundle the built admin UI into the jar (classpath:/static). Served only when the panel is enabled;
# in bot-only mode it sits unused.
COPY --from=web /web/dist ./src/main/resources/static
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

# Only listened on when the admin panel is enabled; the bot itself uses long polling (outbound only).
# BOT_TOKEN is supplied at runtime, never baked in.
EXPOSE 8080
ENTRYPOINT ["java", "-Djava.awt.headless=true", "-jar", "/app/app.jar"]
