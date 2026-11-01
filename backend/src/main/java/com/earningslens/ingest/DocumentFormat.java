package com.earningslens.ingest;

/** File format of a source document. Decides which parser reads it (Jsoup or PDFBox). */
public enum DocumentFormat {
    HTML("html"),
    PDF("pdf");

    private final String extension;

    DocumentFormat(String extension) {
        this.extension = extension;
    }

    public String extension() {
        return extension;
    }
}
