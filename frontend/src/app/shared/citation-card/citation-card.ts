import { Component, computed, input, output } from '@angular/core';
import { DOC_TYPE_LABELS, periodLabel } from '../../core/labels';
import { Citation } from '../../core/models';

/** Same as the backend's snippet length (spec section 10: first 300 characters of the chunk). */
export const SNIPPET_LENGTH = 300;

/**
 * One cited source as a DESIGN.md source row: gold number box, title (document type) and mono meta (ticker, fiscal
 * quarter, page or section, speaker), and "View passage", which asks the parent to open the source drawer.
 * output(): an event the parent listens to, like (viewPassage)="...".
 * input.required(): a value the parent must pass in, read like a signal: citation().
 * The host element gets id "citation-<marker>" so a [n] marker can find, scroll to and focus it.
 */
@Component({
  selector: 'app-citation-card',
  templateUrl: './citation-card.html',
  styleUrl: './citation-card.scss',
  host: {
    '[id]': '"citation-" + citation().marker',
    '[class.highlighted]': 'highlighted()',
    '[attr.aria-label]': '"Source " + citation().marker',
    role: 'article',
    tabindex: '-1',
  },
})
export class CitationCard {
  readonly citation = input.required<Citation>();
  readonly highlighted = input(false);

  /** "View passage" was pressed: the parent opens the source drawer. */
  readonly viewPassage = output<void>();

  readonly docType = computed(() => DOC_TYPE_LABELS[this.citation().docType] ?? this.citation().docType);

  /** Mono meta line: "META · FY2026 Q3 · Page 7 · Susan Li, CFO". */
  readonly meta = computed(() => {
    const c = this.citation();
    return [
      c.ticker,
      periodLabel({ year: c.fiscalYear, quarter: c.fiscalQuarter }),
      c.page !== null ? `Page ${c.page}` : null,
      c.section,
      c.speaker,
    ]
      .filter((part): part is string => !!part)
      .join(' · ');
  });
}
