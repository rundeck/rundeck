package rundeck.data.exceptions

class ExecutionServiceExecutionException extends Exception {
    private String code;
    ExecutionServiceExecutionException() {
    }

    ExecutionServiceExecutionException(String s) {
        super(s);
    }

    ExecutionServiceExecutionException(String message, String code) {
        super(message);
        this.code = code;
    }

    ExecutionServiceExecutionException(String s, Throwable throwable) {
        super(s, throwable);
    }

    String getCode() {
        return code;
    }
}
