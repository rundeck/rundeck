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
import com.dtolabs.rundeck.core.execution.workflow.steps.node.impl.ExecNodeStepExecutor;
import com.dtolabs.rundeck.core.execution.workflow.steps.node.impl.ScriptFileNodeStepExecutor;
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

    public void testResetDefaultProviders() throws Exception {
        final NodeStepExecutionService service = NodeStepExecutionService.getInstanceForFramework(
            getFrameworkInstance());

        final NodeStepExecutionItem item = new NodeStepExecutionItem() {
            public String getType() {
                return "NodeDispatch";
            }

            @Override
            public String getNodeStepType() {
                return "exec";
            }
        };
        final NodeStepExecutionItem item2 = new NodeStepExecutionItem() {
            public String getType() {
                return "NodeDispatch";
            }

            @Override
            public String getNodeStepType() {
                return "script";
            }
        };
        {
            final NodeStepExecutor interpreterForExecutionItem = service.getExecutorForExecutionItem(
                item);
            assertNotNull(interpreterForExecutionItem);
            assertTrue(interpreterForExecutionItem instanceof ExecNodeStepExecutor);
            final NodeStepExecutor interpreter2 = service.getExecutorForExecutionItem(item2);
            assertNotNull(interpreter2);
            assertTrue(interpreter2 instanceof ScriptFileNodeStepExecutor);
        }

        {
            final NodeStepExecutor testprovider = new NodeStepExecutor() {
                public NodeStepResult executeNodeStep(StepExecutionContext context, NodeStepExecutionItem item,
                                                         INodeEntry node) throws
                                                                          NodeStepException {
                    return null;
                }
            };
            final NodeStepExecutor testprovider2 = new NodeStepExecutor() {
                public NodeStepResult executeNodeStep(StepExecutionContext context, NodeStepExecutionItem item,
                                                         INodeEntry node) throws
                                                                          NodeStepException {
                    return null;
                }
            };
            service.registerInstance("exec", testprovider);
            service.registerInstance("script", testprovider2);
            final NodeStepExecutor interpreter = service.getExecutorForExecutionItem(item);
            assertNotNull(interpreter);
            assertTrue(interpreter == testprovider);

            final NodeStepExecutor interpreter2 = service.getExecutorForExecutionItem(item2);
            assertNotNull(interpreter2);
            assertTrue(interpreter2 == testprovider2);
        }

        //reset providers
        service.resetDefaultProviders();

        {
            final NodeStepExecutor interpreter = service.getExecutorForExecutionItem(item);
            assertNotNull(interpreter);
            assertTrue(interpreter instanceof ExecNodeStepExecutor);
            final NodeStepExecutor interpreter2 = service.getExecutorForExecutionItem(item2);
            assertNotNull(interpreter2);
            assertTrue(interpreter2 instanceof ScriptFileNodeStepExecutor);
        }


    }

    public void testGetInterpreterForExecutionItem() throws Exception {
        final NodeStepExecutionService service = NodeStepExecutionService.getInstanceForFramework(
            getFrameworkInstance());

        {
            //exec item should return default ExecNodeStepExecutor
            final NodeStepExecutionItem item = new NodeStepExecutionItem() {
                public String getType() {
                    return "NodeDispatch";
                }

                @Override
                public String getNodeStepType() {
                    return "exec";
                }
            };
            final NodeStepExecutor interpreterForExecutionItem = service.getExecutorForExecutionItem(
                item);
            assertNotNull(interpreterForExecutionItem);
            assertTrue(interpreterForExecutionItem instanceof ExecNodeStepExecutor);
        }
        {
            //script item should return default ScriptFileNodeStepExecutor
            final NodeStepExecutionItem item = new NodeStepExecutionItem() {
                public String getType() {
                    return "NodeDispatch";
                }

                @Override
                public String getNodeStepType() {
                    return "script";
                }
            };

            final NodeStepExecutor interpreter2 = service.getExecutorForExecutionItem(item);
            assertNotNull(interpreter2);
            assertTrue(interpreter2 instanceof ScriptFileNodeStepExecutor);
        }

        //test invalid provider name
        try {
            service.getExecutorForExecutionItem(new NodeStepExecutionItem() {
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
            service.getExecutorForExecutionItem(new NodeStepExecutionItem() {
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
            framework);
        assertNotNull(service);
        final FrameworkSupportService foundservice = framework.getService(ServiceNameConstants.WorkflowNodeStep);
        assertNotNull(foundservice);
        assertTrue(foundservice instanceof NodeStepExecutionService);
        assertEquals(foundservice, service);
    }

    public void testGetName() throws Exception {
        final NodeStepExecutionService service = NodeStepExecutionService.getInstanceForFramework(
            getFrameworkInstance());
        assertEquals(ServiceNameConstants.WorkflowNodeStep, service.getName());
    }
}
