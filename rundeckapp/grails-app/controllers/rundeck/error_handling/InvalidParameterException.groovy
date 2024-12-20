package rundeck.error_handling;

class InvalidParameterException extends Exception {
    String paramName
    String suppliedValue
    String errorDescription
}