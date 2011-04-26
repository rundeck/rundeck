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
* TestNodeDispatcherService.java
* 
* User: Greg Schueler <a href="mailto:greg@dtosolutions.com">greg@dtosolutions.com</a>
* Created: 3/24/11 2:41 PM
* 
*/
package com.dtolabs.rundeck.core.execution.dispatch;

import com.dtolabs.rundeck.core.common.Framework;
import com.dtolabs.rundeck.core.common.FrameworkProject;
import com.dtolabs.rundeck.core.execution.ExecutionContext;
import com.dtolabs.rundeck.core.execution.ExecutionListener;
import com.dtolabs.rundeck.core.tools.AbstractBaseTest;
import com.dtolabs.rundeck.core.utils.FileUtils;
import com.dtolabs.rundeck.core.utils.NodeSet;
import junit.framework.TestCase;

import java.io.File;
import java.io.IOException;
import java.util.*;

/**
 * TestNodeDispatcherService is ...
 *
 * @author Greg Schueler <a href="mailto:greg@dtosolutions.com">greg@dtosolutions.com</a>
 */
public class TestNodeDispatcherService extends AbstractBaseTest {
    private static final String PROJ_NAME = "TestNodeDispatcherService";
    private File resourcesfile;

    public TestNodeDispatcherService(String name) {
        super(name);
    }

    public void setUp() {
        super.setUp();
        final Framework frameworkInstance = getFrameworkInstance();
        final FrameworkProject frameworkProject = frameworkInstance.getFrameworkProjectMgr().createFrameworkProject(
            PROJ_NAME);
        resourcesfile = new File(frameworkProject.getNodesResourceFilePath());
        //copy test nodes to resources file
        try {
            FileUtils.copyFileStreams(new File("src/test/com/dtolabs/rundeck/core/common/test-nodes1.xml"), resourcesfile);
        } catch (IOException e) {
            throw new RuntimeException("Caught Setup exception: " + e.getMessage(), e);
        }

    }

    public void tearDown() throws Exception {
        super.tearDown();
        File projectdir = new File(getFrameworkProjectsBase(), PROJ_NAME);
        FileUtils.deleteDir(projectdir);

    }

    public void testGetNodeDispatcher() throws Exception {
        final Framework frameworkInstance = getFrameworkInstance();
        final NodeDispatcherService service = NodeDispatcherService.getInstanceForFramework(
            frameworkInstance);
        {
            final NodeSet nodeSet = new NodeSet();
            nodeSet.createInclude().setName(".*");
            nodeSet.setThreadCount(2);
            //get node dispatcher for a context.  nodeset>1 and threadcount>1 returns parallel provider
            final ExecutionContext context = new ExecutionContext() {
                public String getFrameworkProject() {
                    return PROJ_NAME;
                }

                public Framework getFramework() {
                    return frameworkInstance;
                }

                public String getUser() {
                    return "blah";
                }

                public NodeSet getNodeSet() {

                    return nodeSet;
                }

                public String[] getArgs() {
                    return new String[0];
                }

                public int getLoglevel() {
                    return 0;
                }

                public Map<String, Map<String, String>> getDataContext() {
                    return null;
                }

                public ExecutionListener getExecutionListener() {
                    return null;
                }
            };
            final NodeDispatcher nodeDispatcher = service.getNodeDispatcher(context);
            assertNotNull(nodeDispatcher);
            assertTrue(nodeDispatcher instanceof ParallelNodeDispatcher);

        }

        {   //get node dispatcher for a context.  nodeset>1 and threadcount<2 returns sequential provider

            final NodeSet nodeSet = new NodeSet();
            nodeSet.createInclude().setName(".*");
            nodeSet.setThreadCount(1);
            final ExecutionContext context = new ExecutionContext() {
                public String getFrameworkProject() {
                    return PROJ_NAME;
                }

                public Framework getFramework() {
                    return frameworkInstance;
                }

                public String getUser() {
                    return "blah";
                }

                public NodeSet getNodeSet() {

                    return nodeSet;
                }

                public String[] getArgs() {
                    return new String[0];
                }

                public int getLoglevel() {
                    return 0;
                }

                public Map<String, Map<String, String>> getDataContext() {
                    return null;
                }

                public ExecutionListener getExecutionListener() {
                    return null;
                }
            };
            final NodeDispatcher nodeDispatcher = service.getNodeDispatcher(context);
            assertNotNull(nodeDispatcher);
            assertTrue(nodeDispatcher instanceof SequentialNodeDispatcher);

        }

        {   //get node dispatcher for a context.  nodeset<2 and threadcount<2 returns sequential provider

            final NodeSet nodeSet = new NodeSet();
            nodeSet.setSingleNodeName("test1");
            nodeSet.setThreadCount(1);
            final ExecutionContext context = new ExecutionContext() {
                public String getFrameworkProject() {
                    return PROJ_NAME;
                }

                public Framework getFramework() {
                    return frameworkInstance;
                }

                public String getUser() {
                    return "blah";
                }

                public NodeSet getNodeSet() {

                    return nodeSet;
                }

                public String[] getArgs() {
                    return new String[0];
                }

                public int getLoglevel() {
                    return 0;
                }

                public Map<String, Map<String, String>> getDataContext() {
                    return null;
                }

                public ExecutionListener getExecutionListener() {
                    return null;
                }
            };
            final NodeDispatcher nodeDispatcher = service.getNodeDispatcher(context);
            assertNotNull(nodeDispatcher);
            assertTrue(nodeDispatcher instanceof SequentialNodeDispatcher);

        }

        {   //get node dispatcher for a context.  nodeset<2 and threadcount>1 returns sequential provider

            final NodeSet nodeSet = new NodeSet();
            nodeSet.setSingleNodeName("test1");
            nodeSet.setThreadCount(2);
            final ExecutionContext context = new ExecutionContext() {
                public String getFrameworkProject() {
                    return PROJ_NAME;
                }

                public Framework getFramework() {
                    return frameworkInstance;
                }

                public String getUser() {
                    return "blah";
                }

                public NodeSet getNodeSet() {

                    return nodeSet;
                }

                public String[] getArgs() {
                    return new String[0];
                }

                public int getLoglevel() {
                    return 0;
                }

                public Map<String, Map<String, String>> getDataContext() {
                    return null;
                }

                public ExecutionListener getExecutionListener() {
                    return null;
                }
            };
            final NodeDispatcher nodeDispatcher = service.getNodeDispatcher(context);
            assertNotNull(nodeDispatcher);
            assertTrue(nodeDispatcher instanceof SequentialNodeDispatcher);

        }
    }
}
