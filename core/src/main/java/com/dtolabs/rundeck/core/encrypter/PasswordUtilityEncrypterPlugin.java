package com.dtolabs.rundeck.core.encrypter;

import java.util.Map;

public interface PasswordUtilityEncrypterPlugin {

    Map encrypt(Map config);
}
