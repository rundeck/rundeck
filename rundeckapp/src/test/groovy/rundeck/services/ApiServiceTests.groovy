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

package rundeck.services

import groovy.mock.interceptor.MockFor

import static org.junit.Assert.*

import grails.test.mixin.*
import groovy.util.slurpersupport.GPathResult
import groovy.xml.MarkupBuilder
import org.grails.plugins.testing.GrailsMockHttpServletResponse
import org.junit.*
import rundeck.interceptors.ApiVersionInterceptor
import com.dtolabs.rundeck.app.api.ApiVersions

import javax.servlet.http.HttpServletResponse

/**
 * See the API for {@link grails.test.mixin.services.ServiceUnitTestMixin} for usage instructions
 */
@TestFor(ApiService)
class ApiServiceTests {
    void testRequireVersion() {
        def mock = new MockFor(HttpServletResponse)
        def apiService = new ApiService()
        assertTrue(apiService.requireVersion([api_version:1],mock.proxyInstance(),1))
    }
    void testRequireVersionInvalid() {
        def mock = new MockFor(GrailsMockHttpServletResponse)
        mock.demand.getFormat {->
            'xml'
        }
        mock.demand.setStatus { int status ->
            assertEquals(400, status)
        }
        mock.demand.setContentType{String contentype->
            assertEquals("text/xml",contentype)
        }
        mock.demand.setCharacterEncoding{String encoding->
            assertEquals("UTF-8", encoding)
        }
        mock.demand.setHeader{String name, String value->
            assertEquals("X-Rundeck-API-Version", name)
            assertEquals(ApiVersions.API_CURRENT_VERSION.toString(), value)
        }
        def mockOut = new MockFor(OutputStream)
        def output = []
        mockOut.demand.leftShift { String s ->
            output << s
        }
        mockOut.demand.flush {->
        }
        def outs = mockOut.proxyInstance()
        mock.demand.getOutputStream {->
            outs
        }
        def apiService = new ApiService()
        apiService.messageSource=messageSource
        messageSource.addMessage('api.error.api-version.unsupported',Locale.default,'api.error.api-version.unsupported:{0}:{1}:{2}')
        assertFalse(apiService.requireVersion([api_version:1,forwardURI: '/test/uri'],mock.proxyInstance(),2))
        assertTrue(output.size()==1)
        def gpath = assertXmlErrorText(output[0])
        assertEquals('api.error.api-version.unsupported:1:/test/uri:Minimum supported version: 2',gpath.error.message.text())
    }

    void testRenderErrorXmlBuilderList() {
        def apiService = new ApiService()
        apiService.messageSource = messageSource
        def xml = apiService.renderErrorXml(['message1', 'message2'], 'test.code')
        assertNotNull(xml)
        def gpath = assertXmlErrorText(xml)
        assertEquals('test.code', gpath.error['@code'].text())
        assertEquals(['message1', 'message2'], gpath.error.messages[0].message*.text())
    }
    void testRenderSuccessXmlCode(){
        def mock = new MockFor(GrailsMockHttpServletResponse)
        mock.demand.setHeader { String name, String value ->
            assertEquals("X-Rundeck-API-XML-Response-Wrapper", name)
            assertEquals("true", value)
        }
        mock.demand.setContentType { String contentype ->
            assertEquals("text/xml", contentype)
        }
        mock.demand.setCharacterEncoding { String encoding ->
            assertEquals("UTF-8", encoding)
        }
        mock.demand.setHeader { String name, String value ->
            assertTrue([(name):value] in [
                    ["X-Rundeck-API-Version": ApiVersions.API_CURRENT_VERSION.toString()],
                    ["X-Rundeck-API-XML-Response-Wrapper": 'true'],
            ])
        }
        def mockOut = new MockFor(OutputStream)
        def output = []
        mockOut.demand.leftShift{String s->
            output<<s
        }
        mockOut.demand.flush{->
        }
        def outs=mockOut.proxyInstance()
        mock.demand.getOutputStream {  ->
            outs
        }
        def apiService = new ApiService()

        messageSource.addMessage('test.code',Locale.default,'test.code.{0}.{1}')
        apiService.messageSource=messageSource

        def result = apiService.renderSuccessXml(mock.proxyInstance(),'test.code',['arg1','arg2'])
        assertNull(result)
        assertEquals(1,output.size())
        GPathResult gpath = assertXmlSuccessText(output[0])
        assertEquals('test.code.arg1.arg2',gpath.success.message.text())
    }

    void testRenderSuccessXmlResponse() {
        def mock = new MockFor(GrailsMockHttpServletResponse)
        mock.demand.setHeader { String name, String value ->
        }
        mock.demand.setContentType { String contentype ->
            assertEquals("text/xml", contentype)
        }
        mock.demand.setCharacterEncoding { String encoding ->
            assertEquals("UTF-8", encoding)
        }
        mock.demand.setHeader { String name, String value ->
            assertTrue([(name): value] in [
                    ["X-Rundeck-API-Version": ApiVersions.API_CURRENT_VERSION.toString()],
                    ["X-Rundeck-API-XML-Response-Wrapper": 'true'],
            ])
        }
        def mockOut = new MockFor(OutputStream)
        def output = []
        mockOut.demand.leftShift { String s ->
            output << s
        }
        mockOut.demand.flush {->
        }
        def outs = mockOut.proxyInstance()
        mock.demand.getOutputStream {->
            outs
        }
        def apiService = new ApiService()
        def closureCalled=false
        def result=apiService.renderSuccessXml(mock.proxyInstance()){
            closureCalled=true
        }
        assertTrue(closureCalled)
        assertNull(result)
        assertEquals(1,output.size())
        GPathResult gpath = assertXmlSuccessText(output[0])
    }
    void testRenderSuccessXmlClosure(){
        def apiService = new ApiService()
        def result=apiService.renderSuccessXml {
            assertTrue(delegate instanceof MarkupBuilder)
            delegate.'test'(name: 'testRenderSuccessXmlClosure')
        }
        assertNotNull(result)
        assertTrue(result instanceof String)
        GPathResult gpath = assertXmlSuccessText(result)
        assertEquals('testRenderSuccessXmlClosure',gpath.test['@name'].text())
    }

    private GPathResult assertXmlSuccessText(String result) {
        def slurper = new XmlSlurper()
        def gpath = slurper.parseText(result)
        assertEquals('result', gpath.name())
        assertEquals('true', gpath['@success'].text())
        assertEquals(ApiVersions.API_CURRENT_VERSION.toString(), gpath['@apiversion'].text())
        gpath
    }
    private GPathResult assertXmlErrorText(String result) {
        def slurper = new XmlSlurper()
        def gpath = slurper.parseText(result)
        assertEquals('result', gpath.name())
        assertEquals('true', gpath['@error'].text())
        assertEquals(ApiVersions.API_CURRENT_VERSION.toString(), gpath['@apiversion'].text())
        gpath
    }
}
