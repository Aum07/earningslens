import { DIALOG_DATA, Dialog, DialogRef } from '@angular/cdk/dialog';
import { Component, WritableSignal, computed, inject, signal } from '@angular/core';
import { DOC_TYPE_LABELS, fiscalQuarterMonths, monthName, periodKey, periodLabel } from '../../../core/labels';
import { Company, DOC_TYPES, DocType } from '../../../core/models';

/** From this many companies on, the Companies group gets a search box. */
export const COMPANY_SEARCH_FROM = 6;

/** The filters as the Ask page keeps them; quarters as period keys such as "2026-2". */
export interface FilterState {
  tickers: string[];
  periods: string[];
  docTypes: DocType[];
}

export interface FiltersModalData {
  companies: Company[];
  /** Set when /companies failed: the modal then offers document types only. */
  companiesError: string | null;
  current: FilterState;
}

/**
 * The Filters modal (DESIGN.md sections 5 and 6, restyle screen 4): 580px, centered over the modal scrim.
 * Groups: Companies, Quarters and Document types, every option a checkbox chip; options come from /companies.
 * Quarters are chips only (Aum's choice, 2026-11-12): presets such as "Latest" do not fit, because one quarter list
 * applies to every company and the companies use different fiscal years. Each quarter chip shows its calendar months
 * per company. The Speaker group is left out (the API has no speaker filter).
 * The modal edits a draft: Apply closes with the new filters, Cancel / Esc / scrim click closes with nothing.
 */
@Component({
  selector: 'app-filters-modal',
  templateUrl: './filters-modal.html',
  styleUrl: './filters-modal.scss',
})
export class FiltersModal {
  readonly data = inject<FiltersModalData>(DIALOG_DATA);
  private readonly ref = inject<DialogRef<FilterState>>(DialogRef);

  readonly tickers = signal<string[]>([...this.data.current.tickers]);
  readonly periods = signal<string[]>([...this.data.current.periods]);
  readonly docTypes = signal<DocType[]>([...this.data.current.docTypes]);

  readonly companyOptions = this.data.companies.map((c) => ({
    ticker: c.ticker,
    name: c.name,
    range: c.periods.length
      ? `${periodLabel(c.periods[0])} to ${periodLabel(c.periods[c.periods.length - 1])}`
      : 'no quarters yet',
  }));

  /** The company search box shows only when the list is long enough to need it. */
  readonly showCompanySearch = this.companyOptions.length > COMPANY_SEARCH_FROM;
  readonly companyQuery = signal('');
  /** Companies matching the search box (ticker or name, any case). Ticked companies stay ticked when hidden. */
  readonly visibleCompanies = computed(() => {
    const q = this.companyQuery().trim().toLowerCase();
    return q
      ? this.companyOptions.filter((c) => c.ticker.toLowerCase().includes(q) || c.name.toLowerCase().includes(q))
      : this.companyOptions;
  });

  /**
   * Every quarter held for any company, oldest first. The second line gives calendar months: for the ticked
   * companies when some are ticked, otherwise for all. Long lists are shortened ("12 companies Oct-Dec 2025"),
   * and with more than three different calendars the chip says the months depend on the company.
   */
  readonly periodOptions = computed(() => {
    const ticked = this.tickers();
    const companies = ticked.length ? this.data.companies.filter((c) => ticked.includes(c.ticker)) : this.data.companies;
    const keys = new Map<string, { key: string; label: string; sort: number }>();
    for (const c of this.data.companies) {
      for (const p of c.periods) keys.set(periodKey(p), { key: periodKey(p), label: periodLabel(p), sort: p.year * 10 + p.quarter });
    }
    return [...keys.values()]
      .sort((a, b) => a.sort - b.sort)
      .map(({ key, label }) => {
        const groups = new Map<string, string[]>(); // calendar months -> tickers
        for (const c of companies) {
          const p = c.periods.find((x) => periodKey(x) === key);
          if (!p) continue;
          const months = fiscalQuarterMonths(c.fiscalYearEndMonth, p.year, p.quarter);
          groups.set(months, [...(groups.get(months) ?? []), c.ticker]);
        }
        const months =
          groups.size === 0
            ? 'Not held for the ticked companies'
            : groups.size > 3
              ? 'Months depend on the company. Tick companies to see them.'
              : [...groups].map(([m, t]) => `${t.length > 3 ? `${t.length} companies` : t.join(', ')} ${m}`).join(' · ');
        return { key, label, months };
      });
  });

  readonly docTypeOptions = DOC_TYPES.map((value) => ({ value, label: DOC_TYPE_LABELS[value] }));

  /** Fiscal year ends grouped by month: "June: Microsoft Corporation. December: Alphabet Inc., Meta Platforms, Inc." */
  readonly fiscalNote = (() => {
    const byMonth = new Map<number, string[]>();
    for (const c of this.data.companies) byMonth.set(c.fiscalYearEndMonth, [...(byMonth.get(c.fiscalYearEndMonth) ?? []), c.name]);
    if (byMonth.size === 0) return null;
    const parts = [...byMonth]
      .sort(([a], [b]) => a - b)
      .map(([m, names]) => `${monthName(m)}: ${names.length > 3 ? `${names.length} companies` : names.join(', ')}`);
    // A name such as "Meta Platforms, Inc." already ends with a full stop: no second one.
    const text = parts.map((p) => p.replace(/\.$/, '')).join('. ');
    return `Quarters are each company's own fiscal quarters. Fiscal year ends in ${text}.`;
  })();

  readonly count = computed(() => this.tickers().length + this.periods().length + this.docTypes().length);
  readonly applyLabel = computed(() => {
    const n = this.count();
    return n === 0 ? 'Apply' : `Apply · ${n} ${n === 1 ? 'filter' : 'filters'}`;
  });

  /** Adds or removes one option. update(): sets a signal from its current value. */
  toggle<T>(list: WritableSignal<T[]>, value: T): void {
    list.update((values) => (values.includes(value) ? values.filter((v) => v !== value) : [...values, value]));
  }

  reset(): void {
    this.tickers.set([]);
    this.periods.set([]);
    this.docTypes.set([]);
  }

  cancel(): void {
    this.ref.close();
  }

  apply(): void {
    // Keep the on-screen order, so chips and the request read the same way every time.
    const order = <T>(options: readonly T[], picked: T[]) => options.filter((o) => picked.includes(o));
    this.ref.close({
      tickers: order(this.companyOptions.map((c) => c.ticker), this.tickers()),
      periods: order(this.periodOptions().map((p) => p.key), this.periods()),
      docTypes: order(DOC_TYPES, this.docTypes()),
    });
  }

  static open(dialog: Dialog, data: FiltersModalData): DialogRef<FilterState, FiltersModal> {
    return dialog.open<FilterState, FiltersModalData, FiltersModal>(FiltersModal, {
      data,
      ariaLabelledBy: 'filters-title',
      backdropClass: 'modal-scrim',
      panelClass: 'modal-panel',
      width: '580px',
      maxWidth: 'calc(100vw - 32px)',
      maxHeight: 'calc(100vh - 48px)',
      autoFocus: 'first-tabbable',
    });
  }
}
