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
import com.dtolabs.rundeck.core.common.IRundeckProject;
import com.dtolabs.rundeck.core.common.NodeFilter;
import com.dtolabs.rundeck.core.execution.ExecutionContext;
import com.dtolabs.rundeck.core.execution.ExecutionContextImpl;
import com.dtolabs.rundeck.core.execution.ExecutionListener;
import com.dtolabs.rundeck.core.resources.FileResourceModelSource;
import com.dtolabs.rundeck.core.tools.AbstractBaseTest;
import com.dtolabs.rundeck.core.utils.FileUtils;
import com.dtolabs.rundeck.core.utils.NodeSet;

import java.io.File;
import java.io.IOException;
import java.util.Map;

/**
 * TestNodeDispatcherService is ...
 *
 * @author Greg Schueler <a href="mailto:greg@dtosolutions.com">greg@dtosolutions.com</a>
 */
public class TestNodeDispatcherService extends AbstractBaseTest {
    private static final String PROJ_NAME = "TestNodeDispatcherService";
    private File resourcesfile;
    private File extResourcesfile;

    public TestNodeDispatcherService(String name) {
        super(name);
    }

    public void setUp() {
        super.setUp();
        final Framework frameworkInstance = getFrameworkInstance();
        final IRundeckProject frameworkProject = frameworkInstance.getFrameworkProjectMgr().createFrameworkProject(
                PROJ_NAME,
                generateProjectResourcesFile(
                        new File("src/test/resources/com/dtolabs/rundeck/core/common/test-nodes1.xml")
                )
        );
        extResourcesfile = new File("src/test/resources/com/dtolabs/rundeck/core/common/test-nodes2.xml");

    }

    public void tearDown() throws Exception {
        super.tearDown();
//        File projectdir = new File(getFrameworkProjectsBase(), PROJ_NAME);
//        FileUtils.deleteDir(projectdir);

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
            final ExecutionContext context = ExecutionContextImpl.builder()
                .frameworkProject(PROJ_NAME)
                .framework(frameworkInstance)
                .user("blah")
                .nodeSelector(nodeSet)
                .nodes(
                        NodeFilter.filterNodes(
                                nodeSet,
                                frameworkInstance.getFrameworkProjectMgr().getFrameworkProject(PROJ_NAME).getNodeSet()
                        ))
                .threadCount(nodeSet.getThreadCount())
                .keepgoing(nodeSet.isKeepgoing())
                .build();


            final NodeDispatcher nodeDispatcher = service.getNodeDispatcher(context);
            assertNotNull(nodeDispatcher);
            assertTrue(nodeDispatcher instanceof ParallelNodeDispatcher);

        }

        {   //get node dispatcher for a context.  nodeset>1 and threadcount<2 returns sequential provider

            final NodeSet nodeSet = new NodeSet();
            nodeSet.createInclude().setName(".*");
            nodeSet.setThreadCount(1);
            final ExecutionContext context = ExecutionContextImpl.builder()
                .frameworkProject(PROJ_NAME)
                .framework(frameworkInstance)
                .user("blah")
                .nodeSelector(nodeSet)
                .threadCount(nodeSet.getThreadCount())
                .keepgoing(nodeSet.isKeepgoing())
                .build();


            final NodeDispatcher nodeDispatcher = service.getNodeDispatcher(context);
            assertNotNull(nodeDispatcher);
            assertTrue(nodeDispatcher instanceof SequentialNodeDispatcher);

        }

        {   //get node dispatcher for a context.  nodeset<2 and threadcount<2 returns sequential provider

            final NodeSet nodeSet = new NodeSet();
            nodeSet.setSingleNodeName("test1");
            nodeSet.setThreadCount(1);
            final ExecutionContext context = ExecutionContextImpl.builder()
                .frameworkProject(PROJ_NAME)
                .framework(frameworkInstance)
                .user("blah")
                .nodeSelector(nodeSet)
                .threadCount(nodeSet.getThreadCount())
                .keepgoing(nodeSet.isKeepgoing())
                .build();


            final NodeDispatcher nodeDispatcher = service.getNodeDispatcher(context);
            assertNotNull(nodeDispatcher);
            assertTrue(nodeDispatcher instanceof SequentialNodeDispatcher);

        }

        {   //get node dispatcher for a context.  nodeset<2 and threadcount>1 returns sequential provider

            final NodeSet nodeSet = new NodeSet();
            nodeSet.setSingleNodeName("test1");
            nodeSet.setThreadCount(2);
            final ExecutionContext context = ExecutionContextImpl.builder()
                .frameworkProject(PROJ_NAME)
                .framework(frameworkInstance)
                .user("blah")
                .nodeSelector(nodeSet)
                .nodes(NodeFilter.filterNodes(
                               nodeSet,
                               frameworkInstance.getFrameworkProjectMgr().getFrameworkProject(PROJ_NAME).getNodeSet()
                       ))
                .threadCount(nodeSet.getThreadCount())
                .keepgoing(nodeSet.isKeepgoing())
                .build();


            final NodeDispatcher nodeDispatcher = service.getNodeDispatcher(context);
            assertNotNull(nodeDispatcher);
            assertTrue(nodeDispatcher instanceof SequentialNodeDispatcher);

        }
    }

    /**
     * Test specifying an external nodes file
     */
    public void testExtResources() throws Exception {
        final Framework frameworkInstance = getFrameworkInstance();
        final NodeDispatcherService service = NodeDispatcherService.getInstanceForFramework(
            frameworkInstance);
        {   //use test-1 file
            final NodeSet nodeSet = new NodeSet();
            nodeSet.createInclude().setTags("priority1"); //matches single nodes in test1 file
            nodeSet.setThreadCount(2);
            //get node dispatcher for a context.  nodeset<2 and threadcount>1 returns sequential provider
            final ExecutionContext context = ExecutionContextImpl.builder()
                .frameworkProject(PROJ_NAME)
                .framework(frameworkInstance)
                .user("blah")
                .nodeSelector(nodeSet)
                .threadCount(nodeSet.getThreadCount())
                .keepgoing(nodeSet.isKeepgoing())
                .nodesFile(resourcesfile)
                .nodes(NodeFilter.filterNodes(
                               nodeSet,
                               frameworkInstance.getFrameworkProjectMgr().getFrameworkProject(PROJ_NAME).getNodeSet()
                       ))
                .build();


            final NodeDispatcher nodeDispatcher = service.getNodeDispatcher(context);
            assertNotNull(nodeDispatcher);
            assertTrue(nodeDispatcher instanceof SequentialNodeDispatcher);
        }
        {
            final NodeSet nodeSet = new NodeSet();
            nodeSet.createInclude().setTags("priority1"); //matches two nodes in external file
            nodeSet.setThreadCount(2);
            //get node dispatcher for a context.  nodeset>1 and threadcount>1 returns parallel provider
            final ExecutionContext context = ExecutionContextImpl.builder()
                .frameworkProject(PROJ_NAME)
                .framework(frameworkInstance)
                .user("blah")
                .nodeSelector(nodeSet)
                .threadCount(nodeSet.getThreadCount())
                .keepgoing(nodeSet.isKeepgoing())
                .nodesFile(extResourcesfile)
                .nodes(NodeFilter.filterNodes(nodeSet,FileResourceModelSource.parseFile(extResourcesfile, frameworkInstance, PROJ_NAME)))
                .build();
            assertEquals(2,context.getNodes().getNodeNames().size());

            final NodeDispatcher nodeDispatcher = service.getNodeDispatcher(context);
            assertNotNull(nodeDispatcher);
            assertTrue(nodeDispatcher instanceof ParallelNodeDispatcher);
        }
    }
}
