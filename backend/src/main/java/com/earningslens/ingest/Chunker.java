package com.earningslens.ingest;

import com.earningslens.config.ChunkingProperties;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.regex.Pattern;

/**
 * Splits cleaned blocks into chunks for embedding (roadmap E1.6, spec 7.2 step 4).
 *
 * 1. Group blocks. A new group starts when the section, the speaker (transcripts) or the
 *    forward-looking tag changes. A chunk never crosses a group, so its section, speaker
 *    and tag are always correct.
 * 2. Inside a group, pack whole paragraphs until the chunk would pass the target size
 *    (600 tokens). A paragraph too big for one chunk is split into sentences; a sentence
 *    too big is split by words (the only case where text is cut mid-sentence). Table rows
 *    are never cut.
 * 3. Each new chunk in a group starts with the last sentences of the previous chunk
 *    (up to 100 tokens of overlap), so a fact near a boundary is found in both.
 * 4. Every chunk starts with a header line like "[MSFT | FY2026 Q2 | TRANSCRIPT]", then a
 *    "Section: ..." line when the text sits under a heading and, in transcripts, a
 *    "Speaker: ..." line. The section line makes short chunks such as "None." meaningful
 *    on their own ("Section: Item 3. Defaults Upon Senior Securities"), for the embedding
 *    and for the LLM. No chunk is ever over the max size (900 tokens).
 */
@Component
public class Chunker {

    private static final Pattern SENTENCE_BREAK = Pattern.compile("(?<=[.!?][\"”’)]?)\\s+(?=[\"“(]?[A-Z0-9$])");
    private static final int SEPARATOR_SAFETY_TOKENS = 16;
    private static final int MIN_PIECE_BUDGET = 50;
    private static final int MAX_SECTION_CHARS = 300;   // chunks.section is VARCHAR(300)
    private static final int MAX_SPEAKER_CHARS = 200;   // chunks.speaker is VARCHAR(200)
    private static final int MAX_SECTION_LINE_CHARS = 200;  // long risk-factor headings are cut in the header

    private final TokenCounter tokens;
    private final ChunkingProperties sizes;

    public Chunker(TokenCounter tokens, ChunkingProperties sizes) {
        this.tokens = tokens;
        this.sizes = sizes;
    }

    public List<Chunk> chunk(ManifestEntry document, List<CleanedBlock> blocks) {
        String header = "[%s | %s | %s]".formatted(document.ticker(), document.periodLabel(), document.docType());
        boolean transcript = document.docType() == DocType.TRANSCRIPT;

        List<Chunk> chunks = new ArrayList<>();
        for (Group group : group(blocks, transcript)) {
            chunkGroup(group, header, chunks);
        }
        return chunks;
    }

    // ---------- Step 1: groups ----------

    /** Text that must stay together: same section, same speaker, same forward-looking tag. */
    private static final class Group {
        final String section;
        final String speaker;
        final boolean forwardLooking;
        final List<Piece> pieces = new ArrayList<>();

        Group(String section, String speaker, boolean forwardLooking) {
            this.section = section;
            this.speaker = speaker;
            this.forwardLooking = forwardLooking;
        }
    }

    /**
     * A paragraph, sentence or table part.
     *
     * @param startsParagraph joined with a blank line before it; otherwise with a space
     *                        (sentences) or a newline (table rows)
     */
    private record Piece(String text, Integer page, boolean table, boolean startsParagraph) {
    }

    private static List<Group> group(List<CleanedBlock> blocks, boolean transcript) {
        List<Group> groups = new ArrayList<>();
        Map<String, String> knownSpeakers = new HashMap<>();
        Group current = null;
        String speaker = null;

        for (CleanedBlock block : blocks) {
            String text = block.text();
            boolean newTurn = false;
            if (transcript && block.kind() == BlockKind.TEXT) {
                Optional<SpeakerLabel> label = SpeakerLabel.parse(text, knownSpeakers);
                if (label.isPresent()) {
                    speaker = label.get().speaker();
                    text = label.get().rest();
                    newTurn = true;
                }
            }
            if (newTurn || current == null
                    || !Objects.equals(current.section, block.section())
                    || !Objects.equals(current.speaker, speaker)
                    || current.forwardLooking != block.forwardLooking()) {
                current = new Group(block.section(), speaker, block.forwardLooking());
                groups.add(current);
            }
            if (!text.isBlank()) {
                current.pieces.add(new Piece(text, block.page(), block.kind() == BlockKind.TABLE, true));
            }
        }
        groups.removeIf(group -> group.pieces.isEmpty());
        return groups;
    }

    // ---------- Steps 2 to 4: chunks ----------

    private void chunkGroup(Group group, String header, List<Chunk> out) {
        String fixed = header;
        if (group.section != null) {
            fixed += "\nSection: " + truncate(group.section, MAX_SECTION_LINE_CHARS);
        }
        if (group.speaker != null) {
            fixed += "\nSpeaker: " + group.speaker;
        }
        int pieceBudget = Math.max(MIN_PIECE_BUDGET,
                sizes.maxTokens() - tokens.count(fixed) - sizes.overlapTokens() - SEPARATOR_SAFETY_TOKENS);

        List<Piece> current = new ArrayList<>();
        boolean hasNewText = false;   // false while the chunk holds only overlap from the previous one
        for (Piece piece : splitOversized(group.pieces, pieceBudget)) {
            if (hasNewText) {
                current.add(piece);
                if (tokens.count(render(fixed, current)) <= sizes.targetTokens()) {
                    continue;
                }
                current.remove(current.size() - 1);
                out.add(toChunk(out.size(), fixed, current, group));
                current = new ArrayList<>(overlapFrom(current));
            }
            // The first new piece always fits: overlap + piece budget stays under the max.
            current.add(piece);
            hasNewText = true;
        }
        if (hasNewText) {
            out.add(toChunk(out.size(), fixed, current, group));
        }
    }

    /** Splits pieces bigger than the budget: paragraphs into sentences, tables into rows, then words. */
    private List<Piece> splitOversized(List<Piece> pieces, int budget) {
        List<Piece> result = new ArrayList<>();
        for (Piece piece : pieces) {
            if (tokens.count(piece.text()) <= budget) {
                result.add(piece);
                continue;
            }
            List<String> parts = piece.table()
                    ? piece.text().lines().filter(line -> !line.isBlank()).toList()
                    : sentences(piece.text());
            boolean first = true;
            for (String part : parts) {
                List<String> units = tokens.count(part) <= budget ? List.of(part) : splitByWords(part, budget);
                for (String unit : units) {
                    result.add(new Piece(unit, piece.page(), piece.table(), first && piece.startsParagraph()));
                    first = false;
                }
            }
        }
        return result;
    }

    /** Last resort for one sentence or row that is bigger than a whole chunk. */
    private List<String> splitByWords(String text, int budget) {
        List<String> parts = new ArrayList<>();
        StringBuilder part = new StringBuilder();
        for (String word : text.split(" ")) {
            String candidate = part.isEmpty() ? word : part + " " + word;
            if (!part.isEmpty() && tokens.count(candidate) > budget) {
                parts.add(part.toString());
                part.setLength(0);
                part.append(word);
            } else {
                part.setLength(0);
                part.append(candidate);
            }
        }
        if (!part.isEmpty()) {
            parts.add(part.toString());
        }
        return parts;
    }

    /** Whole sentences from the end of the chunk, up to the overlap size. */
    private List<Piece> overlapFrom(List<Piece> pieces) {
        LinkedList<Piece> overlap = new LinkedList<>();
        int used = 0;
        for (int i = pieces.size() - 1; i >= 0; i--) {
            Piece piece = pieces.get(i);
            if (piece.table()) {
                break;   // overlap is prose only; half a table out of context is not useful
            }
            List<String> sentences = sentences(piece.text());
            LinkedList<String> taken = new LinkedList<>();
            for (int j = sentences.size() - 1; j >= 0; j--) {
                int size = tokens.count(sentences.get(j));
                if (used + size > sizes.overlapTokens()) {
                    if (!taken.isEmpty()) {
                        overlap.addFirst(new Piece(String.join(" ", taken), piece.page(), false, true));
                    }
                    return overlap;
                }
                used += size;
                taken.addFirst(sentences.get(j));
            }
            overlap.addFirst(new Piece(String.join(" ", taken), piece.page(), false, piece.startsParagraph()));
        }
        return overlap;
    }

    private Chunk toChunk(int index, String fixed, List<Piece> pieces, Group group) {
        String content = render(fixed, pieces);
        Integer pageStart = pieces.stream().map(Piece::page).filter(Objects::nonNull).min(Integer::compare).orElse(null);
        Integer pageEnd = pieces.stream().map(Piece::page).filter(Objects::nonNull).max(Integer::compare).orElse(null);
        return new Chunk(index, content, tokens.count(content), pageStart, pageEnd,
                truncate(group.section, MAX_SECTION_CHARS), truncate(group.speaker, MAX_SPEAKER_CHARS),
                group.forwardLooking);
    }

    private static String render(String fixed, List<Piece> pieces) {
        StringBuilder text = new StringBuilder(fixed).append('\n');
        for (int i = 0; i < pieces.size(); i++) {
            Piece piece = pieces.get(i);
            if (i > 0) {
                text.append(piece.startsParagraph() ? "\n\n" : piece.table() ? "\n" : " ");
            }
            text.append(piece.text());
        }
        return text.toString();
    }

    static List<String> sentences(String text) {
        return List.of(SENTENCE_BREAK.split(text.strip()));
    }

    private static String truncate(String value, int maxChars) {
        return value == null || value.length() <= maxChars ? value : value.substring(0, maxChars);
    }
}
