package com.earningslens.retrieval;

/**
 * Ablation configs from spec 11.4. Explicit filters (from the UI or an eval item) apply in every mode.
 *
 * A: vector search only, k = 8.
 * B: hybrid (vector + keyword, RRF).
 * C: B + filters from QueryParser + per-period retrieval.
 * D: C + evidence threshold abstention (min-similarity).
 */
public enum RetrievalMode {
    A, B, C, D;

    public boolean hybrid() {
        return this != A;
    }

    public boolean usesParser() {
        return this == C || this == D;
    }

    public boolean usesThreshold() {
        return this == D;
    }
}
