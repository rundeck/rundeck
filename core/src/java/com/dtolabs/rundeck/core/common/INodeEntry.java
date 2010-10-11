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
 * INodeEntry describes a node found in nodes.properties
 *
 * @author Greg Schueler <a href="mailto:greg@dtosolutions.com">greg@dtosolutions.com</a>
 * @version $Revision$
 */
public interface INodeEntry extends INodeBase, INodeDesc {
    public String getOsFamily();
    public String getOsArch();
    public String getOsVersion();
    public String getOsName();
    public String getType();
    public String getNodename();
    public String getUsername();
    public boolean containsUserName();
    public boolean containsPort();
    public String extractUserName();
    public String extractHostname();
    public String extractPort();
    public String getFrameworkProject();
    public String getDescription();

    public Set getTags();

    /**
     * Get the map of attributes for the node.
     * @return attributes
     */
    public Map<String, String> getAttributes();

    /**
     * Return setting name/value map.
     * @return map of setting name to value.
     */
    public Map<String, String> getSettings();

}
