import { DocType, FiscalPeriod } from './models';

/** Human-readable names for the backend's DocType values. */
export const DOC_TYPE_LABELS: Record<DocType, string> = {
  PRESS_RELEASE: 'Press release',
  TRANSCRIPT: 'Earnings call transcript',
  FORM_10Q: 'Form 10-Q',
  FORM_10K: 'Form 10-K',
};

/** "FY2026 Q2": the company's own fiscal quarter, the same label the backend uses for sources. */
export function periodLabel(period: FiscalPeriod): string {
  return `FY${period.year} Q${period.quarter}`;
}

/** Stable string key for a period, e.g. "2026-2" (handy as a select option value). */
export function periodKey(period: FiscalPeriod): string {
  return `${period.year}-${period.quarter}`;
}

export function parsePeriodKey(key: string): FiscalPeriod {
  const [year, quarter] = key.split('-').map(Number);
  return { year, quarter };
}

const MONTHS = ['January', 'February', 'March', 'April', 'May', 'June', 'July', 'August', 'September', 'October', 'November', 'December'];

export function monthName(month: number): string {
  return MONTHS[month - 1] ?? `month ${month}`;
}

const SHORT_MONTHS = ['Jan', 'Feb', 'Mar', 'Apr', 'May', 'Jun', 'Jul', 'Aug', 'Sep', 'Oct', 'Nov', 'Dec'];

/**
 * The calendar months of a fiscal quarter, from its period end date as filed ("2025-12-31" -> "Oct-Dec 2025").
 * DESIGN.md content rule: a fiscal quarter is always shown with its calendar months.
 */
export function calendarMonths(periodEnd: string): string {
  const [year, month] = periodEnd.split('-').map(Number);
  if (!year || !month) return '';
  const startMonth = ((month - 3 + 12) % 12) + 1; // three months ending in `month`
  const startYear = startMonth > month ? year - 1 : year;
  const start = SHORT_MONTHS[startMonth - 1];
  const end = SHORT_MONTHS[month - 1];
  return startYear === year ? `${start}-${end} ${year}` : `${start} ${startYear}-${end} ${year}`;
}

/** "Q2 FY26": the short form for chart axes, where "FY2026 Q2" does not fit. */
export function shortPeriodLabel(period: FiscalPeriod): string {
  return `Q${period.quarter} FY${String(period.year).slice(-2)}`;
}

/**
 * Calendar months of fiscal quarter `quarter` of fiscal year `year`, for a company whose fiscal year ends in
 * `fiscalYearEndMonth` (from /companies). Microsoft (June): FY2026 Q2 -> "Oct-Dec 2025"; Alphabet (December):
 * FY2026 Q2 -> "Apr-Jun 2026".
 */
export function fiscalQuarterMonths(fiscalYearEndMonth: number, year: number, quarter: number): string {
  const end = year * 12 + (fiscalYearEndMonth - 1) - 3 * (4 - quarter); // months since year 0, zero-based
  const endYear = Math.floor(end / 12);
  const endMonth = (end % 12) + 1;
  return calendarMonths(`${endYear}-${String(endMonth).padStart(2, '0')}-01`);
}
