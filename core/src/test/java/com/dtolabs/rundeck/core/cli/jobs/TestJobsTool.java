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

package com.dtolabs.rundeck.core.cli.jobs;
/*
* TestJobsTool.java
* 
* User: Greg Schueler <a href="mailto:greg@dtosolutions.com">greg@dtosolutions.com</a>
* Created: Mar 29, 2010 4:21:29 PM
* $Id$
*/

import com.dtolabs.client.services.DeleteJobResultImpl;
import com.dtolabs.client.services.StoredJobImpl;
import com.dtolabs.client.services.StoredJobLoadResultImpl;
import com.dtolabs.rundeck.core.cli.CLIToolException;
import com.dtolabs.rundeck.core.cli.CLIToolOptionsException;
import com.dtolabs.rundeck.core.cli.SingleProjectResolver;
import com.dtolabs.rundeck.core.common.Framework;
import com.dtolabs.rundeck.core.dispatcher.*;
import com.dtolabs.rundeck.core.tools.AbstractBaseTest;
import junit.framework.Test;
import junit.framework.TestSuite;
import org.apache.commons.cli.CommandLine;

import java.io.File;
import java.io.OutputStream;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Date;


public class TestJobsTool extends AbstractBaseTest {
    JobsTool jobsTool;

    public TestJobsTool(String name) {
        super(name);
    }

    public static Test suite() {
        return new TestSuite(TestJobsTool.class);
    }

    protected void setUp() {
        super.setUp();

    }

    protected void tearDown() throws Exception {
        super.tearDown();
    }

    public static void main(String args[]) {
        junit.textui.TestRunner.run(suite());
    }

    static class testCentralDispatcher1 implements CentralDispatcher {
        boolean purgeStoredJobsCalled = false;
        boolean listStoredJobsCalled = false;
        boolean loadJobsCalled = false;
        ILoadJobsRequest loadRequest;
        java.io.File loadInput;
        IStoredJobsQuery listStoredJobsQuery;
        Collection<String> purgeJobsRequest;
        OutputStream listStoredJobsOutput;
        Collection<IStoredJob> listJobsResult;
        Collection<IStoredJobLoadResult> loadJobsResult;
        Collection<DeleteJobResult> purgeJobsResult;
        JobDefinitionFileFormat loadFormat;
        JobDefinitionFileFormat listFormat;


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

        public ExecutionFollowResult followDispatcherExecution(String id, ExecutionFollowRequest request,
                                                               ExecutionFollowReceiver receiver) throws
            CentralDispatcherException {
            fail("unexpected call to followDispatcherExecution");
            return null;
        }

        public void reportExecutionStatus(String project, String title, String status, int totalNodeCount,
                                          int successNodeCount, String tags, String script, String summary, Date start,
                                          Date end) throws CentralDispatcherException {
            fail("unexpected call to reportExecutionStatus");
        }

        public ExecutionDetail getExecution(String execId) throws CentralDispatcherException {
            fail("unexpected call to getExecution");
            return null;
        }

        public Collection<IStoredJobLoadResult> loadJobs(ILoadJobsRequest request, java.io.File input,
                                                         JobDefinitionFileFormat format) throws
            CentralDispatcherException {
            this.loadJobsCalled = true;
            this.loadRequest = request;
            this.loadInput = input;
            this.loadFormat=format;
            return loadJobsResult;
        }

        public Collection<IStoredJob> listStoredJobs(IStoredJobsQuery query, OutputStream output,
                                                     JobDefinitionFileFormat format) throws
            CentralDispatcherException {
            this.listStoredJobsCalled = true;
            this.listStoredJobsQuery = query;
            this.listStoredJobsOutput = output;
            this.listFormat=format;
            return listJobsResult;
        }

        public Collection<DeleteJobResult> deleteStoredJobs(Collection<String> jobIds) throws CentralDispatcherException {
            this.purgeStoredJobsCalled=true;
            this.purgeJobsRequest=jobIds;
            return purgeJobsResult;
        }
    }

    public void testRun() throws Exception {
        final Framework framework = getFrameworkInstance();
        {
            //test list action
            //test null  result

            final JobsTool tool = new JobsTool(framework);
            final testCentralDispatcher1 centralDispatcher1 = new testCentralDispatcher1();
            framework.setCentralDispatcherMgr(centralDispatcher1);

            try {
                tool.run(new String[]{"list","-p","test"});
                fail("run should fail");
            } catch (JobsToolException e) {
                assertTrue(e.getMessage().startsWith("List request returned null"));
            }
        }
        {
            //test list action missing -p flag
            //test 0 items result

            final JobsTool tool = new JobsTool(framework);
            final testCentralDispatcher1 centralDispatcher1 = new testCentralDispatcher1();
            framework.setCentralDispatcherMgr(centralDispatcher1);

            final ArrayList<IStoredJob> jobs = new ArrayList<IStoredJob>();
            centralDispatcher1.listJobsResult = jobs;

            try {
                tool.run(new String[]{"list"});
                fail("run should fail");
            } catch (CLIToolOptionsException e) {
                assertTrue(e.getMessage().startsWith("list action: -p/--project option is required"));
            }
        }
        {
            //test list action
            //test 0 items result

            final JobsTool tool = new JobsTool(framework);
            final testCentralDispatcher1 centralDispatcher1 = new testCentralDispatcher1();
            framework.setCentralDispatcherMgr(centralDispatcher1);

            final ArrayList<IStoredJob> jobs = new ArrayList<IStoredJob>();
            centralDispatcher1.listJobsResult = jobs;

            tool.run(new String[]{"list", "-p", "test"});
            assertTrue("list action was not called", centralDispatcher1.listStoredJobsCalled);
            assertFalse("load action should not be called", centralDispatcher1.loadJobsCalled);
            assertNotNull(centralDispatcher1.listStoredJobsQuery);
            assertNull(centralDispatcher1.listStoredJobsOutput);
            assertEquals(JobDefinitionFileFormat.xml,centralDispatcher1.listFormat);
        }
        {
            //test list action with output file
            //test 0 items result

            final JobsTool tool = new JobsTool(framework);
            final testCentralDispatcher1 centralDispatcher1 = new testCentralDispatcher1();
            framework.setCentralDispatcherMgr(centralDispatcher1);
            File t = File.createTempFile("TestJobsTool", "xml");
            t.deleteOnExit();

            final ArrayList<IStoredJob> jobs = new ArrayList<IStoredJob>();
            centralDispatcher1.listJobsResult = jobs;

            tool.run(new String[]{"list", "-f", t.getAbsolutePath(), "-p", "test"});
            assertTrue("list action was not called", centralDispatcher1.listStoredJobsCalled);
            assertFalse("load action should not be called", centralDispatcher1.loadJobsCalled);
            assertNotNull(centralDispatcher1.listStoredJobsQuery);
            assertNotNull(centralDispatcher1.listStoredJobsOutput);
            assertEquals(JobDefinitionFileFormat.xml, centralDispatcher1.listFormat);
        }
        {
            //test list action with output file, yaml format
            //test 0 items result

            final JobsTool tool = new JobsTool(framework);
            final testCentralDispatcher1 centralDispatcher1 = new testCentralDispatcher1();
            framework.setCentralDispatcherMgr(centralDispatcher1);
            File t = File.createTempFile("TestJobsTool", "xml");
            t.deleteOnExit();

            final ArrayList<IStoredJob> jobs = new ArrayList<IStoredJob>();
            centralDispatcher1.listJobsResult = jobs;

            tool.run(new String[]{"list", "-f", t.getAbsolutePath(), "-" + JobsTool.FORMAT_OPTION,
                JobDefinitionFileFormat.yaml.getName(), "-p", "test"});
            assertTrue("list action was not called", centralDispatcher1.listStoredJobsCalled);
            assertFalse("load action should not be called", centralDispatcher1.loadJobsCalled);
            assertNotNull(centralDispatcher1.listStoredJobsQuery);
            assertNotNull(centralDispatcher1.listStoredJobsOutput);
            assertEquals(JobDefinitionFileFormat.yaml, centralDispatcher1.listFormat);
        }
        {
            //test list action with query params, -n

            final JobsTool tool = new JobsTool(framework);
            final testCentralDispatcher1 centralDispatcher1 = new testCentralDispatcher1();
            framework.setCentralDispatcherMgr(centralDispatcher1);

            final ArrayList<IStoredJob> jobs = new ArrayList<IStoredJob>();
            centralDispatcher1.listJobsResult = jobs;

            tool.run(new String[]{"list", "-" + JobsTool.NAME_OPTION, "name1", "-p", "test"});
            assertTrue("list action was not called", centralDispatcher1.listStoredJobsCalled);
            assertFalse("load action should not be called", centralDispatcher1.loadJobsCalled);
            assertNull(centralDispatcher1.listStoredJobsOutput);
            assertNotNull(centralDispatcher1.listStoredJobsQuery);
            assertEquals("name1", centralDispatcher1.listStoredJobsQuery.getNameMatch());
            assertEquals(JobDefinitionFileFormat.xml, centralDispatcher1.listFormat);
        }
        {
            //test list action with query params, --name

            final JobsTool tool = new JobsTool(framework);
            final testCentralDispatcher1 centralDispatcher1 = new testCentralDispatcher1();
            framework.setCentralDispatcherMgr(centralDispatcher1);

            final ArrayList<IStoredJob> jobs = new ArrayList<IStoredJob>();
            centralDispatcher1.listJobsResult = jobs;

            tool.run(new String[]{"list", "--" + JobsTool.NAME_OPTION_LONG, "name1", "-p", "test"});
            assertTrue("list action was not called", centralDispatcher1.listStoredJobsCalled);
            assertFalse("load action should not be called", centralDispatcher1.loadJobsCalled);
            assertNull(centralDispatcher1.listStoredJobsOutput);
            assertNotNull(centralDispatcher1.listStoredJobsQuery);
            assertEquals("name1", centralDispatcher1.listStoredJobsQuery.getNameMatch());
            assertEquals(JobDefinitionFileFormat.xml, centralDispatcher1.listFormat);
        }
        {
            //test list action with query params, -g

            final JobsTool tool = new JobsTool(framework);
            final testCentralDispatcher1 centralDispatcher1 = new testCentralDispatcher1();
            framework.setCentralDispatcherMgr(centralDispatcher1);

            final ArrayList<IStoredJob> jobs = new ArrayList<IStoredJob>();
            centralDispatcher1.listJobsResult = jobs;

            tool.run(new String[]{"list", "-" + JobsTool.GROUP_OPTION, "group1", "-p", "test"});
            assertTrue("list action was not called", centralDispatcher1.listStoredJobsCalled);
            assertFalse("load action should not be called", centralDispatcher1.loadJobsCalled);
            assertNull(centralDispatcher1.listStoredJobsOutput);
            assertNotNull(centralDispatcher1.listStoredJobsQuery);
            assertEquals("group1", centralDispatcher1.listStoredJobsQuery.getGroupMatch());
            assertEquals(JobDefinitionFileFormat.xml, centralDispatcher1.listFormat);
        }
        {
            //test list action with query params, --group

            final JobsTool tool = new JobsTool(framework);
            final testCentralDispatcher1 centralDispatcher1 = new testCentralDispatcher1();
            framework.setCentralDispatcherMgr(centralDispatcher1);

            final ArrayList<IStoredJob> jobs = new ArrayList<IStoredJob>();
            centralDispatcher1.listJobsResult = jobs;

            tool.run(new String[]{"list", "--" + JobsTool.GROUP_OPTION_LONG, "group2", "-p", "test"});
            assertTrue("list action was not called", centralDispatcher1.listStoredJobsCalled);
            assertFalse("load action should not be called", centralDispatcher1.loadJobsCalled);
            assertNull(centralDispatcher1.listStoredJobsOutput);
            assertNotNull(centralDispatcher1.listStoredJobsQuery);
            assertEquals("group2", centralDispatcher1.listStoredJobsQuery.getGroupMatch());
            assertEquals(JobDefinitionFileFormat.xml, centralDispatcher1.listFormat);
        }
        {
            //test list action with query params, -i

            final JobsTool tool = new JobsTool(framework);
            final testCentralDispatcher1 centralDispatcher1 = new testCentralDispatcher1();
            framework.setCentralDispatcherMgr(centralDispatcher1);

            final ArrayList<IStoredJob> jobs = new ArrayList<IStoredJob>();
            centralDispatcher1.listJobsResult = jobs;

            tool.run(
                new String[]{"list",  "-" + JobsTool.IDLIST_OPTION, "1,2", "-p", "test"});
            assertTrue("list action was not called", centralDispatcher1.listStoredJobsCalled);
            assertFalse("load action should not be called", centralDispatcher1.loadJobsCalled);
            assertNull(centralDispatcher1.listStoredJobsOutput);
            assertNotNull(centralDispatcher1.listStoredJobsQuery);
            assertEquals("1,2", centralDispatcher1.listStoredJobsQuery.getIdlist());
            assertEquals(JobDefinitionFileFormat.xml, centralDispatcher1.listFormat);
        }
        {
            //test list action with query params, --idlist

            final JobsTool tool = new JobsTool(framework);
            final testCentralDispatcher1 centralDispatcher1 = new testCentralDispatcher1();
            framework.setCentralDispatcherMgr(centralDispatcher1);

            final ArrayList<IStoredJob> jobs = new ArrayList<IStoredJob>();
            centralDispatcher1.listJobsResult = jobs;

            tool.run(
                new String[]{"list", "--" + JobsTool.IDLIST_OPTION_LONG, "3,4", "-p", "test"});
            assertTrue("list action was not called", centralDispatcher1.listStoredJobsCalled);
            assertFalse("load action should not be called", centralDispatcher1.loadJobsCalled);
            assertNull(centralDispatcher1.listStoredJobsOutput);
            assertNotNull(centralDispatcher1.listStoredJobsQuery);
            assertEquals("3,4", centralDispatcher1.listStoredJobsQuery.getIdlist());
            assertEquals(JobDefinitionFileFormat.xml, centralDispatcher1.listFormat);
        }
    }

    public void testLoad() throws Exception {
        final Framework framework = getFrameworkInstance();

        final JobsTool tool = new JobsTool(framework);
        final testCentralDispatcher1 centralDispatcher1 = new testCentralDispatcher1();
        framework.setCentralDispatcherMgr(centralDispatcher1);

        final ArrayList<IStoredJob> jobs = new ArrayList<IStoredJob>();
        centralDispatcher1.loadJobsResult = new ArrayList<IStoredJobLoadResult>();

        File test = File.createTempFile("blah", ".xml");
        tool.run(new String[]{"load", "-f", test.getAbsolutePath()});
        assertFalse("list action was not called", centralDispatcher1.listStoredJobsCalled);
        assertTrue("load action should be called", centralDispatcher1.loadJobsCalled);
        assertNull(centralDispatcher1.listStoredJobsOutput);
        assertNull(centralDispatcher1.listStoredJobsQuery);
        assertNotNull(centralDispatcher1.loadRequest);
        assertNull(centralDispatcher1.loadRequest.getProject());
        assertEquals(StoredJobsRequestDuplicateOption.update, centralDispatcher1.loadRequest.getDuplicateOption());
        assertNotNull(centralDispatcher1.loadInput);
        assertEquals(JobDefinitionFileFormat.xml, centralDispatcher1.loadFormat);
    }
    public void testLoadWithProject() throws Exception {
        final Framework framework = getFrameworkInstance();

        final JobsTool tool = new JobsTool(framework);
        final testCentralDispatcher1 centralDispatcher1 = new testCentralDispatcher1();
        framework.setCentralDispatcherMgr(centralDispatcher1);

        final ArrayList<IStoredJob> jobs = new ArrayList<IStoredJob>();
        centralDispatcher1.loadJobsResult = new ArrayList<IStoredJobLoadResult>();

        File test = File.createTempFile("blah", ".xml");
        tool.run(new String[]{"load", "-f", test.getAbsolutePath(), "-p", "project1"});
        assertFalse("list action was not called", centralDispatcher1.listStoredJobsCalled);
        assertTrue("load action should be called", centralDispatcher1.loadJobsCalled);
        assertNull(centralDispatcher1.listStoredJobsOutput);
        assertNull(centralDispatcher1.listStoredJobsQuery);
        assertNotNull(centralDispatcher1.loadRequest);
        assertEquals("project1", centralDispatcher1.loadRequest.getProject());
        assertEquals(StoredJobsRequestDuplicateOption.update, centralDispatcher1.loadRequest.getDuplicateOption());
        assertNotNull(centralDispatcher1.loadInput);
        assertEquals(JobDefinitionFileFormat.xml, centralDispatcher1.loadFormat);
    }
    public void testPurge() throws Exception{
        final Framework framework = getFrameworkInstance();
        {
            //test purge action with query params, --idlist

            final JobsTool tool = new JobsTool(framework);
            final testCentralDispatcher1 centralDispatcher1 = new testCentralDispatcher1();
            framework.setCentralDispatcherMgr(centralDispatcher1);

            final ArrayList<IStoredJob> jobs = new ArrayList<IStoredJob>();
            jobs.add(StoredJobImpl.create("3", "test3", "blah", "blah", "blah", "test"));
            jobs.add(StoredJobImpl.create("4", "test3", "blah", "blah", "blah", "test"));
            centralDispatcher1.listJobsResult = jobs;
            final ArrayList<DeleteJobResult> results = new ArrayList<DeleteJobResult>();
            centralDispatcher1.purgeJobsResult = results;

            tool.run(
                new String[]{"purge", "--" + JobsTool.IDLIST_OPTION_LONG, "3,4", "-p", "test"});

            assertTrue("list action should be called", centralDispatcher1.listStoredJobsCalled);
            assertNull(centralDispatcher1.listStoredJobsOutput);
            assertNotNull(centralDispatcher1.listStoredJobsQuery);
            assertEquals("3,4", centralDispatcher1.listStoredJobsQuery.getIdlist());

            assertTrue("purge action should be called", centralDispatcher1.purgeStoredJobsCalled);
            assertNotNull(centralDispatcher1.purgeJobsRequest);
            assertEquals(Arrays.asList("3", "4"), centralDispatcher1.purgeJobsRequest);

            assertFalse("load action should not be called", centralDispatcher1.loadJobsCalled);
            assertNull(centralDispatcher1.loadRequest);
        }
        {
            //test purge action with query params, --idlist, no results

            final JobsTool tool = new JobsTool(framework);
            final testCentralDispatcher1 centralDispatcher1 = new testCentralDispatcher1();
            framework.setCentralDispatcherMgr(centralDispatcher1);

            final ArrayList<IStoredJob> jobs = new ArrayList<IStoredJob>();
            centralDispatcher1.listJobsResult = jobs;
            final ArrayList<DeleteJobResult> results = new ArrayList<DeleteJobResult>();
            centralDispatcher1.purgeJobsResult = results;

            tool.run(
                new String[]{"purge", "--" + JobsTool.IDLIST_OPTION_LONG, "3,4", "-p", "test"});

            assertTrue("list action should be called", centralDispatcher1.listStoredJobsCalled);
            assertNull(centralDispatcher1.listStoredJobsOutput);
            assertNotNull(centralDispatcher1.listStoredJobsQuery);
            assertEquals("3,4", centralDispatcher1.listStoredJobsQuery.getIdlist());

            assertFalse("purge action should be called", centralDispatcher1.purgeStoredJobsCalled);
            assertNull(centralDispatcher1.purgeJobsRequest);

            assertFalse("load action should not be called", centralDispatcher1.loadJobsCalled);
            assertNull(centralDispatcher1.loadRequest);
        }
        {
            //test purge: success

            final JobsTool tool = new JobsTool(framework);
            final testCentralDispatcher1 centralDispatcher1 = new testCentralDispatcher1();
            framework.setCentralDispatcherMgr(centralDispatcher1);

            final ArrayList<IStoredJob> jobs = new ArrayList<IStoredJob>();
            jobs.add(StoredJobImpl.create("3", "test3", "blah", "blah", "blah", "test"));
            jobs.add(StoredJobImpl.create("4", "test3", "blah", "blah", "blah", "test"));
            centralDispatcher1.listJobsResult = jobs;
            final ArrayList<DeleteJobResult> results = new ArrayList<DeleteJobResult>();
            results.add(DeleteJobResultImpl.createDeleteJobResultImpl(true, "success", "3", null));
            results.add(DeleteJobResultImpl.createDeleteJobResultImpl(true, "success", "4", null));
            centralDispatcher1.purgeJobsResult = results;

            tool.run(
                new String[]{"purge", "--" + JobsTool.IDLIST_OPTION_LONG, "3,4", "-p", "test"});

            assertTrue("list action should be called", centralDispatcher1.listStoredJobsCalled);
            assertNull(centralDispatcher1.listStoredJobsOutput);
            assertNotNull(centralDispatcher1.listStoredJobsQuery);
            assertEquals("3,4", centralDispatcher1.listStoredJobsQuery.getIdlist());

            assertTrue("purge action should be called", centralDispatcher1.purgeStoredJobsCalled);
            assertNotNull(centralDispatcher1.purgeJobsRequest);
            assertEquals(Arrays.asList("3", "4"), centralDispatcher1.purgeJobsRequest);

            assertFalse("load action should not be called", centralDispatcher1.loadJobsCalled);
            assertNull(centralDispatcher1.loadRequest);
        }
        {
            //test purge: failed purge causes exception

            final JobsTool tool = new JobsTool(framework);
            final testCentralDispatcher1 centralDispatcher1 = new testCentralDispatcher1();
            framework.setCentralDispatcherMgr(centralDispatcher1);

            final ArrayList<IStoredJob> jobs = new ArrayList<IStoredJob>();
            jobs.add(StoredJobImpl.create("3", "test3", "blah", "blah", "blah", "test"));
            jobs.add(StoredJobImpl.create("4", "test3", "blah", "blah", "blah", "test"));
            centralDispatcher1.listJobsResult = jobs;
            final ArrayList<DeleteJobResult> results = new ArrayList<DeleteJobResult>();
            results.add(DeleteJobResultImpl.createDeleteJobResultImpl(true, "success", "3", null));
            results.add(DeleteJobResultImpl.createDeleteJobResultImpl(false, "failed", "4", "error"));
            centralDispatcher1.purgeJobsResult = results;

            try {
                tool.run(
                    new String[]{"purge", "--" + JobsTool.IDLIST_OPTION_LONG, "3,4", "-p", "test"});
                fail("Should not succeed");
            } catch (JobsToolException e) {
                assert e.getMessage().equals("Failed to delete 1 jobs");
            }

            assertTrue("list action should be called", centralDispatcher1.listStoredJobsCalled);
            assertNull(centralDispatcher1.listStoredJobsOutput);
            assertNotNull(centralDispatcher1.listStoredJobsQuery);
            assertEquals("3,4", centralDispatcher1.listStoredJobsQuery.getIdlist());

            assertTrue("purge action should be called", centralDispatcher1.purgeStoredJobsCalled);
            assertNotNull(centralDispatcher1.purgeJobsRequest);
            assertEquals(Arrays.asList("3", "4"), centralDispatcher1.purgeJobsRequest);

            assertFalse("load action should not be called", centralDispatcher1.loadJobsCalled);
            assertNull(centralDispatcher1.loadRequest);
        }
    }
    public void testParseArgs() throws Exception {
        {
            //test invalid action
            final JobsTool tool = new JobsTool(getFrameworkInstance());
            try {
                final String[] args = {"invalid"};
                final CommandLine line = tool.parseArgs(args);
                tool.validateOptions(line, args);
                fail("invalid action should have failed");
            } catch (CLIToolOptionsException e) {
                assertNotNull(e);
            }

        }
        {
            //test valid actions missing require options
            final JobsTool tool = new JobsTool(getFrameworkInstance());
            try {
                final String[] args = {"load"};
                final CommandLine line = tool.parseArgs(args);
                tool.validateOptions(line, args);
                fail("should have thrown missing argument exception.");
            } catch (CLIToolOptionsException e) {
                assertNotNull(e);
            }
        }
        {
            //test valid actions, with invalid option
            final JobsTool tool = new JobsTool(getFrameworkInstance());
            try {
                final String[] args = {"load", "-d", "zamboni"};
                final CommandLine line = tool.parseArgs(args);
                tool.validateOptions(line, args);
                fail("should have thrown missing argument exception.");
            } catch (CLIToolOptionsException e) {
                assertNotNull(e);
                assertTrue("Wrong message: " + e.getMessage(), e.getMessage().startsWith(
                    "Illegal value for --duplicate"));
            }
        }
        {
            //test valid actions, missing required -f
            final JobsTool tool = new JobsTool(getFrameworkInstance());
            try {
                final String[] args = {"load", "-d", "update"};
                final CommandLine line = tool.parseArgs(args);
                tool.validateOptions(line, args);
                fail("should have thrown missing argument exception.");
            } catch (CLIToolOptionsException e) {
                assertNotNull(e);
                assertTrue("wrong message:" + e.getMessage(), e.getMessage().startsWith(
                    "load action: -f/--file option is required"));
            }
        }
        {
            //test valid actions,  -f points to DNE file
            final JobsTool tool = new JobsTool(getFrameworkInstance());
            try {
                final String[] args = {"load", "-f", "doesnotexist"};
                final CommandLine line = tool.parseArgs(args);
                tool.validateOptions(line, args);
                fail("should have thrown missing argument exception.");
            } catch (CLIToolOptionsException e) {
                assertNotNull(e);
                assertTrue("wrong message:" + e.getMessage(), e.getMessage().startsWith(
                    "load action: -f/--file option: File does not exist"));
            }
        }
        {
            //test missing project param
            final JobsTool tool = new JobsTool(getFrameworkInstance());
            tool.internalResolver=new SingleProjectResolver() {
                public boolean hasSingleProject() {
                    return false;
                }

                public String getSingleProjectName() {
                    return null;
                }
            };
            try {
                final String[] args = {"list", "-n", "test1"};
                final CommandLine line = tool.parseArgs(args);
                tool.validateOptions(line, args);
                fail("should have thrown missing argument exception.");
            } catch (CLIToolOptionsException e) {
                assertNotNull(e);
                assertTrue("wrong message:" + e.getMessage(), e.getMessage().startsWith(
                    "list action: -p/--project option is required"));
            }

        }
        {
            //test missing project param, defaulting to single project
            final JobsTool tool = new JobsTool(getFrameworkInstance());
            tool.internalResolver=new SingleProjectResolver() {
                public boolean hasSingleProject() {
                    return true;
                }

                public String getSingleProjectName() {
                    return "testProject";
                }
            };
            try {
                final String[] args = {"list", "-n", "test1"};
                final CommandLine line = tool.parseArgs(args);
                tool.validateOptions(line, args);
                assertEquals("testProject", tool.argProject);
            } catch (CLIToolOptionsException e) {
                fail("unexpected exception: " + e.getMessage());
            }

        }
        {
            //test valid actions
            final JobsTool tool = new JobsTool(getFrameworkInstance());
            boolean success = false;
            try {
                final String[] args = {"list", "-n", "test1", "-p", "test"};
                final CommandLine line = tool.parseArgs(args);
                tool.validateOptions(line, args);
                success = true;
            } catch (CLIToolOptionsException e) {
                fail("unexpected exception: " + e.getMessage());
            }
            assertTrue("parseArgs did not succeed", success);

        }
        {
            //test valid actions
            final JobsTool tool = new JobsTool(getFrameworkInstance());
            try {
                final String[] args = {"list","-p","test"};
                final CommandLine line = tool.parseArgs(args);
                tool.validateOptions(line, args);
            } catch (CLIToolOptionsException e) {
                fail("unexpected exception: " + e.getMessage());
            }

        }
        {
            //test invalid format
            final JobsTool tool = new JobsTool(getFrameworkInstance());
            try {
                final String[] args = {"-F","zamlx"};
                final CommandLine line = tool.parseArgs(args);
                tool.validateOptions(line, args);
                fail("invalid format should have failed");
            } catch (CLIToolOptionsException e) {
                assertNotNull(e);
            }
        }
        {
            //test valid format xml
            final JobsTool tool = new JobsTool(getFrameworkInstance());
            try {
                final String[] args = {"list","-f","test.out","-F","xml","-p","test"};
                final CommandLine line = tool.parseArgs(args);
                tool.validateOptions(line, args);
            } catch (CLIToolOptionsException e) {
                fail("unexpected exception: " + e.getMessage());
            }

        }
        {
            //test valid format yaml
            final JobsTool tool = new JobsTool(getFrameworkInstance());
            try {
                final String[] args = {"list","-f","test.out","-F","yaml", "-p", "test"};
                final CommandLine line = tool.parseArgs(args);
                tool.validateOptions(line, args);
            } catch (CLIToolOptionsException e) {
                fail("unexpected exception: " + e.getMessage());
            }

        }
        {
            //test valid purge command, requires a filter param (-i,-g,-n)
            final JobsTool tool = new JobsTool(getFrameworkInstance());
            try {
                final String[] args = {"purge","-f","test.out","-F","yaml", "-p", "test"};
                final CommandLine line = tool.parseArgs(args);
                tool.validateOptions(line, args);
                fail("Should fail");
            } catch (CLIToolOptionsException e) {
                assert e.getMessage().equals("purge action: Some filter option is required");
            }

        }
        {
            //test valid purge command, with a filter param -i
            final JobsTool tool = new JobsTool(getFrameworkInstance());
            try {
                final String[] args = {"purge","-f","test.out","-F","yaml", "-p", "test","-i","1"};
                final CommandLine line = tool.parseArgs(args);
                tool.validateOptions(line, args);
            } catch (CLIToolOptionsException e) {
                fail("unexpected exception: " + e.getMessage());
            }

        }
    }
}
