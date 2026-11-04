package com.glassstore.common.exception;

import lombok.Getter;

import java.util.Collections;
import java.util.Map;

/**
 * The single exception type services should throw for an expected, business-level failure.
 *
 * <p>Carrying an {@link ErrorCode} keeps the HTTP status and the machine-readable body in
 * sync, so a caller never has to interpret a free-text message to know what happened.</p>
 */
@Getter
public class ApiException extends RuntimeException {

    private final ErrorCode errorCode;
    private final transient Map<String, Object> details;

    public ApiException(ErrorCode errorCode) {
        super(errorCode.getDefaultMessage());
        this.errorCode = errorCode;
        this.details = Collections.emptyMap();
    }

    public ApiException(ErrorCode errorCode, String message) {
        super(message);
        this.errorCode = errorCode;
        this.details = Collections.emptyMap();
    }

    public ApiException(ErrorCode errorCode, String message, Map<String, Object> details) {
        super(message);
        this.errorCode = errorCode;
        this.details = details == null ? Collections.emptyMap() : details;
    }

    public static ApiException of(ErrorCode errorCode) {
        return new ApiException(errorCode);
    }

    public static ApiException of(ErrorCode errorCode, String message) {
        return new ApiException(errorCode, message);
    }

    /** Variant carrying structured context for the client (e.g. price-change details). */
    public static ApiException of(ErrorCode errorCode, String message, Map<String, Object> details) {
        return new ApiException(errorCode, message, details);
    }
}