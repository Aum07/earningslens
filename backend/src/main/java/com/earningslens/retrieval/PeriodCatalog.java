package com.earningslens.retrieval;

import com.earningslens.eval.FiscalPeriod;

import java.util.Map;
import java.util.NavigableSet;

/** The fiscal periods present in the corpus, per ticker. PeriodResolver works against this. */
public interface PeriodCatalog {

    Map<String, NavigableSet<FiscalPeriod>> periodsByTicker();
}
