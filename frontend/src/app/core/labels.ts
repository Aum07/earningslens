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
