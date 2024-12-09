package in.neuw.oauth2.client.exceptions;

public class BootTimeException extends RuntimeException {

    public BootTimeException(String message) {
        super(message);
    }

    public BootTimeException(String message, Throwable cause) {
        super(message, cause);
    }
}
