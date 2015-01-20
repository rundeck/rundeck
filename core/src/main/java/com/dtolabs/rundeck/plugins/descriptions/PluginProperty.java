package com.dtolabs.rundeck.plugins.descriptions;
/*
 * Copyright 2012 DTO Labs, Inc. (http://dtolabs.com)
 * 
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 *
 */


import com.dtolabs.rundeck.core.plugins.configuration.PropertyScope;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;


/**
 * Declares a Plugin class' field as a configurable property
 */
@Retention(RetentionPolicy.RUNTIME)
@Target(ElementType.FIELD)
/*
* PluginProperty.java
* 
* User: Greg Schueler <a href="mailto:greg@dtosolutions.com">greg@dtosolutions.com</a>
* Created: 11/29/12 10:55 AM
* 
*/
public @interface PluginProperty {
    /**
     * @return The property identifier name
     */
    String name() default "";

    /**
     * @return The property display name
     */
    String title() default "";

    /**
     * @return The property description
     */
    String description() default "";

    /**
     * @return The default value as a string
     */
    String defaultValue() default "";

    /**
     * @return True if the property value is required
     */
    boolean required() default false;

    /**
     * @return The scope for resolving the property value at runtime
     */
    PropertyScope scope() default PropertyScope.Unspecified;
}
