import { Component, computed, input, signal } from '@angular/core';
import { usd2, usdCompact } from '../../core/format';
import { periodLabel } from '../../core/labels';
import { KeyFigures, QuarterFigures } from '../../core/models';

/** Chart geometry in SVG units (the SVG scales to the card width). */
const W = 520; // about the width the chart is drawn at, so SVG text stays near its CSS size
const H = 190;
const TOP = 26; // room for the value label above the tallest bar
const BOTTOM = 26; // room for the quarter labels
const PLOT_H = H - TOP - BOTTOM;

interface Bar {
  key: string;
  label: string;
  x: number;
  width: number;
  y: number;
  height: number;
  path: string;
  value: string;
  derived: boolean;
  /** The answer cites this quarter for this company. */
  cited: boolean;
  /** Show the value above the bar (cited bars, or the latest bar when nothing is cited). */
  labelled: boolean;
  aria: string;
  q: QuarterFigures;
}

/**
 * Official key figures per company (from GET /api/v1/key-figures, SEC EDGAR data): a small revenue bar chart and a
 * table with revenue, net income and diluted EPS for each corpus quarter. The companies shown share one revenue
 * scale, so bar heights can be compared. Derived fourth quarters are hatched and marked "derived". Quarters the
 * answer cites (input `cited`, keys "TICKER:year-quarter") are shown in amber and marked "cited".
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

  readonly width = W;
  readonly height = H;
  readonly baseline = H - BOTTOM;
  readonly usdCompact = usdCompact;
  readonly usd2 = usd2;
  readonly periodLabel = periodLabel;

  /** The bar under the pointer or keyboard focus, as "TICKER:index". */
  readonly active = signal<string | null>(null);

  /** Largest revenue across all companies: the shared scale. */
  private readonly maxRevenue = computed(() =>
    Math.max(1, ...this.figures().flatMap((c) => c.quarters.map((q) => q.revenue ?? 0))),
  );

  readonly anyDerived = computed(() => this.figures().some((c) => c.quarters.some((q) => q.derived)));

  readonly charts = computed(() =>
    this.figures().map((company) => ({ company, bars: this.bars(company) })),
  );

  private bars(company: KeyFigures): Bar[] {
    const n = Math.max(company.quarters.length, 1);
    const slot = W / n;
    const width = Math.min(44, slot * 0.55);
    const citedKeys = this.cited();
    const isCited = (q: QuarterFigures) => citedKeys.has(`${company.ticker}:${q.fiscalYear}-${q.fiscalQuarter}`);
    const anyCited = company.quarters.some(isCited);
    return company.quarters.map((q, i) => {
      const cited = isCited(q);
      const value = q.revenue ?? 0;
      const height = Math.max(value > 0 ? 2 : 0, (value / this.maxRevenue()) * PLOT_H);
      const x = slot * i + (slot - width) / 2;
      const y = H - BOTTOM - height;
      const label = periodLabel({ year: q.fiscalYear, quarter: q.fiscalQuarter });
      return {
        key: `${company.ticker}:${i}`,
        label,
        x,
        width,
        y,
        height,
        path: KeyFiguresView.roundedTop(x, y, width, height, 4),
        value: usdCompact(q.revenue),
        derived: q.derived,
        cited,
        labelled: anyCited ? cited : i === company.quarters.length - 1,
        aria:
          `${company.ticker} ${label} revenue ${usdCompact(q.revenue)}` +
          `${q.derived ? ', derived from the annual report' : ''}${cited ? ', cited in the answer' : ''}`,
        q,
      };
    });
  }

  /** A bar with 4px rounded top corners and a square base on the baseline. */
  static roundedTop(x: number, y: number, w: number, h: number, r: number): string {
    const rr = Math.min(r, w / 2, h);
    return `M${x},${y + h} V${y + rr} Q${x},${y} ${x + rr},${y} H${x + w - rr} Q${x + w},${y} ${x + w},${y + rr} V${y + h} Z`;
  }

  /** Position of the tooltip, as a share of the chart width (so it follows the SVG when it scales). */
  tipLeft(bar: Bar): string {
    return `${((bar.x + bar.width / 2) / W) * 100}%`;
  }

  tipTop(bar: Bar): string {
    return `${(bar.y / H) * 100}%`;
  }
}
