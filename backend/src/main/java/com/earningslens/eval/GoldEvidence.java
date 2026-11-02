package com.earningslens.eval;

import com.earningslens.ingest.DocType;
import com.earningslens.ingest.TextNormalizer;

import java.util.Objects;

/**
 * One piece of gold evidence: an exact quote and the document it comes from.
 * A retrieved chunk counts as a hit if it contains the quote after whitespace
 * normalisation (spec 11.1), so the dataset survives chunking changes.
 */
public record GoldEvidence(String ticker, FiscalPeriod period, DocType docType, String quote) {

    /** Shorter quotes match too many chunks by accident. */
    static final int MIN_QUOTE_LENGTH = 15;

    public GoldEvidence {
        Objects.requireNonNull(ticker, "ticker");
        Objects.requireNonNull(period, "period");
        Objects.requireNonNull(docType, "doc_type");
        Objects.requireNonNull(quote, "quote");
        if (!ticker.matches("[A-Z.]{1,10}")) {
            throw new IllegalArgumentException("ticker must be 1 to 10 upper-case letters, got '" + ticker + "'");
        }
        if (quote.strip().length() < MIN_QUOTE_LENGTH) {
            throw new IllegalArgumentException("quote must be at least " + MIN_QUOTE_LENGTH
                    + " characters, got '" + quote + "'");
        }
    }

    /**
     * The quote as stored chunks are compared against: the same clean-up as ingestion
     * (special spaces and hyphens unified, whitespace collapsed to one space).
     */
    public String normalizedQuote() {
        return TextNormalizer.normalize(quote);
    }
}
