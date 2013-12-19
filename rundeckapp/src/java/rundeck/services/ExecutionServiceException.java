package rundeck.services;

/**
 * Exception thrown by the ExecutionService
 */
public class ExecutionServiceException extends Exception {
    private String code;
    public ExecutionServiceException() {
    }

    public ExecutionServiceException(String s) {
        super(s);
    }

    public ExecutionServiceException(String message, String code) {
        super(message);
        this.code = code;
    }

    public ExecutionServiceException(String s, Throwable throwable) {
        super(s, throwable);
    }

    public String getCode() {
        return code;
    }
}
