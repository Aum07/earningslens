import { calendarMonths, fiscalQuarterMonths, periodLabel, shortPeriodLabel } from './labels';

describe('labels', () => {
  it('gives the calendar months of a fiscal quarter from its period end', () => {
    expect(calendarMonths('2025-12-31')).toBe('Oct-Dec 2025'); // MSFT FY2026 Q2
    expect(calendarMonths('2026-06-30')).toBe('Apr-Jun 2026');
    expect(calendarMonths('2026-03-31')).toBe('Jan-Mar 2026');
    expect(calendarMonths('2026-01-31')).toBe('Nov 2025-Jan 2026'); // a quarter across New Year
    expect(calendarMonths('')).toBe('');
  });

  it('has a long and a short fiscal period label', () => {
    expect(periodLabel({ year: 2026, quarter: 2 })).toBe('FY2026 Q2');
    expect(shortPeriodLabel({ year: 2026, quarter: 2 })).toBe('Q2 FY26');
  });

  it('gives the calendar months of a fiscal quarter from the fiscal year end month', () => {
    expect(fiscalQuarterMonths(6, 2026, 2)).toBe('Oct-Dec 2025'); // Microsoft
    expect(fiscalQuarterMonths(6, 2027, 1)).toBe('Jul-Sep 2026');
    expect(fiscalQuarterMonths(12, 2026, 2)).toBe('Apr-Jun 2026'); // Alphabet
    expect(fiscalQuarterMonths(12, 2025, 4)).toBe('Oct-Dec 2025');
  });
});
