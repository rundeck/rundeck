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

package com.dtolabs.rundeck.core.cli.run;
/*
* TestRunTool.java
* 
* User: Greg Schueler <a href="mailto:greg@dtosolutions.com">greg@dtosolutions.com</a>
* Created: Mar 31, 2010 4:25:06 PM
* $Id$
*/

import com.dtolabs.client.services.QueuedItemResultImpl;
import com.dtolabs.rundeck.core.cli.CLIToolOptionsException;
import com.dtolabs.rundeck.core.cli.FailDispatcher;
import com.dtolabs.rundeck.core.common.Framework;
import com.dtolabs.rundeck.core.dispatcher.*;
import com.dtolabs.rundeck.core.tools.AbstractBaseTest;
import junit.framework.Test;
import junit.framework.TestSuite;
import org.apache.commons.cli.CommandLine;

import java.io.OutputStream;
import java.util.Arrays;
import java.util.Collection;
import java.util.List;


public class TestRunTool extends AbstractBaseTest {
    RunTool runTool;

    public TestRunTool(String name) {
        super(name);
    }

    public static Test suite() {
        return new TestSuite(TestRunTool.class);
    }

    protected void setUp()  {
        super.setUp();
    }

    protected void tearDown() throws Exception {
        super.tearDown();
    }

    public static void main(String args[]) {
        junit.textui.TestRunner.run(suite());
    }


    public void testParseArgsInvalidAction() throws Exception {
        //test invalid action
        final RunTool tool = createRunTool();
        try {
            final String[] args = {"invalid"};
            final CommandLine line = tool.parseArgs(args);
            tool.validateOptions(line, args);
            fail("invalid action should have failed");
        } catch (CLIToolOptionsException e) {
            assertNotNull(e);
            assertTrue(e.getMessage().startsWith("Invalid action"));
        }

    }

    public void testParseArgsMissingOptions() throws Exception {
        //test valid actions missing required options
        final RunTool tool = createRunTool();
        try {
            final String[] args = {"run"};
            final CommandLine line = tool.parseArgs(args);
            tool.validateOptions(line, args);
            fail("should have thrown options exception.");
        } catch (CLIToolOptionsException e) {
            assertNotNull(e);
            assertTrue(e.getMessage().endsWith("is required"));
        }
    }

    public void testParseArgsConflictingOptions() throws Exception {
        //run action with conflicting options
        final RunTool tool = createRunTool();
        try {
            final String[] args = {"run", "-i", "1", "-j", "job"};
            final CommandLine line = tool.parseArgs(args);
            tool.validateOptions(line, args);
            fail("should have thrown options exception.");
        } catch (CLIToolOptionsException e) {
            assertNotNull(e);
            assertTrue(
                    "wrong message: " + e.getMessage(), e.getMessage().endsWith(
                            "cannot be combined, please specify only one."
                    )
            );
        }
    }

    public void testParseArgsId() throws Exception {
        //run action with any string ID
        final RunTool tool = createRunTool();
        try {
            final String[] args = {"run", "-i", "35e0eadf-b7a7-40a4-977d-da182e8d67c3"};
            final CommandLine line = tool.parseArgs(args);
            tool.validateOptions(line, args);

        } catch (CLIToolOptionsException e) {
            fail("should not have thrown options exception: " + e);
        }
    }

    public void testParseArgsValid() throws Exception {
        //valid options
        final RunTool tool = createRunTool();
        try {
            final String[] args = {"run", "-i", "1"};
            final CommandLine line = tool.parseArgs(args);
            tool.validateOptions(line, args);

        } catch (CLIToolOptionsException e) {
            fail("unexpected exception: " + e.getMessage());
        }
    }

    public void testParseArgsExtended() throws Exception {
        //valid options
        final RunTool tool = createRunTool();
        try {
            final String[] args = {"run", "-i", "1", "--", "some", "other", "args"};
            final CommandLine line = tool.parseArgs(args);
            tool.validateOptions(line, args);
        } catch (CLIToolOptionsException e) {
            fail("unexpected exception: " + e.getMessage());
        }
    }

    public void testParseArgsMissingProject() throws Exception {
        //-j option but no -p option
        final RunTool tool = createRunTool();
        tool.setCentralDispatcher(listProjectsDispatcher());
        try {
            final String[] args = {"run", "-j", "testjob"};
            final CommandLine line = tool.parseArgs(args);
            tool.validateOptions(line, args);
            fail("should have thrown options exception.");
        } catch (CLIToolOptionsException e) {
            assertNotNull(e);
            assertTrue(
                    "wrong message: " + e.getMessage(), e.getMessage().endsWith(
                            "-j/--job option requires -p/--project option"
                    )
            );
        }
    }

    public void testParseArgsMissingProjectMultiple() throws Exception {
        //-j option but no -p option
        final RunTool tool = createRunTool();
        tool.setCentralDispatcher(listProjectsDispatcher("abc", "def"));
        try {
            final String[] args = {"run", "-j", "testjob"};
            final CommandLine line = tool.parseArgs(args);
            tool.validateOptions(line, args);
            fail("should have thrown options exception.");
        } catch (CLIToolOptionsException e) {
            assertNotNull(e);
            assertTrue(
                    "wrong message: " + e.getMessage(), e.getMessage().endsWith(
                            "-j/--job option requires -p/--project option"
                    )
            );
        }
    }

    public void testParseArgsDefaultedProject() throws Exception {
        //-j option but no -p option, defaulted project name
        final RunTool tool = createRunTool();
        tool.setCentralDispatcher(listProjectsDispatcher("testProject"));
        try {
            final String[] args = {"run", "-j", "testjob"};
            final CommandLine line = tool.parseArgs(args);
            tool.validateOptions(line, args);
            assertEquals("testProject", tool.runOptions.argProject);
        } catch (CLIToolOptionsException e) {
            fail("unexpected exception: " + e.getMessage());
        }
    }

    private RunTool createRunTool() {
        return new RunTool(getFrameworkInstance().getPropertyLookup());
    }


    static class failCentralDispatcher extends FailDispatcher implements CentralDispatcher {
        boolean listStoredJobsCalled = false;
        boolean loadJobsCalled = false;
        ILoadJobsRequest loadRequest;
        java.io.File loadInput;
        IStoredJobsQuery listStoredJobsQuery;
        OutputStream listStoredJobsOutput;
        Collection<IStoredJob> listJobsResult;
        Collection<IStoredJobLoadResult> loadJobsResult;
        JobDefinitionFileFormat loadFormat;
        JobDefinitionFileFormat listFormat;

    }
    CentralDispatcher listProjectsDispatcher(String...names){
        testCentralDispatcher2 testCentralDispatcher2 = new testCentralDispatcher2();
        testCentralDispatcher2.projectNames = Arrays.asList(names);
        return testCentralDispatcher2;
    }

    static class testCentralDispatcher2 extends failCentralDispatcher{
        List<String> projectNames;

        @Override
        public List<String> listProjectNames() throws CentralDispatcherException {
            return projectNames;
        }
    }
    static class testCentralDispatcher1 extends failCentralDispatcher{
        IDispatchedJob queuedJob;
        QueuedItemResult queueDispatcherJobResult;
        @Override
        public QueuedItemResult queueDispatcherJob(IDispatchedJob job) throws CentralDispatcherException {
            queuedJob=job;
            return queueDispatcherJobResult;
        }
    }
    public void testJobOptionGroups() throws Exception {

        {
            //-j option but no -p option, defaulted project name
            final Framework framework = getFrameworkInstance();
            final testCentralDispatcher1 centralDispatcher1 = new testCentralDispatcher1();

            centralDispatcher1.queueDispatcherJobResult = QueuedItemResultImpl.successful("test", "123", "blah",
                "blah");

            final RunTool tool = new RunTool(framework.getPropertyLookup());
            tool.setCentralDispatcher(centralDispatcher1);
            //no group in -j
            tool.run(new String[]{"run", "-j", "testjob", "-p", "testProject"});
            assertNotNull(centralDispatcher1.queuedJob);
            assertEquals(null, centralDispatcher1.queuedJob.getJobRef().getJobId());
            assertEquals(null, centralDispatcher1.queuedJob.getJobRef().getGroup());
            assertEquals("testProject", centralDispatcher1.queuedJob.getJobRef().getProject());
            assertEquals("testjob", centralDispatcher1.queuedJob.getJobRef().getName());

            //empty / in -j
            tool.run(new String[]{"run", "-j", "/testjob", "-p", "testProject"});
            assertNotNull(centralDispatcher1.queuedJob);
            assertEquals(null, centralDispatcher1.queuedJob.getJobRef().getJobId());
            assertEquals(null, centralDispatcher1.queuedJob.getJobRef().getGroup());
            assertEquals("testProject", centralDispatcher1.queuedJob.getJobRef().getProject());
            assertEquals("testjob", centralDispatcher1.queuedJob.getJobRef().getName());

            //group used / in -j
            tool.run(new String[]{"run", "-j", "test/testjob", "-p", "testProject"});
            assertNotNull(centralDispatcher1.queuedJob);
            assertEquals(null, centralDispatcher1.queuedJob.getJobRef().getJobId());
            assertEquals("test", centralDispatcher1.queuedJob.getJobRef().getGroup());
            assertEquals("testProject", centralDispatcher1.queuedJob.getJobRef().getProject());
            assertEquals("testjob", centralDispatcher1.queuedJob.getJobRef().getName());

            //group multilevel used / in -j
            tool.run(new String[]{"run", "-j", "test/blah/monkey/testjob", "-p", "testProject"});
            assertNotNull(centralDispatcher1.queuedJob);
            assertEquals(null, centralDispatcher1.queuedJob.getJobRef().getJobId());
            assertEquals("test/blah/monkey", centralDispatcher1.queuedJob.getJobRef().getGroup());
            assertEquals("testProject", centralDispatcher1.queuedJob.getJobRef().getProject());
            assertEquals("testjob", centralDispatcher1.queuedJob.getJobRef().getName());

            //group multilevel used / in -j, leading slash
            tool.run(new String[]{"run", "-j", "/test/blah/monkey/testjob", "-p", "testProject"});
            assertNotNull(centralDispatcher1.queuedJob);
            assertEquals(null, centralDispatcher1.queuedJob.getJobRef().getJobId());
            assertEquals("test/blah/monkey", centralDispatcher1.queuedJob.getJobRef().getGroup());
            assertEquals("testProject", centralDispatcher1.queuedJob.getJobRef().getProject());
            assertEquals("testjob", centralDispatcher1.queuedJob.getJobRef().getName());


        }
    }
    public void testJobOptionId() throws Exception {

        {
            //-j option but no -p option, defaulted project name
            final Framework framework = getFrameworkInstance();
            final testCentralDispatcher1 centralDispatcher1 = new testCentralDispatcher1();
            centralDispatcher1.queueDispatcherJobResult = QueuedItemResultImpl.successful("test", "123", "blah",
                "blah");

            final RunTool tool = new RunTool(framework.getPropertyLookup());
            tool.setCentralDispatcher(centralDispatcher1);

            //no group in -j
            tool.run(new String[]{"run", "-i", "testjob"});
            assertNotNull(centralDispatcher1.queuedJob);
            assertEquals("testjob", centralDispatcher1.queuedJob.getJobRef().getJobId());
            assertEquals(null, centralDispatcher1.queuedJob.getJobRef().getGroup());
            assertEquals(null, centralDispatcher1.queuedJob.getJobRef().getProject());
            assertEquals(null, centralDispatcher1.queuedJob.getJobRef().getName());
            assertEquals(null, centralDispatcher1.queuedJob.getArgs());

        }
    }
    public void testArgs() throws Exception {

        {
            //-j option but no -p option, defaulted project name
            final Framework framework = getFrameworkInstance();
            final testCentralDispatcher1 centralDispatcher1 = new testCentralDispatcher1();
            centralDispatcher1.queueDispatcherJobResult = QueuedItemResultImpl.successful("test", "123", "blah",
                "blah");

            final RunTool tool = new RunTool(framework.getPropertyLookup());
            tool.setCentralDispatcher(centralDispatcher1);

            //no group in -j
            tool.run(new String[]{"run", "-i", "testjob","--","-test1","arg","-test2","arg2"});
            assertNotNull(centralDispatcher1.queuedJob);
            assertEquals("testjob", centralDispatcher1.queuedJob.getJobRef().getJobId());
            assertEquals(null, centralDispatcher1.queuedJob.getJobRef().getGroup());
            assertEquals(null, centralDispatcher1.queuedJob.getJobRef().getProject());
            assertEquals(null, centralDispatcher1.queuedJob.getJobRef().getName());
            assertNotNull(centralDispatcher1.queuedJob.getArgs());
            assertEquals(Arrays.asList("-test1", "arg", "-test2", "arg2"), Arrays.asList(
                centralDispatcher1.queuedJob.getArgs()));

        }
    }
}