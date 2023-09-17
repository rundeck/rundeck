/*
 * Copyright 2016 SimplifyOps, Inc. (http://simplifyops.com)
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
 */

/*
* Description.java
*
* User: Greg Schueler <a href="mailto:greg@dtosolutions.com">greg@dtosolutions.com</a>
* Created: 7/27/11 4:53 PM
*
*/
package com.dtolabs.rundeck.core.plugins.configuration;

import com.dtolabs.rundeck.plugins.config.PluginGroup;

import java.util.List;
import java.util.Map;

/**
 * Description defines configuration properties of a plugin provider
 *
 * @author Greg Schueler <a href="mailto:greg@dtosolutions.com">greg@dtosolutions.com</a>
 */
public interface Description {
    /**
     * @return the provider name
     */
    public String getName();
    /**
     * @return the textual title of the provider
     */
    public String getTitle();
    /**
     * @return the textual description of this provider.
     */
    public String getDescription();
    /**
     * @return the properties
     */
    public List<Property> getProperties();

    /**
     * @return additional metadata about the provider, may include style or icon information such as 'glyphicon' for
     *         glyphicon icon name, 'faicon' for font-awesome icon name
     */
    public default Map<String, String> getMetadata() {
        return null;
    }
    /**
     * @return a map of config prop keys to external project property names, allowing input values to be in properties files
     */
    public Map<String, String> getPropertiesMapping();
    /**
     * @return a map of config prop keys to external framework property names, allowing input values to be in properties files
     */
    public Map<String, String> getFwkPropertiesMapping();

    /**
     * plugin group type if set, otherwise null
     * @param <T>
     * @return
     */
    public default <T extends PluginGroup> Class<T> getPluginGroupType(){
        return null;
    }

}
