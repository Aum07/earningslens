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
| JaCoCo (`jacoco-maven-plugin`, build plugin only) | 0.8.16 (added in E7.2, latest release on 2026-11-12, released 2026-10-24) |
| Node.js | 24 (dev machine 24.17; CI `actions/setup-node` 24). Angular 22 needs ^22.22.3 or ^24.15.0 |
| Angular framework (`@angular/core`, common, compiler, compiler-cli, forms, platform-browser, router) | 22.2.4 (added in E6.1, npm `latest` on 2026-11-05) |
| Angular CLI and build (`@angular/cli`, `@angular/build`) | 22.2.6 |
| Angular Material and CDK (`@angular/material`, `@angular/cdk`) | 22.2.6 |
| TypeScript | 6.0.3 |
| RxJS | 7.8.2 |
| Vitest (unit test runner for `ng test`) with jsdom | 5.1.0 with jsdom 30.1.3 |
| Prettier (code formatter, CLI default) | 3.9.10 |

Frontend versions are exact in `frontend/package.json` (no `^`), and `package-lock.json` is committed. The framework packages and the CLI/Material packages have separate patch numbers; that is normal for Angular.
