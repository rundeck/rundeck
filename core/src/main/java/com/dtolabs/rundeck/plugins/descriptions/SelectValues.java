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


import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;


/**
 * Declare a String field as a {@link com.dtolabs.rundeck.core.plugins.configuration.Property.Type#Select} or {@link
 * com.dtolabs.rundeck.core.plugins.configuration.Property.Type#FreeSelect}, and specify the selectable values.
 */
@Retention(RetentionPolicy.RUNTIME)
@Target(ElementType.FIELD)
/*
* SelectValues.java
* 
* User: Greg Schueler <a href="mailto:greg@dtosolutions.com">greg@dtosolutions.com</a>
* Created: 11/29/12 3:59 PM
* 
*/
public @interface SelectValues {
    /**
     * @return True if the property allows free text entry in addition to selection from a list
     */
    boolean freeSelect() default false;

    /**
     * @return The set of values that can be selected
     */
    String[] values();
}
