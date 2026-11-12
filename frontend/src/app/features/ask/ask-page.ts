import { Component, DestroyRef, ElementRef, computed, effect, inject, signal, untracked } from '@angular/core';
import { takeUntilDestroyed, toSignal } from '@angular/core/rxjs-interop';
import { FormControl, ReactiveFormsModule } from '@angular/forms';
import { MatButtonModule } from '@angular/material/button';
import { MatFormFieldModule } from '@angular/material/form-field';
import { MatInputModule } from '@angular/material/input';
import { MatSelectModule } from '@angular/material/select';
import { ApiService } from '../../core/api.service';
import { HistoryEntry, SessionHistory } from '../../core/session-history';
import { DOC_TYPE_LABELS, monthName, parsePeriodKey, periodKey, periodLabel } from '../../core/labels';
import {
  ApiError,
  ChatRequest,
  ChatResponse,
  Company,
  KeyFigures,
  DOC_TYPES,
  DocType,
  QUESTION_MAX_LENGTH,
  QUESTION_MIN_LENGTH,
} from '../../core/models';
import { Loading } from '../../shared/loading/loading';
import { ErrorPanel } from '../../shared/error-panel/error-panel';
import { AnswerPanel } from './answer-panel/answer-panel';
import { buildChatRequest } from './chat-request';
import { SAMPLE_QUESTIONS } from './sample-questions';

interface PeriodOption {
  key: string;
  label: string;
}

export type { HistoryEntry } from '../../core/session-history';

/**
 * The Ask page (spec section 12, page 1). E6.3: filters, sample questions, question input.
 * E6.4: answer with citations, abstention panel, loading and error states, session history sidebar.
 *
 * signal(): a value that tells Angular when it changes, so the page re-renders. computed(): a signal derived
 * from other signals, recalculated automatically. FormControl: holds one form field's value (reactive forms).
 */
@Component({
  selector: 'app-ask-page',
  imports: [
    ReactiveFormsModule,
    MatButtonModule,
    MatFormFieldModule,
    MatInputModule,
    MatSelectModule,
    AnswerPanel,
    ErrorPanel,
    Loading,
  ],
  templateUrl: './ask-page.html',
  styleUrl: './ask-page.scss',
})
export class AskPage {
  private readonly api = inject(ApiService);
  private readonly session = inject(SessionHistory);
  private readonly host = inject<ElementRef<HTMLElement>>(ElementRef);
  private readonly destroyRef = inject(DestroyRef);

  readonly samples = SAMPLE_QUESTIONS;
  readonly docTypeOptions = DOC_TYPES.map((value) => ({ value, label: DOC_TYPE_LABELS[value] }));
  readonly minLength = QUESTION_MIN_LENGTH;
  readonly maxLength = QUESTION_MAX_LENGTH;

  // Form fields. nonNullable: reset() goes back to the initial value instead of null.
  readonly question = new FormControl('', { nonNullable: true });
  readonly tickers = new FormControl<string[]>([], { nonNullable: true });
  readonly periods = new FormControl<string[]>([], { nonNullable: true }); // period keys, e.g. "2026-2"
  readonly docTypes = new FormControl<DocType[]>([], { nonNullable: true });

  readonly companies = signal<Company[]>([]);
  /** Official SEC figures for all companies, loaded once; the answer panel picks the cited ones. */
  readonly keyFigures = signal<KeyFigures[] | null>(null);
  readonly companiesError = signal<string | null>(null);
  /** The filter dropdowns' panel (opened from the scope chips until the Filters modal exists). */
  readonly filtersOpen = signal(false);
  readonly loading = signal(false);
  /** The answer on screen: the selected session entry (set after a question, or picked in the sidebar). */
  readonly response = computed<ChatResponse | null>(() => this.session.selected()?.response ?? null);
  /** The question of the answer on screen (shown as the Answer page heading). */
  readonly shownQuestion = computed(() => this.session.selected()?.question ?? null);
  /** Home view (hero + search) when nothing is loading, failed or shown; otherwise the Answer view. */
  readonly isHome = computed(() => !this.loading() && !this.error() && !this.response());
  readonly error = signal<ApiError | null>(null);
  readonly history = this.session.entries;
  readonly currentId = this.session.selectedId;
  private lastRequest: ChatRequest | null = null;

  // toSignal: turns the field's stream of changes into a signal the template can read.
  private readonly questionText = toSignal(this.question.valueChanges, { initialValue: this.question.value });
  readonly length = computed(() => this.questionText().length);
  readonly tooShort = computed(() => {
    const trimmed = this.questionText().trim().length;
    return trimmed > 0 && trimmed < QUESTION_MIN_LENGTH;
  });
  readonly canAsk = computed(() => {
    const trimmed = this.questionText().trim().length;
    return trimmed >= QUESTION_MIN_LENGTH && this.length() <= QUESTION_MAX_LENGTH && !this.loading();
  });

  /** Every quarter held for any company, oldest first. Each company's own fiscal calendar (see fiscalNote). */
  readonly periodOptions = computed<PeriodOption[]>(() => {
    const byKey = new Map<string, PeriodOption & { sort: number }>();
    for (const company of this.companies()) {
      for (const p of company.periods) {
        byKey.set(periodKey(p), { key: periodKey(p), label: periodLabel(p), sort: p.year * 10 + p.quarter });
      }
    }
    return [...byKey.values()].sort((a, b) => a.sort - b.sort).map(({ key, label }) => ({ key, label }));
  });

  /**
   * The Companies dropdown: every supported company with the quarters held, from /companies (not typed in), so the
   * dropdown also tells users what is covered. Periods arrive oldest first.
   */
  readonly companyOptions = computed(() =>
    this.companies().map((c) => ({
      ticker: c.ticker,
      name: c.name,
      range: c.periods.length
        ? `${periodLabel(c.periods[0])} to ${periodLabel(c.periods[c.periods.length - 1])}`
        : 'no quarters yet',
    })),
  );

  private readonly selectedPeriods = toSignal(this.periods.valueChanges, { initialValue: this.periods.value });
  private readonly selectedDocTypes = toSignal(this.docTypes.valueChanges, { initialValue: this.docTypes.value });
  private readonly selectedTickerList = toSignal(this.tickers.valueChanges, { initialValue: this.tickers.value });

  /** Row 2 of the search box: what the next question will search, as chips (gold when narrowed). */
  readonly scopeChips = computed(() => {
    const t = this.selectedTickerList();
    const p = this.selectedPeriods();
    const d = this.selectedDocTypes();
    return [
      { key: 'companies', text: t.length ? t.join(', ') : 'All companies', active: t.length > 0, mono: t.length > 0 },
      {
        key: 'quarters',
        text: p.length ? p.map((k) => periodLabel(parsePeriodKey(k))).join(', ') : 'All quarters',
        active: p.length > 0,
        mono: p.length > 0,
      },
      {
        key: 'docs',
        text: d.length ? d.map((x) => DOC_TYPE_LABELS[x]).join(', ') : 'All documents',
        active: d.length > 0,
        mono: false,
      },
    ];
  });

  toggleFilters(): void {
    this.filtersOpen.update((open) => !open);
  }

  /** Explains that quarters are fiscal, using fiscalYearEndMonth from /companies (not hard-coded). */
  readonly fiscalNote = computed(() => {
    const parts = this.companies().map((c) => `${c.name} ${monthName(c.fiscalYearEndMonth)}`);
    return parts.length === 0
      ? null
      : `Quarters are each company's own fiscal quarters. Fiscal year ends: ${parts.join(', ')}.`;
  });

  constructor() {
    effect(() => {
      if (this.session.newQuestionCount() === 0) return;
      untracked(() => {
        this.error.set(null);
        this.question.setValue('');
        queueMicrotask(() => this.host.nativeElement.querySelector<HTMLTextAreaElement>('textarea')?.focus());
      });
    });
    this.api
      .companies()
      .pipe(takeUntilDestroyed()) // stop listening if the page is closed
      .subscribe({
        next: (companies) => this.companies.set(companies),
        error: (e: ApiError) => this.companiesError.set(e.message),
      });
    // Failure is quiet: the answer simply shows no SEC figures.
    this.api
      .keyFigures()
      .pipe(takeUntilDestroyed())
      .subscribe({ next: (k) => this.keyFigures.set(k), error: () => this.keyFigures.set(null) });
  }

  /** A sample chip only fills the box. It never sends: every question is a paid model call. */
  useSample(text: string): void {
    this.question.setValue(text);
  }

  ask(): void {
    if (!this.canAsk()) return;
    this.send(
      buildChatRequest(this.question.value, {
        tickers: this.tickers.value,
        fiscalPeriods: this.periods.value.map(parsePeriodKey),
        docTypes: this.docTypes.value,
      }),
    );
  }

  /** "Try again" on the error panel: sends the same request again (a paid call, like any question). */
  retry(): void {
    if (this.lastRequest && !this.loading()) this.send(this.lastRequest);
  }

  /** Shows an earlier answer again from memory. No request is sent. */
  showHistory(entry: HistoryEntry): void {
    this.error.set(null);
    this.session.select(entry.id);
  }

  private send(request: ChatRequest): void {
    this.lastRequest = request;
    this.loading.set(true);
    this.error.set(null);
    this.session.clearSelection();
    this.api
      .chat(request)
      .pipe(takeUntilDestroyed(this.destroyRef))
      .subscribe({
        next: (r) => {
          this.session.add(request.question, r); // newest first, and selected
          this.loading.set(false);
        },
        error: (e: ApiError) => {
          this.error.set(e);
          this.loading.set(false);
        },
      });
  }

  /** Ctrl+Enter in the question box sends, like most chat apps. */
  onKeydown(event: KeyboardEvent): void {
    if (event.key === 'Enter' && (event.ctrlKey || event.metaKey)) {
      event.preventDefault();
      this.ask();
    }
  }
}
