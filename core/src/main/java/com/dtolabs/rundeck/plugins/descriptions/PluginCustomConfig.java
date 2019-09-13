package com.dtolabs.rundeck.plugins.descriptions;

import com.dtolabs.rundeck.core.plugins.configuration.PluginCustomConfigValidator;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * Declares a Plugin class' field as a receiving plugin config
 */
@Retention(RetentionPolicy.RUNTIME)
@Target(ElementType.FIELD)
/*
 * PluginProperty.java
 *
 * User: Greg Schueler <a href="mailto:greg@dtosolutions.com">greg@dtosolutions.com</a>
 * Created: 11/29/12 10:55 AM
 *
 */
public @interface PluginCustomConfig {
    /**
     *
     * @return the name of the custom vue component that will be used to provide the configuration for this property
     */
    String vueConfigurationComponent() default "";

    Class<? extends PluginCustomConfigValidator> validator() default PluginCustomConfigValidator.class;
}
