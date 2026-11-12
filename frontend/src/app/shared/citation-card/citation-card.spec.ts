import { TestBed } from '@angular/core/testing';
import { Citation } from '../../core/models';
import { CitationCard } from './citation-card';

describe('CitationCard', () => {
  const base: Citation = {
    marker: 4,
    chunkId: 'c4',
    ticker: 'META',
    fiscalYear: 2026,
    fiscalQuarter: 3,
    docType: 'TRANSCRIPT',
    page: 7,
    section: null,
    speaker: 'Susan Li, CFO',
    sourceUrl: 'https://example.test/meta-q3',
    snippet: 'We expect capital expenditures to be ...',
  };

  async function render(citation: Citation, highlighted = false): Promise<HTMLElement> {
    const fixture = TestBed.createComponent(CitationCard);
    fixture.componentRef.setInput('citation', citation);
    fixture.componentRef.setInput('highlighted', highlighted);
    await fixture.whenStable();
    return fixture.nativeElement as HTMLElement;
  }

  it('shows ticker, period, document type, page, speaker and snippet', async () => {
    const el = await render(base);
    const text = el.textContent ?? '';
    expect(text).toContain('[4]');
    expect(text).toContain('META');
    expect(text).toContain('FY2026 Q3');
    expect(text).toContain('Earnings call transcript');
    expect(text).toContain('Page 7 | Speaker: Susan Li, CFO');
    expect(text).toContain('We expect capital expenditures to be ...');
  });

  it('shows the section when there is no page', async () => {
    const el = await render({ ...base, page: null, speaker: null, section: 'Segment results' });
    expect(el.querySelector('.location')?.textContent?.trim()).toBe('Segment results');
  });

  it('adds "..." to a snippet cut at 300 characters, and not to a shorter one', async () => {
    const long = await render({ ...base, snippet: 'x'.repeat(300) });
    expect(long.querySelector('.snippet')?.textContent).toBe('x'.repeat(300) + '...');
    const short = await render(base);
    expect(short.querySelector('.snippet')?.textContent).toBe('We expect capital expenditures to be ...');
  });

  it('opens the source in a new tab safely', async () => {
    const link = (await render(base)).querySelector('a')!;
    expect(link.getAttribute('href')).toBe('https://example.test/meta-q3');
    expect(link.getAttribute('target')).toBe('_blank');
    expect(link.getAttribute('rel')).toBe('noopener noreferrer');
    expect(link.textContent).toContain('(opens in a new tab)');
  });

  it('can receive focus and carries its marker id', async () => {
    const el = await render(base, true);
    expect(el.id).toBe('citation-4');
    expect(el.getAttribute('tabindex')).toBe('-1');
    expect(el.classList).toContain('highlighted');
  });
});
