import { ComponentFixture, TestBed } from '@angular/core/testing';
import { provideHttpClient, withInterceptors } from '@angular/common/http';
import { HttpTestingController, provideHttpClientTesting } from '@angular/common/http/testing';
import { environment } from '../../../environments/environment';
import { httpErrorInterceptor } from '../../core/http-error.interceptor';
import { ConfigSummary, LatestEvalReport } from '../../core/models';
import { CONFIG_DESCRIPTIONS, EvaluationPage } from './evaluation-page';

/**
 * The page must only show numbers from the report (spec rule 7). The fixture uses values unlike the real report,
 * so a number typed into the template would show up as a mismatch here.
 */
describe('EvaluationPage', () => {
  const base = `${environment.apiBaseUrl}/api/v1/eval`;

  function config(name: string, recallAll: number, correctness: number | null, errors = 0): ConfigSummary {
    const r = { answerableItems: 9, recallAll, recallAny: 0.9, mrr: 0.333, requiredPeriods: 4, coveredPeriods: 3, periodCoverage: 0.75, avgRetrieved: 16 };
    return {
      config: name,
      items: 12,
      errors,
      retrieval: r,
      retrievalByCategory: { single_fact: r },
      generation: {
        judged: 9, faithfulness: 0.876, fullyFaithful: 0.5, citationPrecision: 0.333, citationPairs: 20,
        numberGroundingRate: null, numbers: 0, correctness, correctnessAnsweredAnswerable: correctness,
        correctnessByCategory: {},
      },
      abstention: {
        unanswerable: 3, unanswerableAbstained: 3, answerable: 9 - errors, answerableAbstained: 1,
        precision: 0.75, recall: 1, falseAbstentionRate: 0.111, byReason: { INSUFFICIENT_EVIDENCE: 4 },
      },
      operations: { queries: 12, latencyP50Ms: 1111, latencyP95Ms: 2222, meanCostUsd: 0.00123, totalCostUsd: 0.01, judgeCostUsd: 0.02 },
    };
  }

  const report: LatestEvalReport = {
    date: '2030-01-02',
    split: 'test',
    reports: ['x-A-test.json', 'x-C-test.json'],
    ablationReport: 'x-ablation-test.md',
    settings: {
      k: 7, kPerPeriod: 3, maxPeriods: 4, minSimilarity: 0.6, chunkTargetTokens: 600, chunkOverlapTokens: 100,
      corpusChunks: 10, chatModel: 'chat-x', embeddingModel: 'embed-x', judgeModel: 'judge-x', rubricPromptSha256: 'abc',
    },
    configs: [config('A', 0.123, 3.456, 1), config('C', 0.789, null)],
    judgeAgreement: {
      report: 'x-judge-agreement-dev.json',
      agreement: {
        n: 11,
        faithfulness: { bothYes: 5, bothNo: 3, humanYesJudgeNo: 2, humanNoJudgeYes: 1, agreement: 0.727, kappa: 0.444 },
        correctness: { exact: 0.545, withinOne: 0.909, meanJudgeMinusHuman: 0.25, matrix: [] },
      },
      sameRubric: true,
    },
  };

  let fixture: ComponentFixture<EvaluationPage>;
  let http: HttpTestingController;
  const el = () => fixture.nativeElement as HTMLElement;
  const text = () => (el().textContent ?? '').replace(/\s+/g, ' ');

  beforeEach(() => {
    TestBed.configureTestingModule({
      imports: [EvaluationPage],
      providers: [provideHttpClient(withInterceptors([httpErrorInterceptor])), provideHttpClientTesting()],
    });
    http = TestBed.inject(HttpTestingController);
    fixture = TestBed.createComponent(EvaluationPage);
  });

  afterEach(() => http.verify());

  async function load(mode = 'C'): Promise<void> {
    http.expectOne(`${base}/latest`).flush(report);
    http.expectOne(`${base}/live-config`).flush({ retrievalMode: mode, chatModel: 'chat-x', embeddingModel: 'embed-x' });
    await fixture.whenStable();
  }

  /** All metric cells of one config, across the three group tables, in report column order. */
  function rowCells(config: string): string[] {
    return [...el().querySelectorAll('tbody tr')]
      .filter((r) => r.querySelector('th')?.textContent?.trim().startsWith(config))
      .flatMap((r) => [...r.querySelectorAll('td')].map((td) => td.textContent?.trim() ?? ''));
  }

  it('renders one row per config with numbers formatted from the report', async () => {
    await load();
    expect(el().querySelectorAll('table').length).toBe(3); // retrieval, answer quality, abstention and operations
    expect(el().querySelectorAll('tbody tr').length).toBe(6); // 2 configs x 3 tables
    expect(rowCells('A')).toEqual([
      '0.12', '0.90', '0.33', '0.75', '0.88', '50%', '33%', 'n/a', '3.46', '75%', '100%', '11%', '1111 / 2222', '$0.0012',
    ]);
    expect(rowCells('C')[0]).toBe('0.79');
    expect(rowCells('C')[8]).toBe('n/a'); // a missing metric is never shown as 0
  });

  it('says which config the live API runs and marks that row with a "Live" label', async () => {
    await load('C');
    expect(el().querySelector('.live')?.textContent?.replace(/\s+/g, ' ')).toContain(`The live chat API runs config C (${CONFIG_DESCRIPTIONS['C']}) with chat-x.`);
    const live = [...el().querySelectorAll('tbody tr.live-row')];
    expect(live.length).toBe(3); // the C row in each of the 3 tables
    expect(live[0].querySelector('.live-tag')?.textContent).toBe('Live');
    expect(live[0].querySelector('th')?.textContent).toContain('C');
  });

  it('takes the split, date, question counts, settings and judge agreement from the report', async () => {
    await load();
    expect(text()).toContain('on 2030-01-02');
    expect(text()).toContain('test split: 12 questions (9 answerable, 3 that the documents cannot answer)');
    expect(text()).toContain('k 7, k per quarter 3, evidence threshold 0.6');
    expect(text()).toContain('11 dev-split answers');
    expect(text()).toContain('73%'); // faithfulness agreement 0.727
    expect(text()).toContain('0.44'); // kappa
    expect(text()).toContain('1 item(s) were left out');
  });

  it('shows the caveats from the error analysis', async () => {
    await load();
    expect(text()).toContain("A's higher correctness is not a real win.");
    expect(text()).toContain('The live config was chosen after the test run.');
    expect(text()).toContain('the agreement is optimistic');
  });

  it('shows score cards for the live config, with the 85% rule and a "Where it\'s weaker" callout', async () => {
    await load('C');
    expect(el().textContent).toContain('How config C scored');
    const cards = [...el().querySelectorAll('.card')].map((c) => ({
      label: c.querySelector('.card-label')?.textContent?.trim(),
      value: c.querySelector('.card-value')?.textContent?.trim(),
      weaker: c.classList.contains('weaker'),
      bar: c.querySelector('.fill')?.getAttribute('style') ?? null,
    }));
    expect(cards).toEqual([
      { label: 'Recall@k (all quotes)', value: '0.79', weaker: true, bar: 'width: 78.9%;' },
      { label: 'Faithfulness', value: '0.88', weaker: false, bar: 'width: 87.6%;' },
      { label: 'Citation precision', value: '33%', weaker: true, bar: 'width: 33.3%;' },
      { label: 'Number grounding', value: 'n/a', weaker: false, bar: null }, // missing: no bar, never 0
      { label: 'Correctness', value: 'n/a', weaker: false, bar: null },
      { label: 'Abstention precision', value: '75%', weaker: true, bar: 'width: 75%;' },
      { label: 'Abstention recall', value: '100%', weaker: false, bar: 'width: 100%;' },
      { label: 'False abstention', value: '11%', weaker: false, bar: null }, // lower is better: no bar
    ]);
    expect(el().querySelectorAll('.card .pill-weaker').length).toBe(3);
    const callout = [...el().querySelectorAll('.callout li')].map((li) => li.textContent?.replace(/\s+/g, ' ').trim());
    expect(callout.length).toBe(3);
    expect(callout[0]).toContain('Recall@k (all quotes) 0.79: counts only the exact gold quote');
  });

  it('scores the live config\'s own row: config A when the API runs A', async () => {
    await load('A');
    expect(el().textContent).toContain('How config A scored');
    expect(el().querySelector('.card-value')?.textContent?.trim()).toBe('0.12');
    expect([...el().querySelectorAll('.card')][4].querySelector('.card-value')?.textContent?.trim()).toBe('3.46');
  });

  it('shows a neutral notice when no report exists yet', async () => {
    http.expectOne(`${base}/latest`).flush(
      { status: 404, detail: 'No evaluation report yet. Run the evaluation to create latest.json.' },
      { status: 404, statusText: 'Not Found' },
    );
    http.expectOne(`${base}/live-config`).flush({ retrievalMode: 'C', chatModel: 'x', embeddingModel: 'y' });
    await fixture.whenStable();
    expect(el().querySelector('.info')?.textContent).toContain('No evaluation report yet');
    expect(el().querySelector('app-error-panel')).toBeNull();
    expect(el().querySelector('table')).toBeNull();
  });

  it('still shows the table when the live settings fail to load, without a "Live" row', async () => {
    http.expectOne(`${base}/latest`).flush(report);
    http.expectOne(`${base}/live-config`).flush(null, { status: 500, statusText: 'Server Error' });
    await fixture.whenStable();
    expect(el().querySelectorAll('tbody tr').length).toBe(6);
    expect(el().querySelectorAll('tr.live-row').length).toBe(0);
    expect(el().querySelector('.cards')).toBeNull(); // no live config known: no score cards
    expect(text()).toContain('Could not load which config the live API runs.');
  });
});

describe('CONFIG_DESCRIPTIONS', () => {
  it('describes every ablation config', () => {
    expect(Object.keys(CONFIG_DESCRIPTIONS)).toEqual(['A', 'B', 'C', 'D']);
  });
});
