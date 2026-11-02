package com.earningslens.eval;

/** dev: used for tuning. test: only for reported numbers, never for tuning (spec 11.1). */
public enum EvalSplit {
    DEV,
    TEST;

    public String jsonName() {
        return name().toLowerCase();
    }

    public static EvalSplit fromJson(String value) {
        for (EvalSplit split : values()) {
            if (split.jsonName().equals(value)) {
                return split;
            }
        }
        throw new IllegalArgumentException("unknown split '" + value + "', expected dev or test");
    }
}
