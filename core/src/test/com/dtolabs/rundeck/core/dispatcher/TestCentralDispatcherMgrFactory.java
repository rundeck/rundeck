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

package com.dtolabs.rundeck.core.dispatcher;
/*
* TestCentralDispatcherMgrFactory.java
* 
* User: Greg Schueler <a href="mailto:greg@dtosolutions.com">greg@dtosolutions.com</a>
* Created: Feb 18, 2010 11:39:25 AM
* $Id$
*/

import com.dtolabs.rundeck.core.common.Framework;
import com.dtolabs.rundeck.core.tools.AbstractBaseTest;
import junit.framework.Test;
import junit.framework.TestSuite;

import java.io.OutputStream;
import java.util.Collection;
import java.util.Date;

public class TestCentralDispatcherMgrFactory extends AbstractBaseTest {
    CentralDispatcherMgrFactory centralDispatcherMgrFactory;
    public static final String TEST_CLASSNAME = "com.dtolabs.rundeck.core.dispatcher.TestCentralDispatcherMgrFactory$test1";
    public static final String TEST_FAIL1_CLASSNAME = "com.dtolabs.rundeck.core.dispatcher.TestCentralDispatcherMgrFactory$test_fail1";
    private static final String DEFAULT_DISPATCHER_CLASS = "com.dtolabs.rundeck.core.dispatcher.NoCentralDispatcher";

    public TestCentralDispatcherMgrFactory(String name) {
        super(name);
    }

    public static Test suite() {
        return new TestSuite(TestCentralDispatcherMgrFactory.class);
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


    public void testCreateDefault() throws Exception {
        try {
            CentralDispatcherMgrFactory test1 = CentralDispatcherMgrFactory.create(null, getFrameworkInstance());
            fail("Should have thrown an Exception");
        } catch (java.lang.IllegalArgumentException ex) {
            assertNotNull(ex);
        }
        try {
            CentralDispatcherMgrFactory test1=CentralDispatcherMgrFactory.create("", getFrameworkInstance());
            fail("Should have thrown an Exception");
        } catch (CentralDispatcherException ex) {
            assertNotNull(ex);
        }
        CentralDispatcherMgrFactory test1 = CentralDispatcherMgrFactory.create(getFrameworkInstance().getProperty(
            "framework.centraldispatcher.classname"), getFrameworkInstance());
        assertNotNull("expected default instance of CentralDispatcher", test1);

    }

    public void testCreate() throws Exception {
        try {
            CentralDispatcherMgrFactory test1 = CentralDispatcherMgrFactory.create(TEST_FAIL1_CLASSNAME, getFrameworkInstance());
            fail("Should have thrown an Exception");
        } catch (CentralDispatcherException ex) {
            assertNotNull(ex);
        }
        CentralDispatcherMgrFactory test1 = CentralDispatcherMgrFactory.create(TEST_CLASSNAME, getFrameworkInstance());
        assertNotNull("expected default instance of CentralDispatcher", test1);
    }

    public void testDefaultImplementation() throws Exception {
        CentralDispatcherMgrFactory test1 = CentralDispatcherMgrFactory.create(
            DEFAULT_DISPATCHER_CLASS, getFrameworkInstance());
        assertNotNull("expected default instance of CentralDispatcher", test1);

        CentralDispatcher cd = test1.getCentralDispatcher();

        try {
            cd.killDispatcherExecution(null);
            fail("should not succeed");
        } catch (CentralDispatcherException ex) {
            assertNotNull(ex);
        }
        try {
            cd.listDispatcherQueue();
            fail("should not succeed");
        } catch (CentralDispatcherException ex) {
            assertNotNull(ex);
        }
    }

    /**
     * Fail due to incorrect constructor
     */
    public static class test_fail1 implements CentralDispatcher {

        public QueuedItemResult queueDispatcherScript(IDispatchedScript dispatch) throws CentralDispatcherException {
            return null;
        }

        public QueuedItemResult queueDispatcherJob(IDispatchedJob job) throws CentralDispatcherException {
            return null;
        }

        public Collection<QueuedItem> listDispatcherQueue() throws CentralDispatcherException {
            return null;
        }

        public DispatcherResult killDispatcherExecution(String id) throws CentralDispatcherException {
            return null;
        }

        public Collection<IStoredJob> listStoredJobs(IStoredJobsQuery query, OutputStream output,
                                                     JobDefinitionFileFormat format) throws
            CentralDispatcherException {
            return null;
        }

        public Collection<IStoredJobLoadResult> loadJobs(ILoadJobsRequest request, java.io.File input,
                                                         JobDefinitionFileFormat format) throws
            CentralDispatcherException {
            return null;
        }

        public void reportExecutionStatus(String project, String title, String status, int totalNodeCount,
                                          int successNodeCount, String tags, String script, String summary, Date start,
                                          Date end) throws CentralDispatcherException {
        }
    }

    /**
     * Correct instantiation
     */
    public static class test1 implements CentralDispatcher{
        public test1(Framework framework) {
        }

        public QueuedItemResult queueDispatcherJob(IDispatchedJob job) throws CentralDispatcherException {
            return null;
        }

        public QueuedItemResult queueDispatcherScript(IDispatchedScript dispatch) throws CentralDispatcherException {
            return null;
        }

        public Collection<QueuedItem> listDispatcherQueue() throws CentralDispatcherException {
            return null;
        }

        public DispatcherResult killDispatcherExecution(String id) throws CentralDispatcherException {
            return null;
        }

        public Collection<IStoredJobLoadResult> loadJobs(ILoadJobsRequest request, java.io.File input,
                                                         JobDefinitionFileFormat format) throws
            CentralDispatcherException {
            return null;
        }

        public Collection<IStoredJob> listStoredJobs(IStoredJobsQuery query, OutputStream output,
                                                     JobDefinitionFileFormat format) throws
            CentralDispatcherException {
            return null;
        }

        public void reportExecutionStatus(String project, String title, String status, int totalNodeCount,
                                          int successNodeCount, String tags, String script, String summary, Date start,
                                          Date end) throws CentralDispatcherException {
        }
    }
}