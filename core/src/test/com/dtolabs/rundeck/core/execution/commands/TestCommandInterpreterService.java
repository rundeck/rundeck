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
* TestCommandInterpreterService.java
* 
* User: Greg Schueler <a href="mailto:greg@dtosolutions.com">greg@dtosolutions.com</a>
* Created: 3/24/11 2:16 PM
* 
*/
package com.dtolabs.rundeck.core.execution.commands;

import com.dtolabs.rundeck.core.common.Framework;
import com.dtolabs.rundeck.core.common.FrameworkSupportService;
import com.dtolabs.rundeck.core.common.INodeEntry;
import com.dtolabs.rundeck.core.execution.ExecutionContext;
import com.dtolabs.rundeck.core.execution.ExecutionItem;
import com.dtolabs.rundeck.core.execution.service.ExecutionServiceException;
import com.dtolabs.rundeck.core.execution.service.MissingProviderException;
import com.dtolabs.rundeck.core.tools.AbstractBaseTest;
import junit.framework.Test;
import junit.framework.TestCase;
import junit.framework.TestSuite;

import java.util.*;

/**
 * TestCommandInterpreterService is ...
 *
 * @author Greg Schueler <a href="mailto:greg@dtosolutions.com">greg@dtosolutions.com</a>
 */
public class TestCommandInterpreterService extends AbstractBaseTest {
    public TestCommandInterpreterService(String name) {
        super(name);
    }

    public static Test suite() {
        return new TestSuite(TestCommandInterpreterService.class);
    }

    public void setUp() {
        super.setUp();

    }

    public void tearDown() throws Exception {
        super.tearDown();

    }

    public void testResetDefaultProviders() throws Exception {
        final CommandInterpreterService service = CommandInterpreterService.getInstanceForFramework(
            getFrameworkInstance());

        final ExecutionItem item = new ExecutionItem() {
            public String getType() {
                return "exec";
            }
        };
        final ExecutionItem item2 = new ExecutionItem() {
            public String getType() {
                return "script";
            }
        };
        {
            final CommandInterpreter interpreterForExecutionItem = service.getInterpreterForExecutionItem(
                item);
            assertNotNull(interpreterForExecutionItem);
            assertTrue(interpreterForExecutionItem instanceof ExecCommandInterpreter);
            final CommandInterpreter interpreter2 = service.getInterpreterForExecutionItem(item2);
            assertNotNull(interpreter2);
            assertTrue(interpreter2 instanceof ScriptFileCommandInterpreter);
        }

        {
            final CommandInterpreter testprovider = new CommandInterpreter() {
                public InterpreterResult interpretCommand(ExecutionContext context, ExecutionItem item,
                                                          INodeEntry node) throws
                    InterpreterException {
                    return null;
                }
            };
            final CommandInterpreter testprovider2 = new CommandInterpreter() {
                public InterpreterResult interpretCommand(ExecutionContext context, ExecutionItem item,
                                                          INodeEntry node) throws
                    InterpreterException {
                    return null;
                }
            };
            service.registerInstance("exec", testprovider);
            service.registerInstance("script", testprovider2);
            final CommandInterpreter interpreter = service.getInterpreterForExecutionItem(item);
            assertNotNull(interpreter);
            assertTrue(interpreter == testprovider);

            final CommandInterpreter interpreter2 = service.getInterpreterForExecutionItem(item2);
            assertNotNull(interpreter2);
            assertTrue(interpreter2 == testprovider2);
        }

        //reset providers
        service.resetDefaultProviders();

        {
            final CommandInterpreter interpreter = service.getInterpreterForExecutionItem(item);
            assertNotNull(interpreter);
            assertTrue(interpreter instanceof ExecCommandInterpreter);
            final CommandInterpreter interpreter2 = service.getInterpreterForExecutionItem(item2);
            assertNotNull(interpreter2);
            assertTrue(interpreter2 instanceof ScriptFileCommandInterpreter);
        }


    }

    public void testGetInterpreterForExecutionItem() throws Exception {
        final CommandInterpreterService service = CommandInterpreterService.getInstanceForFramework(
            getFrameworkInstance());

        {
            //exec item should return default ExecCommandInterpreter
            final ExecutionItem item = new ExecutionItem() {
                public String getType() {
                    return "exec";
                }
            };
            final CommandInterpreter interpreterForExecutionItem = service.getInterpreterForExecutionItem(
                item);
            assertNotNull(interpreterForExecutionItem);
            assertTrue(interpreterForExecutionItem instanceof ExecCommandInterpreter);
        }
        {
            //script item should return default ScriptFileCommandInterpreter
            final ExecutionItem item = new ExecutionItem() {
                public String getType() {
                    return "script";
                }
            };

            final CommandInterpreter interpreter2 = service.getInterpreterForExecutionItem(item);
            assertNotNull(interpreter2);
            assertTrue(interpreter2 instanceof ScriptFileCommandInterpreter);
        }

        //test invalid provider name
        try {
            service.getInterpreterForExecutionItem(new ExecutionItem() {
                public String getType() {
                    return "blah";
                }
            });
            fail("Should have thrown exception");
        } catch (ExecutionServiceException e) {
            assertTrue(e instanceof MissingProviderException);
            MissingProviderException mis = (MissingProviderException) e;
            assertEquals("CommandInterpreter", mis.getServiceName());
            assertEquals("blah", mis.getProviderName());
        }
        //test null provider name
        try {
            service.getInterpreterForExecutionItem(new ExecutionItem() {
                public String getType() {
                    return null;
                }
            });
            fail("Should have thrown exception");
        } catch (IllegalArgumentException e) {
            assertNotNull(e);
        }
    }

    public void testGetInstanceForFramework() throws Exception {
        final Framework framework = getFrameworkInstance();
        final CommandInterpreterService service = CommandInterpreterService.getInstanceForFramework(
            framework);
        assertNotNull(service);
        final FrameworkSupportService foundservice = framework.getService("CommandInterpreter");
        assertNotNull(foundservice);
        assertTrue(foundservice instanceof CommandInterpreterService);
        assertEquals(foundservice, service);
    }

    public void testGetName() throws Exception {
        final CommandInterpreterService service = CommandInterpreterService.getInstanceForFramework(
            getFrameworkInstance());
        assertEquals("CommandInterpreter", service.getName());
    }
}
