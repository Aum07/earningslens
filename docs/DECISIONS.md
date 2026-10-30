# Decisions

Format: date, decision, reason, alternative considered.

## 2026-10-30: Plain JdbcTemplate for vector and keyword search
- **Decision:** do not use the Spring AI PGvector VectorStore. Vector search, full-text search and RRF fusion are written as SQL with `JdbcTemplate`.
- **Reason:** hybrid search needs custom SQL (pgvector cosine, Postgres full-text, metadata filters, per-quarter retrieval), and owning the queries makes the retrieval logic easy to explain and test.
- **Alternative considered:** Spring AI `PgVectorStore`, which handles vector search well but does not cover full-text search or RRF.

## 2026-10-30: Backend generated with Spring Boot 4.1.2 and Spring AI 2.0.1
- **Decision:** latest stable Boot and Spring AI offered together by start.spring.io (through IntelliJ) on 2026-10-30. Configuration in YAML.
- **Reason:** spec section 4 says to use the latest stable pair and pin it.
- **Alternative considered:** Spring Boot 3.5 with Spring AI 1.1, rejected because 3.5 is out of open-source support.

## 2026-10-30: Secrets loaded from a gitignored .env through spring.config.import
- **Decision:** `application.yaml` imports `optional:file:../.env[.properties]` so local runs read OPENAI_API_KEY and DB settings from `.env`. The `prod` profile has no defaults for secrets, so a missing variable stops the app at startup.
- **Reason:** no extra dependency, works the same from IntelliJ and `mvnw`, and Azure supplies real environment variables.
- **Alternative considered:** a dotenv library (not in the locked stack), or setting Windows environment variables by hand (easy to forget, not visible in the repo).

## 2026-10-30: Typed, validated configuration records
- **Decision:** one `@ConfigurationProperties` record per area (LLM, retrieval, rate limit, SEC) with jakarta.validation constraints.
- **Reason:** bad or missing config fails at startup instead of mid-request; model names and tuning values live in config, not code (spec section 4).
- **Alternative considered:** `@Value` on individual fields, which scatters settings and has no validation.
