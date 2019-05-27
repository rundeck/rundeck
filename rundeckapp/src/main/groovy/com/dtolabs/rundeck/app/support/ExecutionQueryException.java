package com.dtolabs.rundeck.app.support;


/**
 * Utilitary exception to pass the message code of errors generated during execution query validation, for use by controllers.
 */
public class ExecutionQueryException extends RuntimeException {

  private final String errorMessageCode;

  public ExecutionQueryException(String errorMessageCode) {
    this.errorMessageCode = errorMessageCode;
  }

  public ExecutionQueryException(String errorMessageCode, Throwable cause) {
    super(errorMessageCode, cause);
    this.errorMessageCode = errorMessageCode;
  }

  public String getErrorMessageCode() {
    return errorMessageCode;
  }
}
