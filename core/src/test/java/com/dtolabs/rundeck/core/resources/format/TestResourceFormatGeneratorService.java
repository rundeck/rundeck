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
* TestResourceFormatGeneratorService.java
* 
* User: Greg Schueler <a href="mailto:greg@dtosolutions.com">greg@dtosolutions.com</a>
* Created: 8/8/11 5:32 PM
* 
*/
package com.dtolabs.rundeck.core.resources.format;

import com.dtolabs.rundeck.core.common.Framework;
import com.dtolabs.rundeck.core.common.FrameworkProject;
import com.dtolabs.rundeck.core.common.INodeSet;
import com.dtolabs.rundeck.core.common.IRundeckProject;
import com.dtolabs.rundeck.core.resources.format.json.ResourceJsonFormatGenerator;
import com.dtolabs.rundeck.core.tools.AbstractBaseTest;
import com.dtolabs.rundeck.core.utils.FileUtils;

import java.io.File;
import java.io.IOException;
import java.io.OutputStream;
import java.util.*;

/**
 * TestResourceFormatGeneratorService is ...
 *
 * @author Greg Schueler <a href="mailto:greg@dtosolutions.com">greg@dtosolutions.com</a>
 */
public class TestResourceFormatGeneratorService extends AbstractBaseTest {
    private static final String PROJ_NAME = "TestResourceFormatGeneratorService";

    public TestResourceFormatGeneratorService(String name) {
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


    public void testBaseFormats() throws Exception {
        final ResourceFormatGeneratorService service = new ResourceFormatGeneratorService(
            getFrameworkInstance());
        assertEquals(3, service.listFormats().size());

        final ResourceFormatGenerator xmlgen = service.getGeneratorForFormat(
            ResourceXMLFormatGenerator.SERVICE_PROVIDER_TYPE);
        assertNotNull(xmlgen);

        final ResourceFormatGenerator gen2 = service.getGeneratorForFormat(
            ResourceYamlFormatGenerator.SERVICE_PROVIDER_TYPE);
        assertNotNull(gen2);

        final ResourceFormatGenerator gen3 = service.getGeneratorForFormat(
            ResourceJsonFormatGenerator.SERVICE_PROVIDER_TYPE);
        assertNotNull(gen3);
    }

    static class testGenerator implements ResourceFormatGenerator {
        HashSet<String> fileExtensions;
        List<String> mimeTypes;
        INodeSet generateNodeset;
        OutputStream generateStream;

        public Set<String> getFileExtensions() {
            return fileExtensions;
        }

        public List<String> getMIMETypes() {
            return mimeTypes;
        }

        public void generateDocument(INodeSet nodeset, OutputStream stream) throws ResourceFormatGeneratorException,
            IOException {
            generateNodeset = nodeset;
            generateStream = stream;

        }
    }

    public void testListFormats() throws Exception {

        final ResourceFormatGeneratorService service = new ResourceFormatGeneratorService(getFrameworkInstance());
        final List<String> strings = service.listFormats();
        assertEquals(3, strings.size());
        assertTrue(strings.contains(ResourceXMLFormatGenerator.SERVICE_PROVIDER_TYPE));
        assertTrue(strings.contains(ResourceYamlFormatGenerator.SERVICE_PROVIDER_TYPE));
        assertTrue(strings.contains(ResourceJsonFormatGenerator.SERVICE_PROVIDER_TYPE));

        //add new format generator
        testGenerator generator = new testGenerator();

        service.registerInstance("testformat1", generator);
        final List<String> strings2 = service.listFormats();
        assertEquals(4, strings2.size());
        assertTrue(strings2.contains(ResourceXMLFormatGenerator.SERVICE_PROVIDER_TYPE));
        assertTrue(strings2.contains(ResourceYamlFormatGenerator.SERVICE_PROVIDER_TYPE));
        assertTrue(strings2.contains(ResourceJsonFormatGenerator.SERVICE_PROVIDER_TYPE));
        assertTrue(strings2.contains("testformat1"));
    }

    public void testGetGeneratorForFileExtension() throws Exception {
        final ResourceFormatGeneratorService service = new ResourceFormatGeneratorService(getFrameworkInstance());
        testGenerator generator = new testGenerator();
        generator.fileExtensions = new HashSet<String>();

        service.registerInstance("testformat1", generator);

        try {
            service.getGeneratorForFileExtension("monkey");
            fail("should fail");
        } catch (UnsupportedFormatException e) {
            assertEquals("No provider available to parse file extension: monkey", e.getMessage());
        }
        try {
            service.getGeneratorForFileExtension(new File("test.monkey"));
            fail("should fail");
        } catch (UnsupportedFormatException e) {
            assertEquals("No provider available to parse file extension: monkey", e.getMessage());
        }

        generator.fileExtensions.add("monkey");

        final ResourceFormatGenerator monkeyGenerator = service.getGeneratorForFileExtension("monkey");
        assertNotNull(monkeyGenerator);
        assertEquals(generator, monkeyGenerator);
        final ResourceFormatGenerator monkeyGenerator2 = service.getGeneratorForFileExtension(new File("test.monkey"));
        assertNotNull(monkeyGenerator2);
        assertEquals(generator, monkeyGenerator2);
    }

    public void testGetGeneratorForMIMEType() throws Exception {
        final ResourceFormatGeneratorService service = new ResourceFormatGeneratorService(getFrameworkInstance());
        testGenerator generator = new testGenerator();
        generator.fileExtensions = new HashSet<String>();
        generator.mimeTypes = new ArrayList<String>();

        service.registerInstance("testformat1", generator);

        try {
            service.getGeneratorForMIMEType("text/monkey");
            fail("should fail");
        } catch (UnsupportedFormatException e) {
            assertEquals("No provider available to parse MIME type: text/monkey", e.getMessage());
        }

        generator.mimeTypes.add("text/monkey");

        final ResourceFormatGenerator monkeyGenerator = service.getGeneratorForMIMEType("text/monkey");
        assertNotNull(monkeyGenerator);
        assertEquals(generator, monkeyGenerator);
    }

    public void testGetGeneratorForMIMETypeGlob() throws Exception {
        final ResourceFormatGeneratorService service = new ResourceFormatGeneratorService(getFrameworkInstance());
        testGenerator generator = new testGenerator();
        generator.fileExtensions = new HashSet<String>();
        generator.mimeTypes = new ArrayList<String>();

        service.registerInstance("testformat1", generator);
        generator.mimeTypes.add("text/monkey");

        final ResourceFormatGenerator monkeyGenerator = service.getGeneratorForMIMEType("text/monkey");
        assertNotNull(monkeyGenerator);
        assertEquals(generator, monkeyGenerator);

        final ResourceFormatGenerator monkeyGenerator2 = service.getGeneratorForMIMEType("*/monkey");
        assertNotNull(monkeyGenerator2);
        assertEquals(generator, monkeyGenerator2);

        try {
            service.getGeneratorForMIMEType("application/x-monkey");
            fail("should fail");
        } catch (UnsupportedFormatException e) {
            assertEquals("No provider available to parse MIME type: application/x-monkey", e.getMessage());
        }
    }
}
