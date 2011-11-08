/*
 * Copyright 2011 DTO Solutions, Inc. (http://dtosolutions.com)
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
* NodeFilter.java
* 
* User: Greg Schueler <a href="mailto:greg@dtosolutions.com">greg@dtosolutions.com</a>
* Created: 7/19/11 2:00 PM
* 
*/
package com.dtolabs.rundeck.core.common;

/**
 * NodeFilter is ...
 *
 * @author Greg Schueler <a href="mailto:greg@dtosolutions.com">greg@dtosolutions.com</a>
 */
public class NodeFilter {
    public static INodeSet filterNodes(final NodesSelector selector, final INodeSet nodeSet) {
        final NodeSetImpl nodeSet1 = new NodeSetImpl();
        for (final INodeEntry iNodeEntry : nodeSet.getNodes()) {
            if(selector.acceptNode(iNodeEntry)) {
                nodeSet1.putNode(iNodeEntry);
            }
        }
        return nodeSet1;
    }
}
