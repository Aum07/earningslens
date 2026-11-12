import { HttpErrorResponse, HttpInterceptorFn } from '@angular/common/http';
import { catchError, throwError } from 'rxjs';
import { ApiError, ApiErrorKind, ProblemDetail } from './models';

/**
 * Functional HTTP interceptor: runs on every request made with HttpClient. Here it only looks at failures and
 * turns each one into an ApiError with a message that is safe to show. Successful responses pass through.
 */
export const httpErrorInterceptor: HttpInterceptorFn = (req, next) =>
  next(req).pipe(catchError((error: unknown) => throwError(() => toApiError(error))));

const DEFAULT_MESSAGES: Record<ApiErrorKind, string> = {
  validation: 'The request was not valid.',
  'not-found': 'Not found.',
  'rate-limited': 'Too many requests. Please wait a moment and try again.',
  unavailable: 'The service is not available right now. Please try again in a minute.',
  server: 'Something went wrong on our side. Please try again.',
  network: 'Cannot reach the EarningsLens server. Check that it is running and try again.',
  unknown: 'Something went wrong. Please try again.',
};

export function toApiError(error: unknown): ApiError {
  if (error instanceof ApiError) {
    return error;
  }
  if (!(error instanceof HttpErrorResponse)) {
    return new ApiError('unknown', 0, DEFAULT_MESSAGES.unknown);
  }
  const kind = kindFor(error.status);
  const problem = asProblem(error.error);
  // The backend's "detail" is written for users and never holds internals (GlobalExceptionHandler).
  const message = problem?.detail?.trim() || DEFAULT_MESSAGES[kind];
  return new ApiError(kind, error.status, message, problem?.errors ?? [], retryAfter(error, problem));
}

function kindFor(status: number): ApiErrorKind {
  if (status === 0) return 'network';
  if (status === 400) return 'validation';
  if (status === 404) return 'not-found';
  if (status === 429) return 'rate-limited';
  if (status === 503) return 'unavailable';
  if (status >= 500) return 'server';
  return 'unknown';
}

/** The body as a ProblemDetail, or null when it is not one (e.g. an HTML error page from a proxy). */
function asProblem(body: unknown): ProblemDetail | null {
  if (body === null || typeof body !== 'object' || Array.isArray(body)) {
    return null;
  }
  const problem = body as ProblemDetail;
  const errors = Array.isArray(problem.errors)
    ? problem.errors.filter((e) => typeof e?.field === 'string' && typeof e?.message === 'string')
    : undefined;
  return { ...problem, detail: typeof problem.detail === 'string' ? problem.detail : undefined, errors };
}

function retryAfter(error: HttpErrorResponse, problem: ProblemDetail | null): number | null {
  if (error.status !== 429) return null;
  if (typeof problem?.retryAfterSeconds === 'number') return problem.retryAfterSeconds;
  const header = Number(error.headers?.get('Retry-After'));
  return Number.isFinite(header) && header > 0 ? header : null;
}
