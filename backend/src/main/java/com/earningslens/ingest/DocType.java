package com.earningslens.ingest;

/** Document types. The names match the CHECK constraint on documents.doc_type in V1__init.sql. */
public enum DocType {
    PRESS_RELEASE,
    TRANSCRIPT,
    FORM_10Q,
    FORM_10K
}
