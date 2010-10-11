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

package com.dtolabs.rundeck.core.execution;
/*
* TestBaseExecutionService.java
* 
* User: Greg Schueler <a href="mailto:greg@dtosolutions.com">greg@dtosolutions.com</a>
* Created: Mar 5, 2010 2:20:08 PM
* $Id$
*/

import com.dtolabs.rundeck.core.common.Framework;
import com.dtolabs.rundeck.core.dispatcher.IDispatchedScript;
import com.dtolabs.rundeck.core.tools.AbstractBaseTest;
import junit.framework.Test;
import junit.framework.TestSuite;

import java.util.HashMap;

public class TestBaseExecutionService extends AbstractBaseTest {
    BaseExecutionService baseExecutionService;

    public TestBaseExecutionService(String name) {
        super(name);
    }

    public static Test suite() {
        return new TestSuite(TestBaseExecutionService.class);
    }

    protected void setUp() {
        super.setUp();
        baseExecutionService = new ExecutionServiceImpl(ExecutionServiceFactory.defaultExecutorClasses,
            new HashMap<Class<? extends ExecutionItem>, Executor>(), getFrameworkInstance());
    }

    protected void tearDown() throws Exception {
    }

    public static void main(String args[]) {
        junit.textui.TestRunner.run(suite());
    }

    private class testclass1 implements ExecutionItem {

    }

    private class testclass2 implements DispatchedScriptExecutionItem {

        public IDispatchedScript getDispatchedScript() {
            return null;
        }
    }

    public void testExecutorForItemClass() throws Exception {
        try {
            final Executor executor = baseExecutionService.executorForItemClass(testclass1.class);
            fail("Should have thrown an Exception");
        }
        catch (ExecutionException ex) {
            assertNotNull(ex);
        }
        try {
            final Executor executor = baseExecutionService.executorForItemClass(DispatchedScriptExecutionItem.class);
            assertNotNull(executor);
            assertTrue(executor instanceof DispatchedScriptExecutor);
        }
        catch (ExecutionException ex) {
            ex.printStackTrace();
            fail("unexpected exception: " + ex.getMessage());
        }
        try {
            final Executor executor = baseExecutionService.executorForItemClass(testclass2.class);
            assertNotNull(executor);
            assertTrue(executor instanceof DispatchedScriptExecutor);
        }
        catch (ExecutionException ex) {
            ex.printStackTrace();
            fail("unexpected exception: " + ex.getMessage());
        }
    }

    static class testExecutor1 implements Executor {
        public ExecutionResult executeItem(ExecutionItem item, ExecutionListener listener,
                                           final ExecutionService executionService,
                                           final Framework framework) throws ExecutionException {
            return null;
        }
    }

    static class testExecutionItem1 implements ExecutionItem {

    }

    public void testDefaultExecutorInstances() throws Exception {

        final HashMap<Class<? extends ExecutionItem>, Executor> map =
            new HashMap<Class<? extends ExecutionItem>, Executor>();

        final testExecutor1 executor1 = new testExecutor1();
        map.put(testExecutionItem1.class, executor1);
        baseExecutionService = new ExecutionServiceImpl(ExecutionServiceFactory.defaultExecutorClasses,
            map, getFrameworkInstance());

        testExecutionItem1 testitem1 = new testExecutionItem1();
        final Executor testExecutor1 = baseExecutionService.executorForItem(testitem1);
        assertNotNull(testExecutor1);
        assertTrue(testExecutor1 instanceof testExecutor1);
        
    }
}