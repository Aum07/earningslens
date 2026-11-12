import { Component, DestroyRef, computed, inject, signal } from '@angular/core';
import { takeUntilDestroyed } from '@angular/core/rxjs-interop';
import { ApiService } from '../../core/api.service';
import { fixed2, percent, usd4 } from '../../core/format';
import { ApiError, ConfigSummary, LatestEvalReport, LiveConfig } from '../../core/models';
import { ErrorPanel } from '../../shared/error-panel/error-panel';
import { Loading } from '../../shared/loading/loading';
import { ScoreCards } from './score-cards/score-cards';

/** What each ablation config is (spec 11.4). Words only: every number on this page comes from latest.json. */
export const CONFIG_DESCRIPTIONS: Record<string, string> = {
  A: 'Vector search only (baseline)',
  B: 'Hybrid: vector + keyword search, fused with RRF',
  C: 'B + filters from the query parser + per-quarter retrieval',
  D: 'C + evidence threshold: declines when the best match is too weak',
};

interface Column {
  header: string;
  cell: (c: ConfigSummary) => string;
}

interface ColumnGroup {
  title: string;
  columns: Column[];
}

/** DESIGN.md progress bar rule: gold at or above 85%, red with a "weaker" pill below. A design rule, not a metric. */
export const WEAKER_BELOW = 0.85;

/** One score card for the live config. */
export interface ScoreCard {
  label: string;
  /** Formatted from the report, like the tables. */
  value: string;
  /** Share of the bar to fill (0 to 1); null for "lower is better" metrics, which get no bar. */
  ratio: number | null;
  /** Bar width in percent, rounded to one decimal (no floating-point noise in the style). */
  width: number;
  weaker: boolean;
  /** Short words for the "Where it's weaker" callout. Never a number: the number is `value`. */
  why: string;
  note?: string;
}

/**
 * The score cards, in order: retrieval, answer quality, abstention. Words only; every value is read from the report.
 * `why` follows docs/error-analysis.md.
 */
const CARDS: { label: string; get: (c: ConfigSummary) => number | null; format: 'fixed2' | 'percent' | 'of5'; lowerIsBetter?: boolean; why: string }[] = [
  { label: 'Recall@k (all quotes)', get: (c) => c.retrieval.recallAll, format: 'fixed2',
    why: 'counts only the exact gold quote, and the same figure is often in a press release, a 10-Q and a transcript, so a right answer can still count as a miss' },
  { label: 'Faithfulness', get: (c) => c.generation.faithfulness, format: 'fixed2',
    why: 'some answer claims were not supported by the cited sources' },
  { label: 'Citation precision', get: (c) => c.generation.citationPrecision, format: 'percent',
    why: 'some citations do not support the sentence they are attached to' },
  { label: 'Number grounding', get: (c) => c.generation.numberGroundingRate, format: 'percent',
    why: 'some numbers in answers are not in the cited sources' },
  { label: 'Correctness', get: (c) => c.generation.correctness, format: 'of5',
    why: 'some answers differ from the reference answers' },
  { label: 'Abstention precision', get: (c) => c.abstention.precision, format: 'percent',
    why: 'some of the questions it declined could have been answered' },
  { label: 'Abstention recall', get: (c) => c.abstention.recall, format: 'percent',
    why: 'it answered some questions the documents cannot answer' },
  { label: 'False abstention', get: (c) => c.abstention.falseAbstentionRate, format: 'percent', lowerIsBetter: true,
    why: '' },
];

/**
 * Evaluation page (spec section 12, page 3): the ablation table from GET /api/v1/eval/latest (latest.json,
 * spec rule 7) and which row the live API runs, from GET /api/v1/eval/live-config.
 */
@Component({
  selector: 'app-evaluation-page',
  imports: [ErrorPanel, Loading, ScoreCards],
  templateUrl: './evaluation-page.html',
  styleUrl: './evaluation-page.scss',
})
export class EvaluationPage {
  private readonly api = inject(ApiService);
  private readonly destroyRef = inject(DestroyRef);

  readonly report = signal<LatestEvalReport | null>(null);
  readonly error = signal<ApiError | null>(null);
  readonly live = signal<LiveConfig | null>(null);
  readonly liveError = signal(false);

  readonly descriptions = CONFIG_DESCRIPTIONS;
  readonly percent = percent;
  readonly fixed2 = fixed2;

  /**
   * Ablation columns, in the order of the committed ablation report, split into the metric groups of spec 11.2 so
   * each table fits the page without sideways scrolling.
   */
  readonly groups: ColumnGroup[] = [
    {
      title: 'Retrieval',
      columns: [
        { header: 'Recall@k (all quotes)', cell: (c) => fixed2(c.retrieval.recallAll) },
        { header: 'Recall@k (any quote)', cell: (c) => fixed2(c.retrieval.recallAny) },
        { header: 'MRR', cell: (c) => fixed2(c.retrieval.mrr) },
        { header: 'Period coverage', cell: (c) => fixed2(c.retrieval.periodCoverage) },
      ],
    },
    {
      title: 'Answer quality',
      columns: [
        { header: 'Faithfulness', cell: (c) => fixed2(c.generation.faithfulness) },
        { header: 'Fully faithful', cell: (c) => percent(c.generation.fullyFaithful) },
        { header: 'Citation precision', cell: (c) => percent(c.generation.citationPrecision) },
        { header: 'Number grounding', cell: (c) => percent(c.generation.numberGroundingRate) },
        { header: 'Correctness (1 to 5)', cell: (c) => fixed2(c.generation.correctness) },
      ],
    },
    {
      title: 'Abstention and operations',
      columns: [
        { header: 'Abstention precision', cell: (c) => percent(c.abstention.precision) },
        { header: 'Abstention recall', cell: (c) => percent(c.abstention.recall) },
        { header: 'False abstention', cell: (c) => percent(c.abstention.falseAbstentionRate) },
        { header: 'Latency p50 / p95 (ms)', cell: (c) => `${c.operations.latencyP50Ms} / ${c.operations.latencyP95Ms}` },
        { header: 'Cost per query', cell: (c) => usd4(c.operations.meanCostUsd) },
      ],
    },
  ];

  readonly configNames = Object.keys(CONFIG_DESCRIPTIONS);

  /**
   * Question counts, read from the report. Every config ran the same items; a config's own answerable count can be
   * lower when an item errored, so answerable = items - unanswerable.
   */
  readonly counts = computed(() => {
    const configs = this.report()?.configs ?? [];
    if (configs.length === 0) return null;
    const items = configs[0].items;
    const unanswerable = Math.max(...configs.map((c) => c.abstention.unanswerable));
    return { items, answerable: items - unanswerable, unanswerable };
  });

  /** The live config's row from the report (null until both the report and the live settings are loaded). */
  readonly liveRow = computed(() => {
    const mode = this.live()?.retrievalMode;
    return this.report()?.configs.find((c) => c.config === mode) ?? null;
  });

  /** Score cards for the live config, with DESIGN.md's 85% rule. */
  readonly cards = computed<ScoreCard[]>(() => {
    const row = this.liveRow();
    if (!row) return [];
    return CARDS.map((card) => {
      const raw = card.get(row);
      const ratio = raw === null || card.lowerIsBetter ? null : card.format === 'of5' ? raw / 5 : raw;
      return {
        label: card.label,
        value: card.format === 'percent' ? percent(raw) : fixed2(raw),
        ratio,
        width: ratio === null ? 0 : Math.round(Math.min(Math.max(ratio, 0), 1) * 1000) / 10,
        weaker: ratio !== null && ratio < WEAKER_BELOW,
        why: card.why,
        note: card.lowerIsBetter ? 'lower is better' : card.format === 'of5' ? 'out of 5' : undefined,
      };
    });
  });

  readonly totalErrors = computed(() => (this.report()?.configs ?? []).reduce((sum, c) => sum + c.errors, 0));

  constructor() {
    this.load();
  }

  load(): void {
    this.error.set(null);
    this.report.set(null);
    this.api
      .latestEval()
      .pipe(takeUntilDestroyed(this.destroyRef))
      .subscribe({ next: (r) => this.report.set(r), error: (e: ApiError) => this.error.set(e) });
    this.liveError.set(false);
    this.api
      .liveConfig()
      .pipe(takeUntilDestroyed(this.destroyRef))
      .subscribe({ next: (c) => this.live.set(c), error: () => this.liveError.set(true) });
  }

  isLive(c: ConfigSummary): boolean {
    return this.live()?.retrievalMode === c.config;
  }
}
