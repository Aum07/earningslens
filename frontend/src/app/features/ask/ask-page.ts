import { Dialog } from '@angular/cdk/dialog';
import { Component, DestroyRef, ElementRef, computed, effect, inject, signal, untracked } from '@angular/core';
import { takeUntilDestroyed, toSignal } from '@angular/core/rxjs-interop';
import { FormControl, ReactiveFormsModule } from '@angular/forms';
import { MatButtonModule } from '@angular/material/button';
import { MatFormFieldModule } from '@angular/material/form-field';
import { MatInputModule } from '@angular/material/input';
import { ApiService } from '../../core/api.service';
import { HistoryEntry, SessionHistory } from '../../core/session-history';
import { DOC_TYPE_LABELS, parsePeriodKey, periodLabel } from '../../core/labels';
import {
  ApiError,
  ChatRequest,
  ChatResponse,
  Company,
  KeyFigures,
  DocType,
  QUESTION_MAX_LENGTH,
  QUESTION_MIN_LENGTH,
} from '../../core/models';
import { Loading } from '../../shared/loading/loading';
import { ErrorPanel } from '../../shared/error-panel/error-panel';
import { AnswerPanel } from './answer-panel/answer-panel';
import { buildChatRequest } from './chat-request';
import { FiltersModal } from './filters-modal/filters-modal';
import { SAMPLE_QUESTIONS } from './sample-questions';

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
  private readonly dialog = inject(Dialog);

  readonly samples = SAMPLE_QUESTIONS;
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

  private readonly selectedPeriods = toSignal(this.periods.valueChanges, { initialValue: this.periods.value });
  private readonly selectedDocTypes = toSignal(this.docTypes.valueChanges, { initialValue: this.docTypes.value });
  private readonly selectedTickerList = toSignal(this.tickers.valueChanges, { initialValue: this.tickers.value });

  /** Row 2 of the search box: what the next question will search, as chips (gold when narrowed). Long picks are
   *  counted ("15 companies") so the chips stay one line when many companies are supported. */
  readonly scopeChips = computed(() => {
    const t = this.selectedTickerList();
    const p = this.selectedPeriods();
    const d = this.selectedDocTypes();
    return [
      { key: 'companies', text: t.length === 0 ? 'All companies' : t.length > 3 ? `${t.length} companies` : t.join(', '), active: t.length > 0, mono: t.length > 0 && t.length <= 3 },
      {
        key: 'quarters',
        text: p.length === 0 ? 'All quarters' : p.length > 3 ? `${p.length} quarters` : p.map((k) => periodLabel(parsePeriodKey(k))).join(', '),
        active: p.length > 0,
        mono: p.length > 0 && p.length <= 3,
      },
      {
        key: 'docs',
        text: d.length === 0 ? 'All documents' : d.length > 2 ? `${d.length} document types` : d.map((x) => DOC_TYPE_LABELS[x]).join(', '),
        active: d.length > 0,
        mono: false,
      },
    ];
  });

  /** A scope chip opens the Filters modal; Apply copies its choices into the form fields, Cancel changes nothing. */
  openFilters(): void {
    FiltersModal.open(this.dialog, {
      companies: this.companies(),
      companiesError: this.companiesError(),
      current: { tickers: this.tickers.value, periods: this.periods.value, docTypes: this.docTypes.value },
    }).closed.subscribe((result) => {
      if (!result) return;
      this.tickers.setValue(result.tickers);
      this.periods.setValue(result.periods);
      this.docTypes.setValue(result.docTypes);
    });
  }

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
