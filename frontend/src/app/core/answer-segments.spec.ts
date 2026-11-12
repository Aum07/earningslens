import { sentencesCiting, splitAnswer } from './answer-segments';

describe('splitAnswer', () => {
  const text = (t: string, bold = false) => ({ kind: 'text', text: t, bold });
  const marker = (m: number) => ({ kind: 'marker', marker: m });

  it('splits text and markers in order', () => {
    expect(splitAnswer('Revenue was $81.3 billion [13]. Up 17% [2][13].', new Set([2, 13]))).toEqual([
      text('Revenue was $81.3 billion '),
      marker(13),
      text('. Up 17% '),
      marker(2),
      marker(13),
      text('.'),
    ]);
  });

  it('keeps numbers in brackets that are not citations as text', () => {
    expect(splitAnswer('See note [4] and [1].', new Set([1]))).toEqual([text('See note [4] and '), marker(1), text('.')]);
  });

  it('returns one text segment when there are no markers', () => {
    expect(splitAnswer('No markers here.', new Set())).toEqual([text('No markers here.')]);
  });

  it('returns nothing for an empty answer', () => {
    expect(splitAnswer('', new Set([1]))).toEqual([]);
  });

  it('turns **text** into bold text without the asterisks', () => {
    expect(splitAnswer('**FY2026 Q3**\n\nMeta launched Muse [4].', new Set([4]))).toEqual([
      text('FY2026 Q3', true),
      text('\n\nMeta launched Muse '),
      marker(4),
      text('.'),
    ]);
  });

  it('keeps markers working inside bold text', () => {
    expect(splitAnswer('**Revenue [1]** grew.', new Set([1]))).toEqual([
      text('Revenue ', true),
      marker(1),
      text(' grew.'),
    ]);
  });

  it('leaves a lone ** as typed', () => {
    expect(splitAnswer('Growth ** was strong.', new Set())).toEqual([text('Growth ** was strong.')]);
  });
});

describe('sentencesCiting', () => {
  const answer =
    '**FY2026 Q2**\n\nRevenue was $81.3 billion, up 17% [3]. Microsoft Cloud grew [3][12]. Guidance was not given. [7]';

  it('returns the sentences that cite a marker, without markers or asterisks', () => {
    expect(sentencesCiting(answer, 3)).toEqual(['Revenue was $81.3 billion, up 17%.', 'Microsoft Cloud grew.']);
    expect(sentencesCiting(answer, 12)).toEqual(['Microsoft Cloud grew.']);
  });

  it('keeps a marker written after the full stop with its sentence', () => {
    expect(sentencesCiting(answer, 7)).toEqual(['Guidance was not given.']);
  });

  it('returns nothing for a marker the answer does not use', () => {
    expect(sentencesCiting(answer, 99)).toEqual([]);
  });
});
