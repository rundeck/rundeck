/*
 * Copyright 2011 DTO Labs, Inc. (http://dtolabs.com)
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
* Nodes.java
* 
* User: Greg Schueler <a href="mailto:greg@dtosolutions.com">greg@dtosolutions.com</a>
* Created: 8/8/11 11:28 AM
* 
*/
package com.dtolabs.rundeck.plugin.resources.format.json;

import java.util.*;

/**
* Simple POJO for a set of nodes mapped by name
*
* @author Greg Schueler <a href="mailto:greg@dtosolutions.com">greg@dtosolutions.com</a>
*/
class Nodes {
    private TreeMap<String, Node> nodes;

    public Nodes() {
        this.nodes = new TreeMap<String, Node>();
    }

    public TreeMap<String, Node> getNodes() {
        return nodes;
    }

    public void setNodes(final TreeMap<String, Node> nodes) {
        this.nodes = nodes;
    }
}
