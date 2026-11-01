-- Corpus sanity check (roadmap E1.9).
-- Run against the local database after an ingest. In IntelliJ: open this file, pick the
-- "earningslens" data source at the top of the editor, put the cursor in a query and press Ctrl+Enter.
-- Every query only reads; nothing here changes data.

-- 1. Chunks per document, with average and largest chunk size (tokens).
SELECT d.ticker,
       'FY' || d.fiscal_year || ' Q' || d.fiscal_quarter AS period,
       d.doc_type,
       count(c.id)                AS chunks,
       round(avg(c.token_count))  AS avg_tokens,
       max(c.token_count)         AS max_tokens
FROM documents d
JOIN chunks c ON c.document_id = d.id
GROUP BY d.id, d.ticker, d.fiscal_year, d.fiscal_quarter, d.doc_type
ORDER BY d.ticker, d.fiscal_year, d.fiscal_quarter, d.doc_type;

-- 2. Totals: documents, chunks, average tokens, forward-looking chunks.
SELECT (SELECT count(*) FROM documents)                     AS documents,
       count(*)                                             AS chunks,
       round(avg(token_count))                              AS avg_tokens,
       count(*) FILTER (WHERE forward_looking)              AS forward_looking_chunks
FROM chunks;

-- 3. Chunks with neither a section (HTML heading) nor a page (PDF), per document type.
--    Expected: some for the HTML transcripts (no headings), almost none for filings.
SELECT doc_type,
       count(*)                                                        AS chunks,
       count(*) FILTER (WHERE section IS NULL AND page_start IS NULL)  AS no_section_no_page,
       count(*) FILTER (WHERE speaker IS NOT NULL)                     AS with_speaker
FROM chunks
GROUP BY doc_type
ORDER BY doc_type;

-- 4. Five random chunks (spec). Run it a few times; aim to read about 20 chunks in total.
SELECT ticker, fiscal_year, fiscal_quarter, doc_type, section, speaker, page_start, page_end,
       token_count, forward_looking, content
FROM chunks
ORDER BY random()
LIMIT 5;

-- 5. Extra: possible garbage. Very small chunks, ordered by size.
SELECT ticker, fiscal_year, fiscal_quarter, doc_type, section, speaker, token_count, content
FROM chunks
WHERE token_count < 40
ORDER BY token_count, ticker
LIMIT 50;

-- 6. Extra: website or navigation words that should have been cleaned away.
SELECT ticker, fiscal_year, fiscal_quarter, doc_type, left(content, 300) AS start_of_content
FROM chunks
-- \m and \M are Postgres word boundaries, so "subscribers" or "design in" no longer match.
WHERE content ~* '\m(cookies?|javascript|subscribe|skip to main|privacy policy|terms of use|sign in)\M'
LIMIT 50;

-- 7. Extra: the same text stored more than once inside one document.
SELECT d.ticker, d.fiscal_year, d.fiscal_quarter, d.doc_type, count(*) AS copies, left(c.content, 200) AS content
FROM chunks c
JOIN documents d ON d.id = c.document_id
GROUP BY d.id, d.ticker, d.fiscal_year, d.fiscal_quarter, d.doc_type, c.content
HAVING count(*) > 1
ORDER BY copies DESC
LIMIT 30;
