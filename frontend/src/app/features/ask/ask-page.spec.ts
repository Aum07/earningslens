import { TestBed, ComponentFixture } from '@angular/core/testing';
import { provideHttpClient, withInterceptors } from '@angular/common/http';
import { HttpTestingController, provideHttpClientTesting } from '@angular/common/http/testing';
import { HarnessLoader } from '@angular/cdk/testing';
import { TestbedHarnessEnvironment } from '@angular/cdk/testing/testbed';
import { MatSelectHarness } from '@angular/material/select/testing';
import { MatInputHarness } from '@angular/material/input/testing';
import { MatButtonHarness } from '@angular/material/button/testing';
import { environment } from '../../../environments/environment';
import { httpErrorInterceptor } from '../../core/http-error.interceptor';
import { Company } from '../../core/models';
import { AskPage } from './ask-page';
import { SAMPLE_QUESTIONS } from './sample-questions';
import { SessionHistory } from '../../core/session-history';

/**
 * Component tests. A "harness" drives Material controls the way a user would (open a select, click an option,
 * type) without depending on Material's internal HTML. HttpTestingController stands in for the backend.
 */
describe('AskPage', () => {
  const base = `${environment.apiBaseUrl}/api/v1`;
  const companies: Company[] = [
    { ticker: 'MSFT', name: 'Microsoft Corporation', fiscalYearEndMonth: 6, periods: [{ year: 2026, quarter: 2 }, { year: 2027, quarter: 1 }] },
    { ticker: 'GOOGL', name: 'Alphabet Inc.', fiscalYearEndMonth: 12, periods: [{ year: 2025, quarter: 4 }, { year: 2026, quarter: 2 }] },
  ];

  let fixture: ComponentFixture<AskPage>;
  let loader: HarnessLoader;
  let http: HttpTestingController;

  beforeEach(async () => {
    TestBed.configureTestingModule({
      imports: [AskPage],
      providers: [provideHttpClient(withInterceptors([httpErrorInterceptor])), provideHttpClientTesting()],
    });
    http = TestBed.inject(HttpTestingController);
    fixture = TestBed.createComponent(AskPage);
    loader = TestbedHarnessEnvironment.loader(fixture);
  });

  afterEach(() => http.verify());

  async function loadCompanies(): Promise<void> {
    http.expectOne(`${base}/companies`).flush(companies);
    http.expectOne(`${base}/key-figures`).flush([]);
    await fixture.whenStable();
  }

  async function selects(): Promise<MatSelectHarness[]> {
    return loader.getAllHarnesses(MatSelectHarness);
  }

  it('fills the filter options from /companies', async () => {
    await loadCompanies();
    const [companySelect, periodSelect, docTypeSelect] = await selects();

    await companySelect.open();
    // Each option shows the company and the quarters held for it.
    const options = (fixture.nativeElement as HTMLElement).ownerDocument.querySelectorAll('mat-option');
    expect([...options].map((o) => [o.querySelector('.option-name')?.textContent, o.querySelector('.option-range')?.textContent])).toEqual([
      ['Microsoft Corporation (MSFT)', 'FY2026 Q2 to FY2027 Q1'],
      ['Alphabet Inc. (GOOGL)', 'FY2025 Q4 to FY2026 Q2'],
    ]);
    await periodSelect.open();
    // Union of both companies' quarters, oldest first, each once.
    expect(await Promise.all((await periodSelect.getOptions()).map((o) => o.getText()))).toEqual([
      'FY2025 Q4',
      'FY2026 Q2',
      'FY2027 Q1',
    ]);
    await docTypeSelect.open();
    expect((await docTypeSelect.getOptions()).length).toBe(4);

    const text = (fixture.nativeElement as HTMLElement).textContent ?? '';
    expect(text).toContain('Fiscal year ends: Microsoft Corporation June, Alphabet Inc. December.');
  });

  it('labels the Companies dropdown with how many companies are supported', async () => {
    await loadCompanies();
    expect((fixture.nativeElement as HTMLElement).querySelector('mat-label')?.textContent?.trim()).toBe(
      'Companies (2 supported)',
    );
  });

  it('sends the selected filters and the trimmed question in the request body', async () => {
    await loadCompanies();
    const [companySelect, periodSelect, docTypeSelect] = await selects();
    await companySelect.open();
    await companySelect.clickOptions({ text: /Microsoft Corporation \(MSFT\)/ });
    await periodSelect.open();
    await periodSelect.clickOptions({ text: 'FY2026 Q2' });
    await docTypeSelect.open();
    await docTypeSelect.clickOptions({ text: 'Earnings call transcript' });
    await (await loader.getHarness(MatInputHarness)).setValue('  How did Azure grow?  ');

    await (await loader.getHarness(MatButtonHarness.with({ text: 'Ask ↵' }))).click();

    const req = http.expectOne(`${base}/chat`);
    expect(req.request.method).toBe('POST');
    expect(req.request.body).toEqual({
      question: 'How did Azure grow?',
      filters: { tickers: ['MSFT'], fiscalPeriods: [{ year: 2026, quarter: 2 }], docTypes: ['TRANSCRIPT'] },
    });
    req.flush({ answer: 'ok', abstained: false, citations: [], groundingWarnings: [], appliedFilters: {}, latencyMs: 1, model: null });
  });

  it('sends no filters object when nothing is selected', async () => {
    await loadCompanies();
    await (await loader.getHarness(MatInputHarness)).setValue('What was Microsoft revenue?');
    await (await loader.getHarness(MatButtonHarness.with({ text: 'Ask ↵' }))).click();

    const req = http.expectOne(`${base}/chat`);
    expect(req.request.body).toEqual({ question: 'What was Microsoft revenue?' });
    req.flush({ answer: 'ok', abstained: false, citations: [], groundingWarnings: [], appliedFilters: {}, latencyMs: 1, model: null });
  });

  it('a sample chip fills the question box and sends nothing', async () => {
    await loadCompanies();
    (fixture.nativeElement as HTMLElement).querySelector<HTMLButtonElement>('.example')!.click();
    await fixture.whenStable();

    expect(await (await loader.getHarness(MatInputHarness)).getValue()).toBe(SAMPLE_QUESTIONS[0]);
    http.expectNone(`${base}/chat`);
  });

  it('shows a character counter and only enables Ask for 3 or more characters', async () => {
    await loadCompanies();
    const input = await loader.getHarness(MatInputHarness);
    const ask = await loader.getHarness(MatButtonHarness.with({ text: 'Ask ↵' }));
    const counter = () => (fixture.nativeElement as HTMLElement).querySelector('.count')?.textContent?.trim();

    expect(counter()).toBe('0 / 500');
    expect((fixture.nativeElement as HTMLElement).querySelector('.shortcut')?.textContent).toBe('Ctrl+Enter to ask');
    expect(await ask.isDisabled()).toBe(true);

    await input.setValue('ab');
    expect(counter()).toBe('2 / 500');
    expect(await ask.isDisabled()).toBe(true);
    expect((fixture.nativeElement as HTMLElement).textContent).toContain('At least 3 characters.');

    await input.setValue('abc');
    expect(await ask.isDisabled()).toBe(false);
  });

  it('caps the question box at 500 characters', async () => {
    await loadCompanies();
    const textarea = (fixture.nativeElement as HTMLElement).querySelector('textarea');
    expect(textarea?.getAttribute('maxlength')).toBe('500');
  });

  it('still lets you ask when /companies fails, without claiming "0 supported"', async () => {
    http.expectOne(`${base}/companies`).flush(null, { status: 503, statusText: 'Service Unavailable' });
    http.expectOne(`${base}/key-figures`).flush(null, { status: 503, statusText: 'Service Unavailable' });
    await fixture.whenStable();
    expect((fixture.nativeElement as HTMLElement).querySelector('mat-label')?.textContent?.trim()).toBe('Companies');
    expect((fixture.nativeElement as HTMLElement).textContent).toContain('You can still ask without filters.');
  });

  describe('answer, errors and history (E6.4)', () => {
    const reply = (answer: string, abstained = false) => ({
      answer,
      abstained,
      citations: [],
      groundingWarnings: [],
      appliedFilters: { tickers: [], fiscalPeriods: [], docTypes: [] },
      latencyMs: 1000,
      model: 'gpt-4o-mini',
    });

    async function askQuestion(text: string): Promise<void> {
      await (await loader.getHarness(MatInputHarness)).setValue(text);
      await (await loader.getHarness(MatButtonHarness.with({ text: 'Ask ↵' }))).click();
    }

    const el = () => fixture.nativeElement as HTMLElement;

    it('shows a loading state while waiting, then the answer', async () => {
      await loadCompanies();
      await askQuestion('First question?');
      expect(el().querySelector('app-loading')?.textContent).toContain('Searching the documents');

      http.expectOne(`${base}/chat`).flush(reply('First answer.'));
      await fixture.whenStable();
      expect(el().querySelector('app-loading')).toBeNull();
      expect(el().querySelector('app-answer-panel')?.textContent).toContain('First answer.');
    });

    it('shows an error with "Try again", which sends the same request again', async () => {
      await loadCompanies();
      await askQuestion('Will this fail?');
      http.expectOne(`${base}/chat`).flush(
        { status: 503, detail: 'The language model is not available right now. Please try again in a minute.' },
        { status: 503, statusText: 'Service Unavailable' },
      );
      await fixture.whenStable();
      expect(el().querySelector('app-error-panel')?.textContent).toContain('The language model is not available');

      await (await loader.getHarness(MatButtonHarness.with({ text: 'Try again' }))).click();
      const again = http.expectOne(`${base}/chat`);
      expect(again.request.body).toEqual({ question: 'Will this fail?' });
      again.flush(reply('Now it works.'));
      await fixture.whenStable();
      expect(el().querySelector('app-error-panel')).toBeNull();
      expect(el().textContent).toContain('Now it works.');
    });

    it('keeps answered questions in the session history, newest first, and shows a selected one without a request', async () => {
      await loadCompanies();
      await askQuestion('First question?');
      http.expectOne(`${base}/chat`).flush(reply('First answer.'));
      await askQuestion('Second question?');
      http.expectOne(`${base}/chat`).flush(reply('Nothing found.', true));
      await fixture.whenStable();

      const session = TestBed.inject(SessionHistory);
      expect(session.entries().map((e) => e.question)).toEqual(['Second question?', 'First question?']);
      expect(session.selected()?.question).toBe('Second question?');

      session.select(session.entries()[1].id);
      await fixture.whenStable();
      http.expectNone(`${base}/chat`);
      expect(el().querySelector('app-answer-panel')?.textContent).toContain('First answer.');
    });

    it('"+ New question" goes back to the Home view with an empty box', async () => {
      await loadCompanies();
      await askQuestion('First question?');
      http.expectOne(`${base}/chat`).flush(reply('First answer.'));
      await fixture.whenStable();
      expect(el().querySelector('.hero')).toBeNull();

      TestBed.inject(SessionHistory).newQuestion();
      await fixture.whenStable();
      expect(el().querySelector('.hero')?.textContent).toContain('actually reported.');
      expect(await (await loader.getHarness(MatInputHarness)).getValue()).toBe('');
    });

  });
});

