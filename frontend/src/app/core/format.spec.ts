import { fixed2, percent, usd2, usd4, usdCompact } from './format';

/** Values taken from eval/reports/latest.json (config A) and the strings the ablation report shows for them. */
describe('eval number formats', () => {
  it('match the ablation report', () => {
    expect(fixed2(0.4772727272727273)).toBe('0.48');
    expect(fixed2(4.510204081632653)).toBe('4.51');
    expect(percent(0.8636363636363636)).toBe('86%');
    expect(percent(0.9107142857142857)).toBe('91%');
    expect(percent(0)).toBe('0%');
    expect(usd4(0.00090614)).toBe('$0.0009');
  });

  it('show n/a for a missing metric, never 0', () => {
    expect(fixed2(null)).toBe('n/a');
    expect(percent(null)).toBe('n/a');
    expect(usd4(undefined)).toBe('n/a');
  });

  it('formats SEC figures compactly', () => {
    expect(usdCompact(81273000000)).toBe('$81.3B');
    expect(usdCompact(950000000)).toBe('$950M');
    expect(usdCompact(-1200000000)).toBe('-$1.2B');
    expect(usdCompact(null)).toBe('n/a');
    expect(usd2(5.16)).toBe('$5.16');
    expect(usd2(null)).toBe('n/a');
  });
});
