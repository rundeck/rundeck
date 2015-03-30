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
* TestNodeExecutorService.java
* 
* User: Greg Schueler <a href="mailto:greg@dtosolutions.com">greg@dtosolutions.com</a>
* Created: 3/24/11 2:55 PM
* 
*/
package com.dtolabs.rundeck.core.execution.service;

import com.dtolabs.rundeck.core.common.Framework;
import com.dtolabs.rundeck.core.common.FrameworkProject;
import com.dtolabs.rundeck.core.common.IRundeckProject;
import com.dtolabs.rundeck.core.common.NodeEntryImpl;
import com.dtolabs.rundeck.core.execution.impl.jsch.JschNodeExecutor;
import com.dtolabs.rundeck.core.execution.impl.local.LocalNodeExecutor;
import com.dtolabs.rundeck.core.tools.AbstractBaseTest;
import com.dtolabs.rundeck.core.utils.FileUtils;

import java.io.File;
import java.io.IOException;
import java.util.*;

/**
 * TestNodeExecutorService is ...
 *
 * @author Greg Schueler <a href="mailto:greg@dtosolutions.com">greg@dtosolutions.com</a>
 */
public class TestNodeExecutorService extends AbstractBaseTest {
    private static final String PROJ_NAME = "TestNodeExecutorService";

    public TestNodeExecutorService(String name) {
        super(name);
    }

    public void setUp() {
        super.setUp();
        final Framework frameworkInstance = getFrameworkInstance();
        final IRundeckProject frameworkProject = frameworkInstance.getFrameworkProjectMgr().createFrameworkProject(
            PROJ_NAME);
        generateProjectResourcesFile(
                new File("src/test/resources/com/dtolabs/rundeck/core/common/test-nodes1.xml"),
                frameworkProject
        );

    }

    public void tearDown() throws Exception {
        super.tearDown();
        File projectdir = new File(getFrameworkProjectsBase(), PROJ_NAME);
        FileUtils.deleteDir(projectdir);


    }

    public void testGetProviderForNode() throws Exception {
        final NodeExecutorService service = NodeExecutorService.getInstanceForFramework(
            getFrameworkInstance());
        {
            //default for local node should be local provider
            final NodeEntryImpl test1 = new NodeEntryImpl("test1");
            final NodeExecutor provider = service.getProviderForNodeAndProject(test1, PROJ_NAME);
            assertNotNull(provider);
            assertTrue(provider instanceof LocalNodeExecutor);
        }
        {
            //default for non-node should be jsch-ssh provider
            final NodeEntryImpl test1 = new NodeEntryImpl("testnode2");
            final NodeExecutor provider = service.getProviderForNodeAndProject(test1, PROJ_NAME);
            assertNotNull(provider);
            assertTrue(provider instanceof JschNodeExecutor);
        }

        //test specifying attribute to explicitly retrive particular provider
        {
            //use local node, specify jsch provider using local-exec-service attribute
            final NodeEntryImpl test1 = new NodeEntryImpl("test1");
            //set attribute
            test1.setAttributes(new HashMap<String, String>());
            test1.getAttributes().put(NodeExecutorService.LOCAL_NODE_SERVICE_SPECIFIER_ATTRIBUTE, "jsch-ssh");
            final NodeExecutor provider = service.getProviderForNodeAndProject(test1, PROJ_NAME);
            assertNotNull(provider);
            assertTrue(provider instanceof JschNodeExecutor);
        }
        {
            //use non-local node, specify local provider using "exec-service" attribute
            final NodeEntryImpl test1 = new NodeEntryImpl("testnode2");
            //set attribute
            test1.setAttributes(new HashMap<String, String>());
            test1.getAttributes().put(NodeExecutorService.NODE_SERVICE_SPECIFIER_ATTRIBUTE, "local");
            final NodeExecutor provider = service.getProviderForNodeAndProject(test1, PROJ_NAME);
            assertNotNull(provider);
            assertTrue(provider instanceof LocalNodeExecutor);
        }

    }
}
