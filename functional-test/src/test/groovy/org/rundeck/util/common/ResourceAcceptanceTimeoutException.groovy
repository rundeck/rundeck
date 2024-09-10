package org.rundeck.util.common

class ResourceAcceptanceTimeoutException extends RuntimeException {
    ResourceAcceptanceTimeoutException(String message) {
        super(message)
    }

    ResourceAcceptanceTimeoutException(String message, Throwable cause) {
        super(message, cause)
    }
}
