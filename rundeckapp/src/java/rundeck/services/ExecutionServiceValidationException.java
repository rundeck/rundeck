package rundeck.services;

import java.util.Map;

/**
 * $INTERFACE is ... User: greg Date: 10/21/13 Time: 10:36 AM
 */
public class ExecutionServiceValidationException extends ExecutionServiceException {

    Map<String, String> options;
    Map<String, String> errors;

    public ExecutionServiceValidationException() {
    }

    public ExecutionServiceValidationException(String s, Map<String, String> options, Map<String, String> errors) {
        super(s);
        this.options = options;
        this.errors = errors;
    }

    public ExecutionServiceValidationException(String s, Map<String, String> options, Map<String, String> errors,
            Throwable throwable) {
        super(s, throwable);
        this.options = options;
        this.errors = errors;
    }

    public Map<String, String> getOptions() {
        return options;
    }

    public Map<String, String> getErrors() {
        return errors;
    }
}
