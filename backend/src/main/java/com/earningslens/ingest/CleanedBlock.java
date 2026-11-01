package com.earningslens.ingest;

/**
 * A {@link ParsedBlock} after cleaning (roadmap E1.5).
 *
 * @param forwardLooking true for forward-looking-statement / safe-harbor boilerplate.
 *                       Kept, not deleted, so evaluation can later decide whether to exclude it.
 */
public record CleanedBlock(String section, Integer page, BlockKind kind, String text, boolean forwardLooking) {
}
