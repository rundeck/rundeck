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
* NodeSetSequence.java
* 
* User: Greg Schueler <a href="mailto:greg@dtosolutions.com">greg@dtosolutions.com</a>
* Created: 7/19/11 1:42 PM
* 
*/
package com.dtolabs.rundeck.core.common;

import java.util.*;

/**
 * NodeSetSequence is ...
 *
 * @author Greg Schueler <a href="mailto:greg@dtosolutions.com">greg@dtosolutions.com</a>
 */
public class AdditiveListNodeSet implements INodeSet {
    List<INodeSet> nodeSetList;
    TreeSet<String> nodeNames;
    HashMap<String,INodeSet> nodeIndex;

    public AdditiveListNodeSet() {
        nodeSetList = new ArrayList<INodeSet>();
        nodeNames = new TreeSet<String>();
        nodeIndex=new HashMap<String, INodeSet>();
    }

    public void addNodeSet(final INodeSet nodeSet) {
        if(null==nodeSet){
            return;
        }
        nodeSetList.add(nodeSet);
        nodeNames.addAll(nodeSet.getNodeNames());
        for (final String name : nodeSet.getNodeNames()) {
            nodeIndex.put(name, nodeSet);
        }
    }

    public Collection<INodeEntry> getNodes() {
        final ArrayList<INodeEntry> nodes = new ArrayList<INodeEntry>();
        for (final String nodeName : nodeNames) {
            nodes.add(nodeIndex.get(nodeName).getNode(nodeName));
        }
        return nodes;
    }

    public INodeEntry getNode(final String name) {
        INodeEntry result = null;
        for (final INodeSet iNodeSet : nodeSetList) {
            final INodeEntry node = iNodeSet.getNode(name);
            if (null != node) {
                result = node;
            }
        }
        return result;
    }

    public Collection<String> getNodeNames() {
        return nodeNames;
    }

    @Override
    public Iterator<INodeEntry> iterator() {
        return getNodes().iterator();
    }
}
