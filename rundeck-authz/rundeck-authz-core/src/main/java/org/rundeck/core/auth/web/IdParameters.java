package org.rundeck.core.auth.web;

import java.lang.annotation.Documented;
import java.lang.annotation.Retention;
import java.lang.annotation.Target;

import static java.lang.annotation.ElementType.METHOD;
import static java.lang.annotation.ElementType.TYPE;
import static java.lang.annotation.RetentionPolicy.RUNTIME;

/**
 * Repeatable container for {@link IdParameter}
 */
@Documented
@Retention(RUNTIME)
@Target({TYPE, METHOD})
public @interface IdParameters {
    IdParameter[] value();
}
