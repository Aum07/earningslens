package com.earningslens.retrieval;

import com.earningslens.config.RetrievalProperties;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TreeMap;

/**
 * Brings retrieval together (roadmap E3.5, spec section 8).
 *
 * 1. Parse the question and resolve periods (modes C and D). Explicit filters override what the
 *    parser found: explicit tickers replace parsed tickers, explicit periods replace parsed periods.
 * 2. If the question only asks for periods outside the corpus, abstain before any OpenAI call.
 * 3. Two or more company + quarter targets: one search per target, k-per-period chunks each
 *    (at most max-periods targets). Otherwise one search with k chunks. Both use the full question:
 *    removing its period words was tried on dev and made recall worse (DECISIONS.md).
 * 4. Each search: vector top N, keyword top N (not in mode A), RRF, dedupe of overlapping neighbours.
 * 5. Mode D: abstain if the best vector similarity is below min-similarity (chunks are still returned,
 *    so eval can see what was found).
 */
@Component
public class Retriever {

    private final QueryParser parser;
    private final PeriodResolver resolver;
    private final VectorSearchRepository vectorSearch;
    private final KeywordSearchRepository keywordSearch;
    private final QueryEmbedder embedder;
    private final RetrievalProperties settings;

    public Retriever(QueryParser parser, PeriodResolver resolver, VectorSearchRepository vectorSearch,
                     KeywordSearchRepository keywordSearch, QueryEmbedder embedder, RetrievalProperties settings) {
        this.parser = parser;
        this.resolver = resolver;
        this.vectorSearch = vectorSearch;
        this.keywordSearch = keywordSearch;
        this.embedder = embedder;
        this.settings = settings;
    }

    /** Uses the mode from config (earningslens.retrieval.mode). */
    public RetrievalResult retrieve(String question, SearchFilters explicitFilters) {
        return retrieve(question, explicitFilters, settings.mode());
    }

    public RetrievalResult retrieve(String question, SearchFilters explicitFilters, RetrievalMode mode) {
        if (question == null || question.isBlank()) {
            throw new IllegalArgumentException("question is empty");
        }
        SearchFilters explicit = explicitFilters == null ? SearchFilters.none() : explicitFilters;

        SearchFilters singleFilters = explicit;
        List<PeriodTarget> targets = List.of();
        List<PeriodTarget> skipped = List.of();
        List<PeriodTarget> missing = List.of();

        if (mode.usesParser()) {
            ResolvedQuery resolved = resolver.resolve(withExplicitFilters(parser.parse(question), explicit));
            missing = resolved.missing();
            if (resolved.onlyMissingPeriods()) {
                return new RetrievalResult(mode, RetrievalResult.Strategy.NONE, List.of(), List.of(), List.of(),
                        missing, null, RetrievalResult.AbstainReason.PERIOD_NOT_IN_CORPUS);
            }
            List<PeriodTarget> capped = cap(resolved.targets(), settings.maxPeriods());
            targets = capped;
            skipped = resolved.targets().stream().filter(t -> !capped.contains(t)).toList();
            if (targets.size() == 1) {
                PeriodTarget only = targets.get(0);
                singleFilters = new SearchFilters(Set.of(only.ticker()), Set.of(only.period()), explicit.docTypes());
            } else if (targets.isEmpty()) {
                singleFilters = new SearchFilters(resolved.tickers(), Set.of(), explicit.docTypes());
            }
        }

        float[] queryEmbedding = embedder.embed(question);
        List<FusedHit> chunks;
        Double best;
        RetrievalResult.Strategy strategy;

        if (targets.size() >= 2) {
            strategy = RetrievalResult.Strategy.PER_PERIOD;
            List<List<FusedHit>> perTarget = new ArrayList<>();
            best = null;
            for (PeriodTarget target : targets) {
                SearchFilters filters = new SearchFilters(Set.of(target.ticker()), Set.of(target.period()), explicit.docTypes());
                Search search = search(question, queryEmbedding, filters, settings.kPerPeriod(), mode);
                perTarget.add(search.hits());
                best = max(best, search.bestSimilarity());
            }
            chunks = interleave(perTarget);
        } else {
            strategy = RetrievalResult.Strategy.SINGLE;
            Search search = search(question, queryEmbedding, singleFilters, settings.k(), mode);
            chunks = search.hits();
            best = search.bestSimilarity();
        }

        RetrievalResult.AbstainReason reason = null;
        if (mode.usesThreshold() && (best == null || best < settings.minSimilarity())) {
            reason = RetrievalResult.AbstainReason.LOW_SIMILARITY;
        }
        return new RetrievalResult(mode, strategy, chunks, targets, skipped, missing, best, reason);
    }

    /** Explicit tickers or periods win over what the parser found in the question text. */
    static ParsedQuestion withExplicitFilters(ParsedQuestion parsed, SearchFilters explicit) {
        Set<String> tickers = explicit.tickers().isEmpty() ? parsed.tickers() : explicit.tickers();
        List<PeriodRef> refs = explicit.periods().isEmpty()
                ? parsed.periodRefs()
                : explicit.periods().stream().sorted().map(p -> (PeriodRef) new PeriodRef.Fiscal(p)).toList();
        return new ParsedQuestion(tickers, refs);
    }

    /**
     * At most max targets. Companies take turns, newest quarter first, so a comparison of two
     * companies over many quarters keeps both companies. Returned sorted (ticker, then period).
     */
    static List<PeriodTarget> cap(List<PeriodTarget> targets, int max) {
        if (targets.size() <= max) {
            return targets;
        }
        Map<String, List<PeriodTarget>> byTicker = new TreeMap<>();
        for (PeriodTarget t : targets) {
            byTicker.computeIfAbsent(t.ticker(), k -> new ArrayList<>()).add(t);
        }
        byTicker.values().forEach(list -> list.sort(Comparator.comparing(PeriodTarget::period).reversed()));

        List<PeriodTarget> chosen = new ArrayList<>();
        for (int round = 0; chosen.size() < max; round++) {
            for (List<PeriodTarget> list : byTicker.values()) {
                if (round < list.size() && chosen.size() < max) {
                    chosen.add(list.get(round));
                }
            }
        }
        chosen.sort(Comparator.naturalOrder());
        return chosen;
    }

    /** Rank 1 of every target, then rank 2 of every target, ...; re-numbered 1..n. */
    static List<FusedHit> interleave(List<List<FusedHit>> perTarget) {
        List<FusedHit> out = new ArrayList<>();
        int longest = perTarget.stream().mapToInt(List::size).max().orElse(0);
        for (int i = 0; i < longest; i++) {
            for (List<FusedHit> list : perTarget) {
                if (i < list.size()) {
                    FusedHit h = list.get(i);
                    out.add(new FusedHit(h.chunk(), h.score(), out.size() + 1, h.vectorRank(), h.keywordRank(),
                            h.vectorSimilarity()));
                }
            }
        }
        return out;
    }

    private Search search(String question, float[] queryEmbedding, SearchFilters filters, int limit, RetrievalMode mode) {
        List<VectorHit> vector = vectorSearch.search(queryEmbedding, filters, settings.vectorCandidates());
        List<KeywordHit> keyword = mode.hybrid()
                ? keywordSearch.search(question, filters, settings.keywordCandidates())
                : List.of();
        // Fuse more than needed, so dropping overlapping neighbours still leaves `limit` chunks.
        List<FusedHit> fused = ChunkDeduper.dedupe(RrfFusion.fuse(vector, keyword, settings.rrfK(), limit * 2));
        List<FusedHit> top = new ArrayList<>();
        for (FusedHit h : fused.subList(0, Math.min(limit, fused.size()))) {
            top.add(new FusedHit(h.chunk(), h.score(), top.size() + 1, h.vectorRank(), h.keywordRank(), h.vectorSimilarity()));
        }
        Double best = vector.isEmpty() ? null : vector.get(0).similarity();
        return new Search(top, best);
    }

    private static Double max(Double a, Double b) {
        if (a == null) {
            return b;
        }
        return b == null ? a : Math.max(a, b);
    }

    private record Search(List<FusedHit> hits, Double bestSimilarity) {
    }
}
