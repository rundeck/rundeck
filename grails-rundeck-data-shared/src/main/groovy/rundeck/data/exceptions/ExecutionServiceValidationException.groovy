package rundeck.data.exceptions


class ExecutionServiceValidationException extends ExecutionServiceExecutionException {

    Map<String, String> options;
    Map<String, String> errors;

    ExecutionServiceValidationException() {}

    ExecutionServiceValidationException(String s, Map<String, String> options, Map<String, String> errors) {
        super(s);
        this.options = options;
        this.errors = errors;
    }

    ExecutionServiceValidationException(String s, Map<String, String> options, Map<String, String> errors,
                                               Throwable throwable) {
        super(s, throwable);
        this.options = options;
        this.errors = errors;
    }

    Map<String, String> getOptions() {
        return options;
    }

    Map<String, String> getErrors() {
        return errors;
    }
}
