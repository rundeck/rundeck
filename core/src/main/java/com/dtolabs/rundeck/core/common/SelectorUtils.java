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
* SelectorUtils.java
* 
* User: Greg Schueler <a href="mailto:greg@dtosolutions.com">greg@dtosolutions.com</a>
* Created: 7/19/11 4:48 PM
* 
*/
package com.dtolabs.rundeck.core.common;

import java.util.Collection;
import java.util.HashSet;

/**
 * SelectorUtils is ...
 *
 * @author Greg Schueler <a href="mailto:greg@dtosolutions.com">greg@dtosolutions.com</a>
 */
public class SelectorUtils {
    private abstract static class ChainNodesSelector implements NodesSelector{
        NodesSelector aselector;
        NodesSelector bselector;

        public ChainNodesSelector(NodesSelector aselector, NodesSelector bselector) {
            this.aselector = aselector;
            this.bselector = bselector;
        }

    }
    public static class AndNodesSelector extends ChainNodesSelector{
        public AndNodesSelector(NodesSelector aselector, NodesSelector bselector) {
            super(aselector, bselector);
        }

        public boolean acceptNode(INodeEntry entry) {
            return aselector.acceptNode(entry) && bselector.acceptNode(entry);
        }
    }
    public static class OrNodesSelector extends ChainNodesSelector{
        public OrNodesSelector(NodesSelector aselector, NodesSelector bselector) {
            super(aselector, bselector);
        }

        public boolean acceptNode(INodeEntry entry) {
            return aselector.acceptNode(entry) || bselector.acceptNode(entry);
        }
    }

    public static NodesSelector and(NodesSelector a,NodesSelector b) {
        return new AndNodesSelector(a, b);
    }
    public static NodesSelector or(NodesSelector a,NodesSelector b) {
        return new OrNodesSelector(a, b);
    }
    public static NodesSelector singleNode(final String nodename){
        return new MultiNodeSelector(nodename);
    }

    public static NodesSelector nodeList(final Collection<String> nodenames) {
        return new MultiNodeSelector(nodenames);
    }

    public static class MultiNodeSelector implements NodesSelector{
        private final HashSet<String> nodenames;

        public MultiNodeSelector(final String nodename) {
            this.nodenames = new HashSet<String>();
            nodenames.add(nodename);
        }
        public MultiNodeSelector(final Collection<String> nodenames) {
            this.nodenames = new HashSet<String>(nodenames);
        }

        public boolean acceptNode(final INodeEntry entry) {
            return nodenames.contains(entry.getNodename());
        }

        @Override
        public String toString() {
            return "MultiNodeSelector{" +
                   "nodenames=" + nodenames +
                   '}';
        }

        @Override
        public boolean equals(Object o) {
            if (this == o) {
                return true;
            }
            if (o == null || getClass() != o.getClass()) {
                return false;
            }

            MultiNodeSelector that = (MultiNodeSelector) o;

            if (nodenames != null ? !nodenames.equals(that.nodenames) : that.nodenames != null) {
                return false;
            }

            return true;
        }

        @Override
        public int hashCode() {
            return nodenames != null ? nodenames.hashCode() : 0;
        }
    }

}
