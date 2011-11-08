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
* TestURLFileUpdater.java
* 
* User: Greg Schueler <a href="mailto:greg@dtosolutions.com">greg@dtosolutions.com</a>
* Created: 7/26/11 10:14 AM
* 
*/
package com.dtolabs.rundeck.core.common.impl;

import junit.framework.TestCase;
import org.apache.commons.httpclient.Header;
import org.apache.commons.httpclient.HttpClient;
import org.apache.commons.httpclient.HttpMethod;

import java.io.ByteArrayInputStream;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.net.URL;
import java.util.HashMap;

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


    static class test1 implements URLFileUpdater.httpClientInteraction {
        int httpResultCode = 0;
        private String httpStatusText;
        InputStream bodyStream;
        HttpMethod method;
        HttpClient client;
        IOException toThrowExecute;
        IOException toThrowResponseBody;
        boolean releaseConnectionCalled;
        Boolean followRedirects;
        HashMap<String, String> requestHeaders = new HashMap<String, String>();
        HashMap<String, Header> responseHeaders = new HashMap<String, Header>();

        public void setMethod(HttpMethod method) {
            this.method = method;
        }

        public void setClient(HttpClient client) {
            this.client = client;
        }

        public int executeMethod() throws IOException {
            return httpResultCode;
        }

        public String getStatusText() {
            return httpStatusText;
        }

        public InputStream getResponseBodyAsStream() throws IOException {
            return bodyStream;
        }

        public void releaseConnection() {
            releaseConnectionCalled = true;
        }

        public void setRequestHeader(String name, String value) {
            requestHeaders.put(name, value);
        }

        public Header getResponseHeader(String name) {
            return responseHeaders.get(name);
        }


        public void setFollowRedirects(boolean follow) {
            followRedirects = follow;
        }
    }

    public void testUpdateBasic() throws Exception {
        URLFileUpdater updater = new URLFileUpdater(new URL("http://example.com/test"), null, -1, null, null, false,
            null, null);

        final test1 test1 = new test1();
        test1.httpResultCode = 200;
        test1.httpStatusText = "OK";
        test1.responseHeaders.put("Content-Type", new Header("Content-Type", "text/yaml"));
        String yamlcontent = YAML_NODES_TEST;
        ByteArrayInputStream stringStream = new ByteArrayInputStream(yamlcontent.getBytes());
        test1.bodyStream = stringStream;
        updater.setInteraction(test1);
        File tempfile = File.createTempFile("test", ".yaml");
        tempfile.deleteOnExit();
        updater.updateFile(tempfile);

        assertNotNull(test1.method);
        assertNotNull(test1.client);
        assertNotNull(test1.followRedirects);
        assertNotNull(test1.releaseConnectionCalled);

    }


    public void testUpdateCaching() throws Exception {

        File tempfile = File.createTempFile("test", ".yaml");
        tempfile.deleteOnExit();
        File cachemeta = File.createTempFile("test", ".properties");
        cachemeta.deleteOnExit();
        URLFileUpdater updater = new URLFileUpdater(new URL("http://example.com/test"), null, -1, cachemeta, tempfile,
            true, null, null);


        final test1 test1 = new test1();
        test1.httpResultCode = 200;
        test1.httpStatusText = "OK";
        test1.responseHeaders.put("Content-Type", new Header("Content-Type", "text/yaml"));
        //include etag, last-modified
        test1.responseHeaders.put("ETag", new Header("ETag", "monkey1"));
        test1.responseHeaders.put("Last-Modified", new Header("Last-Modified", "blahblee"));

        final ByteArrayInputStream stringStream = new ByteArrayInputStream(YAML_NODES_TEST.getBytes());
        test1.bodyStream = stringStream;
        updater.setInteraction(test1);

        updater.updateFile(tempfile);
        assertTrue(tempfile.isFile());
        assertTrue(tempfile.length() > 0);

        assertNotNull(test1.method);
        assertNotNull(test1.client);
        assertNotNull(test1.followRedirects);
        assertNotNull(test1.releaseConnectionCalled);

        //make another request. assert etag, If-modified-since are used.


        final test1 test2 = new test1();
        test2.httpResultCode = 304;
        test2.httpStatusText = "Not Modified";
        //include etag, last-modified
        test2.responseHeaders.put("ETag", new Header("ETag", "monkey1"));
        test2.responseHeaders.put("Last-Modified", new Header("Last-Modified", "blahblee"));

        test2.bodyStream = null;
        updater.setInteraction(test2);

        updater.updateFile(tempfile);
        assertTrue(tempfile.isFile());
        assertTrue(tempfile.length()>0);


        assertNotNull(test2.method);
        assertNotNull(test2.client);
        assertNotNull(test2.followRedirects);
        assertNotNull(test2.releaseConnectionCalled);
        assertEquals("monkey1", test2.requestHeaders.get("If-None-Match"));
        assertEquals("blahblee", test2.requestHeaders.get("If-Modified-Since"));


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
