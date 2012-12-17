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
* StubNodeExecutor.java
* 
* User: Greg Schueler <a href="mailto:greg@dtosolutions.com">greg@dtosolutions.com</a>
* Created: 3/31/11 3:14 PM
* 
*/
package com.dtolabs.rundeck.plugin.stub;

import com.dtolabs.rundeck.core.Constants;
import com.dtolabs.rundeck.core.common.INodeEntry;
import com.dtolabs.rundeck.core.execution.ExecutionContext;
import com.dtolabs.rundeck.core.execution.service.NodeExecutor;
import com.dtolabs.rundeck.core.execution.service.NodeExecutorResult;
import com.dtolabs.rundeck.core.execution.service.NodeExecutorResultImpl;
import com.dtolabs.rundeck.core.execution.workflow.steps.FailureReason;
import com.dtolabs.rundeck.core.plugins.Plugin;
import com.dtolabs.rundeck.core.plugins.configuration.AbstractBaseDescription;
import com.dtolabs.rundeck.core.plugins.configuration.Describable;
import com.dtolabs.rundeck.core.plugins.configuration.Description;
import com.dtolabs.rundeck.core.utils.StringArrayUtil;
import com.dtolabs.rundeck.plugins.ServiceNameConstants;


/**
 * StubNodeExecutor is ...
 *
 * @author Greg Schueler <a href="mailto:greg@dtosolutions.com">greg@dtosolutions.com</a>
 */
@Plugin(name = "stub", service = ServiceNameConstants.NodeExecutor)
public class StubNodeExecutor implements NodeExecutor, Describable {
    public static final String SERVICE_PROVIDER_NAME = "stub";
    private static final String STUB_EXEC_SUCCESS = "stub-exec-success";
    private static final String STUB_RESULT_CODE = "stub-result-code";

    public NodeExecutorResult executeCommand(final ExecutionContext context, final String[] command,
                                             final INodeEntry node) {
        //replace data context in args
        int tcode = 0;
        boolean tsuccess = true;
        if (null != node.getAttributes() && null != node.getAttributes().get(STUB_RESULT_CODE)) {
            try {
                tcode = Integer.parseInt(node.getAttributes().get(STUB_RESULT_CODE));
            } catch (NumberFormatException e) {
                context.getExecutionListener().log(Constants.WARN_LEVEL,
                                                   "[stub] (failed to parse stub-result-code for node)");
            }
        }
        if (null != node.getAttributes() && null != node.getAttributes().get(STUB_EXEC_SUCCESS)) {
            try {
                tsuccess = Boolean.parseBoolean(node.getAttributes().get(STUB_EXEC_SUCCESS));
            } catch (NumberFormatException e) {
                context.getExecutionListener().log(Constants.WARN_LEVEL,
                                                   "[stub] (failed to parse " + STUB_EXEC_SUCCESS + " for node)");
            }
        }
        if (tsuccess) {
            context.getExecutionListener().log(Constants.WARN_LEVEL,
                                               "[stub] execute on node " + node.getNodename() + ": "
                                               + StringArrayUtil.asString(command, " "));
            return NodeExecutorResultImpl.createSuccess(node);
        } else {
            context.getExecutionListener().log(Constants.ERR_LEVEL,
                                               "[stub] fail on node " + node.getNodename() + ": "
                                               + StringArrayUtil.asString(command, " "));
            return NodeExecutorResultImpl.createFailure(Reason.Intentional, "Intentional failure", node);
        }
    }

    static enum Reason implements FailureReason {
        Intentional
    }

    static final Description DESC = new AbstractBaseDescription() {
        public String getName() {
            return SERVICE_PROVIDER_NAME;
        }

        public String getTitle() {
            return "Stub";
        }

        public String getDescription() {
            return "Prints the command instead of executing it. (Useful for mocking processes.)";
        }
    };

    public Description getDescription() {
        return DESC;
    }
}
