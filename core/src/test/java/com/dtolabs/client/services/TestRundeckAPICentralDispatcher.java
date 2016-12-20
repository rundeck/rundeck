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

package com.dtolabs.client.services;
/*
* TestRundeckAPICentralDispatcher.java
* 
* User: Greg Schueler <a href="mailto:greg@dtosolutions.com">greg@dtosolutions.com</a>
* Created: Mar 31, 2010 11:18:40 AM
* $Id$
*/

import com.dtolabs.client.utils.WebserviceResponse;
import com.dtolabs.rundeck.core.CoreException;
import com.dtolabs.rundeck.core.common.INodeSet;
import com.dtolabs.rundeck.core.dispatcher.CentralDispatcherException;
import com.dtolabs.rundeck.core.dispatcher.IDispatchedScript;
import com.dtolabs.rundeck.core.dispatcher.QueuedItemResult;
import com.dtolabs.rundeck.core.utils.NodeSet;
import junit.framework.Test;
import junit.framework.TestCase;
import junit.framework.TestSuite;
import org.dom4j.Document;
import org.dom4j.DocumentFactory;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.net.MalformedURLException;
import java.util.HashMap;
import java.util.Map;
import java.util.Properties;


public class TestRundeckAPICentralDispatcher extends TestCase {
    RundeckAPICentralDispatcher rundeckCentralDispatcher;


    public TestRundeckAPICentralDispatcher(String name) {
        super(name);
    }

    public static Test suite() {
        return new TestSuite(TestRundeckAPICentralDispatcher.class);
    }

    protected void setUp() throws Exception {
    }

    protected void tearDown() throws Exception {
    }

    public static void main(String args[]) {
        junit.textui.TestRunner.run(suite());
    }


    public void testAddNodeSetParamsEmpty() throws Exception {
            //test null nodeset
            HashMap<String, String> params = new HashMap<String, String>();
            RundeckAPICentralDispatcher.addAPINodeSetParams(params, null, null, -1, null);
            assertEquals(0, params.size());
        }

    public void testAddNodeSetParamsThreadcount() throws Exception {
            //test empty nodeset, only has threadcount & keepgoing
            HashMap<String, String> params = new HashMap<String, String>();
            NodeSet nodeset = new NodeSet();
            RundeckAPICentralDispatcher.addAPINodeSetParams(params, null, null, 1, null);
            assertEquals(1, params.size());
            assertTrue(params.containsKey("nodeThreadcount"));
            assertEquals("1", params.get("nodeThreadcount"));

        }
    public void testAddNodeSetParamsKeepgoing() throws Exception {
            //test empty nodeset, only has threadcount & keepgoing
            HashMap<String, String> params = new HashMap<String, String>();
            NodeSet nodeset = new NodeSet();
            RundeckAPICentralDispatcher.addAPINodeSetParams(params, false, null, -1, null);
            assertEquals(1, params.size());
            assertTrue(params.containsKey("nodeKeepgoing"));
            assertEquals("false", params.get("nodeKeepgoing"));
        }

    public void testAddNodeSetParamsBasic2() throws Exception {
            //test empty nodeset, only has threadcount & keepgoing
            HashMap<String, String> params = new HashMap<String, String>();
            NodeSet nodeset = new NodeSet();
            nodeset.setThreadCount(2);
            RundeckAPICentralDispatcher.addAPINodeSetParams(params, true, null, 2, null);
            assertEquals(2, params.size());
            assertTrue(params.containsKey("nodeThreadcount"));
            assertEquals("2", params.get("nodeThreadcount"));
            assertTrue(params.containsKey("nodeKeepgoing"));
            assertEquals("true", params.get("nodeKeepgoing"));

        }

    public void testAddNodeSetParamsFilter() throws Exception {
            //test basic hostname
            HashMap<String, String> params = new HashMap<String, String>();
            RundeckAPICentralDispatcher.addAPINodeSetParams(params, false, "hostname: testhostname1", 1, null);
            assertEquals("incorrect size: " + params, 3 /* keepgoing==false */, params.size());
            assertTrue(params.containsKey("nodeThreadcount"));
            assertEquals("1", params.get("nodeThreadcount"));
            assertTrue(params.containsKey("nodeKeepgoing"));
            assertEquals("false", params.get("nodeKeepgoing"));
            assertTrue(params.containsKey("filter"));
            assertEquals("hostname: testhostname1", params.get("filter"));
        }


    /**
     * blank filter string should not be included
     * @throws Exception
     */
    public void testAddNodeSetParamsBlankFilter() throws Exception {
            //test basic hostname
            HashMap<String, String> params = new HashMap<String, String>();
            RundeckAPICentralDispatcher.addAPINodeSetParams(params, false, "", 1, null);
            assertEquals("incorrect size: " + params, 2 /* keepgoing==false */, params.size());
            assertTrue(params.containsKey("nodeThreadcount"));
            assertEquals("1", params.get("nodeThreadcount"));
            assertTrue(params.containsKey("nodeKeepgoing"));
            assertEquals("false", params.get("nodeKeepgoing"));
            assertFalse(params.containsKey("filter"));
        }


    /**
     * precedence value should only be included if filter is included
     * @throws Exception
     */
    public void testAddNodeSetParamsNoPrecedenceWithoutFilter() throws Exception {
        HashMap<String, String> params = new HashMap<String, String>();
        RundeckAPICentralDispatcher.addAPINodeSetParams(params, null, null, -1, true);
        assertEquals("incorrect size: " + params, 0 /* keepgoing==false */, params.size());
    }


    public void testAddNodeSetParamsFiltersPrecedence() throws Exception {
            //test precedence filters
            HashMap<String, String> params = new HashMap<String, String>();
            //test other include filters
            RundeckAPICentralDispatcher.addAPINodeSetParams(params, null, "blah", -1, true);
            assertEquals("incorrect size: " + params, 2, params.size());
            assertEquals("blah", params.get("filter"));
            assertEquals("true", params.get("exclude-precedence"));
        }

    public void testAddNodeSetParamsFiltersPrecedence2() throws Exception {
        //test precedence filters
        HashMap<String, String> params = new HashMap<String, String>();

        //test other include filters
        RundeckAPICentralDispatcher.addAPINodeSetParams(params, null, "blah", -1, false);
        assertEquals("incorrect size: " + params, 2, params.size());
        assertEquals("blah", params.get("filter"));
        assertEquals("false", params.get("exclude-precedence"));
    }
    static class TestServerService extends ServerService{
        public TestServerService(final String url, final String username, final String password) {
            super(url, username, password);
        }

        public TestServerService(final WebConnectionParameters connParams) {
            super(connParams);
        }

        @Override
        public WebserviceResponse makeRundeckRequest(
                final String urlPath,
                final Map queryParams,
                final File uploadFile,
                final String method,
                final String uploadFileParam
        ) throws CoreException, MalformedURLException
        {
            fail("Unexpected request");
            return super.makeRundeckRequest(urlPath, queryParams, uploadFile, method, uploadFileParam);
        }

        @Override
        public WebserviceResponse makeRundeckRequest(
                final String urlPath, final Map queryParams, final Map<String, ? extends Object> formData
        ) throws CoreException, MalformedURLException
        {
            fail("Unexpected request");
            return super.makeRundeckRequest(urlPath, queryParams, formData);
        }

        @Override
        public WebserviceResponse makeRundeckRequest(
                final String urlPath,
                final Map queryParams,
                final File uploadFile,
                final String method,
                final String expectedContentType,
                final String uploadFileParam
        ) throws CoreException, MalformedURLException
        {
            fail("Unexpected request");
            return super.makeRundeckRequest(
                    urlPath,
                    queryParams,
                    uploadFile,
                    method,
                    expectedContentType,
                    uploadFileParam
            );
        }

        @Override
        public WebserviceResponse makeRundeckRequest(
                final String urlPath,
                final Map queryParams,
                final File uploadFile,
                final String method,
                final String expectedContentType,
                final Map<String, ? extends Object> formData,
                final String uploadFileParam
        ) throws CoreException, MalformedURLException
        {
            fail("Unexpected request");
            return super.makeRundeckRequest(
                    urlPath,
                    queryParams,
                    uploadFile,
                    method,
                    expectedContentType,
                    formData,
                    uploadFileParam
            );
        }
    }
    static class TestResponse implements WebserviceResponse{
        private boolean errorResponse;
        private boolean hasResultDoc;
        private boolean validResponse;
        private Document resultDoc;
        private String responseMessage;
        private InputStream resultStream;
        private String resultContentType;
        private byte[] responseBody;
        private int resultCode;

        @Override
        public boolean isErrorResponse() {
            return errorResponse;
        }

        public boolean hasResultDoc() {
            return hasResultDoc;
        }

        @Override
        public boolean isValidResponse() {
            return validResponse;
        }

        @Override
        public Document getResultDoc() {
            return resultDoc;
        }

        @Override
        public String getResponseMessage() {
            return responseMessage;
        }

        @Override
        public InputStream getResultStream() {
            return resultStream;
        }

        @Override
        public String getResultContentType() {
            return resultContentType;
        }

        @Override
        public byte[] getResponseBody() {
            return responseBody;
        }

        @Override
        public int getResultCode() {
            return resultCode;
        }
    }

    public void testCreateProjectEmptyResponse() throws Exception {
        final String testUrl = "http://localhost:4440/test";
        final RundeckAPICentralDispatcher rundeckAPICentralDispatcher = new RundeckAPICentralDispatcher(
                testUrl,
                "test",
                "test"
        );
        final TestResponse testResponse = new TestResponse();
        testResponse.resultCode=201;
        TestServerService testService = new TestServerService(
                testUrl,
                "test",
                "test"
        )
        {

            @Override
            public WebserviceResponse makeRundeckRequest(
                    final String urlPath,
                    final Map queryParams,
                    final File uploadFile,
                    final String method,
                    final String expectedContentType,
                    final String uploadFileParam
            ) throws CoreException, MalformedURLException
            {
                assertEquals("/api/11/projects", urlPath);
                assertEquals(0, queryParams.size());
                assertNotNull(uploadFile);
                assertTrue(uploadFile.exists());
                assertEquals("POST", method);
                assertEquals("text/xml", expectedContentType);
                assertEquals("text/xml", uploadFileParam);


                return testResponse;
            }
        };
        rundeckAPICentralDispatcher.setServerService(testService);
        try {
            rundeckAPICentralDispatcher.createProject("test1", new Properties());
            fail("expected failure");
        } catch (CentralDispatcherException e) {
            assertTrue(e.getMessage().contains("unexpectedly empty"));
        }
    }
    public void testCreateProjectWrongResponseCode() throws Exception {
        final String testUrl = "http://localhost:4440/test";
        final RundeckAPICentralDispatcher rundeckAPICentralDispatcher = new RundeckAPICentralDispatcher(
                testUrl,
                "test",
                "test"
        );
        final TestResponse testResponse = new TestResponse();
        testResponse.resultDoc= DocumentFactory.getInstance().createDocument();
        testResponse.resultDoc.addElement("result").addElement("project").addElement("name").addText("test1");
        testResponse.resultCode=200;
        TestServerService testService = new TestServerService(
                testUrl,
                "test",
                "test"
        )
        {
            @Override
            public WebserviceResponse makeRundeckRequest(
                    final String urlPath,
                    final Map queryParams,
                    final File uploadFile,
                    final String method,
                    final String expectedContentType,
                    final String uploadFileParam
            ) throws CoreException, MalformedURLException
            {
                assertEquals("/api/11/projects", urlPath);
                assertEquals(0, queryParams.size());
                assertNotNull(uploadFile);
                assertTrue(uploadFile.exists());
                assertEquals("POST", method);
                assertEquals("text/xml", expectedContentType);
                assertEquals("text/xml", uploadFileParam);


                return testResponse;
            }
        };
        rundeckAPICentralDispatcher.setServerService(testService);
        try {
            rundeckAPICentralDispatcher.createProject("test1", new Properties());
            fail("expected failure");
        } catch (CentralDispatcherException e) {
            assertTrue(e.getMessage().contains("Failed to create the project, result code: 200"));
        }
    }
    public void testCreateProjectResultDoc() throws Exception {
        final String testUrl = "http://localhost:4440/test";
        final RundeckAPICentralDispatcher rundeckAPICentralDispatcher = new RundeckAPICentralDispatcher(
                testUrl,
                "test",
                "test"
        );
        final TestResponse testResponse = new TestResponse();
        testResponse.resultDoc= DocumentFactory.getInstance().createDocument();
        testResponse.resultDoc.addElement("result").addElement("project").addElement("name").addText("test1");
        testResponse.resultCode=201;
        TestServerService testService = new TestServerService(
                testUrl,
                "test",
                "test"
        )
        {
            @Override
            public WebserviceResponse makeRundeckRequest(
                    final String urlPath,
                    final Map queryParams,
                    final File uploadFile,
                    final String method,
                    final String expectedContentType,
                    final String uploadFileParam
            ) throws CoreException, MalformedURLException
            {
                assertEquals("/api/11/projects", urlPath);
                assertEquals(0, queryParams.size());
                assertNotNull(uploadFile);
                assertTrue(uploadFile.exists());
                assertEquals("POST", method);
                assertEquals("text/xml", expectedContentType);
                assertEquals("text/xml", uploadFileParam);


                return testResponse;
            }
        };
        rundeckAPICentralDispatcher.setServerService(testService);
        rundeckAPICentralDispatcher.createProject("test1", new Properties());
    }

    public void testFilterProjectNodes() throws Exception {
        final String testUrl = "http://localhost:4440/test";
        final RundeckAPICentralDispatcher rundeckAPICentralDispatcher = new RundeckAPICentralDispatcher(
                testUrl,
                "test",
                "test"
        );
        final TestResponse testResponse = new TestResponse();
        testResponse.resultDoc= DocumentFactory.getInstance().createDocument();
        testResponse.resultDoc.addElement("project").addElement("node").addAttribute("name", "test1");

        testResponse.resultCode=200;
        TestServerService testService = new TestServerService(
                testUrl,
                "test",
                "test"
        )
        {

            @Override
            public WebserviceResponse makeRundeckRequest(
                    final String urlPath,
                    final Map queryParams,
                    final File uploadFile,
                    final String method,
                    final String expectedContentType,
                    final String uploadFileParam
            ) throws CoreException, MalformedURLException
            {
                assertEquals("/api/2/project/project1/resources", urlPath);
                assertEquals(1, queryParams.size());
                assertEquals("abc", queryParams.get("filter"));
                assertNull(uploadFile);
                assertEquals("GET", method);
                assertEquals("text/xml", expectedContentType);
                assertEquals(null, uploadFileParam);

                return testResponse;
            }
        };
        rundeckAPICentralDispatcher.setServerService(testService);
        INodeSet result = rundeckAPICentralDispatcher.filterProjectNodes("project1", "abc");
        assertEquals(1, result.getNodes().size());
        assertEquals("test1", result.getNode("test1").getNodename());
    }

    /**
     * null filter indicates all nodes
     * @throws Exception
     */
    public void testFilterProjectNodesAll() throws Exception {
        final String testUrl = "http://localhost:4440/test";
        final RundeckAPICentralDispatcher rundeckAPICentralDispatcher = new RundeckAPICentralDispatcher(
                testUrl,
                "test",
                "test"
        );
        final TestResponse testResponse = new TestResponse();
        testResponse.resultDoc= DocumentFactory.getInstance().createDocument();
        testResponse.resultDoc.addElement("project").addElement("node").addAttribute("name","test1");

        testResponse.resultCode=200;
        TestServerService testService = new TestServerService(
                testUrl,
                "test",
                "test"
        )
        {

            @Override
            public WebserviceResponse makeRundeckRequest(
                    final String urlPath,
                    final Map queryParams,
                    final File uploadFile,
                    final String method,
                    final String expectedContentType,
                    final String uploadFileParam
            ) throws CoreException, MalformedURLException
            {
                assertEquals("/api/2/project/project1/resources", urlPath);
                assertEquals(1, queryParams.size());
                assertEquals(".*", queryParams.get("filter"));
                assertNull(uploadFile);
                assertEquals("GET", method);
                assertEquals("text/xml", expectedContentType);
                assertEquals(null, uploadFileParam);

                return testResponse;
            }
        };
        rundeckAPICentralDispatcher.setServerService(testService);
        INodeSet result = rundeckAPICentralDispatcher.filterProjectNodes("project1", null);
        assertEquals(1, result.getNodes().size());
        assertEquals("test1", result.getNode("test1").getNodename());
    }

    /**
     * null filter indicates all nodes
     * @throws Exception
     */
    public void testFilterProjectNodesInternalError() throws Exception {
        final String testUrl = "http://localhost:4440/test";
        final RundeckAPICentralDispatcher rundeckAPICentralDispatcher = new RundeckAPICentralDispatcher(
                testUrl,
                "test",
                "test"
        );
        final TestResponse testResponse = new TestResponse();
        testResponse.resultDoc= DocumentFactory.getInstance().createDocument();
        testResponse.resultDoc.addElement("project").addElement("node").addAttribute("name","test1");

        testResponse.resultCode=500;
        testResponse.responseMessage = "Internal Error";
        TestServerService testService = new TestServerService(
                testUrl,
                "test",
                "test"
        )
        {

            @Override
            public WebserviceResponse makeRundeckRequest(
                    final String urlPath,
                    final Map queryParams,
                    final File uploadFile,
                    final String method,
                    final String expectedContentType,
                    final String uploadFileParam
            ) throws CoreException, MalformedURLException
            {
                assertEquals("/api/2/project/project1/resources", urlPath);
                assertEquals(1, queryParams.size());
                assertEquals(".*", queryParams.get("filter"));
                assertNull(uploadFile);
                assertEquals("GET", method);
                assertEquals("text/xml", expectedContentType);
                assertEquals(null, uploadFileParam);

                return testResponse;
            }
        };
        rundeckAPICentralDispatcher.setServerService(testService);

        try {
            INodeSet result = rundeckAPICentralDispatcher.filterProjectNodes("project1", null);
            fail("expected failure");
        }catch (CentralDispatcherException e) {
            assertTrue(e.getMessage().contains("500: Internal Error"));
        }
    }

    /**
     * nodes xml result parser error
     * @throws Exception
     */
    public void testFilterProjectNodesParserError() throws Exception {
        final String testUrl = "http://localhost:4440/test";
        final RundeckAPICentralDispatcher rundeckAPICentralDispatcher = new RundeckAPICentralDispatcher(
                testUrl,
                "test",
                "test"
        );
        final TestResponse testResponse = new TestResponse();
        testResponse.resultDoc= DocumentFactory.getInstance().createDocument();
        testResponse.resultDoc.addElement("project").addElement("node").addAttribute("xname","test1");

        testResponse.resultCode=200;
        TestServerService testService = new TestServerService(
                testUrl,
                "test",
                "test"
        )
        {

            @Override
            public WebserviceResponse makeRundeckRequest(
                    final String urlPath,
                    final Map queryParams,
                    final File uploadFile,
                    final String method,
                    final String expectedContentType,
                    final String uploadFileParam
            ) throws CoreException, MalformedURLException
            {
                assertEquals("/api/2/project/project1/resources", urlPath);
                assertEquals(1, queryParams.size());
                assertEquals(".*", queryParams.get("filter"));
                assertNull(uploadFile);
                assertEquals("GET", method);
                assertEquals("text/xml", expectedContentType);
                assertEquals(null, uploadFileParam);

                return testResponse;
            }
        };
        rundeckAPICentralDispatcher.setServerService(testService);
        try {
            INodeSet result = rundeckAPICentralDispatcher.filterProjectNodes("project1", null);
            fail("expected failure");
        }catch (CentralDispatcherException e) {
            assertTrue(e.getMessage().contains("Error parsing result"));
        }
    }
    /**
     * unable to validate result document
     * @throws Exception
     */
    public void testFilterProjectValidationError() throws Exception {
        final String testUrl = "http://localhost:4440/test";
        final RundeckAPICentralDispatcher rundeckAPICentralDispatcher = new RundeckAPICentralDispatcher(
                testUrl,
                "test",
                "test"
        );
        final TestResponse testResponse = new TestResponse();
        testResponse.resultDoc= DocumentFactory.getInstance().createDocument();
        testResponse.resultDoc.addElement("somethingelse").addElement("asdf").addAttribute("xxx","fff");

        testResponse.resultCode=200;
        TestServerService testService = new TestServerService(
                testUrl,
                "test",
                "test"
        )
        {

            @Override
            public WebserviceResponse makeRundeckRequest(
                    final String urlPath,
                    final Map queryParams,
                    final File uploadFile,
                    final String method,
                    final String expectedContentType,
                    final String uploadFileParam
            ) throws CoreException, MalformedURLException
            {
                assertEquals("/api/2/project/project1/resources", urlPath);
                assertEquals(1, queryParams.size());
                assertEquals(".*", queryParams.get("filter"));
                assertNull(uploadFile);
                assertEquals("GET", method);
                assertEquals("text/xml", expectedContentType);
                assertEquals(null, uploadFileParam);

                return testResponse;
            }
        };
        rundeckAPICentralDispatcher.setServerService(testService);
        try {
            INodeSet result = rundeckAPICentralDispatcher.filterProjectNodes("project1", null);
            fail("expected failure");
        }catch (CentralDispatcherException e) {
            assertTrue(e.getMessage().contains("Response had unexpected content"));
        }
    }
    public void testQueueScriptArgs() throws Exception {
        final String testUrl = "http://localhost:4440/test";
        final RundeckAPICentralDispatcher rundeckAPICentralDispatcher = new RundeckAPICentralDispatcher(
                testUrl,
                "test",
                "test"
        );
        final TestResponse testResponse = new TestResponse();
        testResponse.resultDoc= DocumentFactory.getInstance().createDocument();
        testResponse.resultDoc.addElement("result").addElement("execution").addAttribute(
                "id",
                "12"
        );
        testResponse.resultCode=200;
        TestServerService testService = new TestServerService(
                testUrl,
                "test",
                "test"
        )
        {
            @Override
            public WebserviceResponse makeRundeckRequest(final String urlPath,
                                                         final Map queryParams,
                                                         final File uploadFile,
                                                         final String method,
                                                         final String expectedContentType,
                                                         final Map<String, ? extends Object> formData,
                                                         final String uploadFileParam)
                    throws CoreException, MalformedURLException
            {
                assertEquals("/api/2/run/script", urlPath);
                assertEquals("no form data expected",0, formData.size());
                assertEquals(5, queryParams.size());
                assertEquals("testproject", queryParams.get("project"));
                assertEquals("arg1 arg2", queryParams.get("argString"));
                assertNotNull(uploadFile);
                assertEquals("test.sh",uploadFile.getName());
                assertEquals(null, method);
                assertEquals(null, expectedContentType);
                assertEquals("scriptFile", uploadFileParam);

                return testResponse;
            }
        };
        rundeckAPICentralDispatcher.setServerService(testService);
        QueuedItemResult result = rundeckAPICentralDispatcher.queueDispatcherScript(
                new IDispatchedScript() {
                    @Override
                    public String getFrameworkProject() {
                        return "testproject";
                    }

                    @Override
                    public String getScript() {
                        return null;
                    }

                    @Override
                    public InputStream getScriptAsStream() {
                        return null;
                    }

                    @Override
                    public String getServerScriptFilePath() {
                        return "test.sh";
                    }

                    @Override
                    public String getScriptURLString() {
                        return null;
                    }

                    @Override
                    public Boolean getNodeExcludePrecedence() {
                        return null;
                    }

                    @Override
                    public int getNodeThreadcount() {
                        return 1;
                    }

                    @Override
                    public Boolean isKeepgoing() {
                        return false;
                    }

                    @Override
                    public String getNodeFilter() {
                        return null;
                    }

                    @Override
                    public String[] getArgs() {
                        return new String[]{"arg1", "arg2"};
                    }

                    @Override
                    public int getLoglevel() {
                        return 0;
                    }

                    @Override
                    public Map<String, Map<String, String>> getDataContext() {
                        return null;
                    }
                }
        );

        assertEquals(true, result.isSuccessful());

        assertEquals("12", result.getItem().getId());
    }
}
