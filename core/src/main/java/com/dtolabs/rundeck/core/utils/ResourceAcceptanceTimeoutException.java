package com.dtolabs.rundeck.core.utils;

/**
 * Thrown when the timeout is reached waiting for a resource to reach the expected state.
 */
public class ResourceAcceptanceTimeoutException extends RuntimeException {

  public ResourceAcceptanceTimeoutException(String message) {
      super(message);
    }

  public ResourceAcceptanceTimeoutException(String message, Throwable cause) {
      super(message, cause);
    }
}
