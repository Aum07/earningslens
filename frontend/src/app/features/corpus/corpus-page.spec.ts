import { ComponentFixture, TestBed } from '@angular/core/testing';
import { provideHttpClient, withInterceptors } from '@angular/common/http';
import { HttpTestingController, provideHttpClientTesting } from '@angular/common/http/testing';
import { environment } from '../../../environments/environment';
import { httpErrorInterceptor } from '../../core/http-error.interceptor';
import { CorpusDocument } from '../../core/models';
import { CorpusPage } from './corpus-page';

describe('CorpusPage', () => {
  const url = `${environment.apiBaseUrl}/api/v1/documents`;
  const docs: CorpusDocument[] = [
    { id: '1', ticker: 'MSFT', companyName: 'Microsoft Corporation', fiscalYear: 2026, fiscalQuarter: 2, docType: 'PRESS_RELEASE', sourceUrl: 'https://example.test/1', chunkCount: 12 },
    { id: '2', ticker: 'MSFT', companyName: 'Microsoft Corporation', fiscalYear: 2026, fiscalQuarter: 2, docType: 'TRANSCRIPT', sourceUrl: 'https://example.test/2', chunkCount: 30 },
    { id: '3', ticker: 'META', companyName: 'Meta Platforms, Inc.', fiscalYear: 2026, fiscalQuarter: 3, docType: 'FORM_10Q', sourceUrl: 'https://example.test/3', chunkCount: 1058 },
  ];

  let fixture: ComponentFixture<CorpusPage>;
  let http: HttpTestingController;
  const el = () => fixture.nativeElement as HTMLElement;

  beforeEach(() => {
    TestBed.configureTestingModule({
      imports: [CorpusPage],
      providers: [provideHttpClient(withInterceptors([httpErrorInterceptor])), provideHttpClientTesting()],
    });
    http = TestBed.inject(HttpTestingController);
    fixture = TestBed.createComponent(CorpusPage);
  });

  afterEach(() => http.verify());

  it('shows a loading state, then one row per document in the order the API sends', async () => {
    await fixture.whenStable();
    expect(el().querySelector('app-loading')).not.toBeNull();

    http.expectOne(url).flush(docs);
    await fixture.whenStable();
    const rows = [...el().querySelectorAll('tbody tr')].map((r) =>
      [...r.querySelectorAll('td')].slice(0, 4).map((td) => td.textContent?.trim()),
    );
    expect(rows).toEqual([
      ['Microsoft Corporation (MSFT)', 'FY2026 Q2', 'Press release', '12'],
      ['Microsoft Corporation (MSFT)', 'FY2026 Q2', 'Earnings call transcript', '30'],
      ['Meta Platforms, Inc. (META)', 'FY2026 Q3', 'Form 10-Q', '1,058'],
    ]);
  });

  it('counts the totals from the list', async () => {
    http.expectOne(url).flush(docs);
    await fixture.whenStable();
    expect(el().querySelector('.summary')?.textContent?.replace(/\s+/g, ' ').trim()).toBe(
      '3 documents from 2 companies, split into 1,100 searchable chunks.',
    );
  });

  it('links each document to its source in a new tab', async () => {
    http.expectOne(url).flush(docs);
    await fixture.whenStable();
    const link = el().querySelector('tbody a')!;
    expect(link.getAttribute('href')).toBe('https://example.test/1');
    expect(link.getAttribute('rel')).toBe('noopener noreferrer');
  });

  it('shows an error with Try again, which loads the list again', async () => {
    http.expectOne(url).flush(null, { status: 0, statusText: '' });
    await fixture.whenStable();
    expect(el().querySelector('app-error-panel')?.textContent).toContain('Cannot reach the server');

    (el().querySelector('app-error-panel button') as HTMLButtonElement).click();
    http.expectOne(url).flush(docs);
    await fixture.whenStable();
    expect(el().querySelectorAll('tbody tr').length).toBe(3);
  });

  describe('company search', () => {
    async function search(text: string): Promise<void> {
      const input = el().querySelector<HTMLInputElement>('input[type="search"]')!;
      input.value = text;
      input.dispatchEvent(new Event('input'));
      await fixture.whenStable();
    }
    const companies = () => [...el().querySelectorAll('tbody tr td:first-child')].map((td) => td.textContent?.trim());

    beforeEach(async () => {
      http.expectOne(url).flush(docs);
      await fixture.whenStable();
    });

    it('filters the table by ticker or company name, in any case', async () => {
      await search('meta');
      expect(companies()).toEqual(['Meta Platforms, Inc. (META)']);
      expect(el().querySelector('[role="status"]')?.textContent?.trim()).toBe('1 of 3 documents');

      await search('  Microsoft ');
      expect(companies().length).toBe(2);

      await search('MSF');
      expect(companies().length).toBe(2);
    });

    it('says when no company matches, and shows everything again when cleared', async () => {
      await search('apple');
      expect(el().querySelector('tbody .empty')?.textContent?.trim()).toBe('No company matches "apple".');
      await search('');
      expect(companies().length).toBe(3);
      expect(el().querySelector('[role="status"]')?.textContent?.trim()).toBe('3 documents');
    });
  });
});
