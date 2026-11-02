package com.earningslens.eval;

import java.util.Arrays;
import java.util.stream.Collectors;

/** Question categories from spec 11.1. The JSON form is lower case, for example "multi_quarter". */
public enum EvalCategory {
    SINGLE_FACT,
    MULTI_QUARTER,
    QUALITATIVE,
    UNANSWERABLE;

    public String jsonName() {
        return name().toLowerCase();
    }

    public static EvalCategory fromJson(String value) {
        for (EvalCategory category : values()) {
            if (category.jsonName().equals(value)) {
                return category;
            }
        }
        throw new IllegalArgumentException("unknown category '" + value + "', expected one of "
                + Arrays.stream(values()).map(EvalCategory::jsonName).collect(Collectors.joining(", ")));
    }
}
