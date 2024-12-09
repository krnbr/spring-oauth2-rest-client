package in.neuw.oauth2.client.exceptions;

import lombok.Getter;
import org.springframework.http.HttpStatusCode;

@Getter
public class CustomRuntimeException extends RuntimeException {

    private final HttpStatusCode status;
    private final int code;
    private String details;
    private String field;
    private int errorCode;

    public CustomRuntimeException(final String message,
                                  final HttpStatusCode status) {
        super(message);
        this.status = status;
        this.code = status.value();
    }

    public CustomRuntimeException(final String message,
                                  final Exception cause,
                                  final HttpStatusCode status) {
        super(message, cause);
        this.status = status;
        this.code = status.value();
    }

    public CustomRuntimeException(final String message,
                                  final Exception cause,
                                  final HttpStatusCode status,
                                  final String details) {
        super(message, cause);
        this.status = status;
        this.code = status.value();
        this.details = details;
    }

    public CustomRuntimeException(final String message,
                                  final HttpStatusCode status,
                                  final String field, final String details) {
        super(message);
        this.status = status;
        this.code = status.value();
        this.field = field;
        this.details = details;
    }

    public CustomRuntimeException(final String message,
                                  final HttpStatusCode status,
                                  final String details) {
        super(message);
        this.status = status;
        this.code = status.value();
        this.details = details;
    }

    public CustomRuntimeException(final String message,
                                  final Exception cause,
                                  final HttpStatusCode status,
                                  final String field, final String details) {
        super(message, cause);
        this.status = status;
        this.code = status.value();
        this.field = field;
        this.details = details;
    }

    public CustomRuntimeException setDetails(final String details) {
        this.details = details;
        return this;
    }

    public CustomRuntimeException setErrorCode(final int errorCode) {
        this.errorCode = errorCode;
        return this;
    }

}