package com.dtolabs.rundeck.core.encrypter;

import com.dtolabs.rundeck.core.plugins.configuration.Property;

import java.util.List;
import java.util.Map;
import java.util.ServiceLoader;

/**
 * Provide the ability to encrypt values. The PasswordUtilityController will pick up all implementing classes and
 * provide them as options to users as long as they are registered as described by the {@link ServiceLoader} class.
 */
public interface PasswordUtilityEncrypter {
    String name();
    Map encrypt(Map params);
    List<Property> formProperties();
}
