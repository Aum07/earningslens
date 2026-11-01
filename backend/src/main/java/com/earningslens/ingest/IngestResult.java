package com.earningslens.ingest;

/** What happened to one document during ingestion. */
public record IngestResult(boolean inserted, int chunks, int tokens) {

    static IngestResult skipped() {
        return new IngestResult(false, 0, 0);
    }
}
