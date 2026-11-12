import { Dialog } from '@angular/cdk/dialog';
import { Component, ElementRef, Injector, computed, inject, input, linkedSignal } from '@angular/core';
import { sentencesCiting, splitAnswer } from '../../../core/answer-segments';
import { DOC_TYPE_LABELS, periodLabel } from '../../../core/labels';
import { ChatResponse, KeyFigures } from '../../../core/models';
import { CitationCard } from '../../../shared/citation-card/citation-card';
import { KeyFiguresView } from '../../../shared/key-figures/key-figures';
import { SourceDrawer } from '../../../shared/source-drawer/source-drawer';

/**
 * Shows one chat response (spec section 12): the answer with clickable [n] badges and the citation panel, or a
 * neutral abstention panel, plus a quiet notice for grounding warnings. Under an answer it shows the official SEC
 * key figures of the companies the answer cites, with the cited quarters highlighted. The abstention panel shows only the
 * backend's message: it already says what was searched, or which quarter is not in the corpus.
 */
@Component({
  selector: 'app-answer-panel',
  imports: [CitationCard, KeyFiguresView],
  templateUrl: './answer-panel.html',
  styleUrl: './answer-panel.scss',
})
export class AnswerPanel {
  private readonly host = inject<ElementRef<HTMLElement>>(ElementRef);
  private readonly dialog = inject(Dialog);
  private readonly injector = inject(Injector);

  readonly response = input.required<ChatResponse>();
  /** All companies' SEC key figures (null while loading or when they could not be loaded). */
  readonly keyFigures = input<KeyFigures[] | null>(null);

  /** The highlighted source. linkedSignal: goes back to null whenever a different response comes in. */
  readonly selected = linkedSignal<ChatResponse, number | null>({
    source: this.response,
    computation: () => null,
  });

  readonly segments = computed(() =>
    splitAnswer(this.response().answer, new Set(this.response().citations.map((c) => c.marker))),
  );

  /** Key figures of the companies the answer cites, in the order they are first cited. */
  readonly citedFigures = computed(() => {
    const all = this.keyFigures();
    if (!all || this.response().abstained) return [];
    const tickers = [...new Set(this.response().citations.map((c) => c.ticker))];
    return tickers.map((t) => all.find((k) => k.ticker === t)).filter((k): k is KeyFigures => !!k);
  });

  /** Quarters the answer cites, as "MSFT:2026-2", for highlighting in the chart and table. */
  readonly citedQuarters = computed(
    () => new Set(this.response().citations.map((c) => `${c.ticker}:${c.fiscalYear}-${c.fiscalQuarter}`)),
  );

  readonly seconds = computed(() => (this.response().latencyMs / 1000).toFixed(1));

  markerLabel(marker: number): string {
    const c = this.response().citations.find((x) => x.marker === marker);
    return c
      ? `Source ${marker}: ${c.ticker} ${periodLabel({ year: c.fiscalYear, quarter: c.fiscalQuarter })} ${DOC_TYPE_LABELS[c.docType] ?? c.docType}`
      : `Source ${marker}`;
  }

  /** Badge click: highlight the card, scroll it into view and move keyboard focus to it. */
  select(marker: number): void {
    this.selected.set(marker);
    const card = this.host.nativeElement.querySelector<HTMLElement>(`#citation-${marker}`);
    if (!card) return;
    // scrollIntoView is missing in the test DOM (jsdom), hence the ?.
    card.scrollIntoView?.({ behavior: 'smooth', block: 'nearest' });
    card.focus({ preventScroll: true });
  }

  /** "View passage" on a source row: the source drawer, with the answer sentences that cite this source. */
  openSource(marker: number): void {
    const citation = this.response().citations.find((c) => c.marker === marker);
    if (!citation) return;
    this.selected.set(marker);
    SourceDrawer.open(this.dialog, this.injector, {
      citation,
      sentences: sentencesCiting(this.response().answer, marker),
    });
  }
}
