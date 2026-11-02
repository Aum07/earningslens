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

## 2026-11-01: SnakeYAML to read corpus/manifest.yaml
- **Decision:** `ManifestLoader` reads the manifest with SnakeYAML (`org.yaml:snakeyaml`, version managed by Spring Boot) using `SafeConstructor`.
- **Reason:** Spring Boot already ships SnakeYAML to read `application.yaml`, so this adds no new library to the app; declaring it explicitly makes the dependency visible. `SafeConstructor` only builds plain maps, lists and strings, which blocks YAML deserialization attacks.
- **Alternative considered:** Jackson YAML (an extra module, and Boot 4 moved to Jackson 3), or binding the manifest through `@ConfigurationProperties` (mixes data with app config).

## 2026-11-01: Headings in SEC HTML found by bold or underline styling
- **Decision:** `HtmlParser` treats a short block whose text is at least 95% bold or underlined (inline style, `b`/`strong`, or `h1`-`h6`) as a heading. A heading that appears 3 or more times in one document is treated as a running page header and dropped.
- **Reason:** SEC iXBRL filings contain no `h` tags; headings are styled spans. Checked against the real MSFT, GOOGL and META 10-Qs before writing the Java.
- **Alternative considered:** relying on `h1`-`h6` only (finds no headings in SEC filings), or a fixed list of known 10-Q headings (breaks on new companies).

## 2026-11-01: jtokkit for token counting
- **Decision:** count tokens with jtokkit (`cl100k_base`, the encoding of text-embedding-3-small), declared explicitly in the pom at the version Spring AI already uses (1.1.0).
- **Reason:** spec section 4 allows Spring AI `TokenTextSplitter` or jtokkit. The chunker needs exact counts for its own rules (paragraph, then sentence boundaries, speaker turns, overlap of whole sentences), which `TokenTextSplitter` does not offer.
- **Alternative considered:** `TokenTextSplitter`, which splits by token count only and would cut sentences and mix speakers.

## 2026-11-01: Chunks never cross a section, speaker or forward-looking change
- **Decision:** the chunker groups blocks by (section, speaker, forward-looking tag) and packs paragraphs only within a group. Overlap is whole sentences, prose only, never table rows.
- **Reason:** each chunk row stores one `section` and one `speaker`; mixing groups would make those columns wrong and blur citations. Keeping forward-looking text in its own chunks lets eval exclude it cleanly.
- **Alternative considered:** packing across sections for fuller chunks, which gives fewer tiny chunks but wrong metadata.

## 2026-11-01: Speaker labels in transcripts
- **Decision:** a paragraph starts a new speaker turn when it begins with "Name Surname[, Role]:" (2 or 3 capitalised words), is a "Name Surname, Role" line on its own, or starts with "Operator". A short name seen earlier with a role ("Susan Li, CFO") is stored with that role.
- **Reason:** covers all three corpus formats (Alphabet inline, Microsoft capitals, Meta label lines) and rejects false alarms such as "Second:" found in the real transcripts.

## 2026-11-01: Embedding retries in our own code, client retries off
- **Decision:** `EmbeddingService` retries 429, 5xx and network errors with exponential backoff (1s, 2s, 4s, 8s, 16s; max 5 retries) and fails fast on other errors. `spring.ai.openai.max-retries` is set to 0.
- **Reason:** the roadmap asks for our own backoff with a testable "fails twice then succeeds" unit test. With the client's built-in retries (3 by default) left on, one failure could become up to 18 requests.
- **Alternative considered:** relying only on the OpenAI client's built-in retries (simpler, but not visible or testable in our code).

## 2026-11-01: V2 migration adds chunks.forward_looking
- **Decision:** `V2__chunks_forward_looking.sql` adds `forward_looking BOOLEAN NOT NULL DEFAULT false` to `chunks`. V1 stays untouched.
- **Reason:** spec 7.2 step 3 says to keep forward-looking boilerplate but tag it so eval can exclude it later; the V1 schema had no place to store the tag.
- **Alternative considered:** a marker inside `content` (pollutes the embedded text and keyword search), or not storing the tag (loses the option to filter).

## 2026-11-01: Embed before the database transaction
- **Decision:** `IngestionService` hashes, parses, cleans, chunks and embeds first; only then `DocumentStore.store` opens one transaction that inserts the document (JPA) and its chunks (JdbcTemplate batch).
- **Reason:** OpenAI calls can take seconds or fail and retry; holding a database transaction open during them is wasteful and risky. A failed embedding leaves nothing half-written. Duplicate files (same SHA-256) are skipped before any OpenAI call, so a re-run costs nothing.

## 2026-11-01: Section line in every chunk, and E1.9 cleanup rules
- **Decision:** a chunk under a heading gets a "Section: <heading>" line after the "[TICKER | FYyyyy Qn | DOC_TYPE]" header (cut to 200 characters). `TextCleaner` now also removes short repeated tables (page-top tables such as "Table of Contents | Alphabet Inc.") and the cover-page label "(Mark One)".
- **Reason:** the E1.9 sanity check found chunks whose whole body was "None." or "Not applicable." (answers to 10-Q items like "Defaults Upon Senior Securities"). Without the heading these say nothing; with it they answer real questions ("any legal proceedings?") and the embedding knows the topic. It also helps every other chunk: the heading names the topic of the text below it. The same check found Alphabet's per-page header table stored as duplicate chunks.
- **Alternative considered:** deleting tiny chunks (loses real answers) or merging them into the next section (wrong section metadata). Cost of the change: one re-embed of the corpus, about 2 US cents.

## 2026-11-01: Eval dataset format and loader (E2.1)
- **Decision:** `EvalDatasetLoader` reads `eval/dataset.jsonl` one line at a time with Jackson 3 (`tools.jackson`, already in the app through Spring Boot 4, so no new dependency) into a plain Map, then checks it by hand and builds `EvalItem` records. Rules: unknown fields are rejected (catches typos such as `gold_evidnce`); `answerable` must be false only for `unanswerable`; answerable items need at least one quote, unanswerable items none; `multi_quarter` items need evidence from at least 2 periods (period coverage depends on it); quotes are at least 15 characters; question 3 to 500 characters (same as the chat API); ids look like `q001` and are unique. Errors name the file, line, item id and field.
- **`filters` shape:** same as the chat API request in spec 10 (`tickers`, `fiscalPeriods: [{year, quarter}]`, `docTypes`), so an item can be sent to the API unchanged. `null` or missing means no explicit filters.
- **Reason:** clear errors while writing 80 items by hand; letting Jackson bind records directly gives vaguer messages.
- **Alternative considered:** Jackson record binding with `@JsonProperty` (less code, worse error messages), or Jackson 2 (`com.fasterxml`, only present because Spring AI pulls it in).

## 2026-11-01: Dev split has 34 items, 9 of them unanswerable (E2.2)
- **Decision:** 4 extra unanswerable dev items (q031-q034), so dev has 9 unanswerable instead of 5. The test split will have 5 unanswerable instead of 9, so the whole dataset is 84 items with 14 unanswerable (spec 11.1 asks for at least 80 and 15).
- **Reason:** E3.7 tunes the evidence threshold on the dev unanswerables only; with 5 items one odd question can move the threshold. The unanswerable kinds differ: period not in corpus, company not in corpus, topic never discussed, figure not disclosed, period after the corpus, guidance that is never given, monthly figure (only quarterly reported), executive pay (not in these documents).
- **Alternative considered:** keeping the spec proportions exactly (5 dev / 10 test), which gives a less stable threshold.

## 2026-11-01: Gold answers may only state numbers their quotes contain
- **Decision:** `EvalGoldAnswerNumbersTest` fails if a gold answer contains a number that is not in that item's quotes (years, Qn and FYnnnn labels ignored). Runs in CI because it needs no database.
- **Reason:** a wrong number in a gold answer makes the correctness score unfair without anyone noticing. On the first run it found two answers that stated more than their quotes showed (q002 constant-currency growth, q011 period end date); both quotes were extended to the full source sentence.

## 2026-11-01: Test split has 50 items (E2.3)
- **Decision:** q035-q084, 21 single_fact, 13 multi_quarter, 11 qualitative, 5 unanswerable. Whole dataset: 84 items, 32 / 21 / 17 / 14 (spec 11.1 asks for at least 80 with 30 / 20 / 15 / 15). Test items use different facts and quotes from dev, and cover all four document types, including two facts that only appear in 10-K filings.
- **Reason:** keeps the spec proportions within one or two items per category after moving 4 unanswerable items to dev (see the entry above).
- **Rule:** test questions are not run against the system until the final evaluation; nothing is tuned on them.

## 2026-11-02: Testcontainers for database tests (E3.1)
- **Decision:** added three test-scope dependencies, versions managed by Spring Boot 4.1.2 (Testcontainers 2.0.5): `spring-boot-testcontainers`, `testcontainers-junit-jupiter`, `testcontainers-postgresql`. New DB tests start their own `pgvector/pgvector:pg16` container and connect through `@ServiceConnection`. Approved by Aum before adding (listed in spec section 4).
- **Reason:** retrieval tests need exact, hand-made rows. A throwaway container gives that without touching the ingested corpus in the local database, and it runs the same way in CI.
- **Alternative considered:** the local Docker database inside a rolled-back transaction (no new dependency, but the test data would mix with the real corpus and one mistake could delete it).
- **Note:** older DB tests (SchemaMigrationTest, EvalDatasetQuoteCheckTest) still use the local database; that is fine and unchanged.

## 2026-11-02: Vector search query design (E3.1)
- **Decision:** `VectorSearchRepository.search(queryEmbedding, filters, limit)` builds `WHERE ticker IN (?..) AND (fiscal_year, fiscal_quarter) IN ((?, ?)..) AND doc_type IN (?..)` from fixed SQL pieces, leaving out empty filters; all values are bound parameters. It orders by the `distance` alias (`embedding <=> CAST(? AS vector)`), so the query vector is sent once. Ranks are 1..n after filtering. Before the query it runs `SET LOCAL hnsw.iterative_scan = strict_order` inside a read-only transaction.
- **Reason for iterative scan:** without it the HNSW index returns about 40 nearest rows (`hnsw.ef_search`) and the filter is applied afterwards, so a narrow filter (one ticker and one quarter is about 1/36 of the corpus) could return almost nothing. Iterative scan (pgvector 0.8+) keeps searching until the limit is filled; `strict_order` keeps exact distance order. A test checks the pgvector version is at least 0.8.
- **Alternative considered:** one static query with `(? IS NULL OR ticker = ANY(?))` conditions (simpler code, but worse plans once Postgres switches to a generic plan), or always exact search without the index (correct, but ignores the HNSW index the spec asks for).
- **Reuse:** `FiscalPeriod` (eval package) and `DocumentStore.toVectorLiteral` (now public) are reused instead of duplicated.

## 2026-11-02: Keyword search matches ANY word, not ALL words (E3.2)
- **Decision:** `KeywordSearchRepository` still builds the query with `websearch_to_tsquery('english', ?)` (spec section 8), but rewrites its top-level ` & ` to ` | `, so a chunk needs at least one word of the question. `ts_rank_cd` then ranks chunks with more of the words, closer together, higher. Quoted phrases stay phrases (they use `<->`). A minus at the start of a word is removed first, because websearch syntax reads it as NOT and NOT combined with OR matches almost everything. Ties are broken by chunk id so results are repeatable.
- **Reason:** with plain AND a whole question must have every word in one chunk. Checked on Postgres 16: "What was Azure revenue growth in Q1 FY2027?" gives `'azur' & 'revenu' & 'growth' & 'q1' & 'fy2027'` and matches 0 of the 5 test chunks; with OR the Azure chunk ranks first. Users type questions, not keyword lists, so AND would leave keyword search empty most of the time and hybrid search would quietly become vector-only.
- **Known limit:** `ts_rank_cd` has no IDF, so a common word repeated in a chunk ("cloud" twice) can outrank a rare exact term (a product name) when both are in the question. Vector search and RRF balance this; E3.6 will show whether it matters on dev.
- **Alternative considered:** plain AND as the spec line reads literally (precise, but almost always empty for real questions), or `plainto_tsquery` (also AND, and loses phrase support).

## 2026-11-02: One shared Testcontainers database for all DB tests (E3.2)
- **Decision:** `PgvectorContainerTest` (test base class) starts one pgvector container in a static block and exposes it with `@ServiceConnection`. Vector and keyword search tests extend it and share one container and one Spring context. It also holds the fixture helpers (`clearDatabase` with the safety guard, `insertChunk`, `vector`). `testcontainers-junit-jupiter` was removed again: this pattern does not use `@Testcontainers`/`@Container`.
- **Reason:** with `@Container` per test class, each class would start its own container (slower), and Spring's cached context could point at a container that was already stopped. SQL pieces shared by both repositories moved to `ChunkSql` (columns, filter conditions, row mapping).

## 2026-11-02: RRF fusion as a static pure function (E3.3)
- **Decision:** `RrfFusion.fuse(vectorHits, keywordHits, rrfK, limit)` is a static method with no Spring or database. Score = sum of 1 / (rrfK + rank) over the lists a chunk is in (rrfK = 60 from `earningslens.retrieval.rrf-k`). Chunks are matched by chunk id; if one list repeats a chunk, only its best rank counts. Equal scores are ordered by the chunk's best single rank, then by chunk id, so the output never changes for the same input. `FusedHit` keeps the vector rank, keyword rank and vector similarity, so the evidence threshold (E3.7) and the eval reports can use them.
- **Reason:** the roadmap asks for a pure function; a static method is the simplest form and is tested without a database, so it runs in CI. Raw scores are not mixed because cosine distance and ts_rank_cd are on different scales.

## 2026-11-02: Query parser and period resolver conventions (E3.4)
- **Split:** `QueryParser` (pure, no database) finds tickers through `CompanyDirectory` (companies.yaml aliases, whole-word, case-insensitive) and period references. `PeriodResolver` turns them into `PeriodTarget`s (company + quarter) using `PeriodCatalog` (`JdbcPeriodCatalog`: distinct periods in `documents`). Tests use the real companies.yaml and an in-memory copy of the corpus periods.
- **Per company targets, not one period set:** the same words mean different quarters per company ("latest quarter" is MSFT FY2027 Q1 but GOOGL FY2026 Q3; September 2026 is MSFT FY2027 Q1 but GOOGL FY2026 Q3). A flat ticker set times a period set would wrongly add pairs such as MSFT FY2026 Q3 to "Microsoft and Alphabet, latest quarter". E3.5 should therefore count targets (pairs), not periods, for the "2 or more periods" rule.
- **Fiscal labels vs calendar dates:** "Qn YYYY", "Qn FYYYYY", "2Q26", "nth quarter of (fiscal) YYYY", "first half of 2026" are the company's own fiscal labels (what the filings and calls use). Month names ("December 2025", "quarter ended September 30, 2026") and "calendar Qn YYYY" / "CQn YYYY" are calendar dates, converted per company with `fiscal-year-end-month`.
- **Bare years are ignored:** "2026 capex outlook", "full-year 2026 guidance", "CapEx for 2027" and "FY2026 revenue" are usually the topic (guidance for a year), not the quarter to search. Treating them as periods would filter out the call where the guidance was given.
- **Explicit vs derived:** explicitly named quarters for a named company are kept even if not in the corpus and reported in `missing` (dev q026, q030), so E3.5 can abstain instead of answering from another quarter. Derived periods (latest, last N, a quarter without a year = its latest occurrence, each quarter of a year, previous quarter, year ago, quarters after "since") only come from the corpus. With no company named, only pairs in the corpus are kept; if none exist, all explicit pairs are reported missing.
- **Ranges:** "from X to Y", "X through Y", "between X and Y", "Q2 to Q4 of fiscal 2026", "first three quarters of 2026", "since X" expand to every quarter in between. "X and Y" / "X vs Y" without "between" is two quarters, not a range. Ranges are capped at 12 quarters.
- **Pattern order:** most specific first; each match is blanked out of the working text so looser patterns cannot match the same words again.
- **Tested:** 53 cases in `src/test/resources/retrieval/query-cases.txt` (dev questions marked; no test-split questions), plus calendar conversion, alias boundaries and year spellings.
- **Known limits:** a quarter without a year always means its latest occurrence, so "in Q3, what did Meta say about Q4 guidance?" also targets FY2025 Q4. Unknown companies (Amazon, Nvidia) are not detected; the evidence threshold (E3.7) has to catch those. Doc types are not parsed from the question ("on the call").
