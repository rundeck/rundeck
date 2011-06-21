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
* NodeBaseImpl.java
* 
* User: greg
* Created: Sep 23, 2009 10:25:21 AM
* $Id$
*/
package com.dtolabs.rundeck.core.common;

/**
 * Implementation of INodeBase
 *
 * @author Greg Schueler <a href="mailto:greg@dtosolutions.com">greg@dtosolutions.com</a>
 * @version $Revision$
 */
public class NodeBaseImpl implements INodeBase {
    String nodename;

    NodeBaseImpl(){
        
    }
    NodeBaseImpl(final String nodename) {
        setNodename(nodename);
    }

    public String getNodename() {
        return nodename;
    }

    /**
     * Set the node name
     * @param nodename name of the node
     */
    public void setNodename(final String nodename) {
        this.nodename = nodename;
    }

    public boolean equals(final Object o) {
        if (this == o) {
            return true;
        }
        if (!(o instanceof INodeBase)) {
            return false;
        }

        final INodeBase base = (INodeBase) o;

        if (nodename != null ? !nodename.equals(base.getNodename()) : base.getNodename() != null) {
            return false;
        }

        return true;
    }

    public int hashCode() {
        return (nodename != null ? nodename.hashCode() : 0);
    }

    /**
     * Create a NodeBaseImpl
     * @param nodename the node name
     * @return NodeBaseImpl
     */
    public static NodeBaseImpl create(final String nodename) {
        return new NodeBaseImpl(nodename);
    }
}
