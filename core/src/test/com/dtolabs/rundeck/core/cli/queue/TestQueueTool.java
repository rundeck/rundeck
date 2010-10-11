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

package com.dtolabs.rundeck.core.cli.queue;
/*
* TestQueueTool.java
* 
* User: Greg Schueler <a href="mailto:greg@dtosolutions.com">greg@dtosolutions.com</a>
* Created: Feb 24, 2010 2:44:49 PM
* $Id$
*/

import junit.framework.*;
import com.dtolabs.rundeck.core.tools.AbstractBaseTest;
import com.dtolabs.rundeck.core.common.Framework;
import com.dtolabs.rundeck.core.dispatcher.*;
import com.dtolabs.rundeck.core.cli.CLIToolOptionsException;

import java.util.Collection;
import java.util.ArrayList;
import java.io.OutputStream;

/**
 * Test the QueueTool class
 */
public class TestQueueTool extends AbstractBaseTest {
    QueueTool queueTool;

    /**
     * Constructor
     *
     * @param name nam,e
     */
    public TestQueueTool(final String name) {
        super(name);
    }

    /**
     * suite
     *
     * @return suite
     */
    public static Test suite() {
        return new TestSuite(TestQueueTool.class);
    }

    /**
     * Setup
     */
    protected void setUp() {
        super.setUp();
    }

    /**
     * teardown
     *
     * @throws Exception
     */
    protected void tearDown() throws Exception {
        super.tearDown();
    }

    /**
     * main
     *
     * @param args args
     */
    public static void main(final String[] args) {
        junit.textui.TestRunner.run(suite());
    }


    /**
     * Test parseArgs method
     *
     * @throws Exception if exception
     */
    public void testParseArgs() throws Exception {
        {
            //test invalid action
            final QueueTool tool = new QueueTool(getFrameworkInstance());
            try {
                tool.parseArgs(new String[]{"invalid"});
                fail("invalid action should have failed");
            } catch (CLIToolOptionsException e) {
                assertNotNull(e);
            }

        }
        {
            //test valid actions
            final QueueTool tool = new QueueTool(getFrameworkInstance());
            try {
                tool.run(new String[]{"kill"});
                fail("should have thrown argument exception.");
            } catch (CLIToolOptionsException e) {
                assertNotNull(e);
            } catch (QueueToolException e) {
                fail("unexpected exception: " + e.getMessage());
            }
        }
        {
            //test valid actions
            final QueueTool tool = new QueueTool(getFrameworkInstance());
            boolean success = false;
            try {
                tool.parseArgs(new String[]{"kill", "-e", "test1"});
                assertEquals("test1", tool.getExecid());
                success = true;
            } catch (CLIToolOptionsException e) {
                fail("unexpected exception: " + e.getMessage());
            }
            assertTrue("parseArgs did not succeed", success);

        }
        {
            //test valid actions
            final QueueTool tool = new QueueTool(getFrameworkInstance());
            try {
                tool.parseArgs(new String[]{"list"});
            } catch (CLIToolOptionsException e) {
                fail("unexpected exception: " + e.getMessage());
            }

        }
    }

    /**
     * Test run method
     *
     * @throws Exception if exception
     */
    public void testRun() throws Exception {
        final Framework framework = getFrameworkInstance();
        {
            //test list action

            final QueueTool tool = new QueueTool(framework);
            final boolean[] actionCalled = new boolean[]{false};
            framework.setCentralDispatcherMgr(new CentralDispatcher() {

                public QueuedItemResult queueDispatcherJob(IDispatchedJob job) throws CentralDispatcherException {
                    fail("unexpected call to queueDispatcherJob");
                    return null;
                }

                public QueuedItemResult queueDispatcherScript(final IDispatchedScript dispatch) throws
                    CentralDispatcherException {
                    fail("unexpected call to queueDispatcherScript");
                    return null;
                }

                public Collection<QueuedItem> listDispatcherQueue() throws CentralDispatcherException {
                    //
                    actionCalled[0] = true;
                    return new ArrayList<QueuedItem>();
                }

                public DispatcherResult killDispatcherExecution(final String id) throws CentralDispatcherException {
                    fail("unexpected call to killDispatcherExecution");
                    return null;
                }

                public Collection<IStoredJobLoadResult> loadJobs(ILoadJobsRequest request, java.io.File input) throws
                    CentralDispatcherException {
                    fail("unexpected call to loadJobs");
                    return null;
                }

                public Collection<IStoredJob> listStoredJobs(IStoredJobsQuery query, OutputStream output) throws
                    CentralDispatcherException {
                    fail("unexpected call to listStoredJobs");
                    return null;
                }
            });

            //exec the run-exec

            tool.run(new String[]{"list"});
            assertTrue("list action was not called", actionCalled[0]);

        }
        {

            final QueueTool tool = new QueueTool(framework);
            final boolean[] actionCalled = new boolean[]{false};
            final String[] idCalled = new String[]{"wrong"};
            framework.setCentralDispatcherMgr(new CentralDispatcher() {

                public QueuedItemResult queueDispatcherJob(IDispatchedJob job) throws CentralDispatcherException {
                    fail("unexpected call to queueDispatcherJob");
                    return null;
                }

                public QueuedItemResult queueDispatcherScript(final IDispatchedScript dispatch) throws
                    CentralDispatcherException {
                    fail("unexpected call to queueDispatcherScript");
                    return null;
                }

                public Collection<QueuedItem> listDispatcherQueue() throws CentralDispatcherException {
                    fail("unexpected call to listDispatcherQueue");
                    return null;
                }

                public DispatcherResult killDispatcherExecution(final String id) throws CentralDispatcherException {
                    actionCalled[0] = true;
                    idCalled[0] = id;
                    return new DispatcherResult() {
                        public String getMessage() {
                            return "test message 1";
                        }

                        public boolean isSuccessful() {
                            return true;
                        }
                    };
                }

                public Collection<IStoredJob> listStoredJobs(IStoredJobsQuery query, OutputStream output) throws
                    CentralDispatcherException {
                    fail("unexpected call to listStoredJobs");
                    return null;
                }

                public Collection<IStoredJobLoadResult> loadJobs(ILoadJobsRequest request, java.io.File input) throws
                    CentralDispatcherException {
                    fail("unexpected call to loadJobs");
                    return null;
                }
            });

            //test kill action without required argument -e

            try {
                tool.run(new String[]{"kill"});
                fail("should have thrown argument exception.");
            } catch (CLIToolOptionsException e) {
                assertNotNull(e);
            } catch (QueueToolException e) {
                fail("unexpected exception: " + e.getMessage());
            }
            assertFalse("kill action should not have been called", actionCalled[0]);
            assertFalse("unexpected id", "test1".equals(idCalled[0]));
            //test kill action with required argument -e

            try {
                tool.run(new String[]{"kill", "-e", "test1"});
                assertTrue("kill action should have been called", actionCalled[0]);
                assertEquals("unexpected id", "test1", idCalled[0]);
            } catch (CLIToolOptionsException e) {
                fail("unexpected exception: " + e.getMessage());
            } catch (QueueToolException e) {
                fail("unexpected exception: " + e.getMessage());
            }


        }
    }
}