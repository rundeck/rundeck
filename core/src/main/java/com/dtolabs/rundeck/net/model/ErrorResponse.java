package com.dtolabs.rundeck.net.model;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;


@JsonIgnoreProperties(ignoreUnknown = true)
public class ErrorResponse implements ErrorDetail {


    public String error;

    public int apiversion;


    public String errorCode;


    public String message;

    public String toCodeString() {
        if (null != errorCode) {
            return String.format(
                    "[code: %s; APIv%d]",
                    errorCode,
                    apiversion
            );
        }
        return "";
    }

    @Override
    public String getErrorCode() {
        return errorCode;
    }

    @Override
    public String getErrorMessage() {
        return message != null ? message : error;
    }

    @Override
    public int getApiVersion() {
        return apiversion;
    }


    @Override
    public String toString() {
        return String.format(
                "%s%n%s%n",
                getErrorMessage() != null ? getErrorMessage() : "(no message)",
                toCodeString()
        );
    }
}
