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
* TestNodeStepExecutorService.java
* 
* User: Greg Schueler <a href="mailto:greg@dtosolutions.com">greg@dtosolutions.com</a>
* Created: 3/24/11 2:16 PM
* 
*/
package com.dtolabs.rundeck.core.execution.workflow.steps.node;

import com.dtolabs.rundeck.core.common.Framework;
import com.dtolabs.rundeck.core.common.FrameworkSupportService;
import com.dtolabs.rundeck.core.common.INodeEntry;
import com.dtolabs.rundeck.core.execution.service.ExecutionServiceException;
import com.dtolabs.rundeck.core.execution.service.MissingProviderException;
import com.dtolabs.rundeck.core.execution.workflow.StepExecutionContext;
import com.dtolabs.rundeck.core.tools.AbstractBaseTest;
import com.dtolabs.rundeck.plugins.ServiceNameConstants;
import junit.framework.Test;
import junit.framework.TestSuite;


/**
 * TestNodeStepExecutorService is ...
 *
 * @author Greg Schueler <a href="mailto:greg@dtosolutions.com">greg@dtosolutions.com</a>
 */
public class TestNodeStepExecutorService extends AbstractBaseTest {
    public TestNodeStepExecutorService(String name) {
        super(name);
    }

    public static Test suite() {
        return new TestSuite(TestNodeStepExecutorService.class);
    }

    public void setUp() {
        super.setUp();

    }

    public void tearDown() throws Exception {
        super.tearDown();

    }
    abstract class BaseNodeStepExecutionItem implements NodeStepExecutionItem{
        @Override
        public String getLabel() {
            return null;
        }
    }

    public void testGetInterpreterForExecutionItem() throws Exception {
        final NodeStepExecutionService service = NodeStepExecutionService.getInstanceForFramework(
            getFrameworkInstance(),getFrameworkInstance());

        //test invalid provider name
        try {
            service.getExecutorForExecutionItem(new BaseNodeStepExecutionItem() {
                public String getType() {
                    return "NodeDispatch";
                }

                @Override
                public String getNodeStepType() {
                    return "blah";
                }
            });
            fail("Should have thrown exception");
        } catch (ExecutionServiceException e) {
            assertTrue(e instanceof MissingProviderException);
            MissingProviderException mis = (MissingProviderException) e;
//            assertEquals(ServiceNameConstants.WorkflowNodeStep, mis.getServiceName());
            assertEquals("blah", mis.getProviderName());
        }
        //test null provider name
        try {
            service.getExecutorForExecutionItem(new BaseNodeStepExecutionItem() {
                public String getType() {
                    return "NodeDispatch";
                }

                @Override
                public String getNodeStepType() {
                    return null;
                }
            });
            fail("Should have thrown exception");
        } catch (NullPointerException e) {
            assertNotNull(e);
        }
    }

    public void testGetInstanceForFramework() throws Exception {
        final Framework framework = getFrameworkInstance();
        final NodeStepExecutionService service = NodeStepExecutionService.getInstanceForFramework(
            framework,framework);
        assertNotNull(service);
        final FrameworkSupportService foundservice = framework.getService(ServiceNameConstants.WorkflowNodeStep);
        assertNotNull(foundservice);
        assertTrue(foundservice instanceof NodeStepExecutionService);
        assertEquals(foundservice, service);
    }

    public void testGetName() throws Exception {
        final NodeStepExecutionService service = NodeStepExecutionService.getInstanceForFramework(
            getFrameworkInstance(),createTestFramework());
        assertEquals(ServiceNameConstants.WorkflowNodeStep, service.getName());
    }
}
