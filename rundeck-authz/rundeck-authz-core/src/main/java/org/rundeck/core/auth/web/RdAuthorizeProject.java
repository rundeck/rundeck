package org.rundeck.core.auth.web;

import org.rundeck.core.auth.app.RundeckAccess;

import java.lang.annotation.Documented;
import java.lang.annotation.Retention;
import java.lang.annotation.Target;

import static java.lang.annotation.ElementType.METHOD;
import static java.lang.annotation.ElementType.TYPE;
import static java.lang.annotation.RetentionPolicy.RUNTIME;

/**
 * Authorization check for Project resource
 */
@Documented
@Retention(RUNTIME)
@Target({TYPE, METHOD})
public @interface RdAuthorizeProject {
    /**
     * Named auth group, defaults to {@link RundeckAccess.Project#GROUP}
     */
    String group() default RundeckAccess.Project.GROUP;

    /**
     * Named auth value
     */
    String value();

    /**
     * Optional description
     */
    String description() default "";
}
