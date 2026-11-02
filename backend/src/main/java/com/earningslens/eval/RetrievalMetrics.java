package com.earningslens.eval;

import com.earningslens.ingest.TextNormalizer;
import com.earningslens.retrieval.FusedHit;
import com.earningslens.retrieval.RetrievedChunk;

import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;

/**
 * Retrieval metrics from spec 11.2, as pure functions (no database, no Spring).
 *
 * - A chunk is a hit for a gold quote if its text contains the quote after the same
 *   whitespace normalisation as ingestion (spec 11.1).
 * - Recall@k (all): share of answerable items where every gold quote is in the retrieved set.
 *   Recall@k (any): share where at least one gold quote is.
 * - MRR: mean of 1 / rank of the first chunk containing any gold quote (0 if none).
 * - Period coverage: for multi_quarter items, share of the required company + quarter pairs
 *   (from the gold evidence) with at least one retrieved chunk, pooled over all items.
 */
public final class RetrievalMetrics {

    private RetrievalMetrics() {
    }

    /** Scores of one item for one config. Unanswerable items have goldQuotes = 0. */
    public record ItemScore(String id, EvalCategory category, int retrieved, int goldQuotes, int goldFound,
                            Integer firstHitRank, int requiredPeriods, int coveredPeriods, List<String> missedQuotes) {

        public boolean allFound() {
            return goldQuotes > 0 && goldFound == goldQuotes;
        }

        public boolean anyFound() {
            return goldFound > 0;
        }

        public double reciprocalRank() {
            return firstHitRank == null ? 0.0 : 1.0 / firstHitRank;
        }
    }

    /**
     * Averages over the answerable items in a group.
     *
     * @param periodCoverage null when the group has no multi_quarter items
     */
    public record Summary(int answerableItems, double recallAll, double recallAny, double mrr,
                          int requiredPeriods, int coveredPeriods, Double periodCoverage, double avgRetrieved) {
    }

    public static ItemScore score(EvalItem item, List<FusedHit> hits) {
        List<String> contents = hits.stream().map(h -> TextNormalizer.normalize(h.chunk().content())).toList();

        int found = 0;
        Integer firstHitRank = null;
        List<String> missed = new ArrayList<>();
        for (GoldEvidence evidence : item.goldEvidence()) {
            String quote = evidence.normalizedQuote();
            boolean hit = false;
            for (int i = 0; i < hits.size(); i++) {
                if (contents.get(i).contains(quote)) {
                    hit = true;
                    int rank = hits.get(i).rank();
                    firstHitRank = firstHitRank == null ? rank : Math.min(firstHitRank, rank);
                }
            }
            if (hit) {
                found++;
            } else {
                missed.add(quote.length() <= 80 ? quote : quote.substring(0, 77) + "...");
            }
        }

        Set<String> required = new LinkedHashSet<>();
        item.goldEvidence().forEach(e -> required.add(key(e.ticker(), e.period().year(), e.period().quarter())));
        Set<String> retrievedPeriods = new LinkedHashSet<>();
        for (FusedHit h : hits) {
            RetrievedChunk c = h.chunk();
            retrievedPeriods.add(key(c.ticker(), c.fiscalYear(), c.fiscalQuarter()));
        }
        int covered = (int) required.stream().filter(retrievedPeriods::contains).count();

        return new ItemScore(item.id(), item.category(), hits.size(), item.goldEvidence().size(), found,
                firstHitRank, required.size(), covered, missed);
    }

    /** Summary over the answerable items in the list; period coverage over its multi_quarter items. */
    public static Summary summarize(List<ItemScore> scores) {
        List<ItemScore> answerable = scores.stream().filter(s -> s.goldQuotes() > 0).toList();
        int n = answerable.size();
        double recallAll = n == 0 ? 0 : answerable.stream().filter(ItemScore::allFound).count() / (double) n;
        double recallAny = n == 0 ? 0 : answerable.stream().filter(ItemScore::anyFound).count() / (double) n;
        double mrr = n == 0 ? 0 : answerable.stream().mapToDouble(ItemScore::reciprocalRank).sum() / n;
        double avgRetrieved = n == 0 ? 0 : answerable.stream().mapToInt(ItemScore::retrieved).sum() / (double) n;

        List<ItemScore> multi = answerable.stream().filter(s -> s.category() == EvalCategory.MULTI_QUARTER).toList();
        int required = multi.stream().mapToInt(ItemScore::requiredPeriods).sum();
        int covered = multi.stream().mapToInt(ItemScore::coveredPeriods).sum();
        Double coverage = required == 0 ? null : covered / (double) required;

        return new Summary(n, recallAll, recallAny, mrr, required, covered, coverage, avgRetrieved);
    }

    private static String key(String ticker, int year, int quarter) {
        return ticker + " FY" + year + " Q" + quarter;
    }
}
