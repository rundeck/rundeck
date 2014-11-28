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
     * Return the OS family
     */
    public String getOsFamily();

    /**
     * Return the OS architecture
     */
    public String getOsArch();

    /**
     * Return the OS version
     */
    public String getOsVersion();

    /**
     * Return the OS name
     */
    public String getOsName();

    /**
     * Return the name of the node
     */
    public String getNodename();

    /**
     * Return the username
     */
    public String getUsername();

    /**
     * Return true if the hostname string includes embedded username in the "username@hostname" pattern
     */
    public boolean containsUserName();

    /**
     * Return true if the hostname string includes embedded port in the "hostname:port" pattern
     */
    public boolean containsPort();

    /**
     * Return the username extracted from the hostname
     */
    public String extractUserName();

    /**
     * Return the standalone hostname value extracted from the hostname string
     */
    public String extractHostname();

    /**
     * Return the port string extracted from the hostname
     */
    public String extractPort();

    /**
     * Return the project name if it is set
     */
    public String getFrameworkProject();

    /**
     * Return the description
     */
    public String getDescription();

    /**
     * Return the set of tag strings
     */
    public Set getTags();

    /**
     * Get the map of attributes for the node, which includes all of the attributes for these accessors as well:
     * <p/>
     * osFamily, osArch, osVersion, osName, name*, username, description, tags*.
     * <p/>
     * The Node name is returned as under the key "name".
     * <p/>
     * The tags are returned as a String under the key "tags".
     *
     * @return attributes
     */
    public Map<String, String> getAttributes();

}
