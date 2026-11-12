import { DecimalPipe } from '@angular/common';
import { Component, DestroyRef, computed, inject, signal } from '@angular/core';
import { takeUntilDestroyed } from '@angular/core/rxjs-interop';
import { ApiService } from '../../core/api.service';
import { DOC_TYPE_LABELS, periodLabel } from '../../core/labels';
import { ApiError, CorpusDocument } from '../../core/models';
import { ErrorPanel } from '../../shared/error-panel/error-panel';
import { Loading } from '../../shared/loading/loading';

/** Corpus page (spec section 12, page 2): every document by company and quarter, from GET /api/v1/documents. */
@Component({
  selector: 'app-corpus-page',
  imports: [DecimalPipe, ErrorPanel, Loading],
  templateUrl: './corpus-page.html',
  styleUrl: './corpus-page.scss',
})
export class CorpusPage {
  private readonly api = inject(ApiService);
  private readonly destroyRef = inject(DestroyRef);

  readonly documents = signal<CorpusDocument[] | null>(null);
  readonly error = signal<ApiError | null>(null);

  /** Totals counted from the list itself (no typed-in numbers). */
  readonly summary = computed(() => {
    const docs = this.documents() ?? [];
    return {
      documents: docs.length,
      chunks: docs.reduce((sum, d) => sum + d.chunkCount, 0),
      companies: new Set(docs.map((d) => d.ticker)).size,
    };
  });

  readonly docTypeLabels = DOC_TYPE_LABELS;

  constructor() {
    this.load();
  }

  load(): void {
    this.error.set(null);
    this.documents.set(null);
    this.api
      .documents()
      .pipe(takeUntilDestroyed(this.destroyRef))
      .subscribe({
        next: (docs) => this.documents.set(docs),
        error: (e: ApiError) => this.error.set(e),
      });
  }

  period(d: CorpusDocument): string {
    return periodLabel({ year: d.fiscalYear, quarter: d.fiscalQuarter });
  }
}
