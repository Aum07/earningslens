package com.earningslens.retrieval;

import com.earningslens.PgvectorContainerTest;
import com.earningslens.eval.FiscalPeriod;
import com.earningslens.ingest.DocType;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.dao.InvalidDataAccessApiUsageException;

import java.sql.SQLException;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/**
 * Roadmap E3.2: keyword (full-text) search with filters, against the shared pgvector test container.
 * Embeddings do not matter here, so every chunk gets the same one.
 */
class KeywordSearchRepositoryTest extends PgvectorContainerTest {

    private static final FiscalPeriod FY2026_Q3 = new FiscalPeriod(2026, 3);
    private static final FiscalPeriod FY2026_Q4 = new FiscalPeriod(2026, 4);
    private static final FiscalPeriod FY2027_Q1 = new FiscalPeriod(2027, 1);

    private static final String AZURE = "Azure revenue grew 39% driven by demand for AI infrastructure.";
    private static final String MSFT_CALL = "We continue to see strong demand across our cloud and AI offerings, and cloud revenue was strong.";
    private static final String GEMINI = "Google Cloud revenue increased, with strong demand for Gemini models and cloud infrastructure.";
    private static final String META_ADS = "Family of Apps revenue grew as advertising impressions increased.";
    private static final String WAYMO = "Waymo continues to expand its autonomous ride-hailing service.";

    @Autowired KeywordSearchRepository repository;

    @BeforeEach
    void setUp() throws SQLException {
        clearDatabase();
        float[] same = vector(1, 0);
        insertChunk("MSFT", FY2027_Q1, DocType.PRESS_RELEASE, AZURE, same);
        insertChunk("MSFT", FY2026_Q4, DocType.TRANSCRIPT, MSFT_CALL, same);
        insertChunk("GOOGL", FY2026_Q3, DocType.TRANSCRIPT, GEMINI, same);
        insertChunk("META", FY2026_Q3, DocType.PRESS_RELEASE, META_ADS, same);
        insertChunk("GOOGL", FY2026_Q3, DocType.FORM_10Q, WAYMO, same);
    }

    @Test
    void productNameRanksItsChunkFirst() {
        assertThat(contents(repository.search("How is Waymo expanding?", SearchFilters.none(), 30)))
                .first().isEqualTo(WAYMO);
        assertThat(contents(repository.search("Which Gemini models did Google mention?", SearchFilters.none(), 30)))
                .first().isEqualTo(GEMINI);
    }

    @Test
    void wholeQuestionMatchesEvenWhenNoChunkHasEveryWord() {
        // No chunk contains all of azure, revenue, growth, q1, fy2027. With plain AND this finds nothing.
        List<KeywordHit> hits = repository.search("What was Azure revenue growth in Q1 FY2027?", SearchFilters.none(), 30);

        assertThat(hits).isNotEmpty();
        assertThat(hits.get(0).chunk().content()).isEqualTo(AZURE);
    }

    @Test
    void ranksStartAtOneAndScoresGoDown() {
        List<KeywordHit> hits = repository.search("cloud revenue demand", SearchFilters.none(), 30);

        assertThat(hits).hasSizeGreaterThanOrEqualTo(3);
        assertThat(hits).extracting(KeywordHit::rank).startsWith(1, 2, 3);
        assertThat(hits).extracting(KeywordHit::score).isSortedAccordingTo((a, b) -> Double.compare(b, a));
        assertThat(hits).allSatisfy(h -> assertThat(h.score()).isPositive());
    }

    @Test
    void tickerFilterExcludesOtherTickers() {
        // The GOOGL Gemini chunk matches "cloud revenue demand" well, but must not appear.
        List<KeywordHit> hits = repository.search("cloud revenue demand", SearchFilters.tickers("MSFT"), 30);

        assertThat(contents(hits)).containsExactlyInAnyOrder(AZURE, MSFT_CALL);
        assertThat(hits).extracting(h -> h.chunk().ticker()).containsOnly("MSFT");
    }

    @Test
    void periodAndDocTypeFiltersAreCombinedWithAnd() {
        SearchFilters filters = SearchFilters.none().withPeriods(FY2026_Q3).withDocTypes(DocType.PRESS_RELEASE);

        assertThat(contents(repository.search("revenue", filters, 30))).containsExactly(META_ADS);
    }

    @Test
    void quotedPhraseStaysAPhrase() {
        // "AI infrastructure" in the Azure chunk must not count as "cloud infrastructure".
        assertThat(contents(repository.search("\"cloud infrastructure\"", SearchFilters.none(), 30)))
                .containsExactly(GEMINI);
    }

    @Test
    void leadingMinusIsNotTreatedAsNot() {
        // With "-cloud" read as NOT cloud, OR would match every chunk without "cloud", such as Meta's.
        List<String> found = contents(repository.search("Waymo -cloud", SearchFilters.none(), 30));

        assertThat(found).contains(WAYMO);
        assertThat(found).doesNotContain(META_ADS);
    }

    @Test
    void noMatchingWordsReturnsEmptyList() {
        assertThat(repository.search("Nintendo Switch", SearchFilters.none(), 30)).isEmpty();
        assertThat(repository.search("what is the", SearchFilters.none(), 30)).isEmpty();   // only stop words
        assertThat(repository.search("Waymo", SearchFilters.tickers("MSFT"), 30)).isEmpty();
    }

    @Test
    void limitKeepsOnlyTheBest() {
        assertThat(repository.search("cloud revenue demand", SearchFilters.none(), 1)).hasSize(1);
    }

    @Test
    void rejectsBadArguments() {
        assertThatThrownBy(() -> repository.search("  ", SearchFilters.none(), 5))
                .isInstanceOf(InvalidDataAccessApiUsageException.class)
                .hasRootCauseInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("question is empty");
        assertThatThrownBy(() -> repository.search("revenue", SearchFilters.none(), 0))
                .isInstanceOf(InvalidDataAccessApiUsageException.class)
                .hasRootCauseInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("limit must be positive");
    }

    @Test
    void withoutNegationOnlyRemovesMinusAtTheStartOfAWord() {
        assertThat(KeywordSearchRepository.withoutNegation("Waymo -cloud")).isEqualTo("Waymo cloud");
        assertThat(KeywordSearchRepository.withoutNegation("-cloud")).isEqualTo("cloud");
        assertThat(KeywordSearchRepository.withoutNegation("year-over-year growth")).isEqualTo("year-over-year growth");
        assertThat(KeywordSearchRepository.withoutNegation("revenue - costs")).isEqualTo("revenue - costs");
    }

    private static List<String> contents(List<KeywordHit> hits) {
        return hits.stream().map(h -> h.chunk().content()).toList();
    }
}
