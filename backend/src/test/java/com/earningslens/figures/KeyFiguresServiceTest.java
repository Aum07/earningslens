package com.earningslens.figures;

import com.earningslens.api.CorpusService;
import com.earningslens.api.dto.CompanyDto;
import com.earningslens.api.dto.FiscalPeriodDto;
import com.earningslens.api.dto.KeyFiguresDto;
import com.earningslens.retrieval.Company;
import com.earningslens.retrieval.CompanyDirectory;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.time.ZoneId;
import java.time.ZoneOffset;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

/** Caching, failure handling and the corpus-quarter filter. The SEC is never called (fake client). */
class KeyFiguresServiceTest {

    private static final String CIK = "0000789019";

    /** Two quarters as the SEC would send them: FY2026 Q2 (in the corpus) and FY2026 Q1 (not in the corpus). */
    private static final String FACTS = """
            {"facts":{"us-gaap":{
              "RevenueFromContractWithCustomerExcludingAssessedTax":{"units":{"USD":[
                {"start":"2025-07-01","end":"2025-09-30","val":77673000000,"accn":"0000950170-25-000001","form":"10-Q","filed":"2025-10-29"},
                {"start":"2025-10-01","end":"2025-12-31","val":81273000000,"accn":"0000950170-26-000002","form":"10-Q","filed":"2026-01-28"}
              ]}}
            }}}""";

    /** A clock the test can move forward. */
    static final class TestClock extends Clock {
        Instant now = Instant.parse("2026-11-05T12:00:00Z");

        @Override public ZoneId getZone() { return ZoneOffset.UTC; }
        @Override public Clock withZone(ZoneId zone) { return this; }
        @Override public Instant instant() { return now; }
    }

    /** Counts calls and can be told to fail. */
    static final class FakeSec implements SecFactsClient {
        int calls;
        boolean fail;

        @Override
        public String companyFacts(String cik) throws IOException {
            calls++;
            if (fail) {
                throw new IOException("HTTP 503");
            }
            return FACTS;
        }
    }

    private final FakeSec sec = new FakeSec();
    private final TestClock clock = new TestClock();
    private KeyFiguresService service;

    @BeforeEach
    void setUp() {
        CorpusService corpus = mock(CorpusService.class);
        when(corpus.companies()).thenReturn(List.of(new CompanyDto("MSFT", "Microsoft Corporation", 6,
                List.of(new FiscalPeriodDto(2026, 2), new FiscalPeriodDto(2026, 3)))));
        CompanyDirectory companies = new CompanyDirectory(
                List.of(new Company("MSFT", "Microsoft Corporation", 6, List.of("Microsoft"), CIK)));
        service = new KeyFiguresService(corpus, companies, sec, clock);
    }

    @Test
    void returnsOnlyCorpusQuartersWithALinkToTheFiling() {
        KeyFiguresDto msft = service.keyFigures().get(0);

        assertThat(msft.available()).isTrue();
        assertThat(msft.sourceUrl()).isEqualTo("https://data.sec.gov/api/xbrl/companyfacts/CIK0000789019.json");
        assertThat(msft.fetchedAt()).isEqualTo(clock.now);
        // FY2026 Q1 is not in the corpus; FY2026 Q3 is in the corpus but has no SEC figure here
        assertThat(msft.quarters()).hasSize(1);
        KeyFiguresDto.Quarter q2 = msft.quarters().get(0);
        assertThat(q2.fiscalQuarter()).isEqualTo(2);
        assertThat(q2.revenue()).isEqualByComparingTo("81273000000");
        assertThat(q2.filingUrl()).isEqualTo("https://www.sec.gov/Archives/edgar/data/789019/000095017026000002/");
    }

    @Test
    void cachesPerCompanyAndRefreshesAfterTheTtl() {
        service.keyFigures();
        service.keyFigures();
        assertThat(sec.calls).isEqualTo(1);

        clock.now = clock.now.plus(KeyFiguresService.CACHE_TTL).plus(Duration.ofMinutes(1));
        service.keyFigures();
        assertThat(sec.calls).isEqualTo(2);
    }

    @Test
    void aFailureWithNoCopyYetIsUnavailableNotAnError() {
        sec.fail = true;

        KeyFiguresDto msft = service.keyFigures().get(0);

        assertThat(msft.available()).isFalse();
        assertThat(msft.quarters()).isEmpty();
        assertThat(msft.ticker()).isEqualTo("MSFT");
    }

    @Test
    void aFailureAfterASuccessKeepsTheLastGoodCopy() {
        Instant first = clock.now;
        service.keyFigures();
        sec.fail = true;
        clock.now = clock.now.plus(KeyFiguresService.CACHE_TTL).plus(Duration.ofMinutes(1));

        KeyFiguresDto msft = service.keyFigures().get(0);

        assertThat(msft.available()).isTrue();
        assertThat(msft.fetchedAt()).isEqualTo(first);
        assertThat(sec.calls).isEqualTo(2);
    }
}
