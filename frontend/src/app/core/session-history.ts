import { Injectable, computed, signal } from '@angular/core';
import { ChatResponse } from './models';

/** One answered question in the session history (browser memory only, gone on reload: spec 3.2). */
export interface HistoryEntry {
  id: number;
  question: string;
  response: ChatResponse;
}

/**
 * The session's answered questions, shared by the sidebar (which lists them) and the Ask page (which shows the
 * selected one). UI state only: no HTTP calls, nothing saved. Newest first.
 */
@Injectable({ providedIn: 'root' })
export class SessionHistory {
  private nextId = 1;

  readonly entries = signal<HistoryEntry[]>([]);
  readonly selectedId = signal<number | null>(null);
  readonly selected = computed(() => this.entries().find((e) => e.id === this.selectedId()) ?? null);

  /** Counts "+ New question" clicks, so the Ask page can clear its box and focus it. */
  readonly newQuestionCount = signal(0);

  /** Adds an answer and selects it. */
  add(question: string, response: ChatResponse): HistoryEntry {
    const entry: HistoryEntry = { id: this.nextId++, question, response };
    this.entries.update((list) => [entry, ...list]);
    this.selectedId.set(entry.id);
    return entry;
  }

  select(id: number): void {
    this.selectedId.set(id);
  }

  clearSelection(): void {
    this.selectedId.set(null);
  }

  /** "+ New question": back to the Home view with an empty box. */
  newQuestion(): void {
    this.selectedId.set(null);
    this.newQuestionCount.update((n) => n + 1);
  }
}
