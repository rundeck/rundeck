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
* TestResourceFormatParserService.java
* 
* User: Greg Schueler <a href="mailto:greg@dtosolutions.com">greg@dtosolutions.com</a>
* Created: 8/4/11 10:52 AM
* 
*/
package com.dtolabs.rundeck.core.resources.format;

import com.dtolabs.rundeck.core.common.Framework;
import com.dtolabs.rundeck.core.common.FrameworkProject;
import com.dtolabs.rundeck.core.common.INodeSet;
import com.dtolabs.rundeck.core.common.IRundeckProject;
import com.dtolabs.rundeck.core.resources.format.json.ResourceJsonFormatGenerator;
import com.dtolabs.rundeck.core.resources.format.json.ResourceJsonFormatParser;
import com.dtolabs.rundeck.core.tools.AbstractBaseTest;
import com.dtolabs.rundeck.core.utils.FileUtils;
import junit.framework.TestCase;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.util.*;

/**
 * TestResourceFormatParserService is ...
 *
 * @author Greg Schueler <a href="mailto:greg@dtosolutions.com">greg@dtosolutions.com</a>
 */
public class TestResourceFormatParserService extends AbstractBaseTest {
    private static final String PROJ_NAME = "TestResourceFormatParserService";

    public TestResourceFormatParserService(String name) {
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

        final ResourceFormatParserService service = new ResourceFormatParserService(getFrameworkInstance());
        assertEquals(3, service.listFormats().size());

        final ResourceFormatParser parserForFormat = service.getParserForFormat(
            ResourceXMLFormatParser.SERVICE_PROVIDER_TYPE);
        assertNotNull(parserForFormat);
        assertTrue(parserForFormat instanceof ResourceXMLFormatParser);

        final ResourceFormatParser parser2 = service.getParserForFormat(
            ResourceYamlFormatParser.SERVICE_PROVIDER_TYPE);
        assertNotNull(parser2);
        assertTrue(parser2 instanceof ResourceYamlFormatParser);

        final ResourceFormatParser parser3 = service.getParserForFormat(
            ResourceJsonFormatParser.SERVICE_PROVIDER_TYPE);
        assertNotNull(parser3);
        assertTrue(parser3 instanceof ResourceJsonFormatParser);
    }

    static class testParser implements ResourceFormatParser {
        Set<String> fileExtensions;
        Set<String> mimeTypes;
        INodeSet returnSet;
        File parseFile;
        InputStream parseInput;

        public Set<String> getFileExtensions() {
            return fileExtensions;
        }

        public Set<String> getMIMETypes() {
            return mimeTypes;
        }

        public INodeSet parseDocument(File file) throws ResourceFormatParserException {
            parseFile = file;
            return returnSet;
        }

        public INodeSet parseDocument(InputStream input) throws ResourceFormatParserException {
            parseInput = input;
            return returnSet;
        }
    }

    public void testListFormats() throws Exception {

        final ResourceFormatParserService service = new ResourceFormatParserService(getFrameworkInstance());
        final List<String> strings = service.listFormats();
        assertEquals(3, strings.size());
        assertTrue(strings.contains(ResourceXMLFormatParser.SERVICE_PROVIDER_TYPE));
        assertTrue(strings.contains(ResourceYamlFormatParser.SERVICE_PROVIDER_TYPE));
        assertTrue(strings.contains(ResourceJsonFormatParser.SERVICE_PROVIDER_TYPE));

        //add new format parser
        testParser parser = new testParser();

        service.registerInstance("testformat1", parser);
        final List<String> strings2 = service.listFormats();
        assertEquals(4, strings2.size());
        assertTrue(strings2.contains(ResourceXMLFormatParser.SERVICE_PROVIDER_TYPE));
        assertTrue(strings2.contains(ResourceYamlFormatParser.SERVICE_PROVIDER_TYPE));
        assertTrue(strings2.contains(ResourceJsonFormatParser.SERVICE_PROVIDER_TYPE));
        assertTrue(strings2.contains("testformat1"));
    }

    public void testGetParserForFileExtension() throws Exception{
        final ResourceFormatParserService service = new ResourceFormatParserService(getFrameworkInstance());
        testParser parser = new testParser();
        parser.fileExtensions = new HashSet<String>();

        service.registerInstance("testformat1", parser);

        try {
            service.getParserForFileExtension("monkey");
            fail("should fail");
        } catch (UnsupportedFormatException e) {
            assertEquals("No provider available to parse file extension: monkey", e.getMessage());
        }
        try {
            service.getParserForFileExtension(new File("test.monkey"));
            fail("should fail");
        } catch (UnsupportedFormatException e) {
            assertEquals("No provider available to parse file extension: monkey", e.getMessage());
        }

        parser.fileExtensions.add("monkey");

        final ResourceFormatParser monkeyParser = service.getParserForFileExtension("monkey");
        assertNotNull(monkeyParser);
        assertEquals(parser, monkeyParser);
        final ResourceFormatParser monkeyParser2 = service.getParserForFileExtension(new File("test.monkey"));
        assertNotNull(monkeyParser2);
        assertEquals(parser, monkeyParser2);
    }
    public void testGetParserForMIMEType() throws Exception{
        final ResourceFormatParserService service = new ResourceFormatParserService(getFrameworkInstance());
        testParser parser = new testParser();
        parser.fileExtensions = new HashSet<String>();
        parser.mimeTypes = new HashSet<String>();

        service.registerInstance("testformat1", parser);

        try {
            service.getParserForMIMEType("text/monkey");
            fail("should fail");
        } catch (UnsupportedFormatException e) {
            assertEquals("No provider available to parse MIME type: text/monkey", e.getMessage());
        }

        parser.mimeTypes.add("text/monkey");

        final ResourceFormatParser monkeyParser = service.getParserForMIMEType("text/monkey");
        assertNotNull(monkeyParser);
        assertEquals(parser, monkeyParser);
        try {
            final ResourceFormatParser monkeyParser2= service.getParserForMIMEType(null);
            fail("should throw exception");
        } catch (IllegalArgumentException e) {
            assertEquals("Invalid MIME type: null", e.getMessage());
        }
    }
    public void testGetParserForMIMETypeGlob() throws Exception{
        final ResourceFormatParserService service = new ResourceFormatParserService(getFrameworkInstance());
        testParser parser = new testParser();
        parser.fileExtensions = new HashSet<String>();
        parser.mimeTypes = new HashSet<String>();

        service.registerInstance("testformat1", parser);
        parser.mimeTypes.add("*/monkey");

        final ResourceFormatParser monkeyParser = service.getParserForMIMEType("text/monkey");
        assertNotNull(monkeyParser);
        assertEquals(parser, monkeyParser);
        
        final ResourceFormatParser monkeyParser2 = service.getParserForMIMEType("application/monkey");
        assertNotNull(monkeyParser2);
        assertEquals(parser, monkeyParser2);

        try {
            service.getParserForMIMEType("application/x-monkey");
            fail("should fail");
        } catch (UnsupportedFormatException e) {
            assertEquals("No provider available to parse MIME type: application/x-monkey", e.getMessage());
        }
    }

    public void testValidMimeType() throws Exception {
        assertTrue(ResourceFormatParserService.validMimeType("a/b"));
        assertTrue(ResourceFormatParserService.validMimeType("*/b"));
        assertTrue(ResourceFormatParserService.validMimeType("*/*"));
        assertFalse(ResourceFormatParserService.validMimeType(null));
        assertFalse(ResourceFormatParserService.validMimeType("/b"));
        assertFalse(ResourceFormatParserService.validMimeType("a/"));
        assertFalse(ResourceFormatParserService.validMimeType("a/b/c"));
    }
}
