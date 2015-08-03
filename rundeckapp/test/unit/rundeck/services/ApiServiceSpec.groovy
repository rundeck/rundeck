package rundeck.services

import com.dtolabs.rundeck.core.authorization.Validation
import grails.converters.JSON
import grails.test.mixin.TestFor
import grails.test.mixin.TestMixin
import grails.test.mixin.web.ControllerUnitTestMixin
import grails.web.JSONBuilder
import groovy.xml.MarkupBuilder
import spock.lang.Specification

/**
 * Created by greg on 7/28/15.
 */
@TestFor(ApiService)
@TestMixin(ControllerUnitTestMixin)
class ApiServiceSpec extends Specification {
    def "renderWrappedFileContents json"(){
        given:
        def builder = new JSONBuilder()
        when:
        def result=builder.build {
            service.renderWrappedFileContents('x','json',delegate)
        }

        then:
        result.toString()=='{"contents":"x"}'

    }
    def "renderWrappedFileContents xml"(){
        given:
        def sw = new StringWriter()
        def builder = new MarkupBuilder(sw)
        when:
        service.renderWrappedFileContents('x','xml',builder)

        then:
        sw.toString()=='<contents><![CDATA[x]]></contents>'

    }
    def "jsonRenderDirlist"(){
        given:
        def builder = new JSONBuilder()
        when:
        def result=builder.build {
            service.jsonRenderDirlist(
                    '',
                    {it},
                    {"http://localhost:8080/api/14/project/test/acl/${it}"},
                    ['blah.aclpolicy','adir/'],
                    delegate
            )
        }
        def parsed=JSON.parse(result.toString())

        then:
        parsed==[
                resources:[
                        [
                                name:'blah.aclpolicy',
                                path: 'blah.aclpolicy',
                                type: 'file',
                                href: 'http://localhost:8080/api/14/project/test/acl/blah.aclpolicy'

                        ],
                        [
                                path:'adir/',
                                type: 'directory',
                                href: 'http://localhost:8080/api/14/project/test/acl/adir/'
                        ]
                ],
                path:'',
                type:'directory',
                href:'http://localhost:8080/api/14/project/test/acl/']
    }
    def "xmlRenderDirList"(){
        given:
        def sw = new StringWriter()
        def builder = new MarkupBuilder(sw)
        when:
        service.xmlRenderDirList(
                '',
                {it},
                {"http://localhost:8080/api/14/project/test/acl/${it}"},
                ['blah.aclpolicy','adir/'],
                builder
        )
        def parsed=new XmlSlurper().parse(new StringReader(sw.toString()))

        then:
        parsed.'@path'.text()==''
        parsed.'@type'.text()=='directory'
        parsed.'@href'.text()=='http://localhost:8080/api/14/project/test/acl/'
        parsed.contents.size()==1
        parsed.contents.resource.size()==2
        parsed.contents.resource[0].'@path'.text()=='blah.aclpolicy'
        parsed.contents.resource[0].'@name'.text()=='blah.aclpolicy'
        parsed.contents.resource[0].'@type'.text()=='file'
        parsed.contents.resource[0].'@href'.text()=='http://localhost:8080/api/14/project/test/acl/blah.aclpolicy'
        parsed.contents.resource[1].'@path'.text()=='adir/'
        parsed.contents.resource[1].'@type'.text()=='directory'
        parsed.contents.resource[1].'@href'.text()=='http://localhost:8080/api/14/project/test/acl/adir/'
    }

    def "renderJsonAclpolicyValidation"(){
        given:

        def builder = new JSONBuilder()
        when:
        def validation=Stub(Validation){
            isValid()>>false
            getErrors()>>['file1[1]':['error1','error2'],'file2[1]':['error3','error4']]
        }
        def result=builder.build {
            service.renderJsonAclpolicyValidation(
                    validation,
                    delegate
            )
        }
        def parsed=JSON.parse(result.toString())
        then:
        parsed==[valid:false,
        policies:[
                [policy:'file1[1]',errors:['error1','error2']],
                [policy:'file2[1]',errors:['error3','error4']]
        ]]
    }
    def "renderXmlAclpolicyValidation"(){
        given:
        def sw = new StringWriter()
        def builder = new MarkupBuilder(sw)
        when:
        def validation=Stub(Validation){
            isValid()>>false
            getErrors()>>['file1[1]':['error1','error2'],'file2[1]':['error3','error4']]
        }
        service.renderXmlAclpolicyValidation(
                validation,
                builder
        )
        def parsed=new XmlSlurper().parse(new StringReader(sw.toString()))
        then:
        parsed.name()=='validation'
        parsed.'@valid'.text()=='false'
        parsed.'policy'.size()==2
        parsed.'policy'[0].'@id'.text()=='file1[1]'
        parsed.'policy'[0].'error'.size()==2
        parsed.'policy'[0].'error'[0].text()=='error1'
        parsed.'policy'[0].'error'[1].text()=='error2'

        parsed.'policy'[1].'@id'.text()=='file2[1]'
        parsed.'policy'[1].'error'.size()==2
        parsed.'policy'[1].'error'[0].text()=='error3'
        parsed.'policy'[1].'error'[1].text()=='error4'

    }
}
