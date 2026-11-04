package com.earningslens.eval;

/** The judge's reply could not be parsed or broke a rule of the rubric (wrong pairs, score out of range, ...). */
public class JudgeOutputException extends RuntimeException {

    public JudgeOutputException(String message) {
        super(message);
    }

    public JudgeOutputException(String message, Throwable cause) {
        super(message, cause);
    }
}
