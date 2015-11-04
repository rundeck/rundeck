package com.dtolabs.rundeck.app.api

/**
 * Wrapper for a string that is marshalled wrappedin CDATA to preserve whitespace
 */
class CDataString {
    String value
    static CDataString from(String value) {
        return new CDataString(value: value)
    }
}
