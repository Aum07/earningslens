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
