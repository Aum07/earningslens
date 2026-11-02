package com.earningslens.retrieval;

import com.earningslens.PgvectorContainerTest;
import com.earningslens.config.RetrievalProperties;
import com.earningslens.eval.FiscalPeriod;
import com.earningslens.ingest.DocType;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

import java.sql.SQLException;
import java.util.List;
import java.util.Set;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.stream.Collectors;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Roadmap E3.5: the whole retriever against the shared pgvector test container.
 *
 * Fixture: Microsoft FY2026 Q2 to FY2027 Q1 and Alphabet FY2026 Q1 to Q3. Microsoft's FY2026 Q4
 * has 10 chunks that are much closer to the question than any other quarter, so a single search
 * fills all 8 places with FY2026 Q4. Per-period retrieval must still return the other quarters.
 * The query embedding is a fake (no OpenAI calls): always (1, 0, 0, ...).
 */
class RetrieverTest extends PgvectorContainerTest {

    private static final FiscalPeriod FY2026_Q1 = new FiscalPeriod(2026, 1);
    private static final FiscalPeriod FY2026_Q2 = new FiscalPeriod(2026, 2);
    private static final FiscalPeriod FY2026_Q3 = new FiscalPeriod(2026, 3);
    private static final FiscalPeriod FY2026_Q4 = new FiscalPeriod(2026, 4);
    private static final FiscalPeriod FY2027_Q1 = new FiscalPeriod(2027, 1);

    private static final String LAST_THREE = "What was Microsoft's total revenue over the last three quarters?";

    @Autowired QueryParser parser;
    @Autowired PeriodResolver resolver;
    @Autowired VectorSearchRepository vectorSearch;
    @Autowired KeywordSearchRepository keywordSearch;

    private final AtomicInteger embedCalls = new AtomicInteger();

    @BeforeEach
    void setUp() throws SQLException {
        clearDatabase();
        embedCalls.set(0);
        for (int i = 0; i < 10; i++) {
            insertChunk("MSFT", FY2026_Q4, DocType.PRESS_RELEASE,
                    "Microsoft total revenue grew. Total revenue was strong, part " + i + ".", vector(1, 0.01f * i));
        }
        for (FiscalPeriod p : List.of(FY2026_Q2, FY2026_Q3, FY2027_Q1)) {
            insertChunk("MSFT", p, DocType.PRESS_RELEASE, "Microsoft commentary with revenue for " + p, vector(0.5f, 0.5f));
            insertChunk("MSFT", p, DocType.TRANSCRIPT, "Microsoft call remarks about demand for " + p, vector(0.4f, 0.6f));
        }
        for (FiscalPeriod p : List.of(FY2026_Q1, FY2026_Q2, FY2026_Q3)) {
            insertChunk("GOOGL", p, DocType.PRESS_RELEASE, "Alphabet revenue for " + p, vector(0.3f, 0.7f));
        }
    }

    @Test
    void threeQuarterQuestionReturnsChunksFromAllThreeQuarters() {
        RetrievalResult result = retriever(0.0).retrieve(LAST_THREE, SearchFilters.none(), RetrievalMode.C);

        assertThat(result.strategy()).isEqualTo(RetrievalResult.Strategy.PER_PERIOD);
        assertThat(result.searchedTargets()).extracting(PeriodTarget::period)
                .containsExactly(FY2026_Q3, FY2026_Q4, FY2027_Q1);
        assertThat(periods(result)).containsExactlyInAnyOrder(FY2026_Q3, FY2026_Q4, FY2027_Q1);
        assertThat(result.chunks()).hasSizeLessThanOrEqualTo(9);   // 3 targets x k-per-period 3
        assertThat(result.chunks()).extracting(FusedHit::rank).startsWith(1, 2, 3);
        assertThat(result.abstained()).isFalse();
    }

    @Test
    void withoutPerPeriodOneQuarterCrowdsOutTheOthers() {
        // Config B: hybrid, but no parser filters. Shows why per-period retrieval exists.
        RetrievalResult result = retriever(0.0).retrieve(LAST_THREE, SearchFilters.none(), RetrievalMode.B);

        assertThat(result.strategy()).isEqualTo(RetrievalResult.Strategy.SINGLE);
        assertThat(result.chunks()).hasSize(8);
        assertThat(periods(result)).containsExactly(FY2026_Q4);
    }

    @Test
    void perPeriodResultsTakeTurnsByRank() {
        RetrievalResult result = retriever(0.0).retrieve(LAST_THREE, SearchFilters.none(), RetrievalMode.C);

        // The first three chunks are the best chunk of each quarter, in target order.
        assertThat(result.chunks().subList(0, 3)).extracting(h -> h.chunk().fiscalQuarter() + "/" + h.chunk().fiscalYear())
                .containsExactly("3/2026", "4/2026", "1/2027");
    }

    @Test
    void singleQuarterQuestionUsesOneFilteredSearch() {
        RetrievalResult result = retriever(0.0).retrieve("Microsoft revenue in Q3 FY2026", SearchFilters.none(), RetrievalMode.C);

        assertThat(result.strategy()).isEqualTo(RetrievalResult.Strategy.SINGLE);
        assertThat(result.searchedTargets()).containsExactly(new PeriodTarget("MSFT", FY2026_Q3));
        assertThat(periods(result)).containsExactly(FY2026_Q3);
        assertThat(result.chunks()).extracting(h -> h.chunk().ticker()).containsOnly("MSFT");
    }

    @Test
    void companyWithoutPeriodFiltersByTickerOnly() {
        RetrievalResult result = retriever(0.0).retrieve("What did Alphabet say about revenue?", SearchFilters.none(), RetrievalMode.C);

        assertThat(result.strategy()).isEqualTo(RetrievalResult.Strategy.SINGLE);
        assertThat(result.chunks()).hasSize(3);
        assertThat(result.chunks()).extracting(h -> h.chunk().ticker()).containsOnly("GOOGL");
    }

    @Test
    void periodNotInCorpusAbstainsWithoutCallingOpenAi() {
        RetrievalResult result = retriever(0.0).retrieve("What was Microsoft's revenue in Q1 FY2025?", SearchFilters.none(), RetrievalMode.C);

        assertThat(result.abstainReason()).isEqualTo(RetrievalResult.AbstainReason.PERIOD_NOT_IN_CORPUS);
        assertThat(result.missing()).containsExactly(new PeriodTarget("MSFT", new FiscalPeriod(2025, 1)));
        assertThat(result.chunks()).isEmpty();
        assertThat(result.strategy()).isEqualTo(RetrievalResult.Strategy.NONE);
        assertThat(embedCalls.get()).isZero();
    }

    @Test
    void explicitFiltersOverrideTheParser() {
        // The question says FY2026 Q3, the explicit filter says FY2027 Q1 and transcripts only: the filter wins.
        SearchFilters explicit = SearchFilters.none().withPeriods(FY2027_Q1).withDocTypes(DocType.TRANSCRIPT);

        RetrievalResult result = retriever(0.0).retrieve("Microsoft revenue in Q3 FY2026", explicit, RetrievalMode.C);

        assertThat(periods(result)).containsExactly(FY2027_Q1);
        assertThat(result.chunks()).extracting(h -> h.chunk().docType()).containsOnly(DocType.TRANSCRIPT);
    }

    @Test
    void explicitFiltersApplyInModesAAndBToo() {
        SearchFilters explicit = SearchFilters.tickers("GOOGL");

        assertThat(retriever(0.0).retrieve(LAST_THREE, explicit, RetrievalMode.A).chunks())
                .extracting(h -> h.chunk().ticker()).containsOnly("GOOGL");
        assertThat(retriever(0.0).retrieve(LAST_THREE, explicit, RetrievalMode.B).chunks())
                .extracting(h -> h.chunk().ticker()).containsOnly("GOOGL");
    }

    @Test
    void modeAIsVectorOnly() {
        RetrievalResult result = retriever(0.0).retrieve(LAST_THREE, SearchFilters.none(), RetrievalMode.A);

        assertThat(result.chunks()).hasSize(8);
        assertThat(result.chunks()).allSatisfy(h -> assertThat(h.keywordRank()).isNull());
        assertThat(result.chunks()).allSatisfy(h -> assertThat(h.vectorRank()).isNotNull());
    }

    @Test
    void tooManyTargetsAreCappedWithBothCompaniesKept() {
        RetrievalResult result = retriever(0.0).retrieve(
                "Microsoft and Alphabet revenue over the last three quarters", SearchFilters.none(), RetrievalMode.C);

        // 6 targets (3 per company), max-periods 4: newest first, companies taking turns.
        assertThat(result.searchedTargets()).containsExactly(
                new PeriodTarget("GOOGL", FY2026_Q2), new PeriodTarget("GOOGL", FY2026_Q3),
                new PeriodTarget("MSFT", FY2026_Q4), new PeriodTarget("MSFT", FY2027_Q1));
        assertThat(result.skippedTargets()).containsExactly(
                new PeriodTarget("GOOGL", FY2026_Q1), new PeriodTarget("MSFT", FY2026_Q3));
    }

    @Test
    void modeDAbstainsBelowTheSimilarityThreshold() {
        // Only FY2027 Q1 is searched; its best chunk is (0.5, 0.5): similarity 0.707 to the query.
        String question = "Microsoft revenue in Q1 FY2027";

        RetrievalResult strict = retriever(0.9).retrieve(question, SearchFilters.none(), RetrievalMode.D);
        assertThat(strict.abstainReason()).isEqualTo(RetrievalResult.AbstainReason.LOW_SIMILARITY);
        assertThat(strict.bestSimilarity()).isCloseTo(Math.sqrt(0.5), org.assertj.core.api.Assertions.within(1e-4));
        assertThat(strict.chunks()).isNotEmpty();   // still returned, for eval

        assertThat(retriever(0.5).retrieve(question, SearchFilters.none(), RetrievalMode.D).abstained()).isFalse();
        // Mode C never uses the threshold.
        assertThat(retriever(0.9).retrieve(question, SearchFilters.none(), RetrievalMode.C).abstained()).isFalse();
    }

    // ---- helpers ----

    private Retriever retriever(double minSimilarity) {
        RetrievalProperties settings = new RetrievalProperties(8, 3, 4, 30, 30, 60, minSimilarity, RetrievalMode.D);
        QueryEmbedder fake = question -> {
            embedCalls.incrementAndGet();
            return vector(1, 0);
        };
        return new Retriever(parser, resolver, vectorSearch, keywordSearch, fake, settings);
    }

    private static Set<FiscalPeriod> periods(RetrievalResult result) {
        return result.chunks().stream()
                .map(h -> new FiscalPeriod(h.chunk().fiscalYear(), h.chunk().fiscalQuarter()))
                .collect(Collectors.toSet());
    }
}
