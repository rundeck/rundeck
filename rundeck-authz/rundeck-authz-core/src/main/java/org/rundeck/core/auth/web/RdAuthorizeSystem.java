package org.rundeck.core.auth.web;

import org.rundeck.core.auth.app.RundeckAccess;

import java.lang.annotation.Documented;
import java.lang.annotation.Retention;
import java.lang.annotation.Target;

import static java.lang.annotation.ElementType.METHOD;
import static java.lang.annotation.ElementType.TYPE;
import static java.lang.annotation.RetentionPolicy.RUNTIME;

/**
 * Authorization check for System
 */
@Documented
@Retention(RUNTIME)
@Target({TYPE, METHOD})
public @interface RdAuthorizeSystem {
    /**
     * Named auth group, defaults to {@link RundeckAccess.System#NAME}
     */
    String group() default RundeckAccess.System.NAME;

    /**
     * Named auth value
     */
    String value();

    /**
     * optional description
     */
    String description() default "";
}
