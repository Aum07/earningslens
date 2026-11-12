import { Component, computed, input } from '@angular/core';
import { DOC_TYPE_LABELS, periodLabel } from '../../core/labels';
import { Citation } from '../../core/models';

/** Same as the backend's snippet length (spec section 10: first 300 characters of the chunk). */
export const SNIPPET_LENGTH = 300;

/**
 * One cited source (spec section 12: ticker, period, doc type, page or section, speaker, snippet, link).
 * input.required(): a value the parent must pass in, read like a signal: citation().
 * The host element gets id "citation-<marker>" so a [n] badge can find, scroll to and focus it.
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

  readonly period = computed(() =>
    periodLabel({ year: this.citation().fiscalYear, quarter: this.citation().fiscalQuarter }),
  );
  readonly docType = computed(() => DOC_TYPE_LABELS[this.citation().docType] ?? this.citation().docType);
  /** The backend sends the first SNIPPET_LENGTH characters; "..." shows when the text was cut there. */
  readonly snippet = computed(() => {
    const text = this.citation().snippet;
    return text.length >= SNIPPET_LENGTH ? `${text.trimEnd()}...` : text;
  });

  readonly location = computed(() => {
    const c = this.citation();
    return [c.page !== null ? `Page ${c.page}` : null, c.section, c.speaker ? `Speaker: ${c.speaker}` : null]
      .filter((part): part is string => !!part)
      .join(' | ');
  });
}
