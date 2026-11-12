package com.earningslens.figures;

import com.earningslens.api.CorpusService;
import com.earningslens.api.dto.CompanyDto;
import com.earningslens.api.dto.FiscalPeriodDto;
import com.earningslens.api.dto.KeyFiguresDto;
import com.earningslens.eval.FiscalPeriod;
import com.earningslens.retrieval.Company;
import com.earningslens.retrieval.CompanyDirectory;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import tools.jackson.databind.json.JsonMapper;

import java.io.IOException;
import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Key figures per company for the quarters in the corpus, from the SEC's free XBRL API.
 *
 * Results are cached in memory for CACHE_TTL per company, so the SEC is called at most a few times a day.
 * If the SEC cannot be reached, that company is returned with available=false (and the last good copy is kept),
 * so one failure never breaks the page.
 *
 * JAVA CONCEPT, ConcurrentHashMap: a map that several request threads can read and write at the same time.
 * JAVA CONCEPT, Clock: "what time is it now", passed in so tests can move time forward without waiting.
 */
@Service
public class KeyFiguresService {

    private static final Logger log = LoggerFactory.getLogger(KeyFiguresService.class);
    static final Duration CACHE_TTL = Duration.ofHours(6);
    static final String FILING_URL = "https://www.sec.gov/Archives/edgar/data/%s/%s/";

    private final CorpusService corpus;
    private final CompanyDirectory companies;
    private final SecFactsClient sec;
    private final Clock clock;
    private final JsonMapper json = JsonMapper.builder().build();
    private final Map<String, Cached> cache = new ConcurrentHashMap<>();

    private record Cached(Instant fetchedAt, Map<FiscalPeriod, QuarterFigures> figures) {
    }

    // Two constructors: @Autowired tells Spring to use this one (the other lets tests pass a fixed clock).
    @Autowired
    public KeyFiguresService(CorpusService corpus, CompanyDirectory companies, SecFactsClient sec) {
        this(corpus, companies, sec, Clock.systemUTC());
    }

    KeyFiguresService(CorpusService corpus, CompanyDirectory companies, SecFactsClient sec, Clock clock) {
        this.corpus = corpus;
        this.companies = companies;
        this.sec = sec;
        this.clock = clock;
    }

    /** One entry per company that has documents in the corpus, in companies.yaml order. */
    public List<KeyFiguresDto> keyFigures() {
        List<KeyFiguresDto> result = new ArrayList<>();
        for (CompanyDto company : corpus.companies()) {
            Company details = companies.byTicker(company.ticker());
            String cik = details == null ? null : details.cik();
            String sourceUrl = cik == null ? null : HttpSecFactsClient.URL.formatted(cik);
            Cached cached = cik == null ? null : load(company.ticker(), cik, company.fiscalYearEndMonth());
            if (cached == null) {
                result.add(new KeyFiguresDto(company.ticker(), company.name(), false, sourceUrl, null, List.of()));
                continue;
            }
            List<KeyFiguresDto.Quarter> quarters = new ArrayList<>();
            for (FiscalPeriodDto p : company.periods()) {
                QuarterFigures q = cached.figures().get(new FiscalPeriod(p.year(), p.quarter()));
                if (q != null) {
                    quarters.add(toDto(q, cik));
                }
            }
            result.add(new KeyFiguresDto(company.ticker(), company.name(), true, sourceUrl, cached.fetchedAt(),
                    quarters));
        }
        return result;
    }

    /** Fresh cache entry, a new download, or (if the download fails) the last good entry or null. */
    private Cached load(String ticker, String cik, int fiscalYearEndMonth) {
        Instant now = clock.instant();
        Cached cached = cache.get(ticker);
        if (cached != null && cached.fetchedAt().plus(CACHE_TTL).isAfter(now)) {
            return cached;
        }
        try {
            Map<FiscalPeriod, QuarterFigures> figures =
                    KeyFiguresParser.parse(json.readTree(sec.companyFacts(cik)), fiscalYearEndMonth);
            Cached fresh = new Cached(now, figures);
            cache.put(ticker, fresh);
            return fresh;
        } catch (IOException | RuntimeException e) {
            log.warn("Could not load SEC key figures for {}: {}", ticker, e.getMessage());
            return cached;
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            return cached;
        }
    }

    private static KeyFiguresDto.Quarter toDto(QuarterFigures q, String cik) {
        String filingUrl = q.accessionNumber() == null || q.accessionNumber().isBlank() ? null
                : FILING_URL.formatted(Long.parseLong(cik), q.accessionNumber().replace("-", ""));
        return new KeyFiguresDto.Quarter(q.fiscalYear(), q.fiscalQuarter(), q.periodEnd(), q.revenue(),
                q.netIncome(), q.epsDiluted(), q.derived(), q.form(), q.filed(), filingUrl);
    }
}
