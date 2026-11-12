package com.earningslens.figures;

import java.io.IOException;

/** Downloads a company's XBRL "company facts" JSON from SEC EDGAR. An interface so tests never call the SEC. */
public interface SecFactsClient {

    /**
     * @param cik the company's 10-digit Central Index Key, with leading zeros
     * @return the JSON text of data.sec.gov/api/xbrl/companyfacts/CIK{cik}.json
     */
    String companyFacts(String cik) throws IOException, InterruptedException;
}
