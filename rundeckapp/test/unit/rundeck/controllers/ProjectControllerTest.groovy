package rundeck.controllers

import com.dtolabs.rundeck.core.authorization.AuthContext
import com.dtolabs.rundeck.server.authorization.AuthConstants
import grails.test.mixin.TestFor
import org.junit.Test
import org.springframework.context.MessageSource
import rundeck.filters.ApiRequestFilters
import rundeck.services.ApiService
import rundeck.services.FrameworkService

import javax.servlet.http.HttpServletResponse

/**
 * ProjectControllerTest is ...
 * @author greg
 * @since 2014-03-04
 */
@TestFor(ProjectController)
class ProjectControllerTest {

    /**
     * utility method to mock a class
     */
    private mockWith(Class clazz,Closure clos){
        def mock = mockFor(clazz)
        mock.demand.with(clos)
        return mock.createMock()
    }

    @Test
    void apiProjectList_xml(){
        def fwct = mockFor(FrameworkService)
        fwct.demand.getAuthContextForSubject(1..1){subj->
            null
        }
        fwct.demand.projects(1..1){auth->
            ["a", "b"]
        }
        controller.frameworkService=fwct.createMock()

        controller.apiService = mockFor(ApiService).with {
            demand.renderSuccessXml(1..1) { req, resp, clos ->

            }
            createMock()
        }

        response.format='xml'
        controller.apiProjectList()
        assert response.status == HttpServletResponse.SC_OK

    }
    @Test
    void apiProjectList_json(){
        def fwct = mockFor(FrameworkService)
        fwct.demand.getAuthContextForSubject(1..1){subj->
            null
        }
        fwct.demand.projects(1..1){auth->
            [
                    [name:'testproject'],
                    [name:'testproject2'],
            ]
        }
        controller.frameworkService=fwct.createMock()



        response.format='json'
        controller.apiProjectList()
        assert response.status == HttpServletResponse.SC_OK
        assert response.json.size()==2
        assert response.json[0].name=='testproject'
        assert response.json[0].description==''
        assert response.json[0].url=='http://localhost:8080/api/11/project/testproject'
        assert response.json[1].name=='testproject2'
        assert response.json[1].description==''
        assert response.json[1].url=='http://localhost:8080/api/11/project/testproject2'
    }
    @Test
    void apiProjectList_unacceptableReceivesXml(){
        def fwct = mockFor(FrameworkService)
        fwct.demand.getAuthContextForSubject(1..1){subj->
            null
        }
        fwct.demand.projects(1..1){auth->
            [
                    [name:'testproject'],
                    [name:'testproject2'],
            ]
        }
        controller.frameworkService=fwct.createMock()

        controller.apiService = mockFor(ApiService).with {
            demand.renderSuccessXml(1..1) { req, resp, clos ->

            }
            createMock()
        }

        response.format='text'
        controller.apiProjectList()
        assert response.status==HttpServletResponse.SC_OK

    }

    @Test
    void apiProjectGet_missingProjectParam(){
        controller.frameworkService = mockFor(FrameworkService).with{
            demand.getAuthContextForSubject(1..1){subj->
                null
            }
            createMock()
        }

        controller.apiService = mockFor(ApiService).with {
            demand.renderErrorFormat(1..1) { resp, map ->
                assertEquals(HttpServletResponse.SC_BAD_REQUEST,map.status)
                assertEquals('api.error.parameter.required',map.code)
                resp.status=map.status
            }
            createMock()
        }

        response.format='xml'
        controller.apiProjectGet()
        assert response.status==HttpServletResponse.SC_BAD_REQUEST
    }
    @Test
    void apiProjectGet_unauthorized(){
        controller.frameworkService = mockFor(FrameworkService).with{
            demand.getAuthContextForSubject(1..1){subj->
                null
            }
            demand.authorizeApplicationResourceAll(1..1){ AuthContext authContext, Map resource, Collection actions->
                assertEquals('project',resource.type)
                assertEquals('test1',resource.name)
                assertEquals([AuthConstants.ACTION_READ],actions)
                false
            }
            createMock()
        }

        controller.apiService = mockFor(ApiService).with {
            demand.renderErrorFormat(1..1) { resp, map ->
                assertEquals(HttpServletResponse.SC_FORBIDDEN,map.status)
                assertEquals('api.error.item.unauthorized',map.code)
                resp.status=map.status
            }
            createMock()
        }

        response.format='xml'
        params.project='test1'
        controller.apiProjectGet()
        assert response.status==HttpServletResponse.SC_FORBIDDEN
    }
    @Test
    void apiProjectGet_notfound(){
        controller.frameworkService = mockFor(FrameworkService).with{
            demand.getAuthContextForSubject(1..1){subj->
                null
            }
            demand.authorizeApplicationResourceAll(1..1){ AuthContext authContext, Map resource, Collection actions->
                assertEquals('project',resource.type)
                assertEquals('test1',resource.name)
                assertEquals([AuthConstants.ACTION_READ],actions)
                true
            }
            demand.existsFrameworkProject(1..1) { proj ->
                assertEquals('test1',proj)
                false
            }
            createMock()
        }

        controller.apiService = mockFor(ApiService).with {
            demand.renderErrorFormat(1..1) { resp, map ->
                assertEquals(HttpServletResponse.SC_NOT_FOUND,map.status)
                assertEquals('api.error.item.doesnotexist',map.code)
                resp.status=map.status
            }
            createMock()
        }

        response.format='xml'
        params.project='test1'
        controller.apiProjectGet()
        assert response.status==HttpServletResponse.SC_NOT_FOUND
    }

    private Object createFrameworkService(boolean configAuth, String projectName, LinkedHashMap<String,
    String> projectProperties=[:]) {
        mockFor(FrameworkService).with {
            demand.getAuthContextForSubject(1..1) { subj ->
                null
            }
            def count = 0
            demand.authorizeApplicationResourceAll(1..1) { AuthContext authContext, Map resource, Collection actions ->
                assertEquals('project', resource.type)
                assertEquals(projectName, resource.name)
                count++
                if (count == 1) {
                    assertEquals([AuthConstants.ACTION_READ], actions)
                    return true
                } else {
                    assertEquals([AuthConstants.ACTION_CONFIGURE], actions)
                    return configAuth
                }
            }
            demand.existsFrameworkProject(1..1) { proj ->
                assertEquals(projectName, proj)
                true
            }
            demand.authorizeApplicationResourceAll(1..1) { AuthContext authContext, Map resource, Collection actions ->
                assertEquals('project', resource.type)
                assertEquals(projectName, resource.name)
                count++
                if (count == 1) {
                    assertEquals([AuthConstants.ACTION_READ], actions)
                    return true
                } else {
                    assertEquals([AuthConstants.ACTION_CONFIGURE], actions)
                    return configAuth
                }
            }
            demand.getFrameworkProject(1..1) { String name ->
                assertEquals(projectName, name)
                [name: projectName]
            }
            if(configAuth){
                demand.loadProjectProperties(1..1){pject->
                    assertEquals([name:projectName],pject)
                    projectProperties
                }
            }
            createMock()
        }
    }

    @Test
    void apiProjectGet_xml_noconfig_withwrapper() {
        defineBeans {
            apiService(ApiService)
        }
        controller.frameworkService = createFrameworkService(false, 'test1')

        response.format = 'xml'
        params.project = 'test1'
        request.setAttribute('api_version', 10) // trigger xml <result> wrapper
        controller.apiProjectGet()
        assert response.status == HttpServletResponse.SC_OK

        //XML result has wrapper
        assertEquals 'result', response.xml.name()
        assertEquals 'true', response.xml.'@success'.text()
        assertEquals ApiRequestFilters.API_CURRENT_VERSION.toString(), response.xml.'@apiversion'.text()

        assertEquals 1, response.xml.projects.size()
        assertEquals 0, response.xml.project.size()
        assertEquals 1, response.xml.projects.size()
        assertEquals 1, response.xml.projects.project.size()
        def project = response.xml.projects.project[0]

        //test project element
        assertEquals 'test1', project.name.text()
        assertEquals '', project.description.text()
        assertEquals 0, project.config.size()
    }
    @Test
    void apiProjectGet_xml_noconfig_v11(){
        defineBeans {
            apiService(ApiService)
        }
        controller.frameworkService = createFrameworkService(false, 'test1')

        response.format='xml'
        params.project='test1'
        request.setAttribute('api_version', 11) //do not include <result> wrapper
        controller.apiProjectGet()
        assert response.status==HttpServletResponse.SC_OK

        //XML result has no wrapper
        assertEquals 'project', response.xml.name()
        assertEquals 0, response.xml.result.size()
        assertEquals 0, response.xml.projects.size()
        def project=response.xml

        //test project element
        assertEquals 'test1', project.name.text()
        assertEquals '', project.description.text()
        assertEquals 0, project.config.size()
    }
    /**
     * apiversion {@literal <} 11 will result in no {@literal <config>} element, even if authorized
     */
    @Test
    void apiProjectGet_xml_withconfig_withwrapper(){
        defineBeans {
            apiService(ApiService)
        }
        controller.frameworkService = createFrameworkService(true, 'test1', ["test.property": "value1", "test.property2": "value2"])

        response.format='xml'
        params.project='test1'
        request.setAttribute('api_version', 10) //do include <result> wrapper
        controller.apiProjectGet()
        assert response.status==HttpServletResponse.SC_OK

        //XML result has wrapper
        assertEquals 'result', response.xml.name()
        assertEquals 'true', response.xml.'@success'.text()
        assertEquals ApiRequestFilters.API_CURRENT_VERSION.toString(), response.xml.'@apiversion'.text()

        assertEquals 1, response.xml.projects.size()
        assertEquals 0, response.xml.project.size()
        assertEquals 1, response.xml.projects.size()
        assertEquals 1, response.xml.projects.project.size()
        def project = response.xml.projects.project[0]

        //test project element
        assertEquals 'test1', project.name.text()
        assertEquals '', project.description.text()
        assertEquals 0, project.config.size()
    }
    /**
     * apiversion {@literal >=} 11 will result in {@literal <config>} element, if authorized
     */
    @Test
    void apiProjectGet_xml_withconfig_v11(){
        defineBeans {
            apiService(ApiService)
        }
        controller.frameworkService = createFrameworkService(true, 'test1', ["test.property": "value1", "test.property2": "value2"])

        response.format='xml'
        params.project='test1'
        request.setAttribute('api_version', 11) //do not include <result> wrapper
        controller.apiProjectGet()
        assert response.status==HttpServletResponse.SC_OK

        //XML result has wrapper
        assertEquals 'project', response.xml.name()

        assertEquals 0, response.xml.projects.size()
        assertEquals 0, response.xml.project.size()
        def project = response.xml

        //test project element
        assertEquals 'test1', project.name.text()
        assertEquals '', project.description.text()
        assertEquals 1, project.config.size()
        assertEquals 2, project.config.property.size()
        assertEquals 'test.property', project.config.property[0].'@key'.text()
        assertEquals 'value1', project.config.property[0].'@value'.text()
        assertEquals 'test.property2', project.config.property[1].'@key'.text()
        assertEquals 'value2', project.config.property[1].'@value'.text()
    }

    @Test
    void apiProjectGet_json_noconfig() {
        defineBeans {
            apiService(ApiService)
        }
        controller.frameworkService = createFrameworkService(false, 'test1')

        response.format = 'json'
        params.project = 'test1'
        request.setAttribute('api_version', 11) // trigger xml <result> wrapper
        controller.apiProjectGet()
        assert response.status == HttpServletResponse.SC_OK

        def project = response.json

        //test project element
        assertEquals 'test1', project.name
        assertEquals '', project.description
        assertEquals null, project.config
    }
    @Test
    void apiProjectGet_json_withconfig() {
        defineBeans {
            apiService(ApiService)
        }
        controller.frameworkService = createFrameworkService(true, 'test1',['test.property':'value1',
                'test.property2':'value2'])

        response.format = 'json'
        params.project = 'test1'
        request.setAttribute('api_version', 11) // trigger xml <result> wrapper
        controller.apiProjectGet()
        assert response.status == HttpServletResponse.SC_OK

        def project = response.json

        //test project element
        assertEquals 'test1', project.name
        assertEquals '', project.description
        assertEquals(['test.property': 'value1', 'test.property2': 'value2'], project.config)
    }
    @Test
    void apiProjectCreate_xml_unauthorized() {
        defineBeans {
            apiService(ApiService)
        }
        controller.apiService.messageSource= mockFor(MessageSource).with {
            demand.getMessage{code,args,locale->
                code
            }
            createMock()
        }
        controller.frameworkService=mockFor(FrameworkService).with {
            demand.getAuthContextForSubject(1..1){subject->null}
            demand.authorizeApplicationResourceTypeAll{auth,type,actions->
                assert "project"== type
                assert ['create']==actions
                false
            }
            createMock()
        }

        request.xml='<project><name>test1</name></project>'
        request.method='POST'
        response.format = 'xml'
        request.setAttribute('api_version', 11) // require version 11
        controller.apiProjectCreate()
        assert response.status == HttpServletResponse.SC_FORBIDDEN

        def result = response.xml

        //test project element
        assertEquals 'true', result.'@error'.text()
        assertEquals 'api.error.item.unauthorized', result.error.'@code'.text()
        assertEquals 'api.error.item.unauthorized', result.error.message.text()
    }
    /**
     * Missing project name element
     */
    @Test
    void apiProjectCreate_xml_invalid() {
        defineBeans {
            apiService(ApiService)
        }
        controller.apiService.messageSource= mockFor(MessageSource).with {
            demand.getMessage{code,args,locale->
                code
            }
            createMock()
        }
        controller.frameworkService=mockFor(FrameworkService).with {
            demand.getAuthContextForSubject(1..1){subject->null}
            demand.authorizeApplicationResourceTypeAll{auth,type,actions->
                assert "project"== type
                assert ['create']==actions
                true
            }
            createMock()
        }

        request.xml='<project><namex>test1</namex></project>'
        request.method='POST'
        response.format = 'xml'
        request.setAttribute('api_version', 11) // require version 11
        controller.apiProjectCreate()
        assert response.status == HttpServletResponse.SC_BAD_REQUEST

        def result = response.xml

        //test project element
        assertEquals 'true', result.'@error'.text()
        assertEquals 'api.error.invalid.request', result.error.'@code'.text()
        assertEquals 'api.error.invalid.request', result.error.message.text()
    }
    /**
     * project already exists
     */
    @Test
    void apiProjectCreate_xml_projectExists() {
        defineBeans {
            apiService(ApiService)
        }
        controller.apiService.messageSource= mockWith(MessageSource){
            getMessage{code,args,locale->
                code
            }
        }
        controller.frameworkService=mockWith(FrameworkService) {
            getAuthContextForSubject(1..1){subject->null}
            authorizeApplicationResourceTypeAll{auth,type,actions->
                assert "project"== type
                assert ['create']==actions
                true
            }
            existsFrameworkProject{name->
                assert "test1"== name
                true
            }
        }

        request.xml='<project><name>test1</name></project>'
        request.method='POST'
        response.format = 'xml'
        request.setAttribute('api_version', 11) // require version 11
        controller.apiProjectCreate()
        assert response.status == HttpServletResponse.SC_CONFLICT

        def result = response.xml

        //test project element
        assertEquals 'true', result.'@error'.text()
        assertEquals 'api.error.item.alreadyexists', result.error.'@code'.text()
        assertEquals 'api.error.item.alreadyexists', result.error.message.text()
    }
    /**
     * project already exists
     */
    @Test
    void apiProjectCreate_xml_withErrors() {
        defineBeans {
            apiService(ApiService)
        }
        controller.apiService.messageSource= mockWith(MessageSource){
            getMessage{code,args,locale->
                code
            }
        }
        controller.frameworkService=mockWith(FrameworkService) {
            getAuthContextForSubject(1..1){subject->null}
            authorizeApplicationResourceTypeAll{auth,type,actions->
                assert "project"== type
                assert ['create']==actions
                true
            }
            existsFrameworkProject{name->
                assert "test1"== name
                false
            }
            createFrameworkProject{name,props->
                [null,['error1','error2']]
            }
        }

        request.xml='<project><name>test1</name></project>'
        request.method='POST'
        response.format = 'xml'
        request.setAttribute('api_version', 11) // require version 11
        controller.apiProjectCreate()
        assert response.status == HttpServletResponse.SC_INTERNAL_SERVER_ERROR

        def result = response.xml

        //test project element
        assertEquals 'true', result.'@error'.text()
        assertEquals 0,result.error.'@code'.size()
        assertEquals 'error1; error2', result.error.message.text()
    }
    /**
     * project already exists
     */
    @Test
    void apiProjectCreate_xml_success() {
        defineBeans {
            apiService(ApiService)
        }
        controller.apiService.messageSource= mockWith(MessageSource){
            getMessage{code,args,locale->
                code
            }
        }
        controller.frameworkService=mockWith(FrameworkService) {
            getAuthContextForSubject(1..1){subject->null}
            authorizeApplicationResourceTypeAll{auth,type,actions->
                assert "project"== type
                assert ['create']==actions
                true
            }
            existsFrameworkProject{name->
                assert "test1"== name
                false
            }
            createFrameworkProject{name,props->
                assertEquals 'test1',name
                assertEquals 0,props.size()
                [[name:'test1'],[]]
            }
            loadProjectProperties{proj->
                assertEquals 'test1',proj.name
                ['prop1':'value1','prop2':'value2']
            }
        }

        request.xml='<project><name>test1</name></project>'
        request.method='POST'
        response.format = 'xml'
        request.setAttribute('api_version', 11) // require version 11
        controller.apiProjectCreate()
        assert response.status == HttpServletResponse.SC_CREATED

        def result = response.xml

        //test project element
        assertEquals 0, result.'@error'.size()
        def project =result
        assertEquals "test1",project.name.text()
        assertEquals 1,project.config.size()
        assertEquals 2, project.config.property.size()
        assertEquals 'prop1', project.config.property[0].'@key'.text()
        assertEquals 'value1', project.config.property[0].'@value'.text()
        assertEquals 'prop2', project.config.property[1].'@key'.text()
        assertEquals 'value2', project.config.property[1].'@value'.text()
    }
    /**
     * Create project with input config
     */
    @Test
    void apiProjectCreate_xml_withconfig() {
        defineBeans {
            apiService(ApiService)
        }
        controller.apiService.messageSource= mockWith(MessageSource){
            getMessage{code,args,locale->
                code
            }
        }
        controller.frameworkService=mockWith(FrameworkService) {
            getAuthContextForSubject(1..1){subject->null}
            authorizeApplicationResourceTypeAll{auth,type,actions->
                assert "project"== type
                assert ['create']==actions
                true
            }
            existsFrameworkProject{name->
                assert "test1"== name
                false
            }
            createFrameworkProject{name,props->
                assertEquals 'test1',name
                assertEquals (['input1':'value1','input2':'value2'],props)
                [[name:'test1'],[]]
            }
            loadProjectProperties{proj->
                assertEquals 'test1',proj.name
                ['prop1':'value1','prop2':'value2']
            }
        }

        request.xml='<project><name>test1</name><config><property key="input1" value="value1"/><property key="input2"' +
                ' value="value2"/></config></project>'
        request.method='POST'
        response.format = 'xml'
        request.setAttribute('api_version', 11) // require version 11
        controller.apiProjectCreate()
        assert response.status == HttpServletResponse.SC_CREATED

        def result = response.xml

        //test project element
        assertEquals 0, result.'@error'.size()
        def project =result
        assertEquals "test1",project.name.text()
        assertEquals 1,project.config.size()
        assertEquals 2, project.config.property.size()
        assertEquals 'prop1', project.config.property[0].'@key'.text()
        assertEquals 'value1', project.config.property[0].'@value'.text()
        assertEquals 'prop2', project.config.property[1].'@key'.text()
        assertEquals 'value2', project.config.property[1].'@value'.text()
    }
}
