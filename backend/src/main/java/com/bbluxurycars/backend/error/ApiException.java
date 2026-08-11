package com.bbluxurycars.backend.error;

import org.springframework.http.HttpStatus;

/**
 * A refusal the API states deliberately: an HTTP status plus a stable
 * machine-readable code.
 *
 * <p>Extracted from {@code BookingException} once a second area needed the same
 * shape. The code is the part clients branch on -- messages are free to change
 * or be translated, so anything a client must react to specifically belongs in
 * the code, not the prose.
 */
public class ApiException extends RuntimeException {

    private final HttpStatus status;
    private final String code;

    public ApiException(HttpStatus status, String code, String message) {
        super(message);
        this.status = status;
        this.code = code;
    }

    public HttpStatus getStatus() {
        return status;
    }

    public String getCode() {
        return code;
    }

    /**
     * The caller is known but not entitled to this action. 403 rather than 401
     * because their token was valid -- re-authenticating would not help.
     */
    public static ApiException forbidden(String code, String reason) {
        return new ApiException(HttpStatus.FORBIDDEN, code, reason);
    }
}
