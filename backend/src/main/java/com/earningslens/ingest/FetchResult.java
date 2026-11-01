package com.earningslens.ingest;

/** What {@link DocumentFetcher#fetch} did with one manifest entry. */
public enum FetchResult {
    DOWNLOADED,
    SKIPPED
}
