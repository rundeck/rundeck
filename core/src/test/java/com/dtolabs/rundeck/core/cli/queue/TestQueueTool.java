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

import com.dtolabs.rundeck.core.cli.FailDispatcher;
import junit.framework.*;
import com.dtolabs.rundeck.core.tools.AbstractBaseTest;
import com.dtolabs.rundeck.core.common.Framework;
import com.dtolabs.rundeck.core.dispatcher.*;
import com.dtolabs.rundeck.core.cli.CLIToolOptionsException;
import org.apache.commons.cli.CommandLine;

import java.util.*;
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


    void setListProjectsDispatcher(QueueTool tool, final String... projects){
        tool.setCentralDispatcher(
                new FailDispatcher() {
                    @Override
                    public List<String> listProjectNames() throws CentralDispatcherException {
                        return Arrays.asList(projects);
                    }
                }
        );
    }
    /**
     * Test parseArgs method
     *
     * @throws Exception if exception
     */
    public void testParseArgsInvalidAction() throws Exception {
            //test invalid action
            final QueueTool tool = createQueueTool();
            try {
                tool.parseArgs(new String[]{"invalid"});
                fail("invalid action should have failed");
            } catch (CLIToolOptionsException e) {
                assertNotNull(e);
            }

        }
    public void testParseArgsValidKillAction() throws Exception {
            //test valid actions
            final QueueTool tool = createQueueTool();
            try {
                tool.run(new String[]{"kill"});
                fail("should have thrown argument exception.");
            } catch (CLIToolOptionsException e) {
                assertNotNull(e);
            } catch (QueueToolException e) {
                fail("unexpected exception: " + e.getMessage());
            }
        }
    public void testParseArgsKillActionId() throws Exception {
            //test valid actions
            final QueueTool tool = createQueueTool();
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
    public void testParseArgsListWithoutProject() throws Exception {
            //test -p is required for multiple projects
            final QueueTool tool = createQueueTool();
            setListProjectsDispatcher(tool,"a","b");
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
    public void testParseArgsListDefaultProject() throws Exception {
            //test -p is not required for a single project
            final QueueTool tool = createQueueTool();

            setListProjectsDispatcher(tool, "testProject");
            try {
                final String[] args = {"list"};
                final CommandLine commandLine = tool.parseArgs(args);
                tool.validateOptions(commandLine, args);
                assertEquals("testProject",tool.argProject);
            } catch (CLIToolOptionsException e) {
                fail("unexpected exception: " + e.getMessage());
            }
        }
    public void testParseArgsList() throws Exception {
            //test valid actions
            final QueueTool tool = createQueueTool();
            try {
                tool.parseArgs(new String[]{"list"});
            } catch (CLIToolOptionsException e) {
                fail("unexpected exception: " + e.getMessage());
            }

        }
    public void testParseArgsListProject() throws Exception {
            //test valid actions
            final QueueTool tool = createQueueTool();
            try {
                tool.parseArgs(new String[]{"list","-p","test1"});
                assertEquals("test1", tool.argProject);
            } catch (CLIToolOptionsException e) {
                fail("unexpected exception: " + e.getMessage());
            }

    }

    private QueueTool createQueueTool() {
        return new QueueTool(getFrameworkInstance().getPropertyLookup());
    }


    /**
     * Test run method
     *
     * @throws Exception if exception
     */
    public void testRunProjectRequired() throws Exception {

        final QueueTool tool = createQueueTool();
        final boolean[] actionCalled = new boolean[]{false};
        tool.setCentralDispatcher(
                new FailDispatcher() {
                    public Collection<QueuedItem> listDispatcherQueue(final String project)
                            throws CentralDispatcherException
                    {
                        //
                        actionCalled[0] = true;
                        return new ArrayList<QueuedItem>();
                    }

                    @Override
                    public List<String> listProjectNames() throws CentralDispatcherException {
                        return Arrays.asList("test", "test2");
                    }
                }
        );

        //test list action without required -p

        try {
            tool.run(new String[]{"list"});

            fail("should have thrown argument exception.");
        } catch (CLIToolOptionsException e) {
            assertNotNull(e);
        } catch (QueueToolException e) {
            fail("unexpected exception: " + e.getMessage());
        }
        assertFalse("list action was not called", actionCalled[0]);

    }

    public void testRunProjectUsed() throws Exception {
        //exec the dispatch
        final QueueTool tool = createQueueTool();
        final boolean[] actionCalled = new boolean[]{false};
        tool.setCentralDispatcher(
                new FailDispatcher() {

                    @Override
                    public PagedResult<QueuedItem> listDispatcherQueue(
                            final String project, final Paging paging
                    ) throws CentralDispatcherException
                    {
                        //
                        actionCalled[0] = true;
                        return new PagedResult<QueuedItem>() {
                            @Override
                            public Collection<QueuedItem> getResults() {
                                return new ArrayList<QueuedItem>();
                            }

                            @Override
                            public long getTotal() {
                                return 0;
                            }

                            @Override
                            public Paging getPaging() {
                                return null;
                            }
                        };
                    }

                    @Override
                    public List<String> listProjectNames() throws CentralDispatcherException {
                        return Arrays.asList("test", "test2");
                    }
                }
        );
        tool.run(new String[]{"list", "-p", "test"});
        assertTrue("list action was called", actionCalled[0]);

    }
    public void testPaging() throws Exception {
        //exec the dispatch
        final QueueTool tool = createQueueTool();
        final boolean[] actionCalled = new boolean[]{false};
        tool.setCentralDispatcher(
                new FailDispatcher() {

                    @Override
                    public PagedResult<QueuedItem> listDispatcherQueue(
                            final String project, final Paging paging
                    ) throws CentralDispatcherException
                    {
                        //
                        actionCalled[0] = true;
                        assertEquals(10, paging.getOffset());
                        assertEquals(12, paging.getMax());
                        return new PagedResult<QueuedItem>() {
                            @Override
                            public Collection<QueuedItem> getResults() {
                                return new ArrayList<QueuedItem>();
                            }

                            @Override
                            public long getTotal() {
                                return 0;
                            }

                            @Override
                            public Paging getPaging() {
                                return paging;
                            }
                        };
                    }

                    @Override
                    public List<String> listProjectNames() throws CentralDispatcherException {
                        return Arrays.asList("test", "test2");
                    }
                }
        );
        tool.argOffset=10;
        tool.argMax=12;
        tool.run(new String[]{"list", "-p", "test"});
        assertTrue("list action was called", actionCalled[0]);

    }
    public void testRunKillIdRequired() throws Exception {

            final QueueTool tool = createQueueTool();
            final boolean[] actionCalled = new boolean[]{false};
            final String[] idCalled = new String[]{"wrong"};
            tool.setCentralDispatcher(new FailDispatcher() {
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