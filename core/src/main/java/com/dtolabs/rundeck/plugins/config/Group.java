package com.dtolabs.rundeck.plugins.config;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * Declare plugin grouping, specifying the group type which must implement {@link PluginGroup}
 */

@Retention(RetentionPolicy.RUNTIME)
@Target(ElementType.TYPE)
public @interface Group {
    Class<?> value();
}
