/*
 Copyright 2014 SimplifyOps Inc, <http://simplifyops.com>

 Licensed under the Apache License, Version 2.0 (the "License");
 you may not use this file except in compliance with the License.
 You may obtain a copy of the License at

     http://www.apache.org/licenses/LICENSE-2.0

 Unless required by applicable law or agreed to in writing, software
 distributed under the License is distributed on an "AS IS" BASIS,
 WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 See the License for the specific language governing permissions and
 limitations under the License.
 */

/*
 Copyright 2014 SimplifyOps Inc, <http://simplifyops.com>

 Licensed under the Apache License, Version 2.0 (the "License");
 you may not use this file except in compliance with the License.
 You may obtain a copy of the License at

     http://www.apache.org/licenses/LICENSE-2.0

 Unless required by applicable law or agreed to in writing, software
 distributed under the License is distributed on an "AS IS" BASIS,
 WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 See the License for the specific language governing permissions and
 limitations under the License.
 */

package com.dtolabs.rundeck.core.common;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

/**
 * $INTERFACE is ... User: greg Date: 1/21/14 Time: 2:52 PM
 */
public class MergedAttributesNodeSet extends AdditiveListNodeSet {
    @Override
    public void addNodeSet(INodeSet nodeSet) {
        NodeSetImpl iNodeEntries = new NodeSetImpl();
        for (INodeEntry iNodeEntry : nodeSet) {
            INodeEntry node = getNode(iNodeEntry.getNodename());
            if (null!=node) {
                //merge attributes
                HashMap<String, String> newAttributes = new HashMap<String, String>(node.getAttributes());
                //merge tags
                HashSet tags = new HashSet();
                if (null != node.getTags()) {
                    tags.addAll(node.getTags());
                }
                if(null!= iNodeEntry.getTags()) {
                    tags.addAll(iNodeEntry.getTags());
                }
                newAttributes.putAll(iNodeEntry.getAttributes());
                NodeEntryImpl nodeEntry = new NodeEntryImpl(iNodeEntry.getNodename());
                nodeEntry.setAttributes(newAttributes);
                nodeEntry.setTags(tags);
                iNodeEntries.putNode(nodeEntry);
            }else{
                iNodeEntries.putNode(iNodeEntry);
            }
        }
        super.addNodeSet(iNodeEntries);
    }
}
