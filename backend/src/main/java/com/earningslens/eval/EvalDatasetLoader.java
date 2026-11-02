package com.earningslens.eval;

import com.earningslens.ingest.DocType;
import org.springframework.stereotype.Component;
import tools.jackson.core.JacksonException;
import tools.jackson.databind.json.JsonMapper;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * Reads eval/dataset.jsonl (one JSON object per line) into {@link EvalItem}s.
 *
 * Fails fast with the file, line number and item id when a line is invalid:
 * bad JSON, a missing or unknown field, an unknown category, or an item that
 * breaks the rules in {@link EvalItem}. Blank lines are skipped.
 *
 * Each line is read into a plain Map first and checked by hand (same approach as
 * ManifestLoader), which gives clearer messages than letting Jackson bind records.
 */
@Component
public class EvalDatasetLoader {

    private static final Set<String> ITEM_FIELDS = Set.of("id", "split", "category", "question", "filters",
            "answerable", "gold_answer", "gold_evidence");
    private static final Set<String> EVIDENCE_FIELDS = Set.of("ticker", "fiscal_year", "fiscal_quarter",
            "doc_type", "quote");
    private static final Set<String> FILTER_FIELDS = Set.of("tickers", "fiscalPeriods", "docTypes");
    private static final Set<String> PERIOD_FIELDS = Set.of("year", "quarter");

    private final JsonMapper mapper = JsonMapper.builder().build();

    public List<EvalItem> load(Path path) throws IOException {
        List<String> lines = Files.readAllLines(path, StandardCharsets.UTF_8);
        List<EvalItem> items = new ArrayList<>();
        Set<String> seenIds = new HashSet<>();
        for (int i = 0; i < lines.size(); i++) {
            String line = lines.get(i);
            if (line.isBlank()) {
                continue;
            }
            String where = "%s line %d".formatted(path, i + 1);
            Map<?, ?> json;
            try {
                json = mapper.readValue(line, Map.class);
            } catch (JacksonException e) {
                throw new IllegalArgumentException(where + ": not valid JSON (" + e.getOriginalMessage() + ")", e);
            }
            if (json.get("id") instanceof String id) {
                where += " (" + id + ")";
            }
            EvalItem item;
            try {
                item = toItem(json);
            } catch (RuntimeException e) {
                throw new IllegalArgumentException(where + ": " + e.getMessage(), e);
            }
            if (!seenIds.add(item.id())) {
                throw new IllegalArgumentException(where + ": duplicate id " + item.id());
            }
            items.add(item);
        }
        return List.copyOf(items);
    }

    /** Loads the file and keeps only one split, for example dev while tuning. */
    public List<EvalItem> load(Path path, EvalSplit split) throws IOException {
        return load(path).stream().filter(item -> item.split() == split).toList();
    }

    private static EvalItem toItem(Map<?, ?> json) {
        checkFields(json, ITEM_FIELDS, "item");
        List<GoldEvidence> evidence = new ArrayList<>();
        List<?> rawEvidence = list(json, "gold_evidence");
        for (int i = 0; i < rawEvidence.size(); i++) {
            if (!(rawEvidence.get(i) instanceof Map<?, ?> e)) {
                throw new IllegalArgumentException("gold_evidence[" + i + "] must be an object");
            }
            try {
                evidence.add(toEvidence(e));
            } catch (RuntimeException ex) {
                throw new IllegalArgumentException("gold_evidence[" + i + "]: " + ex.getMessage(), ex);
            }
        }
        return new EvalItem(
                text(json, "id"),
                EvalSplit.fromJson(text(json, "split")),
                EvalCategory.fromJson(text(json, "category")),
                text(json, "question"),
                filters(json),
                bool(json, "answerable"),
                text(json, "gold_answer"),
                evidence);
    }

    private static GoldEvidence toEvidence(Map<?, ?> json) {
        checkFields(json, EVIDENCE_FIELDS, "evidence");
        return new GoldEvidence(
                text(json, "ticker"),
                new FiscalPeriod(number(json, "fiscal_year"), number(json, "fiscal_quarter")),
                docType(text(json, "doc_type")),
                text(json, "quote"));
    }

    /** "filters" may be missing or null (no filters), otherwise the chat API shape from spec 10. */
    private static EvalFilters filters(Map<?, ?> json) {
        Object raw = json.get("filters");
        if (raw == null) {
            return null;
        }
        if (!(raw instanceof Map<?, ?> f)) {
            throw new IllegalArgumentException("filters must be an object or null");
        }
        checkFields(f, FILTER_FIELDS, "filters");
        List<String> tickers = new ArrayList<>();
        for (Object t : optionalList(f, "tickers")) {
            tickers.add(String.valueOf(t));
        }
        List<FiscalPeriod> periods = new ArrayList<>();
        for (Object p : optionalList(f, "fiscalPeriods")) {
            if (!(p instanceof Map<?, ?> period)) {
                throw new IllegalArgumentException("filters.fiscalPeriods entries must look like {\"year\": 2026, \"quarter\": 1}");
            }
            checkFields(period, PERIOD_FIELDS, "filters.fiscalPeriods entry");
            periods.add(new FiscalPeriod(number(period, "year"), number(period, "quarter")));
        }
        List<DocType> docTypes = new ArrayList<>();
        for (Object d : optionalList(f, "docTypes")) {
            docTypes.add(docType(String.valueOf(d)));
        }
        return new EvalFilters(tickers, periods, docTypes);
    }

    /** Unknown fields are rejected so a typo such as "gold_evidnce" cannot silently drop data. */
    private static void checkFields(Map<?, ?> json, Set<String> allowed, String what) {
        for (Object key : json.keySet()) {
            if (!allowed.contains(String.valueOf(key))) {
                throw new IllegalArgumentException("unknown " + what + " field '" + key + "'");
            }
        }
    }

    private static DocType docType(String value) {
        try {
            return DocType.valueOf(value);
        } catch (IllegalArgumentException e) {
            throw new IllegalArgumentException("unknown doc_type '" + value
                    + "', expected PRESS_RELEASE, TRANSCRIPT, FORM_10Q or FORM_10K");
        }
    }

    private static String text(Map<?, ?> json, String field) {
        Object value = json.get(field);
        if (value == null) {
            throw new IllegalArgumentException("missing field '" + field + "'");
        }
        if (!(value instanceof String s)) {
            throw new IllegalArgumentException("field '" + field + "' must be a string");
        }
        return s;
    }

    private static int number(Map<?, ?> json, String field) {
        if (!json.containsKey(field)) {
            throw new IllegalArgumentException("missing field '" + field + "'");
        }
        if (!(json.get(field) instanceof Integer n)) {
            throw new IllegalArgumentException("field '" + field + "' must be a whole number");
        }
        return n;
    }

    private static boolean bool(Map<?, ?> json, String field) {
        if (!json.containsKey(field)) {
            throw new IllegalArgumentException("missing field '" + field + "'");
        }
        if (!(json.get(field) instanceof Boolean b)) {
            throw new IllegalArgumentException("field '" + field + "' must be true or false");
        }
        return b;
    }

    private static List<?> list(Map<?, ?> json, String field) {
        if (!json.containsKey(field)) {
            throw new IllegalArgumentException("missing field '" + field + "'");
        }
        if (!(json.get(field) instanceof List<?> l)) {
            throw new IllegalArgumentException("field '" + field + "' must be a list");
        }
        return l;
    }

    private static List<?> optionalList(Map<?, ?> json, String field) {
        Object value = json.get(field);
        if (value == null) {
            return List.of();
        }
        if (!(value instanceof List<?> l)) {
            throw new IllegalArgumentException("filters." + field + " must be a list");
        }
        return l;
    }
}
