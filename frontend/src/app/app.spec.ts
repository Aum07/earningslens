import { TestBed } from '@angular/core/testing';
import { provideRouter } from '@angular/router';
import { provideHttpClient } from '@angular/common/http';
import { HttpTestingController, provideHttpClientTesting } from '@angular/common/http/testing';
import { environment } from '../environments/environment';
import { App } from './app';
import { SessionHistory } from './core/session-history';

describe('App', () => {
  beforeEach(async () => {
    await TestBed.configureTestingModule({
      imports: [App],
      providers: [provideRouter([]), provideHttpClient(), provideHttpClientTesting()],
    }).compileComponents();
  });

  it('creates the app', () => {
    const fixture = TestBed.createComponent(App);
    expect(fixture.componentInstance).toBeTruthy();
  });

  it('shows the brand name in the toolbar', async () => {
    const fixture = TestBed.createComponent(App);
    await fixture.whenStable();
    const el = fixture.nativeElement as HTMLElement;
    expect(el.querySelector('.wordmark')?.textContent).toBe('EarningsLens');
    expect(el.querySelector('.wordmark .lens')?.textContent).toBe('Lens');
  });

  it('has navigation links to Ask, Accuracy report, Corpus and About', async () => {
    const fixture = TestBed.createComponent(App);
    await fixture.whenStable();
    const links = [...(fixture.nativeElement as HTMLElement).querySelectorAll('nav a')];
    expect(links.map((a) => a.textContent?.trim())).toEqual(['Ask', 'Accuracy report', 'Corpus', 'About']);
    expect(links.map((a) => a.getAttribute('href'))).toEqual(['/', '/evaluation', '/corpus', '/about']);
  });

  it('shows the live API status from /eval/live-config, and "API offline" when it fails', async () => {
    const http = TestBed.inject(HttpTestingController);
    const fixture = TestBed.createComponent(App);
    const status = () => (fixture.nativeElement as HTMLElement).querySelector('.status')?.textContent?.replace(/\s+/g, ' ').trim();
    http
      .expectOne(`${environment.apiBaseUrl}/api/v1/eval/live-config`)
      .flush({ retrievalMode: 'C', chatModel: 'gpt-4o-mini', embeddingModel: 'x' });
    await fixture.whenStable();
    expect(status()).toBe('API online: config C, gpt-4o-mini');

    const offline = TestBed.createComponent(App);
    http.expectOne(`${environment.apiBaseUrl}/api/v1/eval/live-config`).flush(null, { status: 0, statusText: '' });
    await offline.whenStable();
    expect((offline.nativeElement as HTMLElement).querySelector('.status')?.textContent).toContain('API offline');
  });

  it('lists this session\'s questions in the sidebar and marks the current one', async () => {
    const session = TestBed.inject(SessionHistory);
    const fixture = TestBed.createComponent(App);
    await fixture.whenStable();
    const el = fixture.nativeElement as HTMLElement;
    expect(el.querySelector('.history')?.textContent).toContain('until you reload the page');

    const answer = { answer: 'a', abstained: false, citations: [], groundingWarnings: [], appliedFilters: { tickers: [], fiscalPeriods: [], docTypes: [] }, latencyMs: 1, model: 'm' };
    session.add('First?', answer);
    session.add('Second?', { ...answer, abstained: true });
    await fixture.whenStable();
    const items = [...el.querySelectorAll<HTMLButtonElement>('.history-item')];
    expect(items.map((b) => b.textContent?.trim())).toEqual(['Second? (no answer)', 'First?']);
    expect(items[0].getAttribute('aria-current')).toBe('true');

    items[1].click();
    await fixture.whenStable();
    expect(session.selected()?.question).toBe('First?');
  });

  it('the Coverage card lists up to 5 companies, then summarises the rest with a link to Corpus', async () => {
    const http = TestBed.inject(HttpTestingController);
    const company = (t: string) => ({ ticker: t, name: t, fiscalYearEndMonth: 12, periods: [{ year: 2026, quarter: 1 }, { year: 2026, quarter: 3 }] });
    const fixture = TestBed.createComponent(App);
    http.expectOne(`${environment.apiBaseUrl}/api/v1/companies`).flush(['A', 'B', 'C', 'D', 'E', 'F', 'G'].map(company));
    await fixture.whenStable();
    const el = fixture.nativeElement as HTMLElement;
    const rows = [...el.querySelectorAll('.coverage li')].map((li) => li.textContent?.replace(/\s+/g, ' ').trim());
    expect(rows).toEqual(['AFY2026 Q1 to FY2026 Q3', 'BFY2026 Q1 to FY2026 Q3', 'CFY2026 Q1 to FY2026 Q3', 'DFY2026 Q1 to FY2026 Q3']);
    expect(el.querySelector('.coverage-more')?.textContent?.replace(/\s+/g, ' ').trim()).toBe('+ 3 more companies');
    expect(el.querySelector('.coverage-link')?.getAttribute('href')).toBe('/corpus');
    expect(el.querySelector('.coverage-link')?.textContent?.replace(/\s+/g, ' ').trim()).toBe('See all 7 on Corpus');
  });

  it('Ctrl+K starts a new question', async () => {
    const session = TestBed.inject(SessionHistory);
    const fixture = TestBed.createComponent(App);
    await fixture.whenStable();
    const before = session.newQuestionCount();
    document.dispatchEvent(new KeyboardEvent('keydown', { key: 'k', ctrlKey: true }));
    expect(session.newQuestionCount()).toBe(before + 1);
  });

  it('shows the disclaimer footer (spec section 12)', async () => {
    const fixture = TestBed.createComponent(App);
    await fixture.whenStable();
    const el = fixture.nativeElement as HTMLElement;
    expect(el.querySelector('.footnote .disclaimer-text')?.textContent?.trim()).toBe(
      'Not investment advice. Answers come only from the listed documents.',
    );
    expect(el.querySelector('.footnote .credit a')?.getAttribute('href')).toBe('/about');
  });
});
