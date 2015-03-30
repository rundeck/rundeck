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
* TestURLNodesProvider.java
* 
* User: Greg Schueler <a href="mailto:greg@dtosolutions.com">greg@dtosolutions.com</a>
* Created: 7/22/11 2:17 PM
* 
*/
package com.dtolabs.rundeck.core.resources;

import com.dtolabs.rundeck.core.common.Framework;
import com.dtolabs.rundeck.core.common.FrameworkProject;
import com.dtolabs.rundeck.core.common.INodeSet;
import com.dtolabs.rundeck.core.common.IRundeckProject;
import com.dtolabs.rundeck.core.common.impl.URLFileUpdater;
import com.dtolabs.rundeck.core.plugins.configuration.ConfigurationException;
import com.dtolabs.rundeck.core.tools.AbstractBaseTest;
import com.dtolabs.rundeck.core.utils.FileUtils;
import org.apache.commons.httpclient.Header;
import org.apache.commons.httpclient.HttpClient;
import org.apache.commons.httpclient.HttpMethod;

import java.io.*;
import java.util.HashMap;
import java.util.Properties;

/**
 * TestURLNodesProvider is ...
 *
 * @author Greg Schueler <a href="mailto:greg@dtosolutions.com">greg@dtosolutions.com</a>
 */
public class TestURLResourceModelSource extends AbstractBaseTest {
    public static final String PROJ_NAME = "TestURLNodesProvider";
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

    public TestURLResourceModelSource(String name) {
        super(name);
    }

    IRundeckProject frameworkProject;

    public void setUp() {
        super.setUp();
        final Framework frameworkInstance = getFrameworkInstance();

        frameworkProject = frameworkInstance.getFrameworkProjectMgr().createFrameworkProject(
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

    public void testConfigureProperties() throws Exception {
        final URLResourceModelSource provider = new URLResourceModelSource(getFrameworkInstance());
        try {
            provider.configure(null);
            fail("Should throw NPE");
        } catch (NullPointerException e) {
            e.printStackTrace();
        }


        Properties props = new Properties();
        try {
            provider.configure(props);
            fail("shouldn't succeed");
        } catch (ConfigurationException e) {
            assertEquals("project is required", e.getMessage());
        }

        props.setProperty("project", PROJ_NAME);
        try {
            provider.configure(props);
            fail("shouldn't succeed");
        } catch (ConfigurationException e) {
            assertEquals("url is required", e.getMessage());
        }
        props.setProperty("url", "blah");
        try {
            provider.configure(props);
            fail("shouldn't succeed");
        } catch (ConfigurationException e) {
            assertTrue(e.getMessage().startsWith("url is malformed"));
        }

        props.setProperty("url", "ftp://example.com/blah");
        try {
            provider.configure(props);
            fail("shouldn't succeed");
        } catch (ConfigurationException e) {
            assertTrue(e.getMessage().startsWith("url protocol not allowed: "));
        }

        props.setProperty("url", "http://example.com/test");
        provider.configure(props);
        assertNotNull(provider.configuration.nodesUrl);
        assertEquals("http://example.com/test", provider.configuration.nodesUrl.toExternalForm());

        assertEquals(PROJ_NAME, provider.configuration.project);

        props.setProperty("url", "https://example.com/test");
        provider.configure(props);
        assertNotNull(provider.configuration.nodesUrl);
        assertEquals("https://example.com/test", provider.configuration.nodesUrl.toExternalForm());

        props.setProperty("url", "file://some/file");
        provider.configure(props);
        assertNotNull(provider.configuration.nodesUrl);
        assertEquals("file://some/file", provider.configuration.nodesUrl.toExternalForm());

        props.setProperty("timeout","notanumber");
        try {
            provider.configure(props);
            fail("shouldn't succeed");
        } catch (ConfigurationException e) {
            assertTrue(e.getMessage().startsWith("timeout is invalid: "));
        }


        props.setProperty("timeout", "12345");
        provider.configure(props);
        assertEquals(12345, provider.configuration.timeout);

        assertEquals(true, provider.configuration.useCache);
        props.setProperty("cache", "false");
        provider.configure(props);
        assertEquals(false, provider.configuration.useCache);
    }

    static class test1 implements URLFileUpdater.httpClientInteraction{
        int httpResultCode=0;
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
            this.method=method;
        }

        public void setClient(HttpClient client) {
            this.client=client;
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
            releaseConnectionCalled=true;
        }

        public void setRequestHeader(String name, String value) {
            requestHeaders.put(name, value);
        }

        public Header getResponseHeader(String name) {
            return responseHeaders.get(name);
        }


        public void setFollowRedirects(boolean follow) {
            followRedirects=follow;
        }
    }
    public void testGetNodesYaml() throws Exception {
        URLResourceModelSource provider = new URLResourceModelSource(getFrameworkInstance());
        final URLResourceModelSource.Configuration build = URLResourceModelSource.Configuration.build();

        build.project(PROJ_NAME);
        build.url("http://example.com/test");

        provider.configure(build.getProperties());
        final test1 test1 = new test1();
        test1.httpResultCode=200;
        test1.httpStatusText="OK";
        test1.responseHeaders.put("Content-Type", new Header("Content-Type", "text/yaml"));
        String yamlcontent = YAML_NODES_TEST;
        ByteArrayInputStream stringStream = new ByteArrayInputStream(yamlcontent.getBytes());
        test1.bodyStream=stringStream;
        provider.interaction= test1;
        final INodeSet nodes = provider.getNodes();
        assertNotNull(nodes);
        assertEquals(1, nodes.getNodes().size());
        assertNotNull(nodes.getNode("testnode1"));

        assertNotNull(test1.method);
        assertNotNull(test1.client);
        assertNotNull(test1.followRedirects);
        assertNotNull(test1.releaseConnectionCalled);

    }
    public void testGetNodesXml() throws Exception {
        URLResourceModelSource provider = new URLResourceModelSource(getFrameworkInstance());
        final URLResourceModelSource.Configuration build = URLResourceModelSource.Configuration.build();

        build.project(PROJ_NAME);
        build.url("http://example.com/test");

        provider.configure(build.getProperties());
        final test1 test1 = new test1();
        test1.httpResultCode=200;
        test1.httpStatusText="OK";
        test1.responseHeaders.put("Content-Type", new Header("Content-Type", "text/xml"));
        ByteArrayInputStream stringStream = new ByteArrayInputStream(XML_NODES_TEXT.getBytes());
        test1.bodyStream=stringStream;
        provider.interaction= test1;
        final INodeSet nodes = provider.getNodes();
        assertNotNull(nodes);
        assertEquals(1, nodes.getNodes().size());
        assertNotNull(nodes.getNode("testnode1"));

        assertNotNull(test1.method);
        assertNotNull(test1.client);
        assertNotNull(test1.followRedirects);
        assertNotNull(test1.releaseConnectionCalled);
    }
    public void testGetNodesCaching() throws Exception {
        URLResourceModelSource provider = new URLResourceModelSource(getFrameworkInstance());
        final URLResourceModelSource.Configuration build = URLResourceModelSource.Configuration.build();

        build.project(PROJ_NAME);
        build.url("http://example.com/test");
        build.cache(true);

        provider.configure(build.getProperties());
        final test1 test1 = new test1();
        test1.httpResultCode=200;
        test1.httpStatusText="OK";
        test1.responseHeaders.put("Content-Type", new Header("Content-Type", "text/yaml"));
        //include etag, last-modified
        test1.responseHeaders.put("ETag", new Header("ETag", "monkey1"));
        test1.responseHeaders.put("Last-Modified", new Header("Last-Modified", "blahblee"));

        final ByteArrayInputStream stringStream = new ByteArrayInputStream(YAML_NODES_TEST.getBytes());
        test1.bodyStream=stringStream;
        provider.interaction= test1;

        final INodeSet nodes = provider.getNodes();
        assertNotNull(nodes);
        assertEquals(1, nodes.getNodes().size());
        assertNotNull(nodes.getNode("testnode1"));

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
        provider.interaction = test2;

        final INodeSet nodes2 = provider.getNodes();
        assertNotNull(nodes2);
        assertEquals(1, nodes2.getNodes().size());
        assertNotNull(nodes2.getNode("testnode1"));

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
    public void testGetNodesFile() throws Exception{
        URLResourceModelSource provider = new URLResourceModelSource(getFrameworkInstance());
        final URLResourceModelSource.Configuration build = URLResourceModelSource.Configuration.build();

        build.project(PROJ_NAME);
        build.url(new File("src/test/resources/com/dtolabs/rundeck/core/common/test-nodes1.xml").toURI().toURL().toExternalForm());

        provider.configure(build.getProperties());

        final INodeSet nodes = provider.getNodes();
        assertNotNull(nodes);
        assertEquals(2, nodes.getNodes().size());
        assertNotNull(nodes.getNode("test1"));
        assertNotNull(nodes.getNode("testnode2"));

        //write yaml to temp file
        File tempyaml = File.createTempFile("nodes", ".yaml");
        BufferedWriter writer = new BufferedWriter(new OutputStreamWriter(new FileOutputStream(tempyaml)));
        writer.write(YAML_NODES_TEST);
        writer.flush();
        writer.close();
        tempyaml.deleteOnExit();

        URLResourceModelSource provider2 = new URLResourceModelSource(getFrameworkInstance());
        final URLResourceModelSource.Configuration build2 = URLResourceModelSource.Configuration.build();

        build2.project(PROJ_NAME);
        build2.url(tempyaml.toURI().toURL().toExternalForm());

        provider2.configure(build2.getProperties());

        final INodeSet nodes2 = provider2.getNodes();
        assertNotNull(nodes2);
        assertEquals(1, nodes2.getNodes().size());
        assertNotNull(nodes2.getNode("testnode1"));

    }
}
