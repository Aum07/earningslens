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
import static org.assertj.core.api.Assertions.within;

/**
 * Roadmap E3.1: vector search with filters, against the shared pgvector test container.
 *
 * Hand-made embeddings: only the first two of the 1536 numbers are set, so cosine distances
 * are easy to work out by hand. The query points along the first axis.
 */
class VectorSearchRepositoryTest extends PgvectorContainerTest {

    private static final float[] QUERY = vector(1, 0);

    private static final FiscalPeriod FY2026_Q1 = new FiscalPeriod(2026, 1);
    private static final FiscalPeriod FY2026_Q3 = new FiscalPeriod(2026, 3);
    private static final FiscalPeriod FY2026_Q4 = new FiscalPeriod(2026, 4);
    private static final FiscalPeriod FY2027_Q1 = new FiscalPeriod(2027, 1);

    @Autowired VectorSearchRepository repository;

    @BeforeEach
    void setUp() throws SQLException {
        clearDatabase();
        // Distances from QUERY = (1, 0), worked out by hand:
        insertChunk("GOOGL", FY2026_Q1, DocType.TRANSCRIPT, "google exact", vector(1, 0));               // 0
        insertChunk("MSFT", FY2027_Q1, DocType.PRESS_RELEASE, "msft close", vector(0.9f, 0.1f));       // 0.0061
        insertChunk("MSFT", FY2026_Q4, DocType.TRANSCRIPT, "msft diagonal", vector(1, 1));             // 0.2929
        insertChunk("MSFT", FY2026_Q4, DocType.FORM_10K, "msft orthogonal", vector(0, 1));             // 1
        insertChunk("META", FY2026_Q3, DocType.FORM_10Q, "meta opposite", vector(-1, 0), 7, 8, true);  // 2
    }

    @Test
    void noFiltersReturnsEveryChunkNearestFirstWithRanks() {
        List<VectorHit> hits = repository.search(QUERY, SearchFilters.none(), 30);

        assertThat(hits).extracting(h -> h.chunk().content()).containsExactly(
                "google exact", "msft close", "msft diagonal", "msft orthogonal", "meta opposite");
        assertThat(hits).extracting(VectorHit::rank).containsExactly(1, 2, 3, 4, 5);
        assertThat(hits.get(0).distance()).isCloseTo(0.0, within(1e-4));
        assertThat(hits.get(1).distance()).isCloseTo(1 - 0.9 / Math.sqrt(0.82), within(1e-4));
        assertThat(hits.get(2).distance()).isCloseTo(1 - 1 / Math.sqrt(2), within(1e-4));
        assertThat(hits.get(3).distance()).isCloseTo(1.0, within(1e-4));
        assertThat(hits.get(4).distance()).isCloseTo(2.0, within(1e-4));
        assertThat(hits.get(0).similarity()).isCloseTo(1.0, within(1e-4));
    }

    @Test
    void tickerFilterExcludesOtherTickersEvenWhenTheyAreCloser() {
        // The GOOGL chunk is an exact match for the query, so it would rank first without the filter.
        List<VectorHit> hits = repository.search(QUERY, SearchFilters.tickers("MSFT"), 30);

        assertThat(hits).hasSize(3);
        assertThat(hits).extracting(h -> h.chunk().ticker()).containsOnly("MSFT");
        assertThat(hits).extracting(h -> h.chunk().content())
                .containsExactly("msft close", "msft diagonal", "msft orthogonal");
        assertThat(hits).extracting(VectorHit::rank).containsExactly(1, 2, 3);   // ranks restart after filtering
    }

    @Test
    void severalTickersAreCombinedWithOr() {
        List<VectorHit> hits = repository.search(QUERY, SearchFilters.tickers("MSFT", "META"), 30);

        assertThat(hits).hasSize(4);
        assertThat(hits).extracting(h -> h.chunk().ticker()).doesNotContain("GOOGL");
    }

    @Test
    void periodFilterKeepsOnlyThatFiscalQuarter() {
        List<VectorHit> hits = repository.search(QUERY, SearchFilters.none().withPeriods(FY2026_Q4), 30);

        assertThat(hits).extracting(h -> h.chunk().content()).containsExactly("msft diagonal", "msft orthogonal");
    }

    @Test
    void severalPeriodsAreCombinedWithOr() {
        List<VectorHit> hits = repository.search(QUERY, SearchFilters.none().withPeriods(FY2026_Q1, FY2027_Q1), 30);

        assertThat(hits).extracting(h -> h.chunk().content()).containsExactly("google exact", "msft close");
    }

    @Test
    void docTypeFilterKeepsOnlyThoseTypes() {
        List<VectorHit> hits = repository.search(QUERY, SearchFilters.none().withDocTypes(DocType.TRANSCRIPT), 30);

        assertThat(hits).extracting(h -> h.chunk().content()).containsExactly("google exact", "msft diagonal");
    }

    @Test
    void allFiltersTogetherAreCombinedWithAnd() {
        SearchFilters filters = SearchFilters.tickers("MSFT").withPeriods(FY2026_Q4).withDocTypes(DocType.FORM_10K);

        List<VectorHit> hits = repository.search(QUERY, filters, 30);

        assertThat(hits).extracting(h -> h.chunk().content()).containsExactly("msft orthogonal");
    }

    @Test
    void filterThatMatchesNothingReturnsEmptyList() {
        assertThat(repository.search(QUERY, SearchFilters.tickers("AMZN"), 30)).isEmpty();
        assertThat(repository.search(QUERY, SearchFilters.tickers("GOOGL").withPeriods(FY2026_Q4), 30)).isEmpty();
    }

    @Test
    void limitKeepsOnlyTheNearest() {
        List<VectorHit> hits = repository.search(QUERY, SearchFilters.none(), 2);

        assertThat(hits).extracting(h -> h.chunk().content()).containsExactly("google exact", "msft close");
    }

    @Test
    void mapsEveryColumn() {
        VectorHit hit = repository.search(QUERY, SearchFilters.none().withDocTypes(DocType.FORM_10Q), 1).get(0);
        RetrievedChunk chunk = hit.chunk();

        assertThat(chunk.id()).isNotNull();
        assertThat(chunk.documentId()).isNotNull();
        assertThat(chunk.chunkIndex()).isZero();
        assertThat(chunk.ticker()).isEqualTo("META");
        assertThat(chunk.fiscalYear()).isEqualTo(2026);
        assertThat(chunk.fiscalQuarter()).isEqualTo(3);
        assertThat(chunk.docType()).isEqualTo(DocType.FORM_10Q);
        assertThat(chunk.tokenCount()).isEqualTo(42);
        assertThat(chunk.pageStart()).isEqualTo(7);
        assertThat(chunk.pageEnd()).isEqualTo(8);
        assertThat(chunk.section()).isEqualTo("Section for meta opposite");
        assertThat(chunk.speaker()).isNull();
        assertThat(chunk.forwardLooking()).isTrue();
    }

    @Test
    void rejectsBadArguments() {
        // @Repository makes Spring wrap exceptions in its DataAccessException family:
        // our IllegalArgumentException arrives as InvalidDataAccessApiUsageException.
        assertThatThrownBy(() -> repository.search(new float[0], SearchFilters.none(), 5))
                .isInstanceOf(InvalidDataAccessApiUsageException.class)
                .hasRootCauseInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("query embedding is empty");
        assertThatThrownBy(() -> repository.search(QUERY, SearchFilters.none(), 0))
                .isInstanceOf(InvalidDataAccessApiUsageException.class)
                .hasRootCauseInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("limit must be positive");
    }

    @Test
    void pgvectorIsNewEnoughForIterativeIndexScans() {
        // hnsw.iterative_scan (used by the repository) arrived in pgvector 0.8.0.
        String version = jdbc.queryForObject("SELECT extversion FROM pg_extension WHERE extname = 'vector'", String.class);
        String[] parts = version.split("\\.");
        int major = Integer.parseInt(parts[0]);
        int minor = Integer.parseInt(parts[1]);
        assertThat(major > 0 || minor >= 8).as("pgvector version %s", version).isTrue();
    }
}
