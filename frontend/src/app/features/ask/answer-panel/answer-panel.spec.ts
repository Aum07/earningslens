import { ComponentFixture, TestBed } from '@angular/core/testing';
import { ChatResponse, Citation, KeyFigures } from '../../../core/models';
import { AnswerPanel } from './answer-panel';

/** Spec section 12 tests: citation marker to card linking, and abstention rendering. */
describe('AnswerPanel', () => {
  const citation = (marker: number, ticker: string): Citation => ({
    marker,
    chunkId: `chunk-${marker}`,
    ticker,
    fiscalYear: 2026,
    fiscalQuarter: 2,
    docType: 'PRESS_RELEASE',
    page: null,
    section: 'Financial highlights',
    speaker: null,
    sourceUrl: `https://example.test/${marker}`,
    snippet: `Snippet of source ${marker}`,
  });

  const answered: ChatResponse = {
    answer: 'Revenue was $81.3 billion [13]. Microsoft Cloud grew [2][13]. Note [7] is not a citation.',
    abstained: false,
    citations: [citation(13, 'MSFT'), citation(2, 'MSFT')],
    groundingWarnings: [],
    appliedFilters: { tickers: ['MSFT'], fiscalPeriods: [{ year: 2026, quarter: 2 }], docTypes: [] },
    latencyMs: 2140,
    model: 'gpt-4o-mini',
  };

  const abstained: ChatResponse = {
    answer: "I can't answer this from the documents.",
    abstained: true,
    citations: [],
    groundingWarnings: [],
    appliedFilters: { tickers: [], fiscalPeriods: [{ year: 2026, quarter: 3 }], docTypes: ['TRANSCRIPT'] },
    latencyMs: 900,
    model: 'gpt-4o-mini',
  };

  let fixture: ComponentFixture<AnswerPanel>;
  const el = () => fixture.nativeElement as HTMLElement;

  async function render(response: ChatResponse): Promise<void> {
    fixture.componentRef.setInput('response', response);
    await fixture.whenStable();
  }

  beforeEach(() => {
    TestBed.configureTestingModule({ imports: [AnswerPanel] });
    fixture = TestBed.createComponent(AnswerPanel);
    // Attach to the page so focus() works in the test DOM.
    document.body.appendChild(fixture.nativeElement);
  });

  afterEach(() => (fixture.nativeElement as HTMLElement).remove());

  it('turns each cited [n] into a button and leaves other brackets as text', async () => {
    await render(answered);
    const markers = [...el().querySelectorAll<HTMLButtonElement>('button.marker')];
    expect(markers.map((b) => b.textContent)).toEqual(['[13]', '[2]', '[13]']);
    expect(markers.every((b) => b.type === 'button')).toBe(true); // keyboard reachable
    expect(el().querySelector('.answer-text')?.textContent).toContain('Note [7] is not a citation.');
    expect(markers[0].getAttribute('aria-label')).toBe('Source 13: MSFT FY2026 Q2 Press release');
  });

  it('shows one citation card per cited source', async () => {
    await render(answered);
    const cards = [...el().querySelectorAll('app-citation-card')];
    expect(cards.map((c) => c.id)).toEqual(['citation-13', 'citation-2']);
  });

  it('clicking a marker highlights its card and moves focus to it', async () => {
    await render(answered);
    const second = el().querySelectorAll<HTMLButtonElement>('button.marker')[1]; // [2]
    second.click();
    await fixture.whenStable();

    const card = el().querySelector<HTMLElement>('#citation-2')!;
    expect(card.classList).toContain('highlighted');
    expect(card.textContent).toContain('Selected'); // not colour alone
    expect(document.activeElement).toBe(card);
    expect(el().querySelector('#citation-13')!.classList).not.toContain('highlighted');
    expect(second.classList).toContain('selected');
  });

  it('"View passage" opens the source drawer with the answer sentence that cites it', async () => {
    await render(answered);
    el().querySelector<HTMLButtonElement>('#citation-2 button.view')!.click();
    await new Promise((r) => setTimeout(r));
    const drawer = document.querySelector('app-source-drawer')!;
    expect(drawer.querySelector('#source-drawer-title')?.textContent).toContain('Press release');
    expect(drawer.querySelector('.supports')?.textContent).toBe('Microsoft Cloud grew.');
    expect(el().querySelector('#citation-2')!.classList).toContain('highlighted');
    drawer.querySelector<HTMLButtonElement>('.outline')!.click();
  });

  it('clears the highlight when a new response arrives', async () => {
    await render(answered);
    el().querySelector<HTMLButtonElement>('button.marker')!.click();
    await fixture.whenStable();
    await render({ ...answered, latencyMs: 1 });
    expect(el().querySelectorAll('app-citation-card.highlighted').length).toBe(0);
  });

  it('renders an abstention as a neutral information panel, not an error', async () => {
    await render(abstained);
    const panel = el().querySelector('.abstain')!;
    expect(panel.getAttribute('role')).toBe('status');
    expect(el().querySelector('[role="alert"]')).toBeNull();
    expect(panel.textContent).toContain('No answer from the documents');
    expect(panel.textContent).toContain("I can't answer this from the documents.");
    expect(panel.querySelectorAll('p').length).toBe(1); // only the backend's message, nothing repeated
    expect(el().querySelectorAll('button.marker').length).toBe(0);
    expect(el().querySelectorAll('app-citation-card').length).toBe(0);
  });

  it('shows grounding warnings as a quiet note under the answer', async () => {
    await render({ ...answered, groundingWarnings: ['Not found in the cited sources: 17%'] });
    const note = el().querySelector('.warnings')!;
    expect(note.getAttribute('role')).toBe('note');
    expect(note.textContent).toContain('Automatic checks flagged 1 item(s).');
    expect(note.textContent).toContain('Not found in the cited sources: 17%');
  });

  it('shows **bold** from the model as bold text, not asterisks', async () => {
    await render({ ...answered, answer: '**FY2026 Q2**\n\nRevenue was $81.3 billion [13].' });
    expect(el().querySelector('.answer-text strong')?.textContent).toBe('FY2026 Q2');
    expect(el().querySelector('.answer-text')?.textContent).not.toContain('**');
  });

  it('shows no warnings note when there are none', async () => {
    await render(answered);
    expect(el().querySelector('.warnings')).toBeNull();
    expect(el().textContent).toContain('gpt-4o-mini');
    expect(el().textContent).toContain('2.1 s');
  });

  describe('official SEC figures under the answer', () => {
    const figures: KeyFigures[] = [
      {
        ticker: 'MSFT', name: 'Microsoft Corporation', available: true, sourceUrl: 'x', fetchedAt: null,
        quarters: [
          { fiscalYear: 2026, fiscalQuarter: 2, periodEnd: '2025-12-31', revenue: 81273000000, netIncome: 38458000000, epsDiluted: 5.16, derived: false, form: '10-Q', filed: '2026-01-28', filingUrl: null },
          { fiscalYear: 2026, fiscalQuarter: 3, periodEnd: '2026-03-31', revenue: 82886000000, netIncome: 31778000000, epsDiluted: 4.27, derived: false, form: '10-Q', filed: '2026-04-29', filingUrl: null },
        ],
      },
      { ticker: 'META', name: 'Meta Platforms, Inc.', available: true, sourceUrl: 'x', fetchedAt: null, quarters: [] },
    ];

    async function renderWithFigures(response: ChatResponse): Promise<void> {
      fixture.componentRef.setInput('keyFigures', figures);
      await render(response);
    }

    it('shows only the companies the answer cites, with the cited quarter marked', async () => {
      await renderWithFigures(answered); // cites MSFT FY2026 Q2 only
      const cards = [...el().querySelectorAll('.sec-figures .company')];
      expect(cards.length).toBe(1);
      expect(cards[0].textContent).toContain('Microsoft Corporation');

      const cited = [...el().querySelectorAll('.sec-figures tr.cited-row')];
      expect(cited.length).toBe(1);
      expect(cited[0].textContent).toContain('FY2026 Q2');
      expect(cited[0].textContent).toContain('cited');
      expect(el().querySelectorAll('.sec-figures .bar.cited').length).toBe(1);
      expect(el().querySelector('.sec-figures .info-note')?.textContent).toContain('MSFT FY2026 Q2 = Oct-Dec 2025');
    });

    it('puts the figures panel next to the answer, and uses one column without it', async () => {
      await renderWithFigures(answered);
      expect(el().querySelector('.layout')?.classList).toContain('with-figures');
      await render({ ...answered, citations: [citation(1, 'GOOGL')], answer: 'Revenue [1].' });
      expect(el().querySelector('.layout')?.classList).not.toContain('with-figures');
    });

    it('shows no figures for an abstention', async () => {
      await renderWithFigures(abstained);
      expect(el().querySelector('.sec-figures')).toBeNull();
    });

    it('shows nothing when the figures are not loaded', async () => {
      await render(answered);
      expect(el().querySelector('.sec-figures')).toBeNull();
    });
  });
});

