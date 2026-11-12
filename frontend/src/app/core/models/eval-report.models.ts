/**
 * Shape of eval/reports/latest.json, returned unchanged by GET /api/v1/eval/latest.
 * Mirrors FullReportWriter.Latest, FullEvalMetrics.Summary, RetrievalMetrics.Summary and AgreementMetrics.Summary.
 *
 * Spec rule 7: the UI shows only numbers from this report. Never type a metric into a template.
 * Java `Double` (can be null) is `number | null` here; Java `double` / `int` / `long` is `number`.
 */

export interface LatestEvalReport {
  date: string;
  /** Which questions the numbers cover ("test" for the final run). */
  split: string;
  /** Per-config report files, e.g. 2026-11-04-A-test.json. */
  reports: string[];
  ablationReport: string;
  settings: EvalSettings;
  configs: ConfigSummary[];
  judgeAgreement: JudgeAgreement;
}

export interface EvalSettings {
  k: number;
  kPerPeriod: number;
  maxPeriods: number;
  minSimilarity: number;
  chunkTargetTokens: number;
  chunkOverlapTokens: number;
  corpusChunks: number;
  chatModel: string;
  embeddingModel: string;
  judgeModel: string;
  rubricPromptSha256: string;
}

/** One ablation row: config A, B, C or D. */
export interface ConfigSummary {
  config: string;
  items: number;
  errors: number;
  retrieval: RetrievalSummary;
  retrievalByCategory: Record<string, RetrievalSummary>;
  generation: GenerationSummary;
  abstention: AbstentionSummary;
  operations: OperationsSummary;
}

export interface RetrievalSummary {
  answerableItems: number;
  recallAll: number;
  recallAny: number;
  mrr: number;
  requiredPeriods: number;
  coveredPeriods: number;
  /** null when no item in the group names periods. */
  periodCoverage: number | null;
  avgRetrieved: number;
}

export interface GenerationSummary {
  judged: number;
  faithfulness: number | null;
  fullyFaithful: number | null;
  citationPrecision: number | null;
  citationPairs: number;
  numberGroundingRate: number | null;
  numbers: number;
  correctness: number | null;
  correctnessAnsweredAnswerable: number | null;
  correctnessByCategory: Record<string, number | null>;
}

export interface AbstentionSummary {
  unanswerable: number;
  unanswerableAbstained: number;
  answerable: number;
  answerableAbstained: number;
  precision: number | null;
  recall: number | null;
  falseAbstentionRate: number | null;
  byReason: Record<string, number>;
}

export interface OperationsSummary {
  queries: number;
  latencyP50Ms: number;
  latencyP95Ms: number;
  meanCostUsd: number;
  totalCostUsd: number;
  judgeCostUsd: number;
}

export interface JudgeAgreement {
  /** Report file of the judge-versus-human comparison (dev split). */
  report: string;
  agreement: AgreementSummary;
  /** True when the judge rubric used in the test run is the one the agreement was measured with. */
  sameRubric: boolean | null;
}

export interface AgreementSummary {
  n: number;
  faithfulness: {
    bothYes: number;
    bothNo: number;
    humanYesJudgeNo: number;
    humanNoJudgeYes: number;
    agreement: number;
    kappa: number | null;
  };
  correctness: {
    exact: number;
    withinOne: number;
    meanJudgeMinusHuman: number;
    /** matrix[h - 1][j - 1] = answers the person scored h and the judge scored j. */
    matrix: number[][];
  };
}

/** GET /api/v1/eval/live-config: what the live chat API runs (LiveConfigDto), read from the server's settings. */
export interface LiveConfig {
  retrievalMode: string;
  chatModel: string;
  embeddingModel: string;
}
