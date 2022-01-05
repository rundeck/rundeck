package org.rundeck.core.auth.web;

import org.rundeck.core.auth.app.RundeckAccess;

import java.lang.annotation.Documented;
import java.lang.annotation.Retention;
import java.lang.annotation.Target;

import static java.lang.annotation.ElementType.METHOD;
import static java.lang.annotation.ElementType.TYPE;
import static java.lang.annotation.RetentionPolicy.RUNTIME;

/**
 * Authorization check for Execution resource
 */
@Documented
@Retention(RUNTIME)
@Target({TYPE, METHOD})
public @interface RdAuthorizeJob {
    /**
     * Named auth check group, defaults to {@link RundeckAccess.Job#GROUP}
     */
    String group() default RundeckAccess.Job.GROUP;

    /**
     * Named auth value
     */
    String value();

    /**
     * Optional description
     */
    String description() default "";
}
