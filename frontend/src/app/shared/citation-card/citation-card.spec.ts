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

  async function render(citation: Citation, highlighted = false) {
    const fixture = TestBed.createComponent(CitationCard);
    fixture.componentRef.setInput('citation', citation);
    fixture.componentRef.setInput('highlighted', highlighted);
    await fixture.whenStable();
    return { fixture, el: fixture.nativeElement as HTMLElement };
  }

  it('shows the number, document type and a meta line', async () => {
    const { el } = await render(base);
    expect(el.querySelector('.number')?.textContent?.trim()).toBe('4');
    expect(el.querySelector('.title')?.textContent?.trim()).toBe('Earnings call transcript');
    expect(el.querySelector('.meta')?.textContent).toBe('META · FY2026 Q3 · Page 7 · Susan Li, CFO');
  });

  it('shows the section when there is no page', async () => {
    const { el } = await render({ ...base, page: null, speaker: null, section: 'Segment results' });
    expect(el.querySelector('.meta')?.textContent).toBe('META · FY2026 Q3 · Segment results');
  });

  it('"View passage" asks the parent to open the source drawer', async () => {
    const { fixture, el } = await render(base);
    let opened = 0;
    fixture.componentInstance.viewPassage.subscribe(() => opened++);
    const button = el.querySelector<HTMLButtonElement>('button.view')!;
    expect(button.getAttribute('aria-label')).toBe('View passage of source 4');
    expect(button.getAttribute('aria-haspopup')).toBe('dialog');
    button.click();
    expect(opened).toBe(1);
  });

  it('can receive focus, carries its marker id, and says "Selected" when highlighted', async () => {
    const { el } = await render(base, true);
    expect(el.id).toBe('citation-4');
    expect(el.getAttribute('tabindex')).toBe('-1');
    expect(el.classList).toContain('highlighted');
    expect(el.textContent).toContain('Selected');
  });
});
