import { Component, computed, inject } from '@angular/core';
import { toSignal } from '@angular/core/rxjs-interop';
import { NavigationEnd, Router, RouterLink, RouterLinkActive, RouterOutlet } from '@angular/router';
import { catchError, filter, map, of } from 'rxjs';
import { ApiService } from './core/api.service';
import { periodLabel } from './core/labels';
import { Company, LiveConfig } from './core/models';
import { SessionHistory } from './core/session-history';

/**
 * Root component: the app shell from DESIGN.md section 5. A 264px sidebar (wordmark, "+ New question", navigation,
 * this session's questions, coverage card, footnote), a 60px top bar and the page area.
 * routerLink / routerLinkActive: router links (no page reload) and the class for the current page;
 * ariaCurrentWhenActive tells screen readers which page is open.
 */
@Component({
  selector: 'app-root',
  imports: [RouterOutlet, RouterLink, RouterLinkActive],
  templateUrl: './app.html',
  styleUrl: './app.scss',
  host: { '(document:keydown)': 'onKeydown($event)' },
})
export class App {
  private readonly api = inject(ApiService);
  private readonly router = inject(Router);
  readonly session = inject(SessionHistory);

  readonly pages = [
    { path: '/', label: 'Ask', exact: true },
    { path: '/evaluation', label: 'Accuracy report', exact: false },
    { path: '/corpus', label: 'Corpus', exact: false },
    { path: '/about', label: 'About', exact: false },
  ];

  /** "Ctrl K" on Windows and Linux, "⌘K" on a Mac. */
  readonly shortcut = /Mac|iPhone|iPad/.test(globalThis.navigator?.platform ?? '') ? '⌘K' : 'Ctrl K';

  /** What the live chat API runs (GET /eval/live-config); null while loading or when the server is down. */
  readonly live = toSignal(
    this.api.liveConfig().pipe(catchError(() => of(null as LiveConfig | null))),
    { initialValue: null },
  );

  /** Coverage card: companies and the quarters held, from /companies. */
  private readonly companies = toSignal(
    this.api.companies().pipe(catchError(() => of([] as Company[]))),
    { initialValue: [] as Company[] },
  );
  /** Up to COVERAGE_ROWS companies are listed; beyond that the card lists the first ones and says how many more. */
  static readonly COVERAGE_ROWS = 5;
  readonly coverageTotal = computed(() => this.companies().length);
  readonly coverageHidden = computed(() =>
    this.coverageTotal() > App.COVERAGE_ROWS ? this.coverageTotal() - (App.COVERAGE_ROWS - 1) : 0,
  );
  readonly coverage = computed(() =>
    this.companies()
      .slice(0, this.coverageHidden() > 0 ? App.COVERAGE_ROWS - 1 : App.COVERAGE_ROWS)
      .map((c) => ({
      ticker: c.ticker,
      range: c.periods.length
        ? `${periodLabel(c.periods[0])} to ${periodLabel(c.periods[c.periods.length - 1])}`
        : '',
    })),
  );

  /** Title shown in the top bar for the current page. */
  private readonly url = toSignal(
    this.router.events.pipe(
      filter((e): e is NavigationEnd => e instanceof NavigationEnd),
      map((e) => e.urlAfterRedirects),
    ),
    { initialValue: '/' },
  );
  readonly pageTitle = computed(() => {
    const path = this.url().split('?')[0];
    return this.pages.find((p) => (p.exact ? path === p.path : path.startsWith(p.path)))?.label ?? 'Ask';
  });

  /** "+ New question": back to the Ask page's Home view with an empty, focused box. */
  newQuestion(): void {
    this.session.newQuestion();
    void this.router.navigateByUrl('/');
  }

  /** Reopens an earlier answer from this session (no request is sent). */
  openEntry(id: number): void {
    this.session.select(id);
    void this.router.navigateByUrl('/');
  }

  onKeydown(event: KeyboardEvent): void {
    if ((event.ctrlKey || event.metaKey) && event.key.toLowerCase() === 'k') {
      event.preventDefault();
      this.newQuestion();
    }
  }
}
