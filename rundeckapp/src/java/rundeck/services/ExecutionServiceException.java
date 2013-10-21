package rundeck.services;

/**
 * Exception thrown by the ExecutionService
 */
public class ExecutionServiceException extends Exception {

    public ExecutionServiceException() {
    }

    public ExecutionServiceException(String s) {
        super(s);
    }

    public ExecutionServiceException(String s, Throwable throwable) {
        super(s, throwable);
    }
}
