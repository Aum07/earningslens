-- V1: initial schema (spec section 6).
-- Flyway runs this once, records it in flyway_schema_history, and never runs it again.
-- Never edit this file after it has been applied: add V2__..., V3__... for changes.

-- pgvector adds the "vector" column type and similarity operators.
CREATE EXTENSION IF NOT EXISTS vector;

-- One row per source document (press release, transcript, 10-Q or 10-K).
CREATE TABLE documents (
  id              UUID PRIMARY KEY,
  ticker          VARCHAR(10)  NOT NULL,
  company_name    VARCHAR(200) NOT NULL,
  fiscal_year     INT          NOT NULL,
  fiscal_quarter  SMALLINT     NOT NULL CHECK (fiscal_quarter BETWEEN 1 AND 4),
  doc_type        VARCHAR(20)  NOT NULL CHECK (doc_type IN ('PRESS_RELEASE','TRANSCRIPT','FORM_10Q','FORM_10K')),
  source_url      TEXT         NOT NULL,
  content_sha256  CHAR(64)     NOT NULL UNIQUE,   -- hash of the file: re-ingesting the same file is skipped
  ingested_at     TIMESTAMPTZ  NOT NULL DEFAULT now()
);

-- One row per chunk of text, with its embedding and full-text vector.
CREATE TABLE chunks (
  id              UUID PRIMARY KEY,
  document_id     UUID NOT NULL REFERENCES documents(id) ON DELETE CASCADE,
  chunk_index     INT  NOT NULL,
  -- denormalised from documents so filters do not need a join
  ticker          VARCHAR(10) NOT NULL,
  fiscal_year     INT         NOT NULL,
  fiscal_quarter  SMALLINT    NOT NULL,
  doc_type        VARCHAR(20) NOT NULL,
  content         TEXT        NOT NULL,
  token_count     INT         NOT NULL,
  page_start      INT,
  page_end        INT,
  section         VARCHAR(300),
  speaker         VARCHAR(200),          -- transcripts only
  embedding       vector(1536) NOT NULL,  -- text-embedding-3-small output size
  -- Postgres keeps this column up to date automatically from "content"
  tsv             tsvector GENERATED ALWAYS AS (to_tsvector('english', content)) STORED,
  UNIQUE (document_id, chunk_index)
);

-- HNSW: approximate nearest-neighbour index for fast cosine-similarity search on embeddings.
CREATE INDEX chunks_embedding_hnsw ON chunks USING hnsw (embedding vector_cosine_ops);
-- GIN: inverted index for fast full-text (keyword) search on tsv.
CREATE INDEX chunks_tsv_gin        ON chunks USING gin (tsv);
-- B-tree on the metadata columns used in WHERE filters.
CREATE INDEX chunks_filter_idx     ON chunks (ticker, fiscal_year, fiscal_quarter, doc_type);

-- One row per chat request, for observability and cost tracking.
CREATE TABLE query_log (
  id                   UUID PRIMARY KEY,
  question             TEXT NOT NULL,
  applied_filters      JSONB NOT NULL,
  retrieved_chunk_ids  UUID[] NOT NULL,
  answer               TEXT,
  abstained            BOOLEAN NOT NULL,
  latency_ms           INT NOT NULL,
  prompt_tokens        INT,
  completion_tokens    INT,
  created_at           TIMESTAMPTZ NOT NULL DEFAULT now()
);
