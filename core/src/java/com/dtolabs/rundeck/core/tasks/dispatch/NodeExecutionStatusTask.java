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
* NodeExecutionStatus.java
* 
* User: Greg Schueler <a href="mailto:greg@dtosolutions.com">greg@dtosolutions.com</a>
* Created: Jun 11, 2010 10:49:41 AM
* $Id$
*/
package com.dtolabs.rundeck.core.tasks.dispatch;

import com.dtolabs.rundeck.core.cli.NodeDispatchStatusListener;
import org.apache.tools.ant.BuildException;
import org.apache.tools.ant.Task;

/**
 * NodeExecutionStatus is an internal task used when executing Parallel dispatches to report success when execution on a
 * node completes.  It is used by {@link com.dtolabs.rundeck.core.cli.DefaultNodeDispatcher} to collate the set of nodes that
 * failed when executing tasks in parallel.
 *
 * @author Greg Schueler <a href="mailto:greg@dtosolutions.com">greg@dtosolutions.com</a>
 * @version $Revision$
 */
public class NodeExecutionStatusTask extends Task {
    private String refId;
    private NodeDispatchStatusListener nodeDispatchStatusListener;
    private String nodeName;
    private boolean failOnError;

    @Override
    public void execute() {
        validate();
        if (null != getRefId()) {
            final Object o = getProject().getReference(getRefId());
            if (null == o) {
                if (failOnError) {
                    throw new BuildException("refId: " + getRefId() + ", was not found");
                } else {
                    return;
                }
            }
            if (!(o instanceof NodeDispatchStatusListener)) {
                if (failOnError) {
                    throw new BuildException(
                        "refId: " + getRefId() + ", was not a NodeDispatchStatusListener: " + o.getClass().getName());
                } else {
                    return;
                }
            }
            setNodeDispatchStatusListener((NodeDispatchStatusListener) o);
        }
        if (null == getNodeDispatchStatusListener()) {
            if (failOnError) {
                throw new BuildException("no listener was configured");
            } else {
                return;
            }
        }
        getNodeDispatchStatusListener().reportSuccess(getNodeName());
    }

    private void validate() {
        if (null == getRefId() && null == getNodeDispatchStatusListener()) {
            throw new BuildException("refId is not set and no listener is configured");
        }
        if (null == getNodeName()) {
            throw new BuildException("nodeName is not set");
        }
    }

    public String getRefId() {
        return refId;
    }

    public void setRefId(final String refId) {
        this.refId = refId;
    }

    public NodeDispatchStatusListener getNodeDispatchStatusListener() {
        return nodeDispatchStatusListener;
    }

    public void setNodeDispatchStatusListener(final NodeDispatchStatusListener nodeDispatchStatusListener) {
        this.nodeDispatchStatusListener = nodeDispatchStatusListener;
    }

    public String getNodeName() {
        return nodeName;
    }

    public void setNodeName(final String nodeName) {
        this.nodeName = nodeName;
    }

    public boolean isFailOnError() {
        return failOnError;
    }

    public void setFailOnError(final boolean failOnError) {
        this.failOnError = failOnError;
    }
}
