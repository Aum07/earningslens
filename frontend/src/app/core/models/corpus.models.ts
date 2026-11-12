import { DocType, FiscalPeriod } from './chat.models';

/** DocumentDto: one document in the corpus (GET /api/v1/documents). */
export interface CorpusDocument {
  id: string;
  ticker: string;
  companyName: string;
  fiscalYear: number;
  fiscalQuarter: number;
  docType: DocType;
  sourceUrl: string;
  chunkCount: number;
}

/** CompanyDto: one company and its quarters (GET /api/v1/companies). */
export interface Company {
  ticker: string;
  name: string;
  /** Month the fiscal year ends: 6 for Microsoft, 12 for Alphabet and Meta. */
  fiscalYearEndMonth: number;
  /** Quarters in the corpus, oldest first. */
  periods: FiscalPeriod[];
}

/** GET /api/v1/key-figures: official quarterly figures from SEC EDGAR XBRL data (KeyFiguresDto). */
export interface KeyFigures {
  ticker: string;
  name: string;
  /** false when the SEC data could not be loaded right now (quarters is then empty). */
  available: boolean;
  sourceUrl: string | null;
  fetchedAt: string | null;
  /** Corpus quarters only, oldest first. */
  quarters: QuarterFigures[];
}

export interface QuarterFigures {
  fiscalYear: number;
  fiscalQuarter: number;
  periodEnd: string;
  /** US dollars. */
  revenue: number | null;
  netIncome: number | null;
  /** US dollars per share; null for derived quarters. */
  epsDiluted: number | null;
  /** Fourth quarter calculated as full year minus nine months (10-K filings give Q4 only as a full year). */
  derived: boolean;
  form: string;
  filed: string;
  filingUrl: string | null;
}
