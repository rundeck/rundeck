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
* PropertyResolver.java
* 
* User: Greg Schueler <a href="mailto:greg@dtosolutions.com">greg@dtosolutions.com</a>
* Created: 12/3/12 3:20 PM
* 
*/
package com.dtolabs.rundeck.core.plugins.configuration;


/**
 * PropertyResolver can return a property value resolved from a particular configuration scope
 *
 * @author Greg Schueler <a href="mailto:greg@dtosolutions.com">greg@dtosolutions.com</a>
 */
public interface PropertyResolver {
    /**
     * @param name property name
     * @param scope property search scope
     * @return a value for a named property found in the given scope, or null if not found.
     */
    public Object resolvePropertyValue(String name, PropertyScope scope);
}
