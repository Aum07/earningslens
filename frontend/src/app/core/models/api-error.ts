/**
 * What every failed API call turns into (made by httpErrorInterceptor), so pages handle one error type.
 * The backend sends errors as RFC 7807 ProblemDetail JSON (GlobalExceptionHandler).
 */

export type ApiErrorKind =
  | 'validation' // 400: the request was not valid
  | 'not-found' // 404: e.g. no eval report yet
  | 'rate-limited' // 429: too many questions
  | 'unavailable' // 503: the language model is down
  | 'server' // other 5xx
  | 'network' // no response at all: backend not running, CORS, offline
  | 'unknown';

/** One entry of the "errors" list in a 400 ProblemDetail, e.g. { field: "question", message: "size must be ..." }. */
export interface FieldError {
  field: string;
  message: string;
}

/** RFC 7807 body as the backend sends it (extra properties are optional). */
export interface ProblemDetail {
  type?: string;
  title?: string;
  status?: number;
  detail?: string;
  instance?: string;
  errors?: FieldError[];
  reason?: string;
  retryAfterSeconds?: number;
}

export class ApiError extends Error {
  constructor(
    readonly kind: ApiErrorKind,
    /** HTTP status, 0 when there was no response. */
    readonly status: number,
    /** Text that is safe to show the user. */
    message: string,
    readonly fieldErrors: FieldError[] = [],
    /** Seconds to wait before retrying (429 only). */
    readonly retryAfterSeconds: number | null = null,
  ) {
    super(message);
    this.name = 'ApiError';
  }

  /** Worth offering a "Try again" button for. */
  get retryable(): boolean {
    return this.kind === 'network' || this.kind === 'unavailable' || this.kind === 'server' || this.kind === 'rate-limited';
  }
}
