package rundeck.services.asyncimport

class AsyncImportException extends RuntimeException {
    AsyncImportException(String errorMessage, Throwable t) {
        super(errorMessage, t)
    }

    AsyncImportException(String errorMessage) {
        super(errorMessage)
    }
}
