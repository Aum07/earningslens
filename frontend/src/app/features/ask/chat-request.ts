import { ChatFilters, ChatRequest, DocType, FiscalPeriod } from '../../core/models';

/** What the user picked in the filter bar. An empty list means "no filter" for that field. */
export interface FilterSelection {
  tickers: string[];
  fiscalPeriods: FiscalPeriod[];
  docTypes: DocType[];
}

/**
 * Builds the body of POST /api/v1/chat (spec section 10). The question is trimmed. Only non-empty filter lists
 * are sent, and `filters` is left out entirely when nothing is selected, so the backend's own parser decides.
 */
export function buildChatRequest(question: string, selection: FilterSelection): ChatRequest {
  const filters: ChatFilters = {};
  if (selection.tickers.length > 0) filters.tickers = [...selection.tickers];
  if (selection.fiscalPeriods.length > 0) filters.fiscalPeriods = selection.fiscalPeriods.map((p) => ({ ...p }));
  if (selection.docTypes.length > 0) filters.docTypes = [...selection.docTypes];

  const request: ChatRequest = { question: question.trim() };
  if (Object.keys(filters).length > 0) request.filters = filters;
  return request;
}
