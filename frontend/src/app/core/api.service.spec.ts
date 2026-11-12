import { TestBed } from '@angular/core/testing';
import { HttpErrorResponse, provideHttpClient, withInterceptors } from '@angular/common/http';
import { HttpTestingController, provideHttpClientTesting } from '@angular/common/http/testing';
import { Observable } from 'rxjs';
import { environment } from '../../environments/environment';
import { ApiService } from './api.service';
import { httpErrorInterceptor, toApiError } from './http-error.interceptor';
import { ApiError, ChatRequest, ChatResponse } from './models';

/**
 * HttpTestingController replaces the real network: each test checks which request the service sent, then
 * answers it with flush() (success) or error responses. No backend and no OpenAI call is involved.
 */
describe('ApiService', () => {
  const base = `${environment.apiBaseUrl}/api/v1`;
  let api: ApiService;
  let http: HttpTestingController;

  beforeEach(() => {
    TestBed.configureTestingModule({
      providers: [provideHttpClient(withInterceptors([httpErrorInterceptor])), provideHttpClientTesting()],
    });
    api = TestBed.inject(ApiService);
    http = TestBed.inject(HttpTestingController);
  });

  // Fails the test if a request was sent that the test did not expect.
  afterEach(() => http.verify());

  /** Subscribes and returns the ApiError the call fails with. */
  function failureOf<T>(call: Observable<T>): { error?: ApiError } {
    const result: { error?: ApiError } = {};
    call.subscribe({ next: () => expect.fail('expected an error'), error: (e: ApiError) => (result.error = e) });
    return result;
  }

  describe('requests', () => {
    it('chat POSTs the request body unchanged to /api/v1/chat', () => {
      const request: ChatRequest = {
        question: 'What was Microsoft revenue in FY2026 Q2?',
        filters: { tickers: ['MSFT'], fiscalPeriods: [{ year: 2026, quarter: 2 }], docTypes: ['PRESS_RELEASE'] },
      };
      const reply: ChatResponse = {
        answer: 'Revenue was $81.3 billion [1].',
        abstained: false,
        citations: [],
        groundingWarnings: [],
        appliedFilters: { tickers: ['MSFT'], fiscalPeriods: [{ year: 2026, quarter: 2 }], docTypes: ['PRESS_RELEASE'] },
        latencyMs: 2100,
        model: 'gpt-4o-mini',
      };
      let received: ChatResponse | undefined;
      api.chat(request).subscribe((r) => (received = r));

      const req = http.expectOne(`${base}/chat`);
      expect(req.request.method).toBe('POST');
      expect(req.request.body).toEqual(request);
      req.flush(reply);
      expect(received).toEqual(reply);
    });

    it('documents GETs /api/v1/documents', () => {
      let count = -1;
      api.documents().subscribe((d) => (count = d.length));
      const req = http.expectOne(`${base}/documents`);
      expect(req.request.method).toBe('GET');
      req.flush([]);
      expect(count).toBe(0);
    });

    it('companies GETs /api/v1/companies', () => {
      let tickers: string[] = [];
      api.companies().subscribe((c) => (tickers = c.map((x) => x.ticker)));
      const req = http.expectOne(`${base}/companies`);
      expect(req.request.method).toBe('GET');
      req.flush([{ ticker: 'MSFT', name: 'Microsoft Corporation', fiscalYearEndMonth: 6, periods: [] }]);
      expect(tickers).toEqual(['MSFT']);
    });

    it('latestEval GETs /api/v1/eval/latest', () => {
      let split = '';
      api.latestEval().subscribe((r) => (split = r.split));
      const req = http.expectOne(`${base}/eval/latest`);
      expect(req.request.method).toBe('GET');
      req.flush({ split: 'dev' }); // shape check is not this test's job
      expect(split).toBe('dev');
    });
  });

  it('liveConfig GETs /api/v1/eval/live-config', () => {
    let mode = '';
    api.liveConfig().subscribe((c) => (mode = c.retrievalMode));
    const req = http.expectOne(`${base}/eval/live-config`);
    expect(req.request.method).toBe('GET');
    req.flush({ retrievalMode: 'C', chatModel: 'gpt-4o-mini', embeddingModel: 'text-embedding-3-small' });
    expect(mode).toBe('C');
  });

  it('keyFigures GETs /api/v1/key-figures', () => {
    let tickers: string[] = [];
    api.keyFigures().subscribe((k) => (tickers = k.map((x) => x.ticker)));
    const req = http.expectOne(`${base}/key-figures`);
    expect(req.request.method).toBe('GET');
    req.flush([{ ticker: 'MSFT', name: 'Microsoft Corporation', available: true, sourceUrl: null, fetchedAt: null, quarters: [] }]);
    expect(tickers).toEqual(['MSFT']);
  });

  describe('errors become ApiError', () => {
    it('400 keeps the detail and the field errors', () => {
      const result = failureOf(api.chat({ question: 'ab' }));
      http.expectOne(`${base}/chat`).flush(
        {
          title: 'Invalid request',
          status: 400,
          detail: 'Invalid request: question: size must be between 3 and 500',
          errors: [{ field: 'question', message: 'size must be between 3 and 500' }],
        },
        { status: 400, statusText: 'Bad Request' },
      );
      expect(result.error).toBeInstanceOf(ApiError);
      expect(result.error?.kind).toBe('validation');
      expect(result.error?.message).toBe('Invalid request: question: size must be between 3 and 500');
      expect(result.error?.fieldErrors).toEqual([{ field: 'question', message: 'size must be between 3 and 500' }]);
      expect(result.error?.retryable).toBe(false);
    });

    it('429 reads retryAfterSeconds from the body', () => {
      const result = failureOf(api.chat({ question: 'abc' }));
      http.expectOne(`${base}/chat`).flush(
        { status: 429, detail: 'Too many questions from your address. Please wait 5 seconds and try again.', reason: 'PER_IP', retryAfterSeconds: 5 },
        { status: 429, statusText: 'Too Many Requests', headers: { 'Retry-After': '5' } },
      );
      expect(result.error?.kind).toBe('rate-limited');
      expect(result.error?.retryAfterSeconds).toBe(5);
      expect(result.error?.message).toContain('Please wait 5 seconds');
      expect(result.error?.retryable).toBe(true);
    });

    it('429 falls back to the Retry-After header', () => {
      const result = failureOf(api.chat({ question: 'abc' }));
      http.expectOne(`${base}/chat`).flush(null, { status: 429, statusText: 'Too Many Requests', headers: { 'Retry-After': '7' } });
      expect(result.error?.retryAfterSeconds).toBe(7);
      expect(result.error?.message).toBe('Too many requests. Please wait a moment and try again.');
    });

    it('503 is "unavailable" and retryable', () => {
      const result = failureOf(api.chat({ question: 'abc' }));
      http.expectOne(`${base}/chat`).flush(
        { status: 503, detail: 'The language model is not available right now. Please try again in a minute.' },
        { status: 503, statusText: 'Service Unavailable' },
      );
      expect(result.error?.kind).toBe('unavailable');
      expect(result.error?.retryable).toBe(true);
    });

    it('404 on eval/latest is "not-found"', () => {
      const result = failureOf(api.latestEval());
      http.expectOne(`${base}/eval/latest`).flush(
        { status: 404, detail: 'No evaluation report yet. Run the evaluation to create latest.json.' },
        { status: 404, statusText: 'Not Found' },
      );
      expect(result.error?.kind).toBe('not-found');
      expect(result.error?.message).toContain('No evaluation report yet');
    });

    it('a 500 without a ProblemDetail body gets the default message', () => {
      const result = failureOf(api.documents());
      http.expectOne(`${base}/documents`).flush('<html>proxy error</html>', { status: 502, statusText: 'Bad Gateway' });
      expect(result.error?.kind).toBe('server');
      expect(result.error?.message).toBe('Something went wrong on our side. Please try again.');
    });

    it('no response at all (backend down) is "network"', () => {
      const result = failureOf(api.companies());
      http.expectOne(`${base}/companies`).error(new ProgressEvent('error'), { status: 0, statusText: '' });
      expect(result.error?.kind).toBe('network');
      expect(result.error?.status).toBe(0);
      expect(result.error?.message).toContain('Cannot reach the EarningsLens server');
    });
  });

  describe('toApiError', () => {
    it('keeps an ApiError as it is', () => {
      const original = new ApiError('validation', 400, 'x');
      expect(toApiError(original)).toBe(original);
    });

    it('turns a non-HTTP error into "unknown"', () => {
      expect(toApiError(new TypeError('boom')).kind).toBe('unknown');
    });

    it('ignores malformed field errors', () => {
      const error = toApiError(
        new HttpErrorResponse({ status: 400, error: { errors: [{ field: 'question' }, 'junk', { field: 'q', message: 'm' }] } }),
      );
      expect(error.fieldErrors).toEqual([{ field: 'q', message: 'm' }]);
    });
  });
});
