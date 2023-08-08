/*
 * Copyright 2016 SimplifyOps, Inc. (http://simplifyops.com)
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

/*
* TestFileCopierService.java
* 
* User: Greg Schueler <a href="mailto:greg@dtosolutions.com">greg@dtosolutions.com</a>
* Created: 3/24/11 3:05 PM
* 
*/
package com.dtolabs.rundeck.core.execution.service;

import com.dtolabs.rundeck.core.common.Framework;
import com.dtolabs.rundeck.core.common.FrameworkProject;
import com.dtolabs.rundeck.core.common.IRundeckProject;
import com.dtolabs.rundeck.core.common.NodeEntryImpl;
import com.dtolabs.rundeck.core.execution.ExecutionContextImpl;
import com.dtolabs.rundeck.core.execution.impl.jsch.JschScpFileCopier;
import com.dtolabs.rundeck.core.execution.impl.local.LocalFileCopier;
import com.dtolabs.rundeck.core.execution.utils.DummyFileCopier;
import com.dtolabs.rundeck.core.execution.workflow.StepExecutionContext;
import com.dtolabs.rundeck.core.tools.AbstractBaseTest;
import com.dtolabs.rundeck.core.utils.FileUtils;
import org.mockito.invocation.InvocationOnMock;
import org.mockito.stubbing.Answer;

import java.io.File;
import java.io.IOException;
import java.util.*;

import static org.mockito.Mockito.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

/**
 * TestFileCopierService is ...
 *
 * @author Greg Schueler <a href="mailto:greg@dtosolutions.com">greg@dtosolutions.com</a>
 */
public class TestFileCopierService extends AbstractBaseTest {
    private static final String PROJ_NAME = "TestFileCopierService";

    public TestFileCopierService(String name) {
        super(name);
    }

    public void setUp() {
        super.setUp();
        final Framework frameworkInstance = getFrameworkInstance();
        final IRundeckProject frameworkProject = frameworkInstance.getFilesystemFrameworkProjectManager().createFrameworkProject(
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
        //final FileCopierService service = FileCopierService.getInstanceForFramework(getFrameworkInstance(),getFrameworkInstance());

        FileCopierService service = mock(FileCopierService.class);
        when(service.getProviderForNodeAndProject(any(NodeEntryImpl.class), any(StepExecutionContext.class)))
                .thenAnswer(new Answer<FileCopier>() {
                    public FileCopier answer(final InvocationOnMock invocation) throws Throwable {
                        final NodeEntryImpl arg = (NodeEntryImpl) invocation.getArguments()[0];
                        boolean isNodeNameValid = arg.getNodename() != null &&
                                !arg.getNodename().equals(getFrameworkInstance().getFrameworkNodeHostname());
                        
                        String localNodeAttribute = arg.getAttributes() != null ?
                                arg.getAttributes().get(FileCopierService.LOCAL_NODE_SERVICE_SPECIFIER_ATTRIBUTE) :
                                null;
                        boolean isLocalNodeAttributeValid = !"local".equals(localNodeAttribute);

                        boolean isNonLocalProvide = isNodeNameValid && isLocalNodeAttributeValid;
                        if (isNonLocalProvide) {
                            return new DummyFileCopier();
                        } else {
                            return new LocalFileCopier(getFrameworkInstance());
                        }
                    }
                });

        final StepExecutionContext context = ExecutionContextImpl.builder()
                .frameworkProject(PROJ_NAME)
                .framework(getFrameworkInstance())
                .user("blah")
                .threadCount(1)
                .build();

        {
            //default for local node should be local provider
            final NodeEntryImpl test1 = new NodeEntryImpl("test1");
            final FileCopier provider = service.getProviderForNodeAndProject(test1, context);
            assertNotNull(provider);
            assertTrue(provider instanceof LocalFileCopier);
        }
        {
            //default for non-local node should be jsch-scp provider
            final NodeEntryImpl test1 = new NodeEntryImpl("testnode2");
            final FileCopier provider = service.getProviderForNodeAndProject(test1, context);
            assertNotNull(provider);
            assertTrue(provider instanceof DummyFileCopier);
        }

        //specify override attributes for node to change file copier provider

        {
            //default for local node should be local provider
            final NodeEntryImpl test1 = new NodeEntryImpl("test1");
            //set attribute
            test1.setAttributes(new HashMap<String, String>());
            test1.getAttributes().put(FileCopierService.LOCAL_NODE_SERVICE_SPECIFIER_ATTRIBUTE, "sshj-scp");

            final FileCopier provider = service.getProviderForNodeAndProject(test1, context);
            assertNotNull(provider);
            assertTrue(provider instanceof DummyFileCopier);
        }
        {
            //default for non-local node should be jsch-scp provider
            final NodeEntryImpl test1 = new NodeEntryImpl("testnode2");
            test1.setAttributes(new HashMap<String, String>());
            test1.getAttributes().put(FileCopierService.REMOTE_NODE_SERVICE_SPECIFIER_ATTRIBUTE, "local");

            final FileCopier provider = service.getProviderForNodeAndProject(test1, context);

            assertNotNull(provider);
            assertTrue(provider instanceof LocalFileCopier);
        }
    }
}
