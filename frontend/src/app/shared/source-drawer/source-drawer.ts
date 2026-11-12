import { DIALOG_DATA, Dialog, DialogRef } from '@angular/cdk/dialog';
import { createGlobalPositionStrategy } from '@angular/cdk/overlay';
import { Component, Injector, computed, inject } from '@angular/core';
import { DOC_TYPE_LABELS, periodLabel } from '../../core/labels';
import { Citation } from '../../core/models';
import { SNIPPET_LENGTH } from '../citation-card/citation-card';

/** What the drawer shows: the source, and the answer sentences that cite it. */
export interface SourceDrawerData {
  citation: Citation;
  sentences: string[];
}

/**
 * The source drawer (DESIGN.md sections 5 and 6, restyle screen 3): 460px, slides in from the right over a scrim.
 * It opens with the CDK Dialog service. CDK Dialog: Angular's building block for pop-ups; it traps keyboard focus
 * inside, closes on Esc or a scrim click, and puts focus back on the button that opened it.
 * Content: a doc-meta box, the passage (the backend's snippet: the first 300 characters of the chunk, so not the
 * exact cited sentence), "Supports this sentence" (from the answer text), and the action buttons.
 */
@Component({
  selector: 'app-source-drawer',
  templateUrl: './source-drawer.html',
  styleUrl: './source-drawer.scss',
})
export class SourceDrawer {
  /** inject(DIALOG_DATA): the data passed to Dialog.open(). */
  readonly data = inject<SourceDrawerData>(DIALOG_DATA);
  private readonly ref = inject(DialogRef);

  readonly c = this.data.citation;
  readonly docType = DOC_TYPE_LABELS[this.c.docType] ?? this.c.docType;

  /** Key/value rows for the doc-meta box; empty values are left out. */
  readonly meta = computed(() =>
    [
      { key: 'Company', value: this.c.ticker },
      { key: 'Fiscal quarter', value: periodLabel({ year: this.c.fiscalYear, quarter: this.c.fiscalQuarter }) },
      { key: 'Document', value: this.docType },
      { key: 'Page', value: this.c.page !== null ? String(this.c.page) : null },
      { key: 'Section', value: this.c.section },
      { key: 'Speaker', value: this.c.speaker },
    ].filter((row): row is { key: string; value: string } => !!row.value),
  );

  /** "..." shows when the backend cut the text at SNIPPET_LENGTH characters. */
  readonly cut = this.c.snippet.length >= SNIPPET_LENGTH;
  readonly snippet = this.cut ? `${this.c.snippet.trimEnd()}...` : this.c.snippet;

  close(): void {
    this.ref.close();
  }

  /** Opens the drawer for one source. Returns the DialogRef, so callers and tests can wait for it to close. */
  static open(dialog: Dialog, injector: Injector, data: SourceDrawerData): DialogRef<unknown, SourceDrawer> {
    return dialog.open(SourceDrawer, {
      data,
      positionStrategy: createGlobalPositionStrategy(injector).right('0').top('0'),
      ariaLabelledBy: 'source-drawer-title',
      backdropClass: 'drawer-scrim',
      panelClass: 'drawer-panel',
      width: '460px',
      maxWidth: '100vw',
      height: '100%',
      autoFocus: 'dialog',
    });
  }
}
