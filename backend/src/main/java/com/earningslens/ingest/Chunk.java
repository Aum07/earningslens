package com.earningslens.ingest;

/**
 * One chunk, ready to embed and store in the chunks table.
 *
 * @param content header line, optional "Speaker: ..." line, then the text
 */
public record Chunk(
        int chunkIndex,
        String content,
        int tokenCount,
        Integer pageStart,
        Integer pageEnd,
        String section,
        String speaker,
        boolean forwardLooking
) {
}
