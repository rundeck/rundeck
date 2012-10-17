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

import com.dtolabs.rundeck.core.cli.SingleProjectResolver;
import junit.framework.*;
import com.dtolabs.rundeck.core.tools.AbstractBaseTest;
import com.dtolabs.rundeck.core.common.Framework;
import com.dtolabs.rundeck.core.dispatcher.*;
import com.dtolabs.rundeck.core.cli.CLIToolOptionsException;
import org.apache.commons.cli.CommandLine;

import java.util.Collection;
import java.util.ArrayList;
import java.util.Date;
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
            //test -p is required for multiple projects
            final QueueTool tool = new QueueTool(getFrameworkInstance());
            tool.internalResolver = new SingleProjectResolver() {
                public boolean hasSingleProject() {
                    return false;
                }

                public String getSingleProjectName() {
                    return null;
                }
            };
            try {
                final String[] args = {"list"};
                final CommandLine commandLine = tool.parseArgs(args);
                tool.validateOptions(commandLine, args);
                fail("Should have thrown exception");
            } catch (CLIToolOptionsException e) {
                assertNotNull(e);
                assertTrue(e.getMessage().endsWith("-p argument is required with list action"));
            }
        }
        {
            //test -p is not required for a single project
            final QueueTool tool = new QueueTool(getFrameworkInstance());
            tool.internalResolver = new SingleProjectResolver() {
                public boolean hasSingleProject() {
                    return true;
                }

                public String getSingleProjectName() {
                    return "testProject";
                }
            };
            try {
                final String[] args = {"list"};
                final CommandLine commandLine = tool.parseArgs(args);
                tool.validateOptions(commandLine, args);
                assertEquals("testProject",tool.argProject);
            } catch (CLIToolOptionsException e) {
                fail("unexpected exception: " + e.getMessage());
            }
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
        {
            //test valid actions
            final QueueTool tool = new QueueTool(getFrameworkInstance());
            try {
                tool.parseArgs(new String[]{"list","-p","test1"});
                assertEquals("test1", tool.argProject);
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
        class FailDispatcher implements CentralDispatcher{

            public QueuedItemResult queueDispatcherJob(IDispatchedJob job) throws CentralDispatcherException {
                fail("unexpected call to queueDispatcherJob");
                return null;
            }

            public QueuedItemResult queueDispatcherScript(final IDispatchedScript dispatch) throws
                CentralDispatcherException {
                fail("unexpected call to queueDispatcherScript");
                return null;
            }

            public Collection<QueuedItem> listDispatcherQueue(final String project) throws CentralDispatcherException {
                //
                fail("unexpected call to listDispatcherQueue");
                return null;
            }
            public Collection<QueuedItem> listDispatcherQueue() throws CentralDispatcherException {
                //
                fail("unexpected call to listDispatcherQueue");
                return null;
            }

            public DispatcherResult killDispatcherExecution(final String id) throws CentralDispatcherException {
                fail("unexpected call to killDispatcherExecution");
                return null;
            }

            public Collection<IStoredJobLoadResult> loadJobs(ILoadJobsRequest request, java.io.File input, JobDefinitionFileFormat format) throws
                CentralDispatcherException {
                fail("unexpected call to loadJobs");
                return null;
            }

            public Collection<IStoredJob> listStoredJobs(IStoredJobsQuery query, OutputStream output,
                                                         JobDefinitionFileFormat format) throws
                CentralDispatcherException {
                fail("unexpected call to listStoredJobs");
                return null;
            }

            public void reportExecutionStatus(String project, String title, String status, int totalNodeCount,
                                              int successNodeCount, String tags, String script, String summary,
                                              Date start,
                                              Date end) throws CentralDispatcherException {
                fail("unexpected call to reportExecutionStatus");
            }

            public Collection<DeleteJobResult> deleteStoredJobs(Collection<String> jobIds) throws CentralDispatcherException {
                fail("unexpected call to deleteStoredJobs");
                return null;
            }
        }
        final Framework framework = getFrameworkInstance();
        {
            final QueueTool tool = new QueueTool(framework);
            final boolean[] actionCalled = new boolean[]{false};
            framework.setCentralDispatcherMgr(new FailDispatcher(){
                public Collection<QueuedItem> listDispatcherQueue(final String project) throws CentralDispatcherException {
                    //
                    actionCalled[0] = true;
                    return new ArrayList<QueuedItem>();
                }

            });

            //test list action without required -p

            try{
                tool.run(new String[]{"list"});

                fail("should have thrown argument exception.");
            } catch (CLIToolOptionsException e) {
                assertNotNull(e);
            } catch (QueueToolException e) {
                fail("unexpected exception: " + e.getMessage());
            }
            assertFalse("list action was not called", actionCalled[0]);


            //exec the dispatch

            tool.run(new String[]{"list","-p","test"});
            assertTrue("list action was not called", actionCalled[0]);

        }
        {

            final QueueTool tool = new QueueTool(framework);
            final boolean[] actionCalled = new boolean[]{false};
            final String[] idCalled = new String[]{"wrong"};
            framework.setCentralDispatcherMgr(new FailDispatcher() {
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