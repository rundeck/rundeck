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
* TestURLFileUpdater.java
* 
* User: Greg Schueler <a href="mailto:greg@dtosolutions.com">greg@dtosolutions.com</a>
* Created: 7/26/11 10:14 AM
* 
*/
package com.dtolabs.rundeck.core.common.impl;

import junit.framework.TestCase;
import okhttp3.mockwebserver.MockResponse;
import okhttp3.mockwebserver.MockWebServer;
import okhttp3.mockwebserver.RecordedRequest;

import java.io.*;
import java.nio.file.Files;

/**
 * TestURLFileUpdater is ...
 *
 * @author Greg Schueler <a href="mailto:greg@dtosolutions.com">greg@dtosolutions.com</a>
 */
public class TestURLFileUpdater extends TestCase {
    public static final String PROJ_NAME = "TestURLFileUpdater";
    public static final String YAML_NODES_TEST = "testnode1: \n"
                                                 + "  nodename: testnode1\n"
                                                 + "  hostname: blah\n"
                                                 + "  username: test1\n"
                                                 + "  description: monkey pie\n";
    public static final String XML_NODES_TEXT = "<project>\n"
                                                + "<node "
                                                + " name=\"testnode1\""
                                                + " hostname=\"testhost\""
                                                + " username=\"testuser\""
                                                + " description=\"a description\""
                                                + " />"
                                                + "</project>";

    public TestURLFileUpdater(String name) {
        super(name);
    }

    MockWebServer server;

    @Override
    protected void setUp() throws Exception {
        server = new MockWebServer();
        server.start();
    }

    @Override
    protected void tearDown() throws Exception {
        server.shutdown();
    }

    public void testUpdateBasic() throws Exception {
        URLFileUpdater updater = new URLFileUpdater(server.url("test").url(), null, -1, null, null, false,
            null, null);

        server.enqueue(new MockResponse().setResponseCode(200).addHeader("Content-Type", "text/yaml").setBody(YAML_NODES_TEST));
        File tempfile = File.createTempFile("test", ".yaml");
        tempfile.deleteOnExit();
        updater.updateFile(tempfile);

        ByteArrayOutputStream tempFileContent = new ByteArrayOutputStream();
        Files.copy(tempfile.toPath(),tempFileContent);
        assertEquals(YAML_NODES_TEST,new String(tempFileContent.toByteArray()));

    }


    public void testUpdateCaching() throws Exception {

        File tempfile = File.createTempFile("test", ".yaml");
        tempfile.deleteOnExit();
        File cachemeta = File.createTempFile("test", ".properties");
        cachemeta.deleteOnExit();
        URLFileUpdater updater = new URLFileUpdater(server.url("cache-test").url(), null, -1, cachemeta, tempfile,
            true, null, null);

        server.enqueue(new MockResponse()
                               .setResponseCode(200)
                               .addHeader("ETag", "monkey1")
                               .addHeader("Last-Modified", "blahblee")
                               .addHeader("Content-Type", "text/yaml")
                               .setBody(YAML_NODES_TEST));

        updater.updateFile(tempfile);
        assertTrue(tempfile.isFile());
        assertTrue(tempfile.length() > 0);
        server.takeRequest();

        //make another request. assert etag, If-modified-since are used.

        server.enqueue(new MockResponse()
                               .setResponseCode(304)
                               .addHeader("ETag", "monkey1")
                               .addHeader("Last-Modified", "blahblee"));
        updater.updateFile(tempfile);
        assertTrue(tempfile.isFile());
        assertTrue(tempfile.length()>0);

        ByteArrayOutputStream tempFileContent = new ByteArrayOutputStream();
        Files.copy(tempfile.toPath(),tempFileContent);
        assertEquals(YAML_NODES_TEST,new String(tempFileContent.toByteArray()));

        RecordedRequest rq = server.takeRequest();
        assertEquals("monkey1", rq.getHeader("If-None-Match"));
        assertEquals("blahblee", rq.getHeader("If-Modified-Since"));

    }

    /**
     * Test use of file: url
     */
    public void testUpdateFileUrl() throws Exception {


        final File file = new File("src/test/resources/com/dtolabs/rundeck/core/common/test-nodes1.xml");
        URLFileUpdater updater = new URLFileUpdater(file
            .toURI().toURL(), null, -1, null, null, false,
            null, null);

        File tempfile = File.createTempFile("test", ".yaml");
        tempfile.deleteOnExit();
        updater.updateFile(tempfile);
        assertTrue(tempfile.isFile());
        assertEquals(file.length(), tempfile.length());
    }
}
