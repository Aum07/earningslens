import { Component, computed, input, signal } from '@angular/core';
import { usd2, usdCompact } from '../../core/format';
import { calendarMonths, periodLabel, shortPeriodLabel } from '../../core/labels';
import { KeyFigures, QuarterFigures } from '../../core/models';

/** The three figures the panel can chart (DESIGN.md section 6: Revenue / Net income / EPS tabs). */
export type Metric = 'revenue' | 'netIncome' | 'epsDiluted';

export const METRICS: { key: Metric; label: string }[] = [
  { key: 'revenue', label: 'Revenue' },
  { key: 'netIncome', label: 'Net income' },
  { key: 'epsDiluted', label: 'EPS' },
];

/** Chart geometry in SVG units, about the width it is drawn at in the 420px panel, so SVG text keeps its CSS size. */
const W = 370;
const H = 168;
const TOP = 22; // room for the value labels above the bars
const BOTTOM = 24; // room for the quarter labels
const PLOT_H = H - TOP - BOTTOM;

interface Bar {
  key: string;
  label: string;
  x: number;
  width: number;
  y: number;
  path: string;
  value: string;
  derived: boolean;
  /** The answer cites this quarter for this company. */
  cited: boolean;
}

interface Row {
  key: string;
  label: string;
  months: string;
  derived: boolean;
  cited: boolean;
  q: QuarterFigures;
}

/**
 * Official key figures per company (GET /api/v1/key-figures, SEC EDGAR data), styled as the DESIGN.md
 * official-figures panel: a segmented control picks the figure, a bar chart shows it per fiscal quarter (inactive
 * bars grey, the quarters the answer cites gold), and a table lists all three figures with the cited rows tinted gold.
 * Derived fourth quarters are hatched in the chart and starred in the table. The blue note gives the calendar months of
 * the cited quarters, as the DESIGN.md content rules ask.
 * The chart is drawn for sighted users; the table carries the same numbers for screen readers.
 */
@Component({
  selector: 'app-key-figures',
  templateUrl: './key-figures.html',
  styleUrl: './key-figures.scss',
})
export class KeyFiguresView {
  readonly figures = input.required<KeyFigures[]>();
  /** Quarters to highlight, as "MSFT:2026-2". */
  readonly cited = input<ReadonlySet<string>>(new Set());

  readonly metrics = METRICS;
  /** The figure the chart shows. signal(): a value the template re-reads whenever it changes. */
  readonly metric = signal<Metric>('revenue');

  readonly width = W;
  readonly height = H;
  readonly baseline = H - BOTTOM;
  readonly usdCompact = usdCompact;
  readonly usd2 = usd2;

  readonly metricLabel = computed(() => METRICS.find((m) => m.key === this.metric())!.label);

  /** Largest value of the chosen figure across the companies shown: one shared scale, so bars compare. */
  private readonly maxValue = computed(() => {
    const m = this.metric();
    return Math.max(Number.EPSILON, ...this.figures().flatMap((c) => c.quarters.map((q) => q[m] ?? 0)));
  });

  readonly anyDerived = computed(() => this.figures().some((c) => c.quarters.some((q) => q.derived)));

  readonly companies = computed(() =>
    this.figures().map((company) => {
      const isCited = (q: QuarterFigures) =>
        this.cited().has(`${company.ticker}:${q.fiscalYear}-${q.fiscalQuarter}`);
      const rows: Row[] = company.quarters.map((q) => ({
        key: `${q.fiscalYear}-${q.fiscalQuarter}`,
        label: periodLabel({ year: q.fiscalYear, quarter: q.fiscalQuarter }),
        months: calendarMonths(q.periodEnd),
        derived: q.derived,
        cited: isCited(q),
        q,
      }));
      // Calendar note: the cited quarters, or the latest one when the answer cites none of this company's quarters.
      const noted = rows.some((r) => r.cited) ? rows.filter((r) => r.cited) : rows.slice(-1);
      return {
        company,
        rows,
        bars: this.bars(company, isCited),
        calendarNote: noted.map((r) => `${company.ticker} ${r.label} = ${r.months}`).join('; '),
      };
    }),
  );

  private bars(company: KeyFigures, isCited: (q: QuarterFigures) => boolean): Bar[] {
    const m = this.metric();
    const n = Math.max(company.quarters.length, 1);
    const slot = W / n;
    const width = Math.min(40, slot * 0.56);
    return company.quarters.map((q, i) => {
      const raw = q[m];
      const value = Math.max(raw ?? 0, 0); // a loss is shown by its label; the bar stays on the baseline
      const height = raw === null ? 0 : Math.max(value > 0 ? 2 : 0, (value / this.maxValue()) * PLOT_H);
      const x = slot * i + (slot - width) / 2;
      const y = this.baseline - height;
      return {
        key: `${company.ticker}:${i}`,
        label: shortPeriodLabel({ year: q.fiscalYear, quarter: q.fiscalQuarter }),
        x,
        width,
        y,
        path: height > 0 ? KeyFiguresView.roundedTop(x, y, width, height, 6) : '',
        value: m === 'epsDiluted' ? usd2(raw) : usdCompact(raw),
        derived: q.derived,
        cited: isCited(q),
      };
    });
  }

  /** A bar with 6px rounded top corners (DESIGN.md: radius 6) and a square base on the baseline. */
  static roundedTop(x: number, y: number, w: number, h: number, r: number): string {
    const rr = Math.min(r, w / 2, h);
    return `M${x},${y + h} V${y + rr} Q${x},${y} ${x + rr},${y} H${x + w - rr} Q${x + w},${y} ${x + w},${y + rr} V${y + h} Z`;
  }
}
