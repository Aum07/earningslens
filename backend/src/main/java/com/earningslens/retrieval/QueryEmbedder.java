package com.earningslens.retrieval;

/** Turns a question into its embedding. An interface so tests can use a fake (no OpenAI calls). */
@FunctionalInterface
public interface QueryEmbedder {

    float[] embed(String question);
}
