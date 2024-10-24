package rundeck.services;

public class MissingScheduledExecutionException extends Exception{

    public MissingScheduledExecutionException(String message) {
        super(message);
    }

    public MissingScheduledExecutionException(String message, Throwable cause) {
        super(message, cause);
    }

}
