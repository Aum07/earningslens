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
  const text = (e: Element | null | undefined) => e?.textContent?.replace(/\s+/g, ' ').trim();

  async function render(cited: ReadonlySet<string> = new Set()): Promise<void> {
    fixture = TestBed.createComponent(KeyFiguresView);
    fixture.componentRef.setInput('figures', figures);
    fixture.componentRef.setInput('cited', cited);
    await fixture.whenStable();
  }

  it('shows a table per company with calendar months, and marks derived quarters', async () => {
    await render();
    const companies = [...el().querySelectorAll('.company')];
    expect(companies.length).toBe(2);
    const rows = [...companies[0].querySelectorAll('tbody tr')].map((r) =>
      [...r.querySelectorAll('th, td')].slice(0, 4).map((c) => text(c)),
    );
    expect(rows).toEqual([
      ['FY2026 Q3, Jan-Mar 2026', '$82.9B', '$31.8B', '$4.27'],
      ['FY2026 Q4*, Apr-Jun 2026, derived', '$90.0B', '$35.8B', 'n/a'],
    ]);
    expect(companies[0].querySelectorAll('.bar.derived').length).toBe(1);
    expect(el().textContent).toContain('EPS cannot be derived that way');
    expect(companies[1].textContent).toContain('could not be loaded right now');
  });

  it('labels every bar, and switches the chart between revenue, net income and EPS', async () => {
    await render();
    const labels = () => [...el().querySelectorAll('.value-label')].map((t) => t.textContent);
    expect(labels()).toEqual(['$82.9B', '$90.0B']);
    expect([...el().querySelectorAll('.quarter-label')].map((t) => t.textContent)).toEqual(['Q3 FY26', 'Q4 FY26']);

    const tabs = [...el().querySelectorAll<HTMLButtonElement>('.tab')];
    expect(tabs.map((t) => text(t))).toEqual(['Revenue', 'Net income', 'EPS']);
    expect(tabs[0].getAttribute('aria-pressed')).toBe('true');

    tabs[1].click();
    await fixture.whenStable();
    expect(labels()).toEqual(['$31.8B', '$35.8B']);
    expect(tabs[1].getAttribute('aria-pressed')).toBe('true');

    tabs[2].click();
    await fixture.whenStable();
    expect(labels()).toEqual(['$4.27', 'n/a']); // derived Q4 has no EPS, so no bar
    expect(el().querySelectorAll('.company')[0].querySelectorAll('.bar').length).toBe(1);
  });

  it('marks the cited quarter in gold, in the table, and in the calendar note', async () => {
    await render();
    expect(text(el().querySelector('.info-note'))).toContain('MSFT FY2026 Q4 = Apr-Jun 2026'); // latest, nothing cited

    await render(new Set(['MSFT:2026-3']));
    expect(el().querySelectorAll('.bar.cited').length).toBe(1);
    const row = el().querySelector('tr.cited-row th');
    expect(text(row)).toContain('FY2026 Q3');
    expect(text(row)).toContain('cited in the answer');
    expect(text(el().querySelector('.info-note'))).toContain('MSFT FY2026 Q3 = Jan-Mar 2026');
  });
});
