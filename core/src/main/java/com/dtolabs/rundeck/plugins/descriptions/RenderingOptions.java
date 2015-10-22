package com.dtolabs.rundeck.plugins.descriptions;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * Declares a Plugin class' field as a configurable property
 */
@Retention(RetentionPolicy.RUNTIME)
@Target(ElementType.FIELD)
/**
 * Define multiple rendering options
 */
public @interface RenderingOptions {
    RenderingOption[] value();
}
