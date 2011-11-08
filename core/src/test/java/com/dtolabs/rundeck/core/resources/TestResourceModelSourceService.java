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
* TestNodesProviderService.java
* 
* User: Greg Schueler <a href="mailto:greg@dtosolutions.com">greg@dtosolutions.com</a>
* Created: 7/21/11 9:06 AM
* 
*/
package com.dtolabs.rundeck.core.resources;

import com.dtolabs.rundeck.core.common.Framework;
import com.dtolabs.rundeck.core.common.FrameworkProject;
import com.dtolabs.rundeck.core.common.INodeEntry;
import com.dtolabs.rundeck.core.common.INodeSet;
import com.dtolabs.rundeck.core.execution.service.ExecutionServiceException;
import com.dtolabs.rundeck.core.plugins.configuration.ConfigurationException;
import com.dtolabs.rundeck.core.tools.AbstractBaseTest;
import com.dtolabs.rundeck.core.utils.FileUtils;

import java.io.File;
import java.io.IOException;
import java.util.Collection;
import java.util.Properties;

/**
 * TestNodesProviderService is ...
 *
 * @author Greg Schueler <a href="mailto:greg@dtosolutions.com">greg@dtosolutions.com</a>
 */
public class TestResourceModelSourceService extends AbstractBaseTest {
    private static final String PROJ_NAME = "TestNodesProviderService";

    public TestResourceModelSourceService(String name) {
        super(name);
    }

    public void setUp() {
        super.setUp();
        final Framework frameworkInstance = getFrameworkInstance();
        final FrameworkProject frameworkProject = frameworkInstance.getFrameworkProjectMgr().createFrameworkProject(
            PROJ_NAME);
        File resourcesfile = new File(frameworkProject.getNodesResourceFilePath());
        //copy test nodes to resources file
        try {
            FileUtils.copyFileStreams(new File("src/test/resources/com/dtolabs/rundeck/core/common/test-nodes1.xml"),
                resourcesfile);
        } catch (IOException e) {
            throw new RuntimeException("Caught Setup exception: " + e.getMessage(), e);
        }

    }

    public void tearDown() throws Exception {
        super.tearDown();
        File projectdir = new File(getFrameworkProjectsBase(), PROJ_NAME);
        FileUtils.deleteDir(projectdir);
    }


    class sourceTest1 implements ResourceModelSource {
        INodeSet toReturn;

        public INodeSet getNodes() throws ResourceModelSourceException {
            return toReturn;
        }
    }

    class test1 implements ResourceModelSourceFactory {
        ResourceModelSource toReturn;
        Properties createNodesProviderConfiguration;
        boolean called;
        ConfigurationException toThrow;

        public ResourceModelSource createResourceModelSource(final Properties configuration) throws ConfigurationException {
            called=true;
            createNodesProviderConfiguration = configuration;
            if(null!=toThrow){
                throw toThrow;
            }
            return toReturn;
        }
    }

    class nodesettest implements INodeSet {

        public Collection<INodeEntry> getNodes() {
            return null;
        }

        public INodeEntry getNode(String name) {
            return null;
        }

        public Collection<String> getNodeNames() {
            return null;
        }
    }

    public void testGetProviderForConfiguration() throws Exception {
        final ResourceModelSourceService service = ResourceModelSourceService.getInstanceForFramework(
            getFrameworkInstance());
        {
            final test1 factory = new test1();
            final sourceTest1 provider = new sourceTest1();
            final nodesettest nodesettest = new nodesettest();
            provider.toReturn = nodesettest;
            factory.toReturn = provider;

            service.registerInstance("test", factory);

            //no properties
            final ResourceModelSource result = service.getSourceForConfiguration("test", null);
            assertNotNull(result);
            assertTrue(factory.called);
            assertNull(factory.createNodesProviderConfiguration);
            assertNotNull(result.getNodes());
            assertEquals(nodesettest, result.getNodes());
        }
        {
            final test1 factory = new test1();
            final sourceTest1 provider = new sourceTest1();
            final nodesettest nodesettest = new nodesettest();
            provider.toReturn = nodesettest;
            factory.toReturn = provider;

            service.registerInstance("test", factory);
            final Properties properties = new Properties();

            //use properties
            final ResourceModelSource result = service.getSourceForConfiguration("test", properties);
            assertNotNull(result);
            assertTrue(factory.called);
            assertNotNull(factory.createNodesProviderConfiguration);
            assertEquals(properties, factory.createNodesProviderConfiguration);
            assertNotNull(result.getNodes());
            assertEquals(nodesettest, result.getNodes());
        }

        {
            final test1 factory = new test1();
            final sourceTest1 provider = new sourceTest1();
            final nodesettest nodesettest = new nodesettest();
            provider.toReturn = nodesettest;
            factory.toReturn = provider;
            factory.toThrow = new ConfigurationException("test1");

            service.registerInstance("test", factory);
            final Properties properties = new Properties();

            //throw configuration exception
            final ResourceModelSource result;
            try {
                result = service.getSourceForConfiguration("test", properties);
                fail("Should have thrown exception");
            } catch (ExecutionServiceException e) {
                assertNotNull(e);
                assertEquals("test1", e.getCause().getMessage());
            }
            assertTrue(factory.called);
            assertNotNull(factory.createNodesProviderConfiguration);
            assertEquals(properties, factory.createNodesProviderConfiguration);
        }

    }
}
