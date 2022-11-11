/*
 * Copyright 2016 SimplifyOps, Inc. (http://simplifyops.com)
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
 */

/*
* TestExecNodeStepExecutor.java
* 
* User: Greg Schueler <a href="mailto:greg@dtosolutions.com">greg@dtosolutions.com</a>
* Created: 3/24/11 3:19 PM
* 
*/
package com.dtolabs.rundeck.core.execution.workflow.steps.node.impl


import com.dtolabs.rundeck.core.common.Framework
import com.dtolabs.rundeck.core.common.IFrameworkServices
import com.dtolabs.rundeck.core.common.NodeEntryImpl
import com.dtolabs.rundeck.core.execution.ExecutionContextImpl
import com.dtolabs.rundeck.core.execution.ExecutionService
import com.dtolabs.rundeck.core.execution.service.NodeExecutorResult
import com.dtolabs.rundeck.core.execution.service.NodeExecutorResultImpl
import com.dtolabs.rundeck.core.execution.workflow.StepExecutionContext
import com.dtolabs.rundeck.core.tools.AbstractBaseTest
import spock.lang.Specification
/**
 * TestExecNodeStepExecutor is ...
 *
 * @author Greg Schueler <a href="mailto:greg@dtosolutions.com">greg@dtosolutions.com</a>
 */
public class ExecNodeStepExecutorSpec extends Specification {
    private static final String PROJ_NAME = "TestExecNodeStepExecutor";



    def testInterpretCommand() throws Exception {
        given:
            def executionService = Mock(ExecutionService)
            Framework frameworkInstance = AbstractBaseTest.createTestFramework(Mock(IFrameworkServices){
                getExecutionService()>> executionService
            })
            ExecNodeStepExecutor interpret = new ExecNodeStepExecutor(frameworkInstance);


            //execute command interpreter on local node
            final NodeEntryImpl test1 = new NodeEntryImpl("testhost", "test1");
            final StepExecutionContext context = ExecutionContextImpl.builder()
                                                                     .frameworkProject(PROJ_NAME)
                                                                     .framework(frameworkInstance)
                                                                     .user("blah")
                                                                     .threadCount(1)
                                                                     .build();

            final String[] strings = ["test", "command"].toArray();

            ExecCommand command = new ExecCommandBase() {
                public String[] getCommand() {

                    return strings;
                }
            };

        when:
            def interpreterResult = interpret.executeNodeStep(context, command, test1);

        then:
            1 * executionService.executeCommand(_, strings,test1)>>NodeExecutorResultImpl.createSuccess(test1)

            //returning null from command
            interpreterResult.isSuccess()
            interpreterResult instanceof NodeExecutorResult
            NodeExecutorResult result = (NodeExecutorResult) interpreterResult
            0 == result.getResultCode()
            test1 == result.getNode()
    }

}
