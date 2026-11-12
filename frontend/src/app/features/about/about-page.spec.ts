import { ComponentFixture, TestBed } from '@angular/core/testing';
import { provideHttpClient } from '@angular/common/http';
import { HttpTestingController, provideHttpClientTesting } from '@angular/common/http/testing';
import { environment } from '../../../environments/environment';
import { AboutPage } from './about-page';

describe('AboutPage', () => {
  const url = `${environment.apiBaseUrl}/api/v1/eval/live-config`;
  let fixture: ComponentFixture<AboutPage>;
  let http: HttpTestingController;
  const el = () => fixture.nativeElement as HTMLElement;

  beforeEach(() => {
    TestBed.configureTestingModule({
      imports: [AboutPage],
      providers: [provideHttpClient(), provideHttpClientTesting()],
    });
    http = TestBed.inject(HttpTestingController);
    fixture = TestBed.createComponent(AboutPage);
  });

  afterEach(() => http.verify());

  it('has the architecture diagram with a text description for screen readers', async () => {
    http.expectOne(url).flush({ retrievalMode: 'C', chatModel: 'gpt-4o-mini', embeddingModel: 'text-embedding-3-small' });
    await fixture.whenStable();
    const svg = el().querySelector('svg')!;
    expect(svg.getAttribute('role')).toBe('img');
    expect(svg.querySelector('desc')?.textContent).toContain('hybrid search retrieves chunks');
  });

  it('states the live config from /eval/live-config', async () => {
    http.expectOne(url).flush({ retrievalMode: 'C', chatModel: 'gpt-4o-mini', embeddingModel: 'text-embedding-3-small' });
    await fixture.whenStable();
    expect(el().querySelector('.live')?.textContent?.replace(/\s+/g, ' ')).toContain(
      'The live API runs config C (B + filters from the query parser + per-quarter retrieval) with gpt-4o-mini',
    );
  });

  it('says so when the live settings cannot be loaded', async () => {
    http.expectOne(url).flush(null, { status: 0, statusText: '' });
    await fixture.whenStable();
    expect(el().querySelector('.live')?.textContent).toContain('could not be loaded');
  });

  it('shows the author with LinkedIn and GitHub links that open safely in a new tab', async () => {
    http.expectOne(url).flush({ retrievalMode: 'C', chatModel: 'x', embeddingModel: 'y' });
    await fixture.whenStable();
    expect(el().querySelector('.author')?.textContent).toContain('Built by Aum Kanakiya, MSc in Artificial Intelligence, University of Galway.');
    const links = [...el().querySelectorAll<HTMLAnchorElement>('.author a')];
    expect(links.map((a) => a.getAttribute('href'))).toEqual([
      'https://www.linkedin.com/in/aum-kanakiya',
      'https://github.com/Aum07/earningslens',
    ]);
    expect(links.every((a) => a.target === '_blank' && a.rel === 'noopener noreferrer')).toBe(true);
  });

  it('lists design decisions and known limitations, with the disclaimer', async () => {
    http.expectOne(url).flush({ retrievalMode: 'C', chatModel: 'x', embeddingModel: 'y' });
    await fixture.whenStable();
    expect(el().querySelectorAll('.pairs dt').length).toBeGreaterThanOrEqual(5);
    expect(el().querySelector('.limits')?.textContent).toContain('Not investment advice.');
  });
});
