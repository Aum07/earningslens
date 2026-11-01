package com.earningslens.ingest;

import com.earningslens.document.DocumentRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.HexFormat;
import java.util.List;

/**
 * Ingests one downloaded document (roadmap E1.8, spec 7.2):
 * hash, skip if already stored, parse, clean, chunk, embed, then store in one transaction.
 *
 * The OpenAI calls happen before the database transaction starts, so a slow network
 * never keeps a transaction open, and a failed embedding leaves nothing half-written.
 */
@Service
public class IngestionService {

    private static final Logger log = LoggerFactory.getLogger(IngestionService.class);

    private final DocumentRepository documents;
    private final HtmlParser htmlParser;
    private final PdfParser pdfParser;
    private final TextCleaner cleaner;
    private final Chunker chunker;
    private final EmbeddingService embeddingService;
    private final DocumentStore store;

    public IngestionService(DocumentRepository documents, HtmlParser htmlParser, PdfParser pdfParser,
                            TextCleaner cleaner, Chunker chunker, EmbeddingService embeddingService,
                            DocumentStore store) {
        this.documents = documents;
        this.htmlParser = htmlParser;
        this.pdfParser = pdfParser;
        this.cleaner = cleaner;
        this.chunker = chunker;
        this.embeddingService = embeddingService;
        this.store = store;
    }

    public IngestResult ingest(ManifestEntry entry, Path file) throws IOException {
        String sha256 = sha256(Files.readAllBytes(file));
        if (documents.existsByContentSha256(sha256)) {
            log.info("Skip {} (already ingested, sha256 {}...)", entry.rawFileName(), sha256.substring(0, 12));
            return IngestResult.skipped();
        }

        DocumentParser parser = entry.format() == DocumentFormat.PDF ? pdfParser : htmlParser;
        List<CleanedBlock> blocks = cleaner.clean(parser.parse(file), entry.docType());
        List<Chunk> chunks = chunker.chunk(entry, blocks);
        if (chunks.isEmpty()) {
            throw new IllegalStateException("No chunks produced for " + entry.rawFileName());
        }

        List<float[]> vectors = embeddingService.embed(chunks.stream().map(Chunk::content).toList());
        store.store(entry, sha256, chunks, vectors);

        int tokens = chunks.stream().mapToInt(Chunk::tokenCount).sum();
        log.info("Inserted {}: 1 document, {} chunks, {} tokens", entry.rawFileName(), chunks.size(), tokens);
        return new IngestResult(true, chunks.size(), tokens);
    }

    static String sha256(byte[] bytes) {
        try {
            return HexFormat.of().formatHex(MessageDigest.getInstance("SHA-256").digest(bytes));
        } catch (NoSuchAlgorithmException e) {
            throw new IllegalStateException("SHA-256 is always available in the JDK", e);
        }
    }
}
