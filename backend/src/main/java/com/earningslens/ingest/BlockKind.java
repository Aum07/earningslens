package com.earningslens.ingest;

/** What a parsed block contains. The chunker treats tables differently from prose. */
public enum BlockKind {
    TEXT,
    TABLE
}
