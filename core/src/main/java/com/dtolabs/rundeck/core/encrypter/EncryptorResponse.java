package com.dtolabs.rundeck.core.encrypter;

import java.util.Map;

/**
 * EncryptorResponse Password Utility Plugin result
 *
 */
public interface EncryptorResponse {

    /**
     * Define the status of the encrypt process.
     *
     * @return true/false
     */
    boolean isValid();

    /**
     * Error message.
     *
     * @return Description of the error message
     */
    String getError();

    /**
     * Detail of encrypt process.
     *
     * @return Result of the encrypt process to be displayed on the GUI. eg: {"enc":"xxxyyxff"}
     */
    Map<String,String> getOutputs();

}