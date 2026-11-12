/**
 * A piece of answer text: plain or bold text, or a [n] citation marker.
 * Bold comes from Markdown **...** that the model sometimes writes (for example "**FY2026 Q3**").
 */
export type AnswerSegment = { kind: 'text'; text: string; bold: boolean } | { kind: 'marker'; marker: number };

const MARKER = /\[(\d{1,3})\]/g;
const BOLD = /\*\*([^*]+?)\*\*/g;

/**
 * Splits an answer into text and markers. The backend writes every marker as "[n]" and n is the source's number
 * (it can be [13] with no [1]). Only numbers that have a citation become markers; anything else stays text.
 * **text** becomes bold text (the asterisks are dropped); a lone ** stays as typed. Nothing is turned into HTML.
 */
export function splitAnswer(answer: string, citedMarkers: ReadonlySet<number>): AnswerSegment[] {
  const segments: AnswerSegment[] = [];
  let last = 0;
  for (const match of answer.matchAll(BOLD)) {
    const start = match.index ?? 0;
    splitMarkers(answer.slice(last, start), false, citedMarkers, segments);
    splitMarkers(match[1], true, citedMarkers, segments);
    last = start + match[0].length;
  }
  splitMarkers(answer.slice(last), false, citedMarkers, segments);
  return segments;
}

function splitMarkers(text: string, bold: boolean, cited: ReadonlySet<number>, out: AnswerSegment[]): void {
  let last = 0;
  for (const match of text.matchAll(MARKER)) {
    const marker = Number(match[1]);
    if (!cited.has(marker)) continue;
    const start = match.index ?? 0;
    if (start > last) out.push({ kind: 'text', text: text.slice(last, start), bold });
    out.push({ kind: 'marker', marker });
    last = start + match[0].length;
  }
  if (last < text.length) out.push({ kind: 'text', text: text.slice(last), bold });
}

/**
 * The answer sentences that cite source `marker`, as plain text (markers and ** removed), for the source drawer's
 * "Supports this sentence". A sentence ends at ".", "!" or "?" followed by a space or a line break; a marker written
 * after the full stop ("... billion. [3]") belongs to the sentence before it.
 */
export function sentencesCiting(answer: string, marker: number): string[] {
  const tag = `[${marker}]`;
  return answer
    .replace(/([.!?])((?:\s*\[\d{1,3}\])+)/g, '$2$1') // move trailing markers inside the sentence
    .split(/(?<=[.!?])\s+|\n+/)
    .filter((s) => s.includes(tag))
    .map((s) => s.replace(/\s*\[\d{1,3}\]/g, '').replace(/\*\*/g, '').trim())
    .filter((s) => s.length > 0);
}
