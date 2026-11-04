package com.earningslens.generation;

import com.earningslens.retrieval.RetrievedChunk;
import org.springframework.core.io.ClassPathResource;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.io.InputStream;
import java.io.UncheckedIOException;
import java.nio.charset.StandardCharsets;
import java.util.List;

/**
 * Builds the system and user messages for answer generation (spec 9.1, roadmap E4.1).
 *
 * User message format:
 * <pre>
 * Sources:
 * [1] (MSFT | FY2026 Q1 | TRANSCRIPT | p.4)
 * chunk text
 *
 * [2] (MSFT | FY2026 Q2 | PRESS_RELEASE | section: Segment Results)
 * chunk text
 *
 * Question: the question
 * </pre>
 *
 * Sources are numbered by their position in the list (1..n), so [n] always points to
 * an existing source. The chunk header lines written by the Chunker ("[MSFT | FY2026 Q2 | TRANSCRIPT]"
 * and, when the section is in the label, "Section: ...") are removed from the body because
 * the label already says the same. The "Speaker: ..." line is kept.
 */
@Component
public class PromptBuilder {

    static final String SYSTEM_PROMPT_PATH = "prompts/answer-system.txt";

    /** Same limit the Chunker uses for the "Section:" header line. */
    static final int MAX_SECTION_LABEL_CHARS = 200;

    private static final String SECTION_LINE_PREFIX = "Section: ";

    private final String systemPrompt;

    public PromptBuilder() {
        this.systemPrompt = loadSystemPrompt();
    }

    public String systemPrompt() {
        return systemPrompt;
    }

    /**
     * @param question the user's question (not blank)
     * @param sources  chunks in citation order (not empty: with no sources the answer service abstains
     *                 without calling the model)
     */
    public Prompt build(String question, List<RetrievedChunk> sources) {
        if (question == null || question.isBlank()) {
            throw new IllegalArgumentException("question must not be blank");
        }
        if (sources == null || sources.isEmpty()) {
            throw new IllegalArgumentException("at least one source is needed to build a prompt");
        }
        StringBuilder user = new StringBuilder("Sources:\n");
        for (int i = 0; i < sources.size(); i++) {
            RetrievedChunk chunk = sources.get(i);
            user.append('[').append(i + 1).append("] (").append(label(chunk)).append(")\n")
                    .append(body(chunk))
                    .append("\n\n");
        }
        user.append("Question: ").append(question.strip());
        return new Prompt(systemPrompt, user.toString(), sources);
    }

    /** "MSFT | FY2026 Q1 | TRANSCRIPT | p.4", "... | section: Segment Results", or the first three parts only. */
    public static String label(RetrievedChunk chunk) {   // public: the eval judge shows sources the same way
        String label = "%s | FY%d Q%d | %s".formatted(
                chunk.ticker(), chunk.fiscalYear(), chunk.fiscalQuarter(), chunk.docType());
        String pages = pages(chunk);
        if (pages != null) {
            return label + " | " + pages;
        }
        String section = sectionLabel(chunk);
        return section == null ? label : label + " | section: " + section;
    }

    /** "p.4" or "p.4-5"; null for HTML documents (no page numbers). */
    static String pages(RetrievedChunk chunk) {
        Integer start = chunk.pageStart();
        if (start == null) {
            return null;
        }
        Integer end = chunk.pageEnd();
        return end == null || end.equals(start) ? "p." + start : "p." + start + "-" + end;
    }

    /** The section on one line, cut to the Chunker's header limit; null if there is none. */
    private static String sectionLabel(RetrievedChunk chunk) {
        if (chunk.section() == null || chunk.section().isBlank()) {
            return null;
        }
        String oneLine = chunk.section().strip().replaceAll("\\s+", " ");
        return oneLine.length() <= MAX_SECTION_LABEL_CHARS ? oneLine : oneLine.substring(0, MAX_SECTION_LABEL_CHARS);
    }

    /** Chunk text without the header lines the label repeats. */
    public static String body(RetrievedChunk chunk) {   // public: the eval judge shows sources the same way
        String text = chunk.content().strip();
        String first = firstLine(text);
        if (first.startsWith("[") && first.endsWith("]")) {
            text = afterFirstLine(text);
        }
        boolean sectionInLabel = pages(chunk) == null && sectionLabel(chunk) != null;
        if (sectionInLabel && firstLine(text).startsWith(SECTION_LINE_PREFIX)) {
            text = afterFirstLine(text);
        }
        return text.strip();
    }

    private static String firstLine(String text) {
        int newline = text.indexOf('\n');
        return newline < 0 ? text : text.substring(0, newline);
    }

    private static String afterFirstLine(String text) {
        int newline = text.indexOf('\n');
        return newline < 0 ? "" : text.substring(newline + 1);
    }

    private static String loadSystemPrompt() {
        try (InputStream in = new ClassPathResource(SYSTEM_PROMPT_PATH).getInputStream()) {
            String text = new String(in.readAllBytes(), StandardCharsets.UTF_8).replace("\r\n", "\n").strip();
            if (text.isEmpty()) {
                throw new IllegalStateException(SYSTEM_PROMPT_PATH + " is empty");
            }
            return text;
        } catch (IOException e) {
            throw new UncheckedIOException("cannot read " + SYSTEM_PROMPT_PATH + " from the classpath", e);
        }
    }
}
