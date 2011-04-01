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
package com.dtolabs.rundeck.core.execution.impl.stub;

import com.dtolabs.rundeck.core.Constants;
import com.dtolabs.rundeck.core.common.INodeEntry;
import com.dtolabs.rundeck.core.dispatcher.DataContextUtils;
import com.dtolabs.rundeck.core.execution.ExecutionContext;
import com.dtolabs.rundeck.core.execution.ExecutionException;
import com.dtolabs.rundeck.core.execution.service.NodeExecutor;
import com.dtolabs.rundeck.core.execution.service.NodeExecutorResult;
import com.dtolabs.rundeck.core.plugins.Plugin;
import com.dtolabs.rundeck.core.utils.StringArrayUtil;

/**
 * StubNodeExecutor is ...
 *
 * @author Greg Schueler <a href="mailto:greg@dtosolutions.com">greg@dtosolutions.com</a>
 */
@Plugin (name="stub",service = "NodeExecutor")
public class StubNodeExecutor implements NodeExecutor {
    private static final String STUB_EXEC_SUCCESS = "stub-exec-success";
    private static final String STUB_RESULT_CODE = "stub-result-code";

    public NodeExecutorResult executeCommand(final ExecutionContext context, final String[] command,
                                             final INodeEntry node) throws
        ExecutionException {
        //replace data context in args
        context.getExecutionListener().log(Constants.WARN_LEVEL,
            "[stub] execute on node " + node.getNodename() + ": " + StringArrayUtil.asString(
                DataContextUtils.replaceDataReferences(command, context.getDataContext()), " "));
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

        final int returnCode = tcode;
        final boolean returnSuccess = tsuccess;
        return new NodeExecutorResult() {
            public int getResultCode() {
                return returnCode;
            }

            public boolean isSuccess() {
                return returnSuccess;
            }
        };
    }
}
