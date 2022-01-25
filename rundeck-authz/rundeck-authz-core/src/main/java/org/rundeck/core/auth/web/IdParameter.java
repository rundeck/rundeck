package org.rundeck.core.auth.web;

import java.lang.annotation.Documented;
import java.lang.annotation.Repeatable;
import java.lang.annotation.Retention;
import java.lang.annotation.Target;

import static java.lang.annotation.ElementType.METHOD;
import static java.lang.annotation.ElementType.TYPE;
import static java.lang.annotation.RetentionPolicy.RUNTIME;

/**
 * Declare a parameter name to use as the identifier for a particular resource
 */
@Documented
@Retention(RUNTIME)
@Target({TYPE, METHOD})
@Repeatable(IdParameters.class)
public @interface IdParameter {
    /**
     * @return parameter name
     */
    String value();

    /**
     * @return resource type name, can be blank for it to be inferred, e.g. if there is only one resource type
     */
    String type();

}
