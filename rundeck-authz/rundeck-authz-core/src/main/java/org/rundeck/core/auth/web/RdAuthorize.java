package org.rundeck.core.auth.web;

import java.lang.annotation.Documented;
import java.lang.annotation.Retention;
import java.lang.annotation.Target;

import static java.lang.annotation.ElementType.METHOD;
import static java.lang.annotation.ElementType.TYPE;
import static java.lang.annotation.RetentionPolicy.RUNTIME;

/**
 * Authorization check with dynamic resource type
 */
@Documented
@Retention(RUNTIME)
@Target({TYPE, METHOD})
public @interface RdAuthorize {
    /**
     * Resource type name
     */
    String type();

    /**
     * Named authorization group
     */
    String group();

    /**
     * Named authorization value
     */
    String access();

    /**
     * @return optional description reported if authorization fails
     */
    String description() default "";
}