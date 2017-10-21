package com.dtolabs.rundeck.net.model;

public interface ErrorDetail {
    String getErrorCode();

    String getErrorMessage();

    int getApiVersion();

    default String toCodeString() {
        if (null != getErrorCode()) {
            return String.format(
                    "[code: %s; APIv%d]",
                    getErrorCode(),
                    getApiVersion()
            );
        }
        return "";
    }

}