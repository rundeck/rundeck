package com.dtolabs.rundeck.plugins.descriptions;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

@Retention(RetentionPolicy.RUNTIME)
@Target(ElementType.FIELD)
public @interface ReplaceDataVariablesWithBlanks {
    /**
     * @return Replace unexpandable variables with blanks if true,
     * otherwise leave value intact
     */
    boolean value() default true;
}
