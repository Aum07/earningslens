import { Dialog, DialogRef } from '@angular/cdk/dialog';
import { TestBed } from '@angular/core/testing';
import { Company } from '../../../core/models';
import { COMPANY_SEARCH_FROM, FilterState, FiltersModal } from './filters-modal';

describe('FiltersModal', () => {
  const companies: Company[] = [
    { ticker: 'MSFT', name: 'Microsoft Corporation', fiscalYearEndMonth: 6, periods: [{ year: 2026, quarter: 2 }] },
    { ticker: 'META', name: 'Meta Platforms, Inc.', fiscalYearEndMonth: 12, periods: [{ year: 2026, quarter: 2 }] },
  ];

  let ref: DialogRef<FilterState, FiltersModal>;
  let result: FilterState | undefined | 'open';
  const modal = () => document.querySelector('app-filters-modal') as HTMLElement;
  const footerButton = (text: string) =>
    [...modal().querySelectorAll<HTMLButtonElement>('footer button')].find((b) => b.textContent?.includes(text))!;
  const chip = (text: string) =>
    [...modal().querySelectorAll<HTMLButtonElement>('[role="checkbox"]')].find((b) =>
      (b.querySelector('.chip-main') ?? b).textContent?.includes(text),
    )!;
  const tick = () => new Promise((r) => setTimeout(r));

  async function open(current: FilterState = { tickers: [], periods: [], docTypes: [] }, list = companies): Promise<void> {
    result = 'open';
    ref = FiltersModal.open(TestBed.inject(Dialog), { companies: list, companiesError: null, current });
    ref.closed.subscribe((r) => (result = r));
    await tick();
  }

  afterEach(() => ref?.close());

  it('starts from the current filters and counts the selection on the Apply button', async () => {
    await open({ tickers: ['META'], periods: [], docTypes: ['FORM_10Q'] });
    expect(chip('Meta Platforms').getAttribute('aria-checked')).toBe('true');
    expect(footerButton('Apply').textContent?.trim()).toBe('Apply · 2 filters');
    chip('Form 10-Q').click();
    await tick();
    expect(footerButton('Apply').textContent?.trim()).toBe('Apply · 1 filter');
    expect(chip('Form 10-Q').getAttribute('aria-checked')).toBe('false');
  });

  it('shows one quarter chip with the months of every company that holds it', async () => {
    await open();
    expect(chip('FY2026 Q2').textContent?.replace(/\s+/g, ' ').trim()).toBe('FY2026 Q2, MSFT Oct-Dec 2025 · META Apr-Jun 2026');
  });

  it('Reset all clears the draft, and Apply returns it in on-screen order', async () => {
    await open({ tickers: ['META'], periods: ['2026-2'], docTypes: [] });
    footerButton('Reset all').click();
    await tick();
    expect(footerButton('Apply').textContent?.trim()).toBe('Apply');
    chip('Meta Platforms').click();
    chip('Microsoft Corporation').click();
    footerButton('Apply').click();
    expect(result).toEqual({ tickers: ['MSFT', 'META'], periods: [], docTypes: [] });
  });

  it('Cancel closes without a result', async () => {
    await open();
    chip('Microsoft Corporation').click();
    footerButton('Cancel').click();
    expect(result).toBeUndefined();
  });

  it('ends the fiscal note with one full stop when a name ends in "Inc."', async () => {
    await open();
    expect(modal().querySelector('.info-note')?.textContent?.trim()).toBe(
      "Quarters are each company's own fiscal quarters. Fiscal year ends in June: Microsoft Corporation. December: Meta Platforms, Inc.",
    );
  });

  it('shows the quarter months only for the ticked companies', async () => {
    await open({ tickers: ['META'], periods: [], docTypes: [] });
    expect(chip('FY2026 Q2').textContent?.replace(/\s+/g, ' ').trim()).toBe('FY2026 Q2, META Apr-Jun 2026');
  });

  describe('with many companies', () => {
    // 20 made-up companies: 16 with a December year end, 4 with a June year end. All hold FY2026 Q2.
    const many: Company[] = Array.from({ length: 20 }, (_, i) => ({
      ticker: `T${String(i).padStart(2, '0')}`,
      name: `Company ${i}`,
      fiscalYearEndMonth: i < 16 ? 12 : 6,
      periods: [{ year: 2026, quarter: 2 }],
    }));

    it('adds a company search box that filters by ticker or name', async () => {
      expect(many.length).toBeGreaterThan(COMPANY_SEARCH_FROM);
      await open(undefined, many);
      const search = modal().querySelector<HTMLInputElement>('input[type="search"]')!;
      search.value = 'company 1';
      search.dispatchEvent(new Event('input'));
      await tick();
      const shown = modal().querySelectorAll('[aria-labelledby="f-companies"] [role="checkbox"]');
      expect(shown.length).toBe(11); // Company 1 and Company 10 to 19
      search.value = 'zzz';
      search.dispatchEvent(new Event('input'));
      await tick();
      expect(modal().textContent).toContain('No company matches "zzz".');
    });

    it('counts long ticker lists and groups the fiscal year ends', async () => {
      await open(undefined, many);
      expect(chip('FY2026 Q2').textContent?.replace(/\s+/g, ' ').trim()).toBe(
        'FY2026 Q2, 16 companies Apr-Jun 2026 · 4 companies Oct-Dec 2025',
      );
      expect(modal().querySelector('.info-note')?.textContent).toContain('Fiscal year ends in June: 4 companies. December: 16 companies.');
    });
  });

  it('has no company search box for a short list', async () => {
    await open();
    expect(modal().querySelector('input[type="search"]')).toBeNull();
  });
});
