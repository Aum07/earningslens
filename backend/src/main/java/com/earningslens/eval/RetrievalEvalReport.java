package com.earningslens.eval;

import java.util.List;
import java.util.Map;

/** Everything a retrieval-only eval run produced (E3.6). Written as JSON and Markdown by RetrievalReportWriter. */
public record RetrievalEvalReport(String date, String split, int items, Settings settings, List<ConfigRun> runs,
                                  List<GoldRank> goldRanks, Threshold threshold) {

    /**
     * E3.7: threshold sweep over the best vector similarity of one config's run (C, or D when C was not run).
     * Null when neither ran.
     */
    public record Threshold(String config, List<ThresholdTuning.Row> rows, ThresholdTuning.Row suggested,
                            List<ThresholdTuning.ItemSimilarity> items) {
    }

    /** The settings the numbers belong to, so a report can be compared with later tuning runs. */
    public record Settings(int k, int kPerPeriod, int maxPeriods, int vectorCandidates, int keywordCandidates,
                           int rrfK, double minSimilarity, int chunkTargetTokens, int chunkOverlapTokens,
                           int chunkMaxTokens, int corpusChunks, String embeddingModel) {
    }

    public record ConfigRun(String config, RetrievalMetrics.Summary overall,
                            Map<String, RetrievalMetrics.Summary> byCategory, double avgLatencyMs, List<ItemRow> items) {
    }

    public record ItemRow(String id, String category, String question, String strategy, String abstainReason,
                          Double bestSimilarity, List<String> searchedTargets, List<String> skippedTargets,
                          RetrievalMetrics.ItemScore score, List<ChunkRow> chunks) {
    }

    /**
     * Diagnostic: where the first chunk containing a gold quote ranks when searching only that
     * quote's company and quarter (no doc type filter), with the full question. Null rank = not in the list.
     *
     * @param vectorRank       rank in a vector search asking for every chunk of the quarter (close to exact)
     * @param keywordRank      rank in a keyword search asking for every chunk of the quarter
     * @param vectorRankTopN   rank in the real vector search (vector-candidates, 30): differs from vectorRank
     *                         when the approximate HNSW index skips the chunk
     * @param fusedRank        rank after RRF of the real top-N lists, before dedupe
     * @param dedupedRank      rank after dropping overlapping neighbours (what a single-quarter search returns, up to k)
     */
    public record GoldRank(String itemId, String evidence, String quote, Integer vectorRank, Integer keywordRank,
                           int chunksInQuarter, Integer vectorRankTopN, Integer fusedRank, Integer dedupedRank) {
    }

    public record ChunkRow(int rank, String ticker, String period, String docType, String section,
                           boolean containsGold, String chunkId) {
    }
}
