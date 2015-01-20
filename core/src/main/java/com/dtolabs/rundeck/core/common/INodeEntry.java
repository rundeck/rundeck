/*
 * Copyright 2010 DTO Labs, Inc. (http://dtolabs.com)
 *
 *  Licensed under the Apache License, Version 2.0 (the "License");
 *  you may not use this file except in compliance with the License.
 *  You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 *  Unless required by applicable law or agreed to in writing, software
 *  distributed under the License is distributed on an "AS IS" BASIS,
 *  WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *  See the License for the specific language governing permissions and
 *  limitations under the License.
 */

/*
* INodeDetail.java
* 
* User: greg
* Created: Mar 31, 2008 4:51:51 PM
* $Id$
*/
package com.dtolabs.rundeck.core.common;

import java.util.Set;
import java.util.Map;


/**
 * INodeEntry describes a node definition
 *
 * @author Greg Schueler <a href="mailto:greg@dtosolutions.com">greg@dtosolutions.com</a>
 */
public interface INodeEntry extends INodeBase, INodeDesc {
    /**
     * @return the OS family
     */
    public String getOsFamily();

    /**
     * @return the OS architecture
     */
    public String getOsArch();

    /**
     * @return the OS version
     */
    public String getOsVersion();

    /**
     * @return the OS name
     */
    public String getOsName();

    /**
     * @return the name of the node
     */
    public String getNodename();

    /**
     * @return the username
     */
    public String getUsername();

    /**
     * @return true if the hostname string includes embedded username in the "username@hostname" pattern
     */
    public boolean containsUserName();

    /**
     * @return true if the hostname string includes embedded port in the "hostname:port" pattern
     */
    public boolean containsPort();

    /**
     * @return the username extracted from the hostname
     */
    public String extractUserName();

    /**
     * @return the standalone hostname value extracted from the hostname string
     */
    public String extractHostname();

    /**
     * @return the port string extracted from the hostname
     */
    public String extractPort();

    /**
     * @return the project name if it is set
     */
    public String getFrameworkProject();

    /**
     * @return the description
     */
    public String getDescription();

    /**
     * @return the set of tag strings
     */
    public Set getTags();

    /**
     * Get the map of attributes for the node, which includes all of the attributes for these accessors as well:
     * <br>
     * osFamily, osArch, osVersion, osName, name*, username, description, tags*.
     * <br>
     * The Node name is returned as under the key "name".
     * <br>
     * The tags are returned as a String under the key "tags".
     *
     * @return attributes
     */
    public Map<String, String> getAttributes();

}
