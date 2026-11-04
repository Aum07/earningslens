package com.earningslens.eval;

import tools.jackson.core.JacksonException;
import tools.jackson.databind.json.JsonMapper;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * Reads and writes eval/human-labels.jsonl (roadmap E5.2). One JSON object per line:
 * {"label_id": "q001-C", "item_id": "q001", "config": "C", "faithful": null, "correctness": null, "note": ""}
 *
 * The person fills in "faithful" (true or false) and "correctness" (1 to 5). Loading checks every line,
 * so a typo is reported with its line number instead of silently skewing the agreement numbers.
 */
final class HumanLabels {

    private static final Set<String> FIELDS = Set.of("label_id", "item_id", "config", "faithful", "correctness", "note");
    private static final JsonMapper MAPPER = JsonMapper.builder().build();

    private HumanLabels() {
    }

    /** All labels in file order. Blank (null) values are allowed here; see {@link #requireComplete}. */
    static List<HumanLabel> load(Path path) throws IOException {
        List<String> lines = Files.readAllLines(path, StandardCharsets.UTF_8);
        List<HumanLabel> labels = new ArrayList<>();
        Set<String> seen = new HashSet<>();
        for (int i = 0; i < lines.size(); i++) {
            String line = lines.get(i);
            if (line.isBlank()) {
                continue;
            }
            String where = "%s line %d".formatted(path.getFileName(), i + 1);
            HumanLabel label;
            try {
                label = parse(MAPPER.readValue(line, Map.class));
            } catch (JacksonException e) {
                throw new IllegalArgumentException(where + ": not valid JSON (" + e.getOriginalMessage() + ")", e);
            } catch (IllegalArgumentException e) {
                throw new IllegalArgumentException(where + ": " + e.getMessage(), e);
            }
            if (!seen.add(label.labelId())) {
                throw new IllegalArgumentException(where + ": duplicate label_id " + label.labelId());
            }
            labels.add(label);
        }
        return List.copyOf(labels);
    }

    private static HumanLabel parse(Map<?, ?> json) {
        for (Object key : json.keySet()) {
            if (!FIELDS.contains(String.valueOf(key))) {
                throw new IllegalArgumentException("unknown field '" + key + "'");
            }
        }
        String labelId = requiredText(json, "label_id");
        String itemId = requiredText(json, "item_id");
        String config = requiredText(json, "config");

        Object faithfulValue = json.get("faithful");
        Boolean faithful;
        if (faithfulValue == null) {
            faithful = null;
        } else if (faithfulValue instanceof Boolean b) {
            faithful = b;
        } else {
            throw new IllegalArgumentException("faithful must be true, false or null (no quotes), got " + faithfulValue);
        }

        Object correctnessValue = json.get("correctness");
        Integer correctness;
        if (correctnessValue == null) {
            correctness = null;
        } else if (correctnessValue instanceof Integer n && n >= 1 && n <= 5) {
            correctness = n;
        } else {
            throw new IllegalArgumentException("correctness must be a whole number 1 to 5 or null, got " + correctnessValue);
        }

        Object noteValue = json.get("note");
        if (noteValue != null && !(noteValue instanceof String)) {
            throw new IllegalArgumentException("note must be text");
        }
        return new HumanLabel(labelId, itemId, config, faithful, correctness, (String) noteValue);
    }

    private static String requiredText(Map<?, ?> json, String field) {
        if (json.get(field) instanceof String s && !s.isBlank()) {
            return s;
        }
        throw new IllegalArgumentException("'" + field + "' is missing or empty");
    }

    /** Throws if any label still has a blank faithful or correctness value, listing which ones. */
    static void requireComplete(List<HumanLabel> labels) {
        List<String> missing = labels.stream().filter(l -> !l.complete()).map(HumanLabel::labelId).toList();
        if (!missing.isEmpty()) {
            throw new IllegalArgumentException(missing.size() + " labels are not filled in yet (faithful and "
                    + "correctness are both needed): " + missing);
        }
    }

    /** True if the file exists and at least one line has a value the person typed. */
    static boolean hasWork(Path path) throws IOException {
        if (!Files.exists(path) || Files.readString(path, StandardCharsets.UTF_8).isBlank()) {
            return false;
        }
        return load(path).stream().anyMatch(HumanLabel::started);
    }

    /** A blank line for every sample, in sample order. */
    static void writeTemplate(Path path, List<LabelSample> samples) throws IOException {
        StringBuilder out = new StringBuilder();
        for (LabelSample sample : samples) {
            Map<String, Object> line = new LinkedHashMap<>();   // keeps the field order on each line
            line.put("label_id", sample.labelId());
            line.put("item_id", sample.itemId());
            line.put("config", sample.config());
            line.put("faithful", null);
            line.put("correctness", null);
            line.put("note", "");
            out.append(MAPPER.writeValueAsString(line)).append('\n');
        }
        Files.writeString(path, out.toString(), StandardCharsets.UTF_8);
    }
}
