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
