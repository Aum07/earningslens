package com.earningslens.ingest;

/**
 * One piece of a parsed document: a paragraph or a whole table.
 *
 * @param section nearest heading above the block (HTML), or null if there is none
 * @param page    page number (PDF only), or null for HTML
 * @param kind    TEXT or TABLE
 * @param text    cleaned text; tables are one row per line with cells joined by " | "
 */
public record ParsedBlock(String section, Integer page, BlockKind kind, String text) {
}
