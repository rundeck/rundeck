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

import grails.testing.services.ServiceUnitTest
import groovy.mock.interceptor.MockFor
import org.junit.Test

import static org.junit.Assert.*

import grails.test.mixin.*
import groovy.util.slurpersupport.GPathResult
import groovy.xml.MarkupBuilder
import com.dtolabs.rundeck.app.api.ApiVersions

import javax.servlet.http.HttpServletResponse

class ApiServiceTests implements ServiceUnitTest<ApiServiceTests> {



    @Test
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
