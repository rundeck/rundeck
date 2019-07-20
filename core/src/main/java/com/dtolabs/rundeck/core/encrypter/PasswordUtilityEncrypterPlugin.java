package com.dtolabs.rundeck.core.encrypter;

import java.util.Map;

public interface PasswordUtilityEncrypterPlugin {

    EncryptorResponse encrypt(Map<String,String> config);
}
