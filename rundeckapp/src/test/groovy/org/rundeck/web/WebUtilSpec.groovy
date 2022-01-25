package org.rundeck.web

import com.dtolabs.rundeck.app.api.ApiVersions
import grails.testing.web.GrailsWebUnitTest
import groovy.mock.interceptor.MockFor
import groovy.util.slurpersupport.GPathResult
import org.grails.plugins.codecs.JSONCodec
import org.grails.web.util.WebUtils
import org.junit.Test
import org.springframework.context.MessageSource
import rundeck.services.ApiService
import spock.lang.Specification
import spock.lang.Unroll

import javax.servlet.http.HttpServletRequest
import javax.servlet.http.HttpServletResponse

import static org.junit.Assert.assertEquals
import static org.junit.Assert.assertEquals
import static org.junit.Assert.assertNotNull
import static org.junit.Assert.assertTrue

class WebUtilSpec extends Specification implements GrailsWebUnitTest{
    WebUtil service
    def setup(){

        mockCodec(JSONCodec)
        service = new WebUtil()
    }

    @Test
    void "require version success"() {
        given:
        def resp = Mock(HttpServletResponse)
        def request=Mock(HttpServletRequest){
            _*getAttribute('api_version')>>value
            _*getRequestURI()>> '/test/uri'
        }
        expect:
        service.requireVersion(request,resp,test)
        where:
            value | test
            1     | 1
            2     | 1
            40    | 16
    }
    @Unroll
    def "create error map"() {
        given:
            service.messageSource = Mock(MessageSource) {
                getMessage(_, _,_, _) >> { it[0] + (it[1] ?: '') }
            }
        when:
            def result = service.createErrorMap(data, code)

        then:
            println result
            result.errorCode==expectCode
            result.apiversion==ApiVersions.API_CURRENT_VERSION
            result.error==true
            result.message==eMessage
            result.messages==eMessages

        where:
            code   | data                                 | expectCode          | eMessage            | eMessages
            null   | [:]                                  | 'api.error.unknown' | 'api.error.unknown' | null
            'test' | [:]                                  | 'test'              | 'api.error.unknown' | null
            'test' | ['a', 'b']                           | 'test'              | null                | ["a","b"]
            'test' | [code: 'acode']                      | 'test'              | 'acode'             | null
            'test' | [code: 'acode', message: 'amessage'] | 'test'              | 'amessage'          | null
            'test' | [code: 'acode', args: ['a', 'b']]    | 'test'              | 'acode[a, b]'       | null
            'test' | [message: 'amessage']                | 'test'              | 'amessage'          | null

    }

    @Unroll
    def "render error xml"() {
        given:
            service.messageSource = Mock(MessageSource) {
                getMessage(_, _,_, _) >> { it[0] + (it[1] ?: '') }
            }
        when:
            def result = service.renderErrorXml(data, code)
        then:
            result == """<result error='true' apiversion='${ApiVersions.API_CURRENT_VERSION}'>
  <error code='${
                code
            }'>
    """+
            (emessage?("<message>${emessage}</message>"):'')+
            (elist?("""<messages>\n      ${elist}\n    </messages>"""):'')+
            """
  </error>
</result>"""

        where:
            code    | data                                 | emessage            | elist
            'acode' | null                                 | 'api.error.unknown' | null
            'acode' | [code: 'acode']                      | 'acode'             | null
            'acode' | [code: 'acode', args: ['a', 'b']]    | 'acode[a, b]'       | null
            'acode' | [code: 'acode', message: 'zmessage'] | 'zmessage'          | null
            'acode' | [message: 'amessage']                | 'amessage'          | null
            'acode' | ['a', 'b']                           | null                | '''<message>a</message>\n      <message>b</message>'''


    }

    def "testRenderErrorXmlBuilderList"() {
        given:
            service.messageSource = messageSource
        when:
            def xml = service.renderErrorXml(['message1', 'message2'], 'test.code')
        then:
            xml != null
            def gpath = assertXmlErrorText(xml)
            'test.code' == gpath.error['@code'].text()
            ['message1', 'message2'] == gpath.error.messages[0].message*.text()
    }

    def "requre version invalid xml"() {
        given:
            service.messageSource = Mock(MessageSource) {
                getMessage(_, _,_, _) >> { it[0] +":"+ (it[1].join(":") ?: '') }
            }
        when:
            request.addHeader('accept','application/xml')
            def check = service.requireVersion(Mock(HttpServletRequest){
                _*getAttribute('api_version')>>1
                _*getRequestURI()>> '/test/uri'
            }, response, 2)

        then:
            def result = assertXmlErrorText(response.text)
            !check
            response.contentType == "application/xml"
            response.characterEncoding == "UTF-8"
            response.status == 400
            response.getHeader("X-Rundeck-API-Version") == ApiVersions.API_CURRENT_VERSION.toString()
            result.error.message.text() == 'api.error.api-version.unsupported:1:/test/uri:Minimum supported version: 2'
    }


    private GPathResult assertXmlErrorText(String result) {
        def slurper = new XmlSlurper()
        def gpath = slurper.parseText(result)
        'result' == gpath.name()
        'true' == gpath['@error'].text()
        ApiVersions.API_CURRENT_VERSION.toString() == gpath['@apiversion'].text()
        gpath
    }
    def "requre version invalid default json"() {
        given:
            service.messageSource = Mock(MessageSource) {
                getMessage(_, _,_, _) >> { it[0] +":"+ (it[1].join(":") ?: '') }
            }
        when:
            def check = service.requireVersion(Mock(HttpServletRequest){
                _*getAttribute('api_version')>>1
                _*getRequestURI()>> '/test/uri'
            },response,2)

        then:
            !check
            response.contentType == "application/json"
            response.characterEncoding == "UTF-8"
            response.status == 400
            response.getHeader("X-Rundeck-API-Version") == ApiVersions.API_CURRENT_VERSION.toString()
            response.json.message == 'api.error.api-version.unsupported:1:/test/uri:Minimum supported version: 2'
    }
}
