/**
 * Number formats for eval metrics, the same as the committed ablation report (eval/reports/*-ablation-test.md),
 * so the page can be checked against that file digit for digit. null (metric not available) shows as "n/a".
 */
const NA = 'n/a';

/** 0.4772 -> "0.48" (recall, MRR, period coverage, faithfulness, correctness). */
export function fixed2(value: number | null | undefined): string {
  return value === null || value === undefined ? NA : value.toFixed(2);
}

/** 0.8636 -> "86%" (fully faithful, citation precision, number grounding, abstention rates, agreement). */
export function percent(value: number | null | undefined): string {
  return value === null || value === undefined ? NA : `${Math.round(value * 100)}%`;
}

/** 0.00090614 -> "$0.0009". */
export function usd4(value: number | null | undefined): string {
  return value === null || value === undefined ? NA : `$${value.toFixed(4)}`;
}

/** 81273000000 -> "$81.3B"; 950000000 -> "$950M"; negative values keep their sign ("-$1.2B"). */
export function usdCompact(value: number | null | undefined): string {
  if (value === null || value === undefined) return 'n/a';
  const sign = value < 0 ? '-' : '';
  const abs = Math.abs(value);
  if (abs >= 1e9) return `${sign}$${(abs / 1e9).toFixed(1)}B`;
  if (abs >= 1e6) return `${sign}$${Math.round(abs / 1e6)}M`;
  return `${sign}$${abs.toLocaleString('en-US')}`;
}

/** 5.16 -> "$5.16". */
export function usd2(value: number | null | undefined): string {
  if (value === null || value === undefined) return 'n/a';
  return `${value < 0 ? '-' : ''}$${Math.abs(value).toFixed(2)}`;
}
