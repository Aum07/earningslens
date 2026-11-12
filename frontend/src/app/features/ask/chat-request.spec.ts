import { buildChatRequest } from './chat-request';

describe('buildChatRequest', () => {
  const none = { tickers: [], fiscalPeriods: [], docTypes: [] };

  it('sends only the trimmed question when no filter is selected', () => {
    expect(buildChatRequest('  What was Microsoft revenue?  ', none)).toEqual({ question: 'What was Microsoft revenue?' });
  });

  it('sends every selected filter', () => {
    expect(
      buildChatRequest('q?', {
        tickers: ['MSFT', 'META'],
        fiscalPeriods: [{ year: 2026, quarter: 2 }],
        docTypes: ['TRANSCRIPT'],
      }),
    ).toEqual({
      question: 'q?',
      filters: { tickers: ['MSFT', 'META'], fiscalPeriods: [{ year: 2026, quarter: 2 }], docTypes: ['TRANSCRIPT'] },
    });
  });

  it('leaves out empty filter lists', () => {
    expect(buildChatRequest('q?', { ...none, docTypes: ['FORM_10Q'] })).toEqual({
      question: 'q?',
      filters: { docTypes: ['FORM_10Q'] },
    });
  });

  it('copies the lists, so later changes to the selection do not change a sent request', () => {
    const tickers = ['MSFT'];
    const request = buildChatRequest('q?', { ...none, tickers });
    tickers.push('META');
    expect(request.filters?.tickers).toEqual(['MSFT']);
  });
});
