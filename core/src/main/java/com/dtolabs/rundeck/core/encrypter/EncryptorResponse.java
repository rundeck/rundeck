package com.dtolabs.rundeck.core.encrypter;

import java.util.Map;

public interface EncryptorResponse {
    boolean isValid();
    String getError();
    Map<String,String> getOutputs();

}
