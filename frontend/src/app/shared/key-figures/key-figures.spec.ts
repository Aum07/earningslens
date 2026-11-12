import { ComponentFixture, TestBed } from '@angular/core/testing';
import { KeyFigures } from '../../core/models';
import { KeyFiguresView } from './key-figures';

describe('KeyFiguresView', () => {
  const figures: KeyFigures[] = [
    {
      ticker: 'MSFT', name: 'Microsoft Corporation', available: true, sourceUrl: 'x', fetchedAt: null,
      quarters: [
        { fiscalYear: 2026, fiscalQuarter: 3, periodEnd: '2026-03-31', revenue: 82886000000, netIncome: 31778000000, epsDiluted: 4.27, derived: false, form: '10-Q', filed: '2026-04-29', filingUrl: 'https://www.sec.gov/Archives/edgar/data/789019/a/' },
        { fiscalYear: 2026, fiscalQuarter: 4, periodEnd: '2026-06-30', revenue: 90007000000, netIncome: 35766000000, epsDiluted: null, derived: true, form: '10-K', filed: '2026-07-29', filingUrl: null },
      ],
    },
    { ticker: 'META', name: 'Meta Platforms, Inc.', available: false, sourceUrl: null, fetchedAt: null, quarters: [] },
  ];

  let fixture: ComponentFixture<KeyFiguresView>;
  const el = () => fixture.nativeElement as HTMLElement;

  async function render(cited: ReadonlySet<string> = new Set()): Promise<void> {
    fixture = TestBed.createComponent(KeyFiguresView);
    fixture.componentRef.setInput('figures', figures);
    fixture.componentRef.setInput('cited', cited);
    await fixture.whenStable();
  }

  it('shows a chart and a table per company, and marks derived quarters', async () => {
    await render();
    const cards = [...el().querySelectorAll('.card')];
    expect(cards.length).toBe(2);
    const rows = [...cards[0].querySelectorAll('tbody tr')].map((r) =>
      [...r.querySelectorAll('th, td')].slice(0, 4).map((c) => c.textContent?.replace(/\s+/g, ' ').trim()),
    );
    expect(rows).toEqual([
      ['FY2026 Q3', '$82.9B', '$31.8B', '$4.27'],
      ['FY2026 Q4 derived', '$90.0B', '$35.8B', 'n/a'],
    ]);
    const bars = [...cards[0].querySelectorAll('.bar-group')];
    expect(bars.map((b) => b.getAttribute('aria-label'))).toEqual([
      'MSFT FY2026 Q3 revenue $82.9B',
      'MSFT FY2026 Q4 revenue $90.0B, derived from the annual report',
    ]);
    expect(bars.every((b) => b.getAttribute('tabindex') === '0')).toBe(true);
    expect(cards[0].querySelectorAll('.bar.derived').length).toBe(1);
    expect(el().textContent).toContain('EPS cannot be derived that way');
    expect(cards[1].textContent).toContain('could not be loaded right now');
  });

  it('labels the latest bar when nothing is cited, and the cited bar when one is', async () => {
    await render();
    expect([...el().querySelectorAll('.value-label')].map((t) => t.textContent)).toEqual(['$90.0B']);

    await render(new Set(['MSFT:2026-3']));
    expect([...el().querySelectorAll('.value-label')].map((t) => t.textContent)).toEqual(['$82.9B']);
    expect(el().querySelector('tr.cited-row th')?.textContent).toContain('cited');
  });

  it('shows a tooltip while a bar has keyboard focus', async () => {
    await render();
    el().querySelector('.bar-group')!.dispatchEvent(new Event('focus'));
    await fixture.whenStable();
    const lines = [...el().querySelectorAll('.tooltip > *')].map((e) => e.textContent?.trim());
    expect(lines).toEqual(['FY2026 Q3', 'Revenue $82.9B', 'Net income $31.8B']);
  });
});
