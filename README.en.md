<p align="center">
  <img src="assets/banner-en.png" alt="QuizArena" width="640">
</p>

<p align="center">
  <a href="LICENSE"><img alt="License" src="https://img.shields.io/badge/License-MIT-green?style=flat-square"></a>
  <img alt="Java" src="https://img.shields.io/badge/Java-21-ED8B00?style=flat-square&logo=openjdk&logoColor=white">
  <img alt="Spring Boot" src="https://img.shields.io/badge/Spring_Boot-3.5-6DB33F?style=flat-square&logo=springboot&logoColor=white">
  <img alt="PostgreSQL" src="https://img.shields.io/badge/PostgreSQL-16-4169E1?style=flat-square&logo=postgresql&logoColor=white">
  <img alt="Redis" src="https://img.shields.io/badge/Redis-DC382D?style=flat-square&logo=redis&logoColor=white">
  <img alt="Gradle" src="https://img.shields.io/badge/Gradle-Kotlin_DSL-02303A?style=flat-square&logo=gradle&logoColor=white">
</p>

<p align="center">
  <a href="README.md">Русский</a> · <b>English</b>
</p>

# QuizArena

A Telegram quiz bot. Play on your own, with a whole chat, or go head-to-head in a one-on-one duel — with ratings, leaderboards, and questions in both Russian and English.

Type `/start` to begin; everything after that runs through inline menu buttons. The bot is written in Java 21 and Spring Boot. Anything that has to be fast and atomic — round timers, double-answer protection, matchmaking — lives in Redis; questions, player answers, and stats are stored in PostgreSQL.

## Features

- Solo quizzes with category and difficulty selection.
- Group games: a join lobby, a per-question timer, and a speed bonus for answering quickly.
- One-on-one duels in three flavors: automatic matchmaking, challenge a friend by link, and challenge someone straight from any chat via inline mode.
- ELO rating and leaderboards — global, per-chat, and weekly.
- A profile with stats derived from the player's actual answers.
- Results, profile, and leaderboards are shown as rendered images rather than walls of text.
- Russian and English UI, switchable from the menu. Questions are served in the player's language, and duels only pair players whose question language matches.

## How it works

A few decisions everything rests on:

- **Virtual threads.** Each Telegram update is handled on its own Java 21 virtual thread, so one slow player doesn't block everyone else.
- **Atomicity via Lua.** Checks like "did this player answer first," "did they answer twice," and "pair two people from the queue" are done with Lua scripts in Redis. Redis is single-threaded and runs a script to completion without interruption, which closes the race windows that two separate commands would leave open.
- **One source of truth.** Stats and ratings are computed from the answers table in Postgres rather than duplicated in counters, so the numbers can't drift apart.
- **Migrations.** The schema is versioned with Flyway and applied automatically on startup.
- **Images.** Result, profile, and leaderboard cards are SVG templates rendered to PNG with Apache Batik; Cyrillic comes from a bundled DejaVu Sans.
- **Layers.** Handlers are thin and only receive updates; all logic sits in services; data access lives in repositories and Redis wrappers.

## Stack

- Java 21, Spring Boot 3.5
- Gradle (Kotlin DSL)
- PostgreSQL 16 + Flyway
- Redis (Lettuce client)
- TelegramBots 10.x, long polling
- Apache Batik — SVG to PNG rendering
- JUnit 5, Mockito, Testcontainers
- Docker / docker-compose

## Running it

You'll need JDK 21, Docker (for Postgres and Redis), and a bot token from [@BotFather](https://t.me/BotFather).

1. Start the database and Redis:

   ```bash
   docker compose up -d
   ```

2. Create a bot with @BotFather and grab the token. If you want inline mode (challenging someone from any chat), enable it with `/setinline` and set a placeholder.

3. Run the app, passing the token through an environment variable.

   Linux / macOS:

   ```bash
   export BOT_TOKEN="your-token-here"
   ./gradlew bootRun
   ```

   Windows (PowerShell):

   ```powershell
   $env:BOT_TOKEN="your-token-here"
   .\gradlew.bat bootRun
   ```

Postgres and Redis connection settings live in `application.yml` and default to what `docker-compose` provides; override them with environment variables if needed.

Russian questions ship in the migrations and load on first startup. English questions can be imported once from [Open Trivia DB](https://opentdb.com/) by running the app with `IMPORT_ENABLED=true`. The import is idempotent — running it again won't create duplicates.

## Layout

- `handler/` — receiving updates: commands, button callbacks, inline queries.
- `service/` — game logic: gameplay, duels, matchmaking, rating, profile, localization.
- `repository/` — Postgres access and Redis wrappers.
- `bot/` — sending messages and talking to the Telegram API.
- Lua scripts — atomic operations in Redis.
- SVG templates — the cards rendered into images.
- `db/migration/` — Flyway migrations.

## Tests

```bash
./gradlew test
```

Unit tests use Mockito; integration tests use Testcontainers, which spin up real Postgres and Redis in Docker — so they need Docker running.

## Roadmap

The core is built and working. Next up:

- Rating-based duel matchmaking (right now the opponent is picked at random within the same language, category, and difficulty).
- Achievements and a daily challenge.
- Stat charts in the profile.
