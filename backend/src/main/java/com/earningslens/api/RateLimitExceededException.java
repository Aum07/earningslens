package com.earningslens.api;

/** A chat request was refused by the rate limit (spec 13). The API answers 429 with a Retry-After header. */
public class RateLimitExceededException extends RuntimeException {

    public enum Reason {
        /** Too many requests from one IP address in the last minute. */
        PER_IP,
        /** The global daily chat cap was reached. */
        DAILY_CAP
    }

    private final Reason reason;
    private final long retryAfterSeconds;

    public RateLimitExceededException(Reason reason, long retryAfterSeconds) {
        super("rate limit: " + reason);
        this.reason = reason;
        this.retryAfterSeconds = retryAfterSeconds;
    }

    public Reason reason() {
        return reason;
    }

    public long retryAfterSeconds() {
        return retryAfterSeconds;
    }
}
