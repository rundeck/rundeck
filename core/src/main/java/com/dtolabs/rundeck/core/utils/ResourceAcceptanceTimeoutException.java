package com.dtolabs.rundeck.core.utils;

public class ResourceAcceptanceTimeoutException extends RuntimeException {

  public ResourceAcceptanceTimeoutException(String message) {
      super(message);
    }

  public ResourceAcceptanceTimeoutException(String message, Throwable cause) {
      super(message, cause);
    }
}
