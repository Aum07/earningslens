# Pinned versions

Do not upgrade mid-project. Use only the documentation for these versions.

| Component | Version |
|---|---|
| Java | 21 (Eclipse Temurin 21.0.12) |
| Spring Boot | 4.1.2 |
| Spring AI | 2.0.1 |
| Build | Maven (wrapper `mvnw`) |
| PostgreSQL | 16 (Docker image `pgvector/pgvector:pg16`) |
| SnakeYAML | managed by Spring Boot |
| Jsoup | 1.23.2 |
| Apache PDFBox | 3.0.8 |
| jtokkit | 1.1.0 |
| Bucket4j (`bucket4j_jdk17-core`) | 8.19.0 (added in E4.7, latest release on 2026-11-04) |
| springdoc-openapi (`springdoc-openapi-starter-webmvc-ui`) | 3.2.0 (added in E4.8; released 2026-10-24, built on Spring Boot 4.1.2 and Spring AI 2.0.1) |
| swagger-core / swagger-annotations / swagger-models (jakarta), plus non-jakarta swagger-annotations | 2.2.55, pinned in pom.xml dependencyManagement (Spring AI alone would bring 2.2.38 and 2.2.31, which break springdoc) |
| Testcontainers | 2.0.5 (managed by Spring Boot, test scope only) |
| Angular | to be filled in E6.1 |
