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

/*
* PropertyScope.java
* 
* User: Greg Schueler <a href="mailto:greg@dtosolutions.com">greg@dtosolutions.com</a>
* Created: 12/3/12 3:41 PM
* 
*/
package com.dtolabs.rundeck.core.plugins.configuration;

/**
 * Available scopes for properties, indicating where the runtime values can be resolved from.
 */
public enum PropertyScope {
    /**
     * Only framework properties
     */
    Framework,
    /**
     * Only Project properties
     */
    ProjectOnly,
    /**
     * Project and Framework properties
     */
    Project,
    /**
     * Only instance properties
     */
    InstanceOnly,
    /**
     * Instance and all earlier levels
     */
    Instance,
    /**
     * No specific scope specified
     */
    Unspecified;

    /**
     * @return true if this scope is {@link #Unspecified}
     */
    public boolean isUnspecified() {
        return this == Unspecified;
    }

    /**
     * @return true if this scope encompasses Instance level properties
     */
    public boolean isInstanceLevel() {
        return this == Instance || this == InstanceOnly;
    }

    /**
     * @return true if this scope encompasses Project level properties
     */
    public boolean isProjectLevel() {
        return this == ProjectOnly || this == Project || this == Instance;
    }

    /**
     * @return true if this scope encompasses Framework level properties
     */
    public boolean isFrameworkLevel() {
        return this == Framework || this == Project || this == Instance;
    }
}
