/**
 * TypeScript mirrors of the chat DTOs in backend/src/main/java/com/earningslens/api/dto (spec section 10).
 * An `interface` only describes the shape of JSON; it adds no code to the build.
 */

/** Matches the Java enum com.earningslens.ingest.DocType. */
export type DocType = 'PRESS_RELEASE' | 'TRANSCRIPT' | 'FORM_10Q' | 'FORM_10K';

export const DOC_TYPES: readonly DocType[] = ['PRESS_RELEASE', 'TRANSCRIPT', 'FORM_10Q', 'FORM_10K'];

/** FiscalPeriodDto: { "year": 2026, "quarter": 1 }. */
export interface FiscalPeriod {
  year: number;
  quarter: number;
}

/** ChatFilters. In a request every field is optional; in a response the backend always sends all three lists. */
export interface ChatFilters {
  tickers?: string[];
  fiscalPeriods?: FiscalPeriod[];
  docTypes?: DocType[];
}

/** Body of POST /api/v1/chat. `question` must be 3 to 500 characters. */
export interface ChatRequest {
  question: string;
  filters?: ChatFilters;
}

/** CitationDto: one source cited in the answer as [marker]. */
export interface Citation {
  marker: number;
  chunkId: string;
  ticker: string;
  fiscalYear: number;
  fiscalQuarter: number;
  docType: DocType;
  /** First page of the chunk (PDF only), else null. */
  page: number | null;
  section: string | null;
  speaker: string | null;
  sourceUrl: string;
  /** First 300 characters of the chunk text. */
  snippet: string;
}

/** Response of POST /api/v1/chat. */
export interface ChatResponse {
  /** The answer with [n] markers, or a plain message when abstained. */
  answer: string;
  abstained: boolean;
  /** One per marker used, in order of first use; empty when abstained. */
  citations: Citation[];
  groundingWarnings: string[];
  appliedFilters: Required<ChatFilters>;
  latencyMs: number;
  /** The model that answered, or null if the model was not called. */
  model: string | null;
}

/** MAX and MIN question length, same as @Size(min = 3, max = 500) on ChatRequest.question. */
export const QUESTION_MIN_LENGTH = 3;
export const QUESTION_MAX_LENGTH = 500;
