
package rundeck.controllers

import com.dtolabs.rundeck.core.authentication.Group
import com.dtolabs.rundeck.core.authentication.Username
import com.dtolabs.rundeck.core.authorization.AuthContext
import com.dtolabs.rundeck.core.common.FrameworkProject
import com.dtolabs.rundeck.server.authorization.AuthConstants

import grails.test.mixin.TestFor

import org.codehaus.groovy.grails.plugins.codecs.JSONCodec;
import org.codehaus.groovy.grails.plugins.testing.GrailsMockHttpServletResponse
import org.junit.Test
import org.springframework.context.MessageSource

import rundeck.filters.ApiRequestFilters
import rundeck.services.ApiService
import rundeck.services.FrameworkService
import rundeck.services.ProjectService

import javax.security.auth.Subject
import javax.servlet.http.HttpServletResponse

/**
 * ProjectControllerTest is ...
 * @author greg
 * @since 2014-03-04
 */
@TestFor(ProjectController)
class ProjectControllerTest {

    @Before
    public void setup(){
        
        controller.apiService = new ApiService()
    }
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
        controller.frameworkService = mockWith(FrameworkService){
            getAuthContextForSubject(1..1){subj->
                null
            }
            projects(1..1){auth->
                [
                        [name: 'testproject'],
                        [name: 'testproject2'],
                ]
            }
        }

        controller.apiService = mockWith(ApiService){
            requireApi(1..1) { req, resp ->
                true
            }
            renderSuccessXml(1..1) { req, resp, clos ->

            }
        }

        response.format='xml'
        controller.apiProjectList()
        assert response.status == HttpServletResponse.SC_OK

    }
    @Test
    void apiProjectList_json(){
        controller.frameworkService = mockWith(FrameworkService) {
            getAuthContextForSubject(1..1) { subj ->
                null
            }
            projects(1..1) { auth ->
                [
                        [name: 'testproject'],
                        [name: 'testproject2'],
                ]
            }
        }
        controller.apiService=mockWith(ApiService){
            requireApi(1..1){req,resp->true}
        }

        response.format='json'
        controller.apiProjectList()
        def base='http://localhost:8080/api/'+ApiRequestFilters.API_CURRENT_VERSION
        assert response.status == HttpServletResponse.SC_OK
        assert response.json.size()==2
        assert response.json[0].name=='testproject'
        assert response.json[0].description==''
        assert response.json[0].url==base+'/project/testproject'
        assert response.json[1].name=='testproject2'
        assert response.json[1].description==''
        assert response.json[1].url==base+'/project/testproject2'
    }
    @Test
    void apiProjectList_unacceptableReceivesXml(){
        controller.frameworkService = mockWith(FrameworkService) {
            getAuthContextForSubject(1..1) { subj ->
                null
            }
            projects(1..1) { auth ->
                [
                        [name: 'testproject'],
                        [name: 'testproject2'],
                ]
            }
        }

        controller.apiService = mockWith(ApiService) {
            requireApi(1..1) { req, resp -> true }
            renderSuccessXml(1..1) { req, resp, clos ->

            }
        }

        response.format='text'
        controller.apiProjectList()
        assert response.status==HttpServletResponse.SC_OK

    }

    @Test
    void apiProjectGet_missingProjectParam(){
        controller.frameworkService = mockWith(FrameworkService){
            getAuthContextForSubject(1..1){subj->
                null
            }
        }

        controller.apiService = mockWith(ApiService) {
            requireApi(1..1) { req, resp -> true }
            renderErrorFormat(1..1) { resp, map ->
                assertEquals(HttpServletResponse.SC_BAD_REQUEST,map.status)
                assertEquals('api.error.parameter.required',map.code)
                resp.status=map.status
            }
        }

        response.format='xml'
        controller.apiProjectGet()
        assert response.status==HttpServletResponse.SC_BAD_REQUEST
    }
    @Test
    void apiProjectGet_unauthorized(){
        controller.frameworkService = mockWith(FrameworkService){
            getAuthContextForSubject(1..1){subj->
                null
            }
            authResourceForProject(1..1){ name->
                assertEquals('test1',name)
            }
            authorizeApplicationResourceAny(1..1){ AuthContext authContext, Map resource, Collection actions->
                assertTrue(AuthConstants.ACTION_READ in actions)
                assertTrue(AuthConstants.ACTION_ADMIN in actions)
                false
            }
        }

        controller.apiService = mockWith(ApiService) {
            requireApi(1..1) { req, resp -> true }
            renderErrorFormat(1..1) { resp, map ->
                assertEquals(HttpServletResponse.SC_FORBIDDEN,map.status)
                assertEquals('api.error.item.unauthorized',map.code)
                resp.status=map.status
            }
        }

        response.format='xml'
        params.project='test1'
        controller.apiProjectGet()
        assert response.status==HttpServletResponse.SC_FORBIDDEN
    }
    @Test
    void apiProjectGet_notfound(){
        controller.frameworkService = mockWith(FrameworkService){
            getAuthContextForSubject(1..1){subj->
                null
            }
            authResourceForProject(1..1) { name ->
                assertEquals('test1', name)
            }
            authorizeApplicationResourceAny(1..1){ AuthContext authContext, Map resource, Collection actions->
                assertTrue(AuthConstants.ACTION_READ in actions)
                assertTrue(AuthConstants.ACTION_ADMIN in actions)
                true
            }
            existsFrameworkProject(1..1) { proj ->
                assertEquals('test1',proj)
                false
            }
        }

        controller.apiService = mockWith(ApiService) {
            requireApi(1..1) { req, resp -> true }
            renderErrorFormat(1..1) { resp, map ->
                assertEquals(HttpServletResponse.SC_NOT_FOUND,map.status)
                assertEquals('api.error.item.doesnotexist',map.code)
                resp.status=map.status
            }
        }

        response.format='xml'
        params.project='test1'
        controller.apiProjectGet()
        assert response.status==HttpServletResponse.SC_NOT_FOUND
    }

    private Object createFrameworkService(boolean configAuth, String projectName, LinkedHashMap<String,
    String> projectProperties=[:]) {
        mockWith(FrameworkService) {
            getAuthContextForSubject(1..1) { subj ->
                null
            }
            authResourceForProject(1..1) { name ->
                assertEquals(projectName, name)
            }
            def count = 0
            authorizeApplicationResourceAny(1..1) { AuthContext authContext, Map resource, Collection actions ->
                count++
                if (count == 1) {
                    assertTrue(AuthConstants.ACTION_READ in actions)
                    assertTrue(AuthConstants.ACTION_ADMIN in actions)
                    return true
                } else {
                    assertTrue(AuthConstants.ACTION_CONFIGURE in actions)
                    assertTrue(AuthConstants.ACTION_ADMIN in actions)
                    return configAuth
                }
            }
            existsFrameworkProject(1..1) { proj ->
                assertEquals(projectName, proj)
                true
            }
            authResourceForProject(1..1) { name ->
                assertEquals(projectName, name)
            }
            authorizeApplicationResourceAny(1..1) { AuthContext authContext, Map resource, Collection actions ->
                count++
                if (count == 1) {
                    assertTrue(AuthConstants.ACTION_READ in actions)
                    assertTrue(AuthConstants.ACTION_ADMIN in actions)
                    return true
                } else {
                    assertTrue(AuthConstants.ACTION_CONFIGURE in actions)
                    assertTrue(AuthConstants.ACTION_ADMIN in actions)
                    return configAuth
                }
            }
            getFrameworkProject(1..1) { String name ->
                assertEquals(projectName, name)
                [name: projectName]
            }
            if(configAuth){
                loadProjectProperties(1..1){pject->
                    assertEquals([name:projectName],pject)
                    projectProperties
                }
            }
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
        controller.apiService.messageSource= mockWith(MessageSource) {
            getMessage{code,args,locale->
                code
            }
        }
        controller.frameworkService = mockFrameworkServiceForProjectCreate(false, true, null, null, null)

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
    @Test
    void apiProjectCreate_json_unauthorized() {
        defineBeans {
            apiService(ApiService)
        }
        mockCodec(JSONCodec)
        controller.apiService.messageSource= mockWith(MessageSource) {
            getMessage{code,args,locale->
                code
            }
        }
        controller.frameworkService = mockFrameworkServiceForProjectCreate(false, true, null, null, null)

        request.json='{"name":"test1"}'
        request.method='POST'
        response.format = 'json'
        request.setAttribute('api_version', 11) // require version 11
        controller.apiProjectCreate()
        assert response.status == HttpServletResponse.SC_FORBIDDEN

        def result = response.json

        //test project element
        assertEquals true, result.error
        assertEquals 'api.error.item.unauthorized', result.errorCode
        assertEquals 'api.error.item.unauthorized', result.message
    }
    /**
     * Missing project name element
     */
    @Test
    void apiProjectCreate_xml_invalid() {
        defineBeans {
            apiService(ApiService)
        }
        controller.apiService.messageSource= mockWith(MessageSource) {
            getMessage{code,args,locale->
                code
            }
        }
        controller.frameworkService = mockFrameworkServiceForProjectCreate(true, true, null, null, null)

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
    }/**
     * Missing project name element
     */
    @Test
    void apiProjectCreate_json_invalid() {
        
        controller.apiService = new ApiService()
        mockCodec(JSONCodec)
        controller.apiService.messageSource= mockWith(MessageSource) {
            getMessage{code,args,locale->
                code
            }
        }

        controller.frameworkService = mockFrameworkServiceForProjectCreate(true, true, null, null, null)

        request.json='{"blame":"monkey"}'
        request.method='POST'
        response.format = 'json'
        request.setAttribute('api_version', 11) // require version 11
        controller.apiProjectCreate()
        assert response.status == HttpServletResponse.SC_BAD_REQUEST

        def result = response.json

        //test project element
        assertEquals true, result.error
        assertEquals 'api.error.invalid.request', result.errorCode
        assertEquals 'api.error.invalid.request', result.message
    }
    /**
     * project already exists
     */
    @Test
    void apiProjectCreate_xml_projectExists() {
        controller.apiService = new ApiService()
        controller.apiService.messageSource= mockWith(MessageSource){
            getMessage{code,args,locale->
                code
            }
        }

        controller.frameworkService = mockFrameworkServiceForProjectCreate(true, true, null, null, null)

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
    void apiProjectCreate_json_projectExists() {
        
        controller.apiService = new ApiService()
        mockCodec(JSONCodec)
        controller.apiService.messageSource= mockWith(MessageSource){
            getMessage{code,args,locale->
                code
            }
        }

        controller.frameworkService = mockFrameworkServiceForProjectCreate(true, true, null, null, null)

        request.json = '{"name":"test1"}'
        request.method='POST'
        response.format = 'json'
        request.setAttribute('api_version', 11) // require version 11
        controller.apiProjectCreate()
        assert response.status == HttpServletResponse.SC_CONFLICT

        def result = response.json

        //test project element
        assertEquals true, result.error
        assertEquals 'api.error.item.alreadyexists', result.errorCode
        assertEquals 'api.error.item.alreadyexists', result.message
    }
    /**
     * Failure to create project
     */
    @Test
    void apiProjectCreate_xml_withErrors() {
        controller.apiService = new ApiService()
        controller.apiService.messageSource= mockWith(MessageSource){
            getMessage{code,args,locale->
                code
            }
        }
        controller.frameworkService = mockFrameworkServiceForProjectCreate(true, false, ['error1', 'error2'], [:], null)

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
     * Failure to create project
     */
    @Test
    void apiProjectCreate_json_withErrors() {
        controller.apiService = new ApiService()
        mockCodec(JSONCodec)
        controller.apiService.messageSource= mockWith(MessageSource){
            getMessage{code,args,locale->
                code
            }
        }
        controller.frameworkService = mockFrameworkServiceForProjectCreate(true, false, ['error1','error2'], [:], null)

        request.json = '{"name":"test1"}'
        request.method='POST'
        response.format = 'json'
        request.setAttribute('api_version', 11) // require version 11
        controller.apiProjectCreate()
        assert response.status == HttpServletResponse.SC_INTERNAL_SERVER_ERROR

        def result = response.json

        //test project element
        assertEquals true, result.error
        assertEquals null, result.errorCode
        assertEquals 'error1; error2', result.message
    }
    /**
     * Successful
     */
    @Test
    void apiProjectCreate_xml_success() {
        controller.apiService = new ApiService()
        controller.apiService.messageSource= mockWith(MessageSource){
            getMessage{code,args,locale->
                code
            }
        }
        controller.frameworkService = mockFrameworkServiceForProjectCreate(true, false, [],
                [:], ['prop1': 'value1', 'prop2': 'value2'])

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
     * Successful
     */
    @Test
    void apiProjectCreate_json_success() {
        controller.apiService = new ApiService()
        mockCodec(JSONCodec)
        controller.apiService.messageSource= mockWith(MessageSource){
            getMessage{code,args,locale->
                code
            }
        }
        controller.frameworkService = mockFrameworkServiceForProjectCreate(true, false, [],
                [:], ['prop1': 'value1', 'prop2': 'value2'])

        request.json = '{"name":"test1"}'
        request.method='POST'
        response.format = 'json'
        request.setAttribute('api_version', 11) // require version 11
        controller.apiProjectCreate()
        assert response.status == HttpServletResponse.SC_CREATED

        def result = response.json

        //test project element
        assertEquals null,result.error
        def project =result
        assertEquals "test1",project.name
        assertEquals 2, project.config.size()
        assertEquals 'value1', project.config['prop1']
        assertEquals 'value2', project.config['prop2']
    }
    /**
     * Create project with input config
     */
    @Test
    void apiProjectCreate_xml_withconfig() {
        controller.apiService = new ApiService()
        controller.apiService.messageSource= mockWith(MessageSource){
            getMessage{code,args,locale->
                code
            }
        }
        controller.frameworkService = mockFrameworkServiceForProjectCreate(true, false, [],
                ['input1': 'value1', 'input2': 'value2'], ['prop1': 'value1', 'prop2': 'value2'])

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
    /**
     * Create project with input config
     */
    @Test
    void apiProjectCreate_json_withconfig() {
        controller.apiService = new ApiService()
        mockCodec(JSONCodec)
        controller.apiService.messageSource= mockWith(MessageSource){ getMessage {code,args,locale-> code } }
        controller.frameworkService= mockFrameworkServiceForProjectCreate(true, false, [],
                ['input1': 'value1', 'input2': 'value2'], ['prop1': 'value1', 'prop2': 'value2'])

        request.json = '{"name":"test1","config": { "input1":"value1","input2":"value2" } }'
        request.method='POST'
        response.format = 'json'
        request.setAttribute('api_version', 11) // require version 11
        controller.apiProjectCreate()
        assert response.status == HttpServletResponse.SC_CREATED

        def result = response.json

        //test project element
        assertEquals null, result.error
        def project = result
        assertEquals "test1", project.name
        assertEquals 2, project.config.size()
        assertEquals 'value1', project.config['prop1']
        assertEquals 'value2', project.config['prop2']
    }

    private Object mockFrameworkServiceForProjectCreate(boolean authorized, boolean exists, ArrayList createErrors,
                                                       LinkedHashMap<String, String> inputProps,
                                                       LinkedHashMap<String, String> configProps) {
        mockWith(FrameworkService) {
            getAuthContextForSubject(1..1) { subject -> null }
            authorizeApplicationResourceTypeAll { auth, type, actions ->
                assert "project" == type
                assert ['create'] == actions
                authorized
            }
            if(!authorized){
                return
            }
            existsFrameworkProject { name ->
                assert "test1" == name
                exists
            }
            if(exists){
                return
            }
            createFrameworkProject { name, props ->
                assertEquals 'test1', name
                assertEquals(inputProps, props)
                [createErrors.size() > 0 ? null: [name: 'test1'], createErrors]
            }
            if(createErrors.size()>0){
                return
            }
            loadProjectProperties { proj ->
                assertEquals 'test1', proj.name
                configProps
            }
        }
    }

    @Test
    void deleteProject_apiversion(){
        controller.apiService = new ApiService()
        controller.apiService.messageSource = mockWith(MessageSource) { getMessage { code, args, locale -> code } }
        request.method = 'DELETE'
        request.setAttribute('api_version', 10) // require version 11
        controller.apiProjectDelete()
        assert response.status == HttpServletResponse.SC_BAD_REQUEST
        assertEquals "true",response.xml.'@error'.text()
        assertEquals "api.error.api-version.unsupported",response.xml.error.'@code'.text()
    }
    @Test
    void deleteProject_xml_missingparam(){
        controller.apiService = new ApiService()
        controller.apiService.messageSource = mockWith(MessageSource) { getMessage { code, args, locale -> code } }
        request.method = 'DELETE'
        request.setAttribute('api_version', 11) // require version 11
        controller.apiProjectDelete()
        assert response.status == HttpServletResponse.SC_BAD_REQUEST
        assertEquals "true", response.xml.'@error'.text()
        assertEquals "api.error.parameter.required", response.xml.error.'@code'.text()
    }
    @Test
    void deleteProject_json_missingparam(){
        controller.apiService = new ApiService()
        controller.apiService.messageSource = mockWith(MessageSource) { getMessage { code, args, locale -> code } }
        mockCodec(JSONCodec)
        request.method = 'DELETE'
        response.format='json'
        request.setAttribute('api_version', 11) // require version 11
        controller.apiProjectDelete()
        assert response.status == HttpServletResponse.SC_BAD_REQUEST
        assertEquals true, response.json.error
        assertEquals "api.error.parameter.required", response.json.errorCode
    }
    @Test
    void deleteProject_xml_notfound(){
        controller.apiService = new ApiService()
        controller.apiService.messageSource = mockWith(MessageSource) { getMessage { code, args, locale -> code } }
        mockCodec(JSONCodec)
        controller.frameworkService=mockFrameworkServiceForProjectDelete(false, false)
        request.method = 'DELETE'
        response.format='xml'
        params.project='test1'
        request.setAttribute('api_version', 11) // require version 11
        controller.apiProjectDelete()
        assert response.status == HttpServletResponse.SC_NOT_FOUND
        assertEquals "true", response.xml.'@error'.text()
        assertEquals "api.error.item.doesnotexist", response.xml.error.'@code'.text()
    }
    @Test
    void deleteProject_json_notfound(){
        controller.apiService = new ApiService()
        controller.apiService.messageSource = mockWith(MessageSource) { getMessage { code, args, locale -> code } }
        mockCodec(JSONCodec)
        controller.frameworkService=mockFrameworkServiceForProjectDelete(false, false)
        request.method = 'DELETE'
        response.format='json'
        params.project='test1'
        request.setAttribute('api_version', 11) // require version 11
        controller.apiProjectDelete()
        assert response.status == HttpServletResponse.SC_NOT_FOUND
        assertEquals true, response.json.error
        assertEquals "api.error.item.doesnotexist", response.json.errorCode
    }
    @Test
    void deleteProject_xml_unauthorized(){
        controller.apiService = new ApiService()
        controller.apiService.messageSource = mockWith(MessageSource) { getMessage { code, args, locale -> code } }
        mockCodec(JSONCodec)
        controller.frameworkService=mockFrameworkServiceForProjectDelete(true, false)
        request.method = 'DELETE'
        response.format='xml'
        params.project='test1'
        request.setAttribute('api_version', 11) // require version 11
        controller.apiProjectDelete()
        assert response.status == HttpServletResponse.SC_FORBIDDEN
        assertEquals "true", response.xml.'@error'.text()
        assertEquals "api.error.item.unauthorized", response.xml.error.'@code'.text()
    }
    @Test
    void deleteProject_json_unauthorized(){
        controller.apiService = new ApiService()
        controller.apiService.messageSource = mockWith(MessageSource) { getMessage { code, args, locale -> code } }
        mockCodec(JSONCodec)
        controller.frameworkService=mockFrameworkServiceForProjectDelete(true, false)
        request.method = 'DELETE'
        response.format='json'
        params.project='test1'
        request.setAttribute('api_version', 11) // require version 11
        controller.apiProjectDelete()
        assert response.status == HttpServletResponse.SC_FORBIDDEN
        assertEquals true, response.json.error
        assertEquals "api.error.item.unauthorized", response.json.errorCode
    }
    @Test
    void deleteProject_xml_haserrors(){
        controller.apiService = new ApiService()
        controller.apiService.messageSource = mockWith(MessageSource) { getMessage { code, args, locale -> code } }
        mockCodec(JSONCodec)
        controller.frameworkService=mockFrameworkServiceForProjectDelete(true, true)
        controller.projectService=mockProjectServiceForProjectDelete(false, 'deleteProjectFailed')
        request.method = 'DELETE'
        response.format='xml'
        params.project='test1'
        request.setAttribute('api_version', 11) // require version 11
        controller.apiProjectDelete()
        assert response.status == HttpServletResponse.SC_INTERNAL_SERVER_ERROR
        assertEquals "true", response.xml.'@error'.text()
        assertEquals "deleteProjectFailed", response.xml.error.message.text()
    }


    @Test
    void deleteProject_json_haserrors(){
        controller.apiService = new ApiService()
        controller.apiService.messageSource = mockWith(MessageSource) { getMessage { code, args, locale -> code } }
        mockCodec(JSONCodec)
        controller.frameworkService=mockFrameworkServiceForProjectDelete(true, true)
        controller.projectService = mockProjectServiceForProjectDelete(false, 'deleteProjectFailed')
        request.method = 'DELETE'
        response.format='json'
        params.project='test1'
        request.setAttribute('api_version', 11) // require version 11
        controller.apiProjectDelete()
        assert response.status == HttpServletResponse.SC_INTERNAL_SERVER_ERROR
        assertEquals true, response.json.error
        assertEquals "deleteProjectFailed", response.json.message
    }
    @Test
    void deleteProject_xml_success(){
        controller.apiService = new ApiService()
        controller.apiService.messageSource = mockWith(MessageSource) { getMessage { code, args, locale -> code } }
        mockCodec(JSONCodec)
        controller.frameworkService=mockFrameworkServiceForProjectDelete(true, true)
        controller.projectService=mockProjectServiceForProjectDelete(true, null)
        request.method = 'DELETE'
        response.format='xml'
        params.project='test1'
        request.setAttribute('api_version', 11) // require version 11
        controller.apiProjectDelete()
        assert response.status == HttpServletResponse.SC_NO_CONTENT
    }


    @Test
    void deleteProject_json_success(){
        controller.apiService = new ApiService()
        controller.apiService.messageSource = mockWith(MessageSource) { getMessage { code, args, locale -> code } }
        mockCodec(JSONCodec)
        controller.frameworkService=mockFrameworkServiceForProjectDelete(true, true)
        controller.projectService = mockProjectServiceForProjectDelete(true, null)
        request.method = 'DELETE'
        response.format='json'
        params.project='test1'
        request.setAttribute('api_version', 11) // require version 11
        controller.apiProjectDelete()
        assert response.status == HttpServletResponse.SC_NO_CONTENT
    }

    def mockProjectServiceForProjectDelete(boolean success, String errorMessage) {
        mockWith(ProjectService) {
            deleteProject { proj, fwk,authctx,user ->
                return [success: success, error: errorMessage]
            }
        }
    }

    private def mockFrameworkServiceForProjectDelete(boolean exists, boolean authorized){
        mockWith(FrameworkService){
            getRundeckFramework{->
                null
            }
            existsFrameworkProject{String name->
                exists
            }
            if(!exists){
                return
            }
            getAuthContextForSubject{subj->
                null
            }
            authResourceForProject(1..1) { name ->
                assertEquals("test1", name)
            }
            authorizeApplicationResourceAny{ctx,resource,actions->
                assertTrue(AuthConstants.ACTION_DELETE in actions)
                assertTrue(AuthConstants.ACTION_ADMIN in actions)
                authorized
            }
            if(!authorized){
                return
            }
            getFrameworkProject{name->
                assertEquals('test1',name)
                [name:name]
            }
        }
    }
    private def mockFrameworkServiceForProjectConfigGet(boolean exists, boolean authorized, String action,
                                                        LinkedHashMap props, String textformat=null){
        mockWith(FrameworkService){
            existsFrameworkProject{String name->
                exists
            }
            if(!exists){
                return
            }
            getAuthContextForSubject{subj->
                null
            }
            authResourceForProject(1..1) { name ->
                assertEquals("test1", name)
            }
            authorizeApplicationResourceAny{ctx,resource,actions->
                assertTrue(action in actions)
                assertTrue(AuthConstants.ACTION_ADMIN in actions)
                authorized
            }
            if(!authorized){
                return
            }
            getFrameworkProject{name->
                assertEquals('test1',name)
                [name:name,propertyFile:[text:textformat]]
            }
            loadProjectProperties{proj->
                props
            }
        }
    }
    private def mockFrameworkServiceForProjectConfigPut(boolean exists, boolean authorized, String action,
                                                        LinkedHashMap props, boolean success, String errorMessage,
                                                        String propFileText){
        mockWith(FrameworkService){
            existsFrameworkProject{String name->
                exists
            }
            if(!exists){
                return
            }
            getAuthContextForSubject{subj->
                null
            }
            authResourceForProject(1..1) { name ->
                assertEquals("test1", name)
            }
            authorizeApplicationResourceAny{ctx,resource,actions->
                assertTrue(action in actions)
                assertTrue(AuthConstants.ACTION_ADMIN in actions)
                authorized
            }
            if(!authorized){
                return
            }
            getFrameworkProject{name->
                assertEquals('test1',name)
                [name:name,propertyFile: [text: propFileText]]
            }
            setFrameworkProjectConfig{proj,configProps->
                assertEquals props,configProps
                [success: success,error: errorMessage]
            }
            if(!success){
                return
            }
            loadProjectProperties { proj ->
                props
            }
        }
    }
    private def mockFrameworkServiceForProjectConfigKeyPut(boolean exists, boolean authorized, String action,
                                                        LinkedHashMap props, boolean success, String errorMessage,
                                                        String propFileText){
        mockWith(FrameworkService){
            existsFrameworkProject{String name->
                exists
            }
            if(!exists){
                return
            }
            getAuthContextForSubject{subj->
                null
            }
            authResourceForProject(1..1) { name ->
                assertEquals("test1", name)
            }
            authorizeApplicationResourceAny{ctx,resource,actions->
                assertTrue(action in actions)
                assertTrue(AuthConstants.ACTION_ADMIN in actions)
                authorized
            }
            if(!authorized){
                return
            }
            getFrameworkProject{name->
                assertEquals('test1',name)
                [name:name,propertyFile: [text: propFileText]]
            }
            updateFrameworkProjectConfig{proj,configProps, prefixes->
                assertEquals props,configProps
                [success: success,error: errorMessage]
            }
            if(!success){
                return
            }
            loadProjectProperties { proj ->
                props
            }
        }
    }
    private def mockFrameworkServiceForProjectConfigKeyDelete(boolean exists, boolean authorized, String action,
                                                        String propname, boolean success, String errorMessage,
                                                        String propFileText){
        mockWith(FrameworkService){
            existsFrameworkProject{String name->
                exists
            }
            if(!exists){
                return
            }
            getAuthContextForSubject{subj->
                null
            }
            authResourceForProject(1..1) { name ->
                assertEquals("test1", name)
            }
            authorizeApplicationResourceAny{ctx,resource,actions->
                assertTrue(action in actions)
                assertTrue(AuthConstants.ACTION_ADMIN in actions)
                authorized
            }
            if(!authorized){
                return
            }
            getFrameworkProject{name->
                assertEquals('test1',name)
                [name:name,propertyFile: [text: propFileText]]
            }
            removeFrameworkProjectConfigProperties{proj,removeSet->
                assertEquals ([propname] as Set,removeSet)
                [success: success,error: errorMessage]
            }
        }
    }
    private def mockFrameworkServiceForProjectExport(boolean exists, boolean authorized, String action){
        mockWith(FrameworkService){
            existsFrameworkProject{String name->
                exists
            }
            if(!exists){
                return
            }
            getAuthContextForSubject{subj->
                null
            }
            authResourceForProject(1..1) { name ->
                assertEquals("test1", name)
            }
            authorizeApplicationResourceAny{ctx,resource,actions->
                assertTrue(action in actions)
                assertTrue(AuthConstants.ACTION_ADMIN in actions)
                authorized
            }
            if(!authorized){
                return
            }
            getFrameworkProject{name->
                assertEquals('test1',name)
                [name:name]
            }
            getRundeckFramework{->
                null
            }
            getAuthContextForSubject{subj->

            }
        }
    }


    @Test
    void apiProjectConfigGet_apiversion(){
        controller.apiService = new ApiService()
        controller.apiService.messageSource = mockWith(MessageSource) { getMessage { code, args, locale -> code } }
        controller.frameworkService= mockFrameworkServiceForProjectConfigGet(false, false, 'read', [:])
        request.api_version=10
        controller.apiProjectConfigGet()
        assertEquals HttpServletResponse.SC_BAD_REQUEST,response.status
        assertEquals "true", response.xml.'@error'.text()
        assertEquals "api.error.api-version.unsupported", response.xml.error.message.text()
    }
    @Test
    void apiProjectConfigGet_xml_missingparam(){
        controller.apiService = new ApiService()
        controller.apiService.messageSource = mockWith(MessageSource) { getMessage { code, args, locale -> code } }
        controller.frameworkService= mockFrameworkServiceForProjectConfigGet(false, false, 'read', [:])
        request.api_version = 11
        controller.apiProjectConfigGet()
        assertXmlError(response, HttpServletResponse.SC_BAD_REQUEST, "api.error.parameter.required")
    }

    private void assertXmlError(GrailsMockHttpServletResponse response, int status, String code) {
        assertEquals status, response.status
        assertEquals "true", response.xml.'@error'.text()
        assertEquals code, response.xml.error.message.text()
    }

    @Test
    void apiProjectConfigGet_json_missingparam(){
        controller.apiService = new ApiService()
        mockCodec(JSONCodec)
        controller.apiService.messageSource = mockWith(MessageSource) { getMessage { code, args, locale -> code } }
        controller.frameworkService= mockFrameworkServiceForProjectConfigGet(false, false, 'read', [:])
        request.api_version = 11
        response.format='json'
        controller.apiProjectConfigGet()
        assertEquals HttpServletResponse.SC_BAD_REQUEST, response.status
        assertEquals true, response.json.error
        assertEquals "api.error.parameter.required", response.json.errorCode
    }
    @Test
    void apiProjectConfigGet_xml_notfound(){
        controller.apiService = new ApiService()
        controller.apiService.messageSource = mockWith(MessageSource) { getMessage { code, args, locale -> code } }
        controller.frameworkService= mockFrameworkServiceForProjectConfigGet(false, false, 'read', [:])
        request.api_version = 11
        params.project='test1'
        controller.apiProjectConfigGet()
        assertEquals HttpServletResponse.SC_NOT_FOUND, response.status
        assertEquals "true", response.xml.'@error'.text()
        assertEquals "api.error.item.doesnotexist", response.xml.error.message.text()
    }
    @Test
    void apiProjectConfigGet_json_notfound(){
        controller.apiService = new ApiService()
        mockCodec(JSONCodec)
        controller.apiService.messageSource = mockWith(MessageSource) { getMessage { code, args, locale -> code } }
        controller.frameworkService= mockFrameworkServiceForProjectConfigGet(false, false, 'read', [:])
        request.api_version = 11
        params.project = 'test1'
        response.format='json'
        controller.apiProjectConfigGet()
        assertEquals HttpServletResponse.SC_NOT_FOUND, response.status
        assertEquals true, response.json.error
        assertEquals "api.error.item.doesnotexist", response.json.errorCode
    }
    @Test
    void apiProjectConfigGet_xml_unauthorized(){
        controller.apiService = new ApiService()
        controller.apiService.messageSource = mockWith(MessageSource) { getMessage { code, args, locale -> code } }
        controller.frameworkService= mockFrameworkServiceForProjectConfigGet(true, false, 'configure', [:])
        request.api_version = 11
        params.project='test1'
        controller.apiProjectConfigGet()
        assertEquals HttpServletResponse.SC_FORBIDDEN, response.status
        assertEquals "true", response.xml.'@error'.text()
        assertEquals "api.error.item.unauthorized", response.xml.error.message.text()
    }
    @Test
    void apiProjectConfigGet_json_unauthorized(){
        controller.apiService = new ApiService()
        mockCodec(JSONCodec)
        controller.apiService.messageSource = mockWith(MessageSource) { getMessage { code, args, locale -> code } }
        controller.frameworkService= mockFrameworkServiceForProjectConfigGet(true, false, 'configure', [:])
        request.api_version = 11
        params.project = 'test1'
        response.format='json'
        controller.apiProjectConfigGet()
        assertEquals HttpServletResponse.SC_FORBIDDEN, response.status
        assertEquals true, response.json.error
        assertEquals "api.error.item.unauthorized", response.json.errorCode
    }
    @Test
    void apiProjectConfigGet_xml_success(){
        controller.apiService = new ApiService()
        controller.apiService.messageSource = mockWith(MessageSource) { getMessage { code, args, locale -> code } }
        controller.frameworkService= mockFrameworkServiceForProjectConfigGet(true, true, 'configure', ["prop1": "value1", "prop2": "value2"])
        request.api_version = 11
        params.project='test1'
        controller.apiProjectConfigGet()
        assertEquals HttpServletResponse.SC_OK, response.status
        assertEquals "config",response.xml.name()
        assertEquals 2,response.xml.property.size()
        assertEquals 'prop1',response.xml.property[0].'@key'.text()
        assertEquals 'value1',response.xml.property[0].'@value'.text()
        assertEquals 'prop2',response.xml.property[1].'@key'.text()
        assertEquals 'value2',response.xml.property[1].'@value'.text()
    }
    @Test
    void apiProjectConfigGet_json_success(){
        controller.apiService = new ApiService()
        mockCodec(JSONCodec)
        controller.apiService.messageSource = mockWith(MessageSource) { getMessage { code, args, locale -> code } }
        controller.frameworkService= mockFrameworkServiceForProjectConfigGet(true, true, 'configure', ["prop1": "value1", "prop2": "value2"])
        request.api_version = 11
        params.project = 'test1'
        response.format='json'
        controller.apiProjectConfigGet()
        assertEquals HttpServletResponse.SC_OK, response.status
        assertEquals "value1",response.json['prop1']
        assertEquals "value2",response.json['prop2']
    }
    @Test
    void apiProjectConfigGet_text_success(){
        controller.apiService = new ApiService()
        mockCodec(JSONCodec)
        controller.apiService.messageSource = mockWith(MessageSource) { getMessage { code, args, locale -> code } }
        controller.frameworkService= mockFrameworkServiceForProjectConfigGet(true, true, 'configure', [:],
                "text format for properties")
        request.api_version = 11
        params.project = 'test1'
        response.format='text'
        controller.apiProjectConfigGet()
        assertEquals HttpServletResponse.SC_OK, response.status
        assertEquals "text format for properties",response.text
    }

    @Test
    void apiProjectConfigPut_xml_success(){
        //controller.apiService.messageSource = mockWith(MessageSource) { getMessage { code, args, locale -> code } }
        controller.apiService = new ApiService()
        controller.frameworkService= mockFrameworkServiceForProjectConfigPut(true, true, 'configure', ['prop1': 'value1',
                prop2: 'value2'], true, null, 'text')
        request.api_version = 11
        params.project = 'test1'
        request.method='PUT'
        request.xml='<config><property key="prop1" value="value1"/><property key="prop2" value="value2"/></config>'
        controller.apiProjectConfigPut()
        assertEquals HttpServletResponse.SC_OK, response.status
        assertEquals "config", response.xml.name()
        assertEquals 2, response.xml.property.size()
        assertEquals 'prop1', response.xml.property[0].'@key'.text()
        assertEquals 'value1', response.xml.property[0].'@value'.text()
        assertEquals 'prop2', response.xml.property[1].'@key'.text()
        assertEquals 'value2', response.xml.property[1].'@value'.text()
    }
    @Test
    void apiProjectConfigPut_json_success(){
        controller.apiService = new ApiService()
        mockCodec(JSONCodec)
        controller.apiService.messageSource = mockWith(MessageSource) { getMessage { code, args, locale -> code } }
        controller.frameworkService= mockFrameworkServiceForProjectConfigPut(true, true, 'configure', ['prop1': 'value1',
                prop2: 'value2'], true, null, 'text')
        request.api_version = 11
        params.project = 'test1'
        request.json='{"prop1" :"value1","prop2":"value2"}'
        request.method='PUT'
        controller.apiProjectConfigPut()
        assertEquals HttpServletResponse.SC_OK, response.status
        assertEquals 'value1', response.json['prop1']
        assertEquals 'value2', response.json['prop2']
    }
    @Test
    void apiProjectConfigPut_text_success(){
        controller.apiService = new ApiService()
        mockCodec(JSONCodec)
        controller.apiService.messageSource = mockWith(MessageSource) { getMessage { code, args, locale -> code } }
        controller.frameworkService= mockFrameworkServiceForProjectConfigPut(true, true, 'configure', ['prop1': 'value1',
                prop2: 'value2'], true, null, 'prop1=value1\nprop2=value2')
        request.api_version = 11
        params.project = 'test1'
        request.content='prop1=value1\nprop2=value2'.bytes
        request.contentType='text/plain'
        request.method='PUT'
        controller.apiProjectConfigPut()
        assertEquals HttpServletResponse.SC_OK, response.status
        assertTrue response.contentType.contains('text/plain')
        assertEquals 'prop1=value1\nprop2=value2', response.text
    }


    @Test
    void apiProjectConfigKeyGet_xml_success() {
        controller.apiService = new ApiService()
        controller.apiService.messageSource = mockWith(MessageSource) { getMessage { code, args, locale -> code } }
        controller.frameworkService = mockFrameworkServiceForProjectConfigGet(true, true, 'configure', ["prop1": "value1", "prop2": "value2"])
        request.api_version = 11
        params.project = 'test1'
        params.keypath = 'prop1'
        response.format='xml'
        controller.apiProjectConfigKeyGet()
        assertEquals HttpServletResponse.SC_OK, response.status
        assertEquals "property", response.xml.name()
        assertEquals 'prop1', response.xml.'@key'.text()
        assertEquals 'value1', response.xml.'@value'.text()
    }

    @Test
    void apiProjectConfigKeyGet_json_success() {
        controller.apiService = new ApiService()
        mockCodec(JSONCodec)
        controller.apiService.messageSource = mockWith(MessageSource) { getMessage { code, args, locale -> code } }
        controller.frameworkService = mockFrameworkServiceForProjectConfigGet(true, true, 'configure', ["prop1": "value1", "prop2": "value2"])
        request.api_version = 11
        params.project = 'test1'
        params.keypath = 'prop1'
        response.format='json'
        controller.apiProjectConfigKeyGet()
        assertEquals HttpServletResponse.SC_OK, response.status
        assertEquals 'prop1', response.json.key
        assertEquals 'value1', response.json.value
    }
    @Test
    void apiProjectConfigKeyGet_text_success() {
        controller.apiService = new ApiService()
        controller.apiService.messageSource = mockWith(MessageSource) { getMessage { code, args, locale -> code } }
        controller.frameworkService = mockFrameworkServiceForProjectConfigGet(true, true, 'configure', ["prop1": "value1", "prop2": "value2"])
        request.api_version = 11
        params.project = 'test1'
        params.keypath = 'prop1'
        response.format='text'
        controller.apiProjectConfigKeyGet()
        assertEquals HttpServletResponse.SC_OK, response.status
        assertEquals 'value1', response.text
    }


    @Test
    void apiProjectConfigKeyPut_xml_success() {
        controller.apiService = new ApiService()
        controller.apiService.messageSource = mockWith(MessageSource) { getMessage { code, args, locale -> code } }
        controller.frameworkService = mockFrameworkServiceForProjectConfigKeyPut(true, true, 'configure',
                ["prop1": "value1"],true,null,null)
        request.api_version = 11
        params.project = 'test1'
        params.keypath = 'prop1'
        request.xml='<property key="prop1" value="value1"/>'
        request.method='PUT'
        controller.apiProjectConfigKeyPut()
        assertEquals HttpServletResponse.SC_OK, response.status
        assertEquals "property", response.xml.name()
        assertEquals 'prop1', response.xml.'@key'.text()
        assertEquals 'value1', response.xml.'@value'.text()
    }

    @Test
    void apiProjectConfigKeyPut_json_success() {
        controller.apiService = new ApiService()
        mockCodec(JSONCodec)
        controller.apiService.messageSource = mockWith(MessageSource) { getMessage { code, args, locale -> code } }
        controller.frameworkService = mockFrameworkServiceForProjectConfigKeyPut(true, true, 'configure',
                ["prop1": "value1"],true,null,null)
        request.api_version = 11
        params.project = 'test1'
        params.keypath = 'prop1'
        request.json='{"key":"prop1","value":"value1"}'
        request.method='PUT'
        controller.apiProjectConfigKeyPut()
        assertEquals HttpServletResponse.SC_OK, response.status
        assertEquals 'prop1', response.json.key
        assertEquals 'value1', response.json.value
    }
    @Test
    void apiProjectConfigKeyPut_text_success() {
        controller.apiService = new ApiService()
        mockCodec(JSONCodec)
        controller.apiService.messageSource = mockWith(MessageSource) { getMessage { code, args, locale -> code } }
        controller.frameworkService = mockFrameworkServiceForProjectConfigKeyPut(true, true, 'configure',
                ["prop1": "value1"],true,null,null)
        request.api_version = 11
        params.project = 'test1'
        params.keypath = 'prop1'
        request.content='value1'
        request.contentType='text/plain'
        request.method='PUT'
        controller.apiProjectConfigKeyPut()
        assertEquals HttpServletResponse.SC_OK, response.status
        assertEquals 'value1', response.text
    }

    @Test
    void apiProjectConfigKeyDelete_success() {
        controller.apiService = new ApiService()
        controller.apiService.messageSource = mockWith(MessageSource) { getMessage { code, args, locale -> code } }
        controller.frameworkService = mockFrameworkServiceForProjectConfigKeyDelete(true, true, 'configure',
                'prop1', true, null, null)
        request.api_version = 11
        params.project = 'test1'
        params.keypath = 'prop1'
        request.method='DELETE'
        controller.apiProjectConfigKeyDelete()
        assertEquals HttpServletResponse.SC_NO_CONTENT, response.status
    }
    @Test
    void apiProjectExport_success() {
        controller.apiService = new ApiService()
        controller.apiService.messageSource = mockWith(MessageSource) { getMessage { code, args, locale -> code } }
        controller.frameworkService = mockFrameworkServiceForProjectExport(true, true, 'export')
        controller.projectService=mockWith(ProjectService){
            exportProjectToOutputStream{project,fwk,stream->
                assertEquals 'test1',project.name
                stream<<'some data'
            }
        }
        request.api_version = 11
        params.project = 'test1'
        controller.apiProjectExport()
        assertEquals HttpServletResponse.SC_OK, response.status
        assertEquals 'application/zip', response.contentType
        assertEquals 'some data', response.text

    }
    @Test
    void apiProjectExport_apiversion() {
        controller.apiService = new ApiService()
        controller.apiService.messageSource = mockWith(MessageSource) { getMessage { code, args, locale -> code } }
        controller.frameworkService = mockFrameworkServiceForProjectExport(true, true, 'export')
        controller.projectService=mockWith(ProjectService){
            exportProjectToOutputStream{project,fwk,stream->
                assertEquals 'test1',project.name
                stream<<'some data'
            }
        }
        request.api_version = 10
        params.project = 'test1'
        controller.apiProjectExport()
        assertXmlError(response, HttpServletResponse.SC_BAD_REQUEST,'api.error.api-version.unsupported')
    }
    @Test
    void apiProjectExport_notfound() {
        controller.apiService = new ApiService()
        controller.apiService.messageSource = mockWith(MessageSource) { getMessage { code, args, locale -> code } }
        controller.frameworkService = mockFrameworkServiceForProjectExport(false, true, 'export')
        controller.projectService=mockWith(ProjectService){
            exportProjectToOutputStream{project,fwk,stream->
                assertEquals 'test1',project.name
                stream<<'some data'
            }
        }
        request.api_version = 11
        params.project = 'test1'
        controller.apiProjectExport()
        assertXmlError(response, HttpServletResponse.SC_NOT_FOUND,'api.error.item.doesnotexist')
    }
    @Test
    void apiProjectExport_unauthorized() {
        controller.apiService = new ApiService()
        controller.apiService.messageSource = mockWith(MessageSource) { getMessage { code, args, locale -> code } }
        controller.frameworkService = mockFrameworkServiceForProjectExport(true, false, 'export')
        controller.projectService=mockWith(ProjectService){
            exportProjectToOutputStream{project,fwk,stream->
                assertEquals 'test1',project.name
                stream<<'some data'
            }
        }
        request.api_version = 11
        params.project = 'test1'
        controller.apiProjectExport()
        assertXmlError(response, HttpServletResponse.SC_FORBIDDEN,'api.error.item.unauthorized')
    }
    @Test
    void apiProjectImport_notfound() {
        controller.apiService = new ApiService()
        controller.apiService.messageSource = mockWith(MessageSource) { getMessage { code, args, locale -> code } }
        controller.frameworkService = mockFrameworkServiceForProjectExport(false, true, 'import')
        request.api_version = 11
        params.project = 'test1'
        request.format='blah'
        request.method='PUT'
        controller.apiProjectImport()
        assertXmlError(response, HttpServletResponse.SC_NOT_FOUND, "api.error.item.doesnotexist")
    }
    @Test
    void apiProjectImport_unauthorized() {
        controller.apiService = new ApiService()
        controller.apiService.messageSource = mockWith(MessageSource) { getMessage { code, args, locale -> code } }
        controller.frameworkService = mockFrameworkServiceForProjectExport(true, false, 'import')
        request.api_version = 11
        params.project = 'test1'
        request.format='blah'
        request.method='PUT'
        controller.apiProjectImport()
        assertXmlError(response, HttpServletResponse.SC_FORBIDDEN, "api.error.item.unauthorized")
    }
    @Test
    void apiProjectImport_invalidFormat() {
        controller.apiService = new ApiService()
        controller.apiService.messageSource = mockWith(MessageSource) { getMessage { code, args, locale -> code } }
        controller.frameworkService = mockFrameworkServiceForProjectExport(true, true, 'import')
        request.api_version = 11
        params.project = 'test1'
        request.format='blah'
        request.method='PUT'
        controller.apiProjectImport()
        assertXmlError(response, HttpServletResponse.SC_BAD_REQUEST, "api.error.invalid.request")
    }
    @Test
    void apiProjectImport_xml_failure() {
        controller.apiService = new ApiService()
        controller.apiService.messageSource = mockWith(MessageSource) { getMessage { code, args, locale -> code } }
        controller.frameworkService = mockFrameworkServiceForProjectExport(true, true, 'import')
        controller.projectService=mockWith(ProjectService){
            importToProject{  project, String user, String roleList,  framework,
                             AuthContext authContext,  InputStream stream, Map options->
                assertEquals('user1',user)
                assertTrue(roleList in ['groupa,groupb', 'groupb,groupa'])
                [success:false,joberrors:['error1','error2']]
            }
        }
        request.api_version = 11
        params.project = 'test1'
        request.format='application/zip'
        response.format='xml'
        request.subject = new Subject(false,[new Username('user1'),new Group('groupa'), new Group('groupb')] as Set,
                [] as Set, [] as Set)
        session.user='user1'
        request.method='PUT'
        controller.apiProjectImport()
        assertEquals HttpServletResponse.SC_OK,response.status
        assertEquals 'failed',response.xml.'@status'.text()
        assertEquals '2',response.xml.errors.'@count'.text()
        assertEquals 2,response.xml.errors.error.size()
        assertEquals 'error1',response.xml.errors.error[0].text()
        assertEquals 'error2',response.xml.errors.error[1].text()
    }
    @Test
    void apiProjectImport_json_failure() {
        controller.apiService = new ApiService()
        controller.apiService.messageSource = mockWith(MessageSource) { getMessage { code, args, locale -> code } }
        controller.frameworkService = mockFrameworkServiceForProjectExport(true, true, 'import')
        controller.projectService=mockWith(ProjectService){
            importToProject{  project, String user, String roleList,  framework,
                             AuthContext authContext,  InputStream stream, Map options->
                assertEquals('user1',user)
                assertTrue(roleList in ['groupa,groupb', 'groupb,groupa'])
                [success:false,joberrors:['error1','error2']]
            }
        }
        request.api_version = 11
        params.project = 'test1'
        request.format='application/zip'
        response.format='json'
        request.subject = new Subject(false,[new Username('user1'),new Group('groupa'), new Group('groupb')] as Set,
                [] as Set, [] as Set)
        session.user='user1'
        request.method='PUT'
        controller.apiProjectImport()
        assertEquals HttpServletResponse.SC_OK,response.status
        assertEquals 'failed',response.json.import_status
        assertEquals 2,response.json.errors.size()
        assertEquals 'error1',response.json.errors[0]
        assertEquals 'error2',response.json.errors[1]
    }
    @Test
    void apiProjectImport_xml_success() {
        controller.apiService = new ApiService()
        controller.apiService.messageSource = mockWith(MessageSource) { getMessage { code, args, locale -> code } }
        controller.frameworkService = mockFrameworkServiceForProjectExport(true, true, 'import')
        controller.projectService=mockWith(ProjectService){
            importToProject{  project, String user, String roleList,  framework,
                             AuthContext authContext,  InputStream stream, Map options->
                assertEquals('user1',user)
                assertTrue(roleList in ['groupa,groupb', 'groupb,groupa'])
                assertEquals([executionImportBehavior:'import', jobUUIDBehavior:'preserve'],options)
                [success:true]
            }
        }
        request.api_version = 11
        params.project = 'test1'
        request.format='application/zip'
        response.format='xml'
        request.subject = new Subject(false,[new Username('user1'),new Group('groupa'), new Group('groupb')] as Set,
                [] as Set, [] as Set)
        session.user='user1'
        request.method='PUT'
        controller.apiProjectImport()
        assertEquals HttpServletResponse.SC_OK,response.status
        assertEquals 'successful',response.xml.'@status'.text()
        assertEquals 0,response.xml.errors.size()
    }
    @Test
    void apiProjectImport_importExecutionsFalse() {
        controller.apiService = new ApiService()
        controller.apiService.messageSource = mockWith(MessageSource) { getMessage { code, args, locale -> code } }
        controller.frameworkService = mockFrameworkServiceForProjectExport(true, true, 'import')
        controller.projectService=mockWith(ProjectService){
            importToProject{  project, String user, String roleList,  framework,
                             AuthContext authContext,  InputStream stream, Map options->
                assertEquals('user1',user)
                assertTrue(roleList in ['groupa,groupb', 'groupb,groupa'])
                assertEquals([executionImportBehavior: 'skip', jobUUIDBehavior: 'preserve'], options)
                [success:true]
            }
        }
        request.api_version = 11
        params.project = 'test1'
        params.importExecutions='false'
        request.format='application/zip'
        request.subject = new Subject(false,[new Username('user1'),new Group('groupa'), new Group('groupb')] as Set,
                [] as Set, [] as Set)
        session.user='user1'
        request.method='PUT'
        controller.apiProjectImport()
        assertEquals HttpServletResponse.SC_OK,response.status
    }
    @Test
    void apiProjectImport_importExecutionsTrue() {
        controller.apiService = new ApiService()
        controller.apiService.messageSource = mockWith(MessageSource) { getMessage { code, args, locale -> code } }
        controller.frameworkService = mockFrameworkServiceForProjectExport(true, true, 'import')
        controller.projectService=mockWith(ProjectService){
            importToProject{  project, String user, String roleList,  framework,
                             AuthContext authContext,  InputStream stream, Map options->
                assertEquals('user1',user)
                assertTrue(roleList in ['groupa,groupb', 'groupb,groupa'])
                assertEquals([executionImportBehavior: 'import', jobUUIDBehavior: 'preserve'], options)
                [success:true]
            }
        }
        request.api_version = 11
        params.project = 'test1'
        params.importExecutions='true'
        request.format='application/zip'
        request.subject = new Subject(false,[new Username('user1'),new Group('groupa'), new Group('groupb')] as Set,
                [] as Set, [] as Set)
        session.user='user1'
        request.method='PUT'
        controller.apiProjectImport()
        assertEquals HttpServletResponse.SC_OK,response.status
    }
    @Test
    void apiProjectImport_jobUUIDBehaviorPreserve() {
        controller.apiService = new ApiService()
        controller.apiService.messageSource = mockWith(MessageSource) { getMessage { code, args, locale -> code } }
        controller.frameworkService = mockFrameworkServiceForProjectExport(true, true, 'import')
        controller.projectService=mockWith(ProjectService){
            importToProject{  project, String user, String roleList,  framework,
                             AuthContext authContext,  InputStream stream, Map options->
                assertEquals('user1',user)
                assertTrue(roleList in ['groupa,groupb', 'groupb,groupa'])
                assertEquals([executionImportBehavior: 'import', jobUUIDBehavior: 'preserve'], options)
                [success:true]
            }
        }
        request.api_version = 11
        params.project = 'test1'
        params.jobUUIDBehavior='preserve'
        request.format='application/zip'
        request.subject = new Subject(false,[new Username('user1'),new Group('groupa'), new Group('groupb')] as Set,
                [] as Set, [] as Set)
        session.user='user1'
        request.method='PUT'
        controller.apiProjectImport()
        assertEquals HttpServletResponse.SC_OK,response.status
    }
    @Test
    void apiProjectImport_jobUUIDBehaviorReplace() {
        controller.apiService = new ApiService()
        controller.apiService.messageSource = mockWith(MessageSource) { getMessage { code, args, locale -> code } }
        controller.frameworkService = mockFrameworkServiceForProjectExport(true, true, 'import')
        controller.projectService=mockWith(ProjectService){
            importToProject{  project, String user, String roleList,  framework,
                             AuthContext authContext,  InputStream stream, Map options->
                assertEquals('user1',user)
                assertTrue(roleList in ['groupa,groupb', 'groupb,groupa'])
                assertEquals([executionImportBehavior: 'import', jobUUIDBehavior: 'preserve'], options)
                [success:true]
            }
        }
        request.api_version = 11
        params.project = 'test1'
        params.jobUUIDBehavior='replace'
        request.format='application/zip'
        request.subject = new Subject(false,[new Username('user1'),new Group('groupa'), new Group('groupb')] as Set,
                [] as Set, [] as Set)
        session.user='user1'
        request.method='PUT'
        controller.apiProjectImport()
        assertEquals HttpServletResponse.SC_OK,response.status
    }
    @Test
    void apiProjectImport_json_success() {
        controller.apiService = new ApiService()
        controller.apiService.messageSource = mockWith(MessageSource) { getMessage { code, args, locale -> code } }
        controller.frameworkService = mockFrameworkServiceForProjectExport(true, true, 'import')
        controller.projectService=mockWith(ProjectService){
            importToProject{  project, String user, String roleList,  framework,
                             AuthContext authContext,  InputStream stream, Map options->
                assertEquals('user1',user)
                assertTrue(roleList in ['groupa,groupb', 'groupb,groupa'])
                [success:true]
            }
        }
        request.api_version = 11
        params.project = 'test1'
        request.format='application/zip'
        response.format='json'
        request.subject = new Subject(false,[new Username('user1'),new Group('groupa'), new Group('groupb')] as Set,
                [] as Set, [] as Set)
        session.user='user1'
        request.method='PUT'
        controller.apiProjectImport()
        assertEquals HttpServletResponse.SC_OK,response.status
        assertEquals 'successful',response.json.import_status
        assertEquals null,response.json.errors
    }
}
