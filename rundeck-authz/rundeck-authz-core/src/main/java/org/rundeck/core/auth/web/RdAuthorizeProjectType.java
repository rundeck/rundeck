package org.rundeck.core.auth.web;

import org.rundeck.core.auth.app.RundeckAccess;

import java.lang.annotation.Documented;
import java.lang.annotation.Retention;
import java.lang.annotation.Target;

import static java.lang.annotation.ElementType.METHOD;
import static java.lang.annotation.ElementType.TYPE;
import static java.lang.annotation.RetentionPolicy.RUNTIME;

/**
 * Authorize a resource type in a project, the {@link #type()} value should specify a Resource Type name, e.g. {@link
 * org.rundeck.core.auth.AuthConstants#TYPE_JOB} and the {@link #access()} value should specify the named * auth access
 * requested. The {@link #group()} can be specified to use a different group of named auth values
 */
@Documented
@Retention(RUNTIME)
@Target({TYPE, METHOD})
public @interface RdAuthorizeProjectType {
    /**
     * Named auth group, defaults to {@link RundeckAccess.ProjectType#GROUP}
     */
    String group() default RundeckAccess.ProjectType.GROUP;

    /**
     * Named auth value
     */
    String access();

    /**
     * Type to authorize
     */
    String type();

    /**
     * Optional description
     */
    String description() default "";
}
