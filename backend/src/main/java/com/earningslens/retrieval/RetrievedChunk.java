package com.earningslens.retrieval;

import com.earningslens.ingest.DocType;

import java.util.UUID;

/** One row of the chunks table as returned by search (everything except the embedding and tsv). */
public record RetrievedChunk(
        UUID id,
        UUID documentId,
        int chunkIndex,
        String ticker,
        int fiscalYear,
        int fiscalQuarter,
        DocType docType,
        String content,
        int tokenCount,
        Integer pageStart,   // null for HTML documents
        Integer pageEnd,
        String section,
        String speaker,      // transcripts only
        boolean forwardLooking
) {
}
