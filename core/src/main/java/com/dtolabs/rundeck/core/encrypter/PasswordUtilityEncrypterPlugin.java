package com.dtolabs.rundeck.core.encrypter;

import java.util.Map;

/**
 * PasswordUtilityEncrypterPlugin Plugin Interface to use it on Password Utility GUI (System Menu > Password Utility).
 *
 */
public interface PasswordUtilityEncrypterPlugin {

    /**
     * Execute the encryption based on the parameter defined on the plugin and set on the GUI.
     *
     * @param config Plugin input attributes, key/value format
     * @return a EncryptorResponse object with the result of the encrypt process, which will be displayed on the GUI
     */
    EncryptorResponse encrypt(Map<String,String> config);
}