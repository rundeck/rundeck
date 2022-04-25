
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

package rundeck.controllers

import com.dtolabs.rundeck.app.api.ApiVersions
import com.dtolabs.rundeck.app.support.ProjectArchiveParams
import com.dtolabs.rundeck.core.authorization.UserAndRolesAuthContext
import com.dtolabs.rundeck.core.common.IRundeckProject
import grails.test.hibernate.HibernateSpec
import grails.testing.web.controllers.ControllerUnitTest
import groovy.mock.interceptor.MockFor
import groovy.mock.interceptor.StubFor
import groovy.xml.MarkupBuilder
import org.grails.plugins.codecs.JSONCodec
import org.rundeck.app.authorization.AppAuthContextProcessor
import org.rundeck.app.authorization.domain.AppAuthorizer
import org.rundeck.app.auth.types.AuthorizingProject
import org.rundeck.app.web.WebExceptionHandler
import org.rundeck.core.auth.AuthConstants
import org.rundeck.core.auth.access.AuthActions
import org.rundeck.core.auth.access.MissingParameter
import org.rundeck.core.auth.access.NotFound
import org.rundeck.core.auth.access.UnauthorizedAccess
import org.rundeck.core.auth.app.RundeckAccess
import org.rundeck.core.auth.web.RdAuthorizeApplicationType
import org.rundeck.core.auth.web.RdAuthorizeProject
import org.rundeck.core.auth.web.WebDefaultParameterNamesMapper
import rundeck.Project
import rundeck.services.ApiService
import rundeck.services.ExecutionService
import rundeck.services.FrameworkService
import rundeck.services.ProjectService
import spock.lang.Unroll
import testhelper.RundeckHibernateSpec

import javax.security.auth.Subject
import javax.servlet.http.HttpServletRequest
import javax.servlet.http.HttpServletResponse
import java.lang.annotation.Annotation

import static org.junit.Assert.*

class ProjectController2Spec extends RundeckHibernateSpec implements ControllerUnitTest<ProjectController> {

    List<Class> getDomainClasses() { [Project] }

    def setup(){
        controller.apiService = Mock(ApiService)

        session.subject = new Subject()
        controller.rundeckWebDefaultParameterNamesMapper=Mock(WebDefaultParameterNamesMapper)
        controller.rundeckExceptionHandler=Mock(WebExceptionHandler)
    }
    /**
     * utility method to mock a class
     */
    private mockWith(Class clazz,Closure clos){
        def mock = new MockFor(clazz)
        mock.demand.with(clos)
        return mock.proxyInstance()
    }

    void apiProjectList_xml(){
        given:
        controller.frameworkService = mockWith(FrameworkService){
            projects(1..1){auth->
                [
                        [name: 'testproject'],
                        [name: 'testproject2'],
                ]
            }
        }
        controller.rundeckAuthContextProcessor = Mock(AppAuthContextProcessor)

        controller.apiService = mockWith(ApiService){
            requireApi(1..1) { req, resp ->
                true
            }
            renderSuccessXml(1..1) { req, resp, clos ->

            }
        }
        when:
        response.format='xml'
        controller.apiProjectList()
        then:
        assert response.status == HttpServletResponse.SC_OK

    }

    void apiProjectList_json(){
        when:
        def prja = new MockFor(IRundeckProject)
        prja.demand.getName(1..3) { -> 'testproject'}
        prja.demand.getProjectProperties(1..2){ -> [:]}
        def prjb = new MockFor(IRundeckProject)
        prjb.demand.getName(1..3) { -> 'testproject2'}
        prjb.demand.getProjectProperties(1..2){ -> [:]}
        controller.frameworkService = mockWith(FrameworkService) {
            projects(1..1) { auth ->
                [
                        prja.proxyInstance(),
                        prjb.proxyInstance(),
                ]
            }
        }
            controller.rundeckAuthContextProcessor = Mock(AppAuthContextProcessor)
        controller.apiService=mockWith(ApiService){
            requireApi(1..1){req,resp->true}
        }
        request.setAttribute('api_version', 24)
        response.format='json'
        controller.apiProjectList()
        def base='http://localhost:8080/api/'+ApiVersions.API_CURRENT_VERSION

        then:
        assert response.status == HttpServletResponse.SC_OK
        assert response.json.size()==2
        assert response.json[0].name=='testproject'
        assert response.json[0].description==''
        assert response.json[0].url==base+'/project/testproject'
        assert response.json[1].name=='testproject2'
        assert response.json[1].description==''
        assert response.json[1].url==base+'/project/testproject2'
    }

    void apiProjectList_v26_json(){
        when:
        def prja = new MockFor(IRundeckProject)
        prja.demand.getName(1..3) { -> 'testproject'}
        prja.demand.getProjectProperties(1..2){ -> [:]}
        def prjb = new MockFor(IRundeckProject)
        prjb.demand.getName(1..3) { -> 'testproject2'}
        prjb.demand.getProjectProperties(1..2){ -> [:]}
        controller.frameworkService = mockWith(FrameworkService) {
            projects(1..1) { auth ->
                [
                        prja.proxyInstance(),
                        prjb.proxyInstance(),
                ]
            }
        }
            controller.rundeckAuthContextProcessor = Mock(AppAuthContextProcessor)
        controller.apiService=mockWith(ApiService){
            requireApi(1..1){req,resp->true}
        }
        request.setAttribute('api_version', 26)
        response.format='json'
        controller.apiProjectList()
        def base='http://localhost:8080/api/'+ApiVersions.API_CURRENT_VERSION
        then:
        assert response.status == HttpServletResponse.SC_OK
        assert response.json.size()==2
        assert response.json[0].name=='testproject'
        assert response.json[0].description==''
        assert response.json[0].label==''
        assert response.json[0].url==base+'/project/testproject'
        assert response.json[1].name=='testproject2'
        assert response.json[1].description==''
        assert response.json[1].label==''
        assert response.json[1].url==base+'/project/testproject2'
    }

    void apiProjectList_withLabels_json(){
        when:
        def labelA = 'Test Project'
        def labelB = 'Test Project 2'
        def prja = new MockFor(IRundeckProject)
        prja.demand.getName(1..3) { -> 'testproject'}
        prja.demand.getProjectProperties(1..2){ -> ['project.label':labelA]}
        def prjb = new MockFor(IRundeckProject)
        prjb.demand.getName(1..3) { -> 'testproject2'}
        prjb.demand.getProjectProperties(1..2){ -> ['project.label':labelB]}
        controller.frameworkService = mockWith(FrameworkService) {
            projects(1..1) { auth ->
                [
                        prja.proxyInstance(),
                        prjb.proxyInstance(),
                ]
            }
        }
            controller.rundeckAuthContextProcessor = Mock(AppAuthContextProcessor)
        controller.apiService=mockWith(ApiService){
            requireApi(1..1){req,resp->true}
        }
        request.setAttribute('api_version', 26)
        response.format='json'
        controller.apiProjectList()
        def base='http://localhost:8080/api/'+ApiVersions.API_CURRENT_VERSION

        then:
        assert response.status == HttpServletResponse.SC_OK
        assert response.json.size()==2
        assert response.json[0].name=='testproject'
        assert response.json[0].description==''
        assert response.json[0].label==labelA
        assert response.json[0].url==base+'/project/testproject'
        assert response.json[1].name=='testproject2'
        assert response.json[1].description==''
        assert response.json[1].label==labelB
        assert response.json[1].url==base+'/project/testproject2'
    }


    void apiProjectList_unacceptableReceivesXml(){
        when:
        controller.frameworkService = mockWith(FrameworkService) {
            projects(1..1) { auth ->
                [
                        [name: 'testproject'],
                        [name: 'testproject2'],
                ]
            }
        }
            controller.rundeckAuthContextProcessor = Mock(AppAuthContextProcessor)

        controller.apiService = mockWith(ApiService) {
            requireApi(1..1) { req, resp -> true }
            renderSuccessXml(1..1) { req, resp, clos ->

            }
        }

        response.format='text'
        controller.apiProjectList()
        then:
        assert response.status==HttpServletResponse.SC_OK

    }

    void apiProjectGet_missingProjectParam() {
        given:
            controller.frameworkService = Mock(FrameworkService) {
            }
            controller.rundeckAuthContextProcessor = Mock(AppAuthContextProcessor)

            controller.apiService = Mock(ApiService) {
                1 * requireApi(_, _) >> true
            }

            response.format = 'xml'
            controller.rundeckExceptionHandler = Mock(WebExceptionHandler)
        when:
            controller.apiProjectGet()
        then:
            1 * controller.rundeckExceptionHandler.handleException(_, _, _ as MissingParameter)
    }


    private <T extends Annotation> T getControllerMethodAnnotation(String name, Class<T> clazz) {
        artefactInstance.getClass().getDeclaredMethods().find { it.name == name }.getAnnotation(clazz)
    }

    @Unroll
    def "RdAuthorizeProject for endpoint #endpoint requires auth #access"() {
        when:
            def result = getControllerMethodAnnotation(endpoint, RdAuthorizeProject)
        then:
            result.value() == access
        where:
            endpoint                    | access
            'apiProjectGet'             | RundeckAccess.General.AUTH_APP_READ
            'apiProjectDelete'          | RundeckAccess.General.AUTH_APP_DELETE
            'apiProjectConfigGet'       | RundeckAccess.Project.AUTH_APP_CONFIGURE
            'apiProjectConfigPut'       | RundeckAccess.Project.AUTH_APP_CONFIGURE
            'apiProjectExport'          | RundeckAccess.Project.AUTH_APP_EXPORT
            'apiProjectImport'          | RundeckAccess.Project.AUTH_APP_IMPORT
            'apiProjectConfigKeyGet'    | RundeckAccess.Project.AUTH_APP_CONFIGURE
            'apiProjectConfigKeyPut'    | RundeckAccess.Project.AUTH_APP_CONFIGURE
            'apiProjectConfigKeyDelete' | RundeckAccess.Project.AUTH_APP_CONFIGURE
    }

    private Object createFrameworkService(boolean configAuth, String projectName, LinkedHashMap<String,
    String> projectProperties=[:]) {
        def prja = new StubFor(IRundeckProject)
        prja.demand.getName(0..10) { -> 'test1'}
        prja.demand.getProjectProperties(0..1){ -> [:]}
        prja.demand.hasProperty(0..1){ String prop -> false}
        def pInstance = prja.proxyInstance()
        mockWith(FrameworkService) {

            existsFrameworkProject(1..1) { proj ->
                assertEquals(projectName, proj)
                true
            }
            getFrameworkProject(1..1) { String name ->
                assertEquals(projectName, name)
                pInstance
            }
            if(configAuth){
                loadProjectProperties(1..1){pject->
                    assertEquals(pInstance,pject)
                    projectProperties
                }
            }
        }
    }

    @Unroll
    def "renderApiProjectXml hasConfig #hasConfig vers #vers"() {
        given:
            Properties projProps = new Properties(
                [
                    'project.description': 'a description',
                    'project.label'      : 'a label',
                    "test.property"      : "value1",
                    "test.property2"     : "value2"
                ]
            )
            def configDate = new Date()
            def rdProject = Mock(IRundeckProject) {
                _ * getName() >> 'test1'
                _ * getProjectProperties() >> projProps
                _ * hasProperty(_) >> {
                    projProps[it[0]] != null
                }
                seeCreated * getConfigCreatedTime() >> configDate
            }
            controller.frameworkService = Mock(FrameworkService) {
                seeConfig * loadProjectProperties(rdProject) >> projProps
            }
            def writer = new StringWriter()
            def builder = new MarkupBuilder(writer)
        when:
            controller.renderApiProjectXml(rdProject, builder, hasConfig, vers)
            def result = writer.toString()
            def response = new XmlSlurper().parseText(result)
        then:

            //XML result has no wrapper
            'project' == response.name()
            0 == response.result.size()
            0 == response.projects.size()

            'test1' == response.name.text()
            'a description' == response.description.text()
            seeConfig == response.config.size()

            (seeConfig?4:0)== response.config.property.size()

            response.config.property[0].'@key'.text() == (seeConfig?'test.property':'')
            response.config.property[0].'@value'.text() == (seeConfig?'value1':'')
            response.config.property[1].'@key'.text() == (seeConfig?'test.property2':'')
            response.config.property[1].'@value'.text() == (seeConfig?'value2':'')

            response.label.size()==seeLabel

            response.label.text()==(seeLabel?'a label':'')

            response.created.size()==seeCreated

            response.created.text()==(seeCreated?configDate.toString():'')

        where:
            hasConfig | vers | seeConfig | seeLabel | seeCreated
            false     | 11   | 0         | 0        | 0
            true      | 11   | 1         | 0        | 0
            false     | 26   | 0         | 1        | 0
            true      | 26   | 1         | 1        | 0
            true      | 33   | 1         | 1        | 1
            false     | 33   | 0         | 1        | 1
    }
    @Unroll
    def "renderApiProjectJson hasConfig #hasConfig vers #vers"() {
        given:
            Properties projProps = new Properties(
                [
                    'project.description': 'a description',
                    'project.label'      : 'a label',
                    "test.property"      : "value1",
                    "test.property2"     : "value2"
                ]
            )
            def configDate = new Date()
            def configDateString = ExecutionService.ISO_8601_DATE_FORMAT.get().format(configDate)
            def rdProject = Mock(IRundeckProject) {
                _ * getName() >> 'test1'
                _ * getProjectProperties() >> projProps
                _ * hasProperty(_) >> {
                    projProps[it[0]] != null
                }
                seeCreated * getConfigCreatedTime() >> configDate
            }
            controller.frameworkService = Mock(FrameworkService) {
                seeConfig * loadProjectProperties(rdProject) >> projProps
            }
        when:
            def response=controller.renderApiProjectJson(rdProject, hasConfig, vers)

        then:

           'test1' == response.name
            'a description' == response.description
            (seeConfig ? false : true) == (response.config == null)

            (seeConfig?4:null)== response.config?.size()

            response.config?.get('test.property') == (seeConfig?'value1':null)
            response.config?.get('test.property2') == (seeConfig?'value2':null)

            response.label==(seeLabel?'a label':null)

            response.created==(seeCreated?configDateString:null)

        where:
            hasConfig | vers | seeConfig | seeLabel | seeCreated
            false     | 11   | 0         | 0        | 0
            true      | 11   | 1         | 0        | 0
            false     | 26   | 0         | 1        | 0
            true      | 26   | 1         | 1        | 0
            true      | 33   | 1         | 1        | 1
            false     | 33   | 0         | 1        | 1
    }



    @Unroll
    def "rd authorize app type annotation required for endpoint #endpoint"() {
        when:
            def result = getControllerMethodAnnotation(endpoint, RdAuthorizeApplicationType)
        then:
            result.access() == access
            result.type() == type
        where:
            endpoint           | type                       | access
            'apiProjectCreate' | AuthConstants.TYPE_PROJECT | RundeckAccess.General.AUTH_APP_CREATE
    }

    /**
     * Missing project name element
     */
    void apiProjectCreate_xml_invalid() {
        given:
        controller.apiService=Mock(ApiService)

        request.xml='<project><namex>test1</namex></project>'
        request.method='POST'
        response.format = 'xml'
        when:
        controller.apiProjectCreate()
        then:
        1 * controller.apiService.requireApi(_,_)>>true
        1 * controller.apiService.parseJsonXmlWith(_,_,_)>>false
        response.contentLength==0

    }/**
     * Missing project name element
     */
    void apiProjectCreate_json_invalid() {
        given:
        controller.apiService=Mock(ApiService)
        mockCodec(JSONCodec)

        request.json='{"blame":"monkey"}'
        request.method='POST'
        response.format = 'json'
        when:
        controller.apiProjectCreate()

        then:
        1 * controller.apiService.requireApi(_,_)>>true
        1 * controller.apiService.parseJsonXmlWith(_,_,_)>>false
        response.contentLength==0
    }
    /**
     * project already exists
     */
    void apiProjectCreate_xml_projectExists() {
        given:
        controller.apiService=Mock(ApiService)

        setupProjectCreate(controller, true, false, null, null, null)
        request.xml='<project><name>test1</name></project>'
        request.method='POST'
        response.format = 'xml'
        when:
        controller.apiProjectCreate()

        //test project element
        then:
            assert response.status == HttpServletResponse.SC_CONFLICT

        1 * controller.apiService.requireApi(_,_)>>true
        1 * controller.apiService.extractResponseFormat(_,_,_)>>'xml'
        1 * controller.apiService.parseJsonXmlWith(_,_,_)>>{
            it[2].get('xml').call(it[0].XML)
            true
        }
        1 * controller.apiService.renderErrorFormat(_, {
            it.code=='api.error.item.alreadyexists'
        })>> {
            it[0].status=it[1].status
        }

    }
    /**
     * project already exists
     */
    void apiProjectCreate_json_projectExists() {
        given:
        controller.apiService=Mock(ApiService)

        setupProjectCreate(controller, true, false, null, null, null)
        request.json = '{"name":"test1"}'
        request.method='POST'
        response.format = 'json'
        when:
        controller.apiProjectCreate()


        //test project element
        then:
            assert response.status == HttpServletResponse.SC_CONFLICT
        1 * controller.apiService.requireApi(_,_)>>true
            1 * controller.apiService.extractResponseFormat(_,_,_)>>'json'
        1 * controller.apiService.parseJsonXmlWith(_,_,_)>>{
            it[2].get('json').call(it[0].JSON)
            true
        }
        1 * controller.apiService.renderErrorFormat(_, {
            it.code=='api.error.item.alreadyexists'
        })>> {
            it[0].status=it[1].status
        }
    }
    /**
     * Failure to create project
     */
    void apiProjectCreate_xml_withErrors() {
        given:
            controller.apiService=Mock(ApiService)
        setupProjectCreate(controller, false, false, ['error1', 'error2'], [:], null)
        request.xml='<project><name>test1</name></project>'
        request.method='POST'
        response.format = 'xml'
        when:
        controller.apiProjectCreate()

        //test project element
        then:
        1 * controller.apiService.requireApi(*_)>>true
            1 * controller.apiService.extractResponseFormat(_,_,_)>>'xml'
            1 * controller.apiService.parseJsonXmlWith(_,_,_)>>{
                it[2].get('xml').call(it[0].XML)
                true
            }
        1 * controller.apiService.renderErrorFormat(_, {
            it.status== HttpServletResponse.SC_INTERNAL_SERVER_ERROR
        })
    }
    /**
     * Failure to create project
     */
    void apiProjectCreate_json_withErrors() {
        given:
            controller.apiService=Mock(ApiService)
        setupProjectCreate(controller, false, false, ['error1', 'error2'], [:], null)
        request.json = '{"name":"test1"}'
        request.method='POST'
        response.format = 'json'
        when:
        controller.apiProjectCreate()
        then:

            1 * controller.apiService.extractResponseFormat(_,_,_)>>'json'
            1 * controller.apiService.requireApi(*_)>>true
            1 * controller.apiService.parseJsonXmlWith(_,_,_)>>{
                it[2].get('json').call(it[0].JSON)
                true
            }
            1 * controller.apiService.renderErrorFormat(_, {
                it.status== HttpServletResponse.SC_INTERNAL_SERVER_ERROR
            })
    }
    /**
     * Successful
     */
    void apiProjectCreate_xml_success() {
        
        given:
            controller.apiService=Mock(ApiService)
        setupProjectCreate(controller, false, true, [], [:], ['prop1': 'value1', 'prop2': 'value2'])
        request.xml='<project><name>test1</name></project>'
        request.method='POST'
        response.format = 'xml'
        when:
        controller.apiProjectCreate()
        then:
            1 * controller.apiService.extractResponseFormat(_,_,_)>>'xml'
            1 * controller.apiService.requireApi(_,_)>>true
            1 * controller.apiService.parseJsonXmlWith(_,_,_)>>{
                it[2].get('xml').call(it[0].XML)
                true
            }
            1 * controller.apiService.renderSuccessXml(HttpServletResponse.SC_CREATED, _ as HttpServletRequest, _ as HttpServletResponse, _ as Closure)
    }
    /**
     * Successful
     */
    void apiProjectCreate_json_success() {

        given:
            controller.apiService=Mock(ApiService)
        setupProjectCreate(
            controller, false, false, [],
            [:], ['prop1': 'value1', 'prop2': 'value2']
        )
        request.json = '{"name":"test1"}'
        request.method='POST'
        response.format = 'json'
        when:
        controller.apiProjectCreate()
        then:

            1 * controller.apiService.requireApi(*_)>>true
            1 * controller.apiService.extractResponseFormat(_,_,_)>>'json'
            1 * controller.apiService.parseJsonXmlWith(_,_,_)>>{
                it[2].get('json').call(it[0].JSON)
                true
            }
         response.status == HttpServletResponse.SC_CREATED

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
    void apiProjectCreate_xml_withconfig() {
        
        given:
        controller.apiService=Mock(ApiService)
        setupProjectCreate(
            controller,
            false,
            true,
            [],
            ['input1': 'value1', 'input2': 'value2'],
            ['prop1': 'value1', 'prop2': 'value2']
        )
        request.xml='<project><name>test1</name><config><property key="input1" value="value1"/><property key="input2"' +
                ' value="value2"/></config></project>'
        request.method='POST'
        response.format = 'xml'
        when:
        controller.apiProjectCreate()
        then:
            1 * controller.apiService.requireApi(*_)>>true
            1 * controller.apiService.parseJsonXmlWith(_,_,_)>>{
                it[2].get('xml').call(it[0].XML)
                true
            }
            1 * controller.apiService.extractResponseFormat(_,_,_)>>'xml'
            1 * controller.apiService.renderSuccessXml(HttpServletResponse.SC_CREATED, _ as HttpServletRequest, _ as HttpServletResponse, _ as Closure)
    }
    /**
     * Create project with input config
     */
    void apiProjectCreate_json_withconfig() {
        given:
            controller.apiService=Mock(ApiService)
            setupProjectCreate(
                controller,
                false,
                false,
                [],
                ['input1': 'value1', 'input2': 'value2'],
                ['prop1': 'value1', 'prop2': 'value2']
            )
            request.json = '{"name":"test1","config": { "input1":"value1","input2":"value2" } }'
            request.method='POST'
            response.format = 'json'
        when:
            controller.apiProjectCreate()
        then:

            response.status == HttpServletResponse.SC_CREATED
            1 * controller.apiService.extractResponseFormat(_,_,_)>>'json'
            1 * controller.apiService.requireApi(*_)>>true
            1 * controller.apiService.parseJsonXmlWith(_,_,_)>>{
                it[2].get('json').call(it[0].JSON)
                true
            }
            def result = response.json
            //test project element
            null == result.error
            "test1" == result.name
            2 == result.config.size()
            'value1' == result.config['prop1']
            'value2' == result.config['prop2']
    }

    private def setupProjectCreate(
        ProjectController controller,
        boolean exists,
        boolean isxml,
        List createErrors,
        Map<String, String> inputProps,
        Map<String, String> configProps
    ) {
        controller.rundeckAuthContextProcessor = Mock(AppAuthContextProcessor)
        def prja = Stub(IRundeckProject) {
            getName()>>'test1'
            getProjectProperties()>>[:]
        }
        controller.frameworkService=Mock(FrameworkService)

        1 * controller.frameworkService.existsFrameworkProject('test1')>>exists


        (exists?0:1)*controller.frameworkService.createFrameworkProject('test1',inputProps)>> [createErrors?.size() > 0 ? null: prja, createErrors]
        if(!exists && !isxml){
            (createErrors?.size() > 0?0:1)*controller.frameworkService.loadProjectProperties(prja)>>configProps
        }
    }

    @Unroll
    void "deleteProject #format project parameter missing"(){
        given:
            controller.apiService = Mock(ApiService)
        when:

            request.method = 'DELETE'
            controller.apiProjectDelete()
        then:
            1 * controller.apiService.requireApi(*_) >> true
            1 * controller.rundeckExceptionHandler.handleException(_, _, _ as MissingParameter)>>true
        where:
            format << ['xml','json']

    }

    private def setupAuthDelete(String name='test1',boolean allowed=true, boolean found=true){
        controller.rundeckAppAuthorizer = Mock(AppAuthorizer){
            1 * project(_,_)>>Mock(AuthorizingProject){
                _ * getAuthContext() >> Mock(UserAndRolesAuthContext)
                1 * getResource()>>{
                    if(!allowed){
                        throw new UnauthorizedAccess('delete','project',name)
                    }
                    if(!found){
                        throw new NotFound('project',name)
                    }
                    Stub(IRundeckProject){
                        getName()>>name
                    }
                }
            }
        }
    }

    void "deleteProject has errors"(){
        
        given:
        controller.apiService=Mock(ApiService)
        mockCodec(JSONCodec)
        controller.frameworkService=mockFrameworkServiceForProjectDelete()
        controller.projectService=mockProjectServiceForProjectDelete(false, 'deleteProjectFailed')

        setupAuthDelete()

        request.method = 'DELETE'
        params.project='test1'
        when:
        controller.apiProjectDelete()
        then:
            1 * controller.apiService.requireApi(_,_)>>true
            1 * controller.apiService.renderErrorFormat(_,[
                status : HttpServletResponse.SC_INTERNAL_SERVER_ERROR,
                code   : "api.error.unknown",
                message: 'deleteProjectFailed',
            ])
    }

    void deleteProject_xml_success(){
        
        given:
            controller.apiService=Mock(ApiService)
        mockCodec(JSONCodec)
        controller.frameworkService=mockFrameworkServiceForProjectDelete()
        controller.projectService=mockProjectServiceForProjectDelete(true, null)

        setupAuthDelete()

            request.method = 'DELETE'
        params.project='test1'
        when:
        controller.apiProjectDelete()
        then:
            1 * controller.apiService.requireApi(_,_)>>true
        assert response.status == HttpServletResponse.SC_NO_CONTENT
    }




    def mockProjectServiceForProjectDelete(boolean success, String errorMessage) {
        Mock(ProjectService) {
            1 * deleteProject (*_)>>new ProjectService.DeleteResponse(success: success, error: errorMessage)
        }
    }

    private def mockFrameworkServiceForProjectDelete(){
        Mock(FrameworkService){
            getFrameworkProject('test1')>>Mock(IRundeckProject){
                getName()>>'test1'
            }
        }
    }
    private def mockFrameworkServiceForProjectConfigGet(boolean exists, boolean authorized, String action,
                                                        LinkedHashMap props, String textformat=null){
        Mock(FrameworkService){
            (exists&&authorized?1:0)*getFrameworkProject('test1')>>Mock(IRundeckProject){
                getName()>>'test1'
                getProjectProperties()>>props
            }
            _*loadProjectProperties(_)>>props
        }
    }
    private def mockFrameworkServiceForProjectConfigPut(boolean exists, boolean authorized, String action,
                                                        LinkedHashMap currentProps, boolean success, String errorMessage,
                                                        String propFileText, boolean handleScheduling = false){
        Map newConfigProps = [:]
        Mock(FrameworkService){

            _ * existsFrameworkProject(_) >> exists
            if(!exists){
                return
            }


            1 * setFrameworkProjectConfig('test1', _) >> {
//                assertEquals props,configProps
                newConfigProps = it[1]
                [success: success,error: errorMessage]
            }
            if(handleScheduling) {
                1 * handleProjectSchedulingEnabledChange(*_)
            }
            if(!success){
                return
            }
            1 * loadProjectProperties(_)>> {
                newConfigProps
            }

        }
    }
    private def mockFrameworkServiceForProjectConfigKeyPut(boolean exists, boolean authorized,
                                                        LinkedHashMap props, boolean success){
        Mock(FrameworkService){
            _ * existsFrameworkProject('test1')>>exists
            if(!exists){
                return
            }

            if(!authorized){
                return
            }
            1 * getFrameworkProject('test1')>>Mock(IRundeckProject){
                _*getName()>>'test1'

            }

            1 * updateFrameworkProjectConfig(_,props,_)>>{

                [success: success]
            }
            if(!success){
                return
            }
            1 * loadProjectProperties(_)>> props

        }
    }
    private def mockFrameworkServiceForProjectConfigKeyDelete(boolean exists, boolean authorized,
                                                        String propname, boolean success){
        Mock(FrameworkService){
            _*existsFrameworkProject('test1')>>exists

            if(!exists){
                return
            }

            if(!authorized){
                return
            }
           1 * getFrameworkProject('test1')>> Mock(IRundeckProject){
               _*getName()>>'test1'
           }
            1 * removeFrameworkProjectConfigProperties(_,_)>>{
                assertEquals ([propname] as Set,it[1])
                [success: success]
            }
        }
    }
    private def mockFrameworkServiceForProjectExport(boolean exists, boolean authorized){
        Mock(FrameworkService){
            if(!exists){
                return
            }

            if(!authorized){
                return
            }
            1 * getFrameworkProject('test1')>>Mock(IRundeckProject){
                _*getName()>>'test1'
            }
            1*getRundeckFramework()

        }
    }
    private def mockFrameworkServiceForProjectImport(){
        Mock(FrameworkService){
            1 * getFrameworkProject('test1')>>Mock(IRundeckProject){
                _*getName()>>'test1'
            }
            _ * getRundeckFramework()

        }
    }



    void apiProjectConfigGet_apiversion(){
        given:
            controller.apiService = Mock(ApiService)
        when:

        controller.frameworkService= Mock(FrameworkService)
        controller.apiProjectConfigGet()
        then:
        1 * controller.apiService.requireApi(_,_)>>false
    }

    void apiProjectConfigGet_xml_missingparam(){
        given:

            controller.apiService=Mock(ApiService){
                requireApi(_,_)>>true
            }

        when:
        controller.frameworkService= Mock(FrameworkService)
        controller.apiProjectConfigGet()
        then:

            1 * controller.apiService.requireApi(*_) >> true
            1 * controller.rundeckExceptionHandler.handleException(_, _, _ as MissingParameter)>>true
    }

    private void setupAuthImport(boolean auth = true,
                                    boolean found = true,
                                    String name = 'test1',
                                    AuthActions actions = RundeckAccess.Project.APP_IMPORT){
        setupAuthAccess(auth, found, name, actions)
        controller.rundeckAuthContextProcessor=Mock(AppAuthContextProcessor)
    }
    private void setupAuthExport(boolean auth = true,
                                    boolean found = true,
                                    String name = 'test1',
                                    AuthActions actions = RundeckAccess.Project.APP_EXPORT){
        setupAuthAccess(auth, found, name, actions)
        controller.rundeckAuthContextProcessor=Mock(AppAuthContextProcessor)
    }
    private void setupAuthConfigure(
        boolean auth = true,
        boolean found = true,
        String name = 'test1',
        AuthActions actions = RundeckAccess.Project.APP_CONFIGURE
    ) {
        setupAuthAccess(auth, found, name, actions)
    }
    private void setupAuthAccess(
        boolean auth = true,
        boolean found = true,
        String name = 'test1',
        AuthActions actions = RundeckAccess.Project.APP_CONFIGURE,
        AuthActions authCheck = null,
        boolean authCheckValue=true
    ) {
        controller.rundeckAppAuthorizer = Mock(AppAuthorizer) {
            (authCheck?2:1) * project(_, _) >> Mock(AuthorizingProject) {
                1 * access(actions) >> {
                    if(!auth) {
                        throw new UnauthorizedAccess(actions.description, 'Project', name)
                    }
                    if(!found) {
                        throw new NotFound( 'Project', name)
                    }
                    Stub(IRundeckProject){
                        getName()>>name
                    }
                }
                (authCheck ? 1 : 0) * authorize(authCheck) >> {
                    if(!authCheckValue){
                        throw new UnauthorizedAccess(authCheck.description, 'Project', name)
                    }
                }
                0*_(*_)
            }
            0*_(*_)
        }
    }
    private void setupGetResource(IRundeckProject pject=null) {
        controller.rundeckAuthContextProcessor=Mock(AppAuthContextProcessor)
        controller.rundeckAppAuthorizer = Mock(AppAuthorizer) {
            1 * project(_, _) >> Mock(AuthorizingProject) {
                1 * getResource() >> {
                    pject?:Stub(IRundeckProject){
                        getName()>>'test'
                    }
                }
                0*_(*_)
            }
            0*_(*_)
        }
    }


    void apiProjectConfigGet_xml_success(){
        
        given:
        controller.apiService=Mock(ApiService)

        setupGetResource(Mock(IRundeckProject){
            getName()>>'test1'
            getProjectProperties()>>["prop1": "value1", "prop2": "value2"]
        })
        request.api_version = 11
        params.project='test1'
        when:
            controller.apiProjectConfigGet()
        then:
            1 * controller.apiService.requireApi(_,_)>>true
            1 * controller.apiService.extractResponseFormat(_,_,_,_)>>'xml'
            1 * controller.apiService.renderSuccessXml(_ as HttpServletRequest,_,_ as Closure)
    }
    def "respondXmlConfig"(){
        given:
            def pject = Mock(IRundeckProject)
            def writer = new StringWriter()
            def builder = new MarkupBuilder(writer)
            controller.frameworkService = Mock(FrameworkService){
                1 * loadProjectProperties(pject)>>[
                    prop1:'value1',
                    prop2:'value2'
                ]
            }
        when:
            controller.renderApiProjectConfigXml(pject,builder)
            def response = new XmlSlurper().parseText(writer.toString())
        then:
            assertEquals "config",response.name()
            assertEquals 2,response.property.size()
            assertEquals 'prop1',response.property[0].'@key'.text()
            assertEquals 'value1',response.property[0].'@value'.text()
            assertEquals 'prop2',response.property[1].'@key'.text()
            assertEquals 'value2',response.property[1].'@value'.text()
    }

    void apiProjectConfigGet_json_success(){
        
        given:
        controller.apiService=Mock(ApiService)
        controller.frameworkService = Mock(FrameworkService){
            1 * loadProjectProperties(_)>>["prop1": "value1", "prop2": "value2"]
        }
        setupGetResource(Mock(IRundeckProject){
            getName()>>'test1'
        })
        request.api_version = 11
        params.project = 'test1'
        response.format='json'
        when:
        controller.apiProjectConfigGet()
        then:
            1 * controller.apiService.requireApi(_,_)>>true
            1 * controller.apiService.extractResponseFormat(_,_,_,_)>>'json'
        assertEquals HttpServletResponse.SC_OK, response.status
        assertEquals "value1",response.json.prop1
        assertEquals "value2",response.json.prop2
    }

    void apiProjectConfigGet_text_success(){
        
        given:
        controller.apiService=Mock(ApiService)
        setupGetResource(Mock(IRundeckProject){
            getName()>>'test1'
            getProjectProperties()>>["prop1": "value1", "prop2": "value2"]
        })
        request.api_version = 11
        params.project = 'test1'
        response.format='text'
        when:
        controller.apiProjectConfigGet()
        then:
            1 * controller.apiService.requireApi(_,_)>>true
            1 * controller.apiService.extractResponseFormat(_,_,_,_)>>'text'
            assertEquals HttpServletResponse.SC_OK, response.status
            assertTrue response.text.startsWith("#\n#")
    }


    void apiProjectConfigPut_xml_success(){
        
        given:
            controller.apiService=Mock(ApiService)
            def pject=Mock(IRundeckProject) {
                _* getName() >> 'test1'
                _* getProjectProperties() >> ['prop1': 'valueA']
            }
            controller.frameworkService= Mock(FrameworkService)
            setupGetResource(pject)

            request.api_version = 11
            params.project = 'test1'
            request.method='PUT'
            request.xml='<config><property key="prop1" value="value1"/><property key="prop2" value="value2"/></config>'

        when:
        controller.apiProjectConfigPut()
        then:
            1 * controller.apiService.requireApi(_,_)>>true
            1 * controller.apiService.extractResponseFormat(_,_,_)>>'xml'
            1 * controller.apiService.parseJsonXmlWith(_,_,_)>>{
                it[2].get('xml').call(it[0].XML)
                true
            }
            1 * controller.frameworkService.setFrameworkProjectConfig('test1', [prop1: 'value1',prop2:'value2']) >> {
                [success: true,error: null]
            }
            1 * controller.apiService.renderSuccessXml (_,_,_ as Closure)
    }

    @Unroll
    void "apiProjectConfigPut xml change scheduling"(){
        
        given:
            controller.apiService=Mock(ApiService)
            def pject=Mock(IRundeckProject) {
                _* getName() >> 'test1'
                _* getProjectProperties() >> [
                    'project.disable.executions': (curExecDisabled).toString(),
                    'project.disable.schedule': (curSchedDisabled).toString()
                ]
            }
            controller.frameworkService= Mock(FrameworkService)

            request.api_version = 11
            params.project = 'test1'
            request.method='PUT'
            request.xml= "<config>" +
                         "<property key=\"project.disable.executions\" value=\"${newExecDisabled}\"/>" +
                         "<property key=\"project.disable.schedule\" value=\"${newSchedDisabled}\"/>" +
                         "</config>"

            setupGetResource(pject)
        when:
            controller.apiProjectConfigPut()
        then:
            1 * controller.apiService.requireApi(_,_)>>true
            1 * controller.apiService.extractResponseFormat(_,_,_)>>'xml'
            1 * controller.apiService.parseJsonXmlWith(_,_,_)>>{
                it[2].get('xml').call(it[0].XML)
                true
            }
            1 * controller.frameworkService.setFrameworkProjectConfig('test1', _) >> {
                [success: true,error: null]
            }
            1 * controller.frameworkService.handleProjectSchedulingEnabledChange(_,curExecDisabled,curSchedDisabled,newExecDisabled,newSchedDisabled)
            1 * controller.apiService.renderSuccessXml (_,_,_ as Closure)
        where:
            curExecDisabled | curSchedDisabled | newExecDisabled | newSchedDisabled
            false           | false            | true            | false
            false           | false            | false           | true
            true            | false            | false           | false
            false           | true             | false           | false
    }

    void apiProjectConfigPut_text_success(){
        
        given:
            controller.apiService=Mock(ApiService)

            def pject=Mock(IRundeckProject) {
                _* getName() >> 'test1'
                _* getProjectProperties() >> ['prop1': 'valueRead']
            }
            setupGetResource(pject)
            controller.frameworkService= Mock(FrameworkService)

            params.project = 'test1'
            request.method='PUT'
            request.contentType = 'text/plain'
            request.content = 'prop1=value1\nprop2=value2\n'.bytes
        when:
            controller.apiProjectConfigPut()
        then:
             assertEquals HttpServletResponse.SC_OK, response.status
            1 * controller.apiService.requireApi(_,_)>>true
            1 * controller.apiService.extractResponseFormat(_,_,_)>>'text'

            1 * controller.frameworkService.setFrameworkProjectConfig('test1', [prop1: 'value1',prop2:'value2']) >> {
                [success: true,error: null]
            }

            response.status == 200
            response.contentType == 'text/plain'
            response.text.split(/[\n\r]/).contains 'prop1=valueRead'
    }

    void apiProjectConfigPut_json_success(){

        given:
            controller.apiService=Mock(ApiService)

            def pject=Mock(IRundeckProject) {
                _* getName() >> 'test1'
                _* getProjectProperties() >> ['prop1': 'valueA']
            }
            setupGetResource(pject)
            controller.frameworkService= Mock(FrameworkService)

            params.project = 'test1'
            request.method='PUT'
            request.json='{"prop1" :"value1","prop2":"value2"}'
        when:
            controller.apiProjectConfigPut()
        then:
             assertEquals HttpServletResponse.SC_OK, response.status
            1 * controller.apiService.requireApi(_,_)>>true
            1 * controller.apiService.extractResponseFormat(_,_,_)>>'json'
            1 * controller.apiService.parseJsonXmlWith(_,_,_)>>{
                it[2].get('json').call(it[0].JSON)
                true
            }
            1 * controller.frameworkService.setFrameworkProjectConfig('test1', [prop1: 'value1',prop2:'value2']) >> {
                [success: true,error: null]
            }
            1 * controller.frameworkService.loadProjectProperties(pject) >> [prop1: 'value1',prop2:'value2',prop3:'value3']
            assertEquals 'value1', response.json.prop1
            assertEquals 'value2', response.json.prop2
            assertEquals 'value3', response.json.prop3
    }


    @Unroll
    void "apiProjectConfigPut json change scheduling"(){

        given:
            controller.apiService=Mock(ApiService)
            def pject=Mock(IRundeckProject) {
                _* getName() >> 'test1'
                _* getProjectProperties() >> [
                    'project.disable.executions': (curExecDisabled).toString(),
                    'project.disable.schedule': (curSchedDisabled).toString()
                ]
            }
            controller.frameworkService= Mock(FrameworkService)

            request.api_version = 11
            params.project = 'test1'
            request.method='PUT'
            request.json= [
                'project.disable.executions':"${newExecDisabled}",
                'project.disable.schedule':"${newSchedDisabled}"
            ]

            setupGetResource(pject)
        when:
            controller.apiProjectConfigPut()
        then:
            1 * controller.apiService.requireApi(_,_)>>true
            1 * controller.apiService.extractResponseFormat(_,_,_)>>'xml'
            1 * controller.apiService.parseJsonXmlWith(_,_,_)>>{
                it[2].get('json').call(it[0].JSON)
                true
            }
            1 * controller.frameworkService.setFrameworkProjectConfig('test1', _) >> {
                [success: true,error: null]
            }
            1 * controller.frameworkService.handleProjectSchedulingEnabledChange(_,curExecDisabled,curSchedDisabled,newExecDisabled,newSchedDisabled)

        where:
            curExecDisabled | curSchedDisabled | newExecDisabled | newSchedDisabled
            false           | false            | true            | false
            false           | false            | false           | true
            true            | false            | false           | false
            false           | true             | false           | false
    }


    @Unroll
    void "apiProjectConfigKeyGet #format success"() {
        given:
        controller.apiService=Mock(ApiService)
        controller.frameworkService =Mock(FrameworkService){
            _*loadProjectProperties(_)>>["prop1": "value1", "prop2": "value2"]
        }
        setupGetResource()
        request.api_version = 11
        params.project = 'test'
        params.keypath = 'prop1'
        response.format=format
        when:
        controller.apiProjectConfigKeyGet()

        then:
        assertEquals HttpServletResponse.SC_OK, response.status
        response.text==expected
        with(controller.apiService) {
            1 * requireApi(_, _) >> true
            1 * restoreUriPath(_, _) >> 'prop1'
            1 * extractResponseFormat(_, _, _, _) >> format
            (format == 'xml' ? 1 : 0) * renderSuccessXml(_, _, _ as Closure) >> {
                it[1].writer << '<property key=\'prop1\' value=\'value1\' />'
            }
        }
        where:
            format | expected
            'xml'  | "<property key='prop1' value='value1' />"
            'json' | '{"key":"prop1","value":"value1"}'
            'text' | 'value1'
    }




    @Unroll
    void "apiProjectConfigKeyPut #format success"() {
        
        given:
            controller.apiService=Mock(ApiService)
            controller.frameworkService =Mock(FrameworkService){
                updateFrameworkProjectConfig(_, ["prop1": "value1"],_)>> [success: true]
            }

            controller.rundeckAuthContextProcessor=Mock(AppAuthContextProcessor)
            controller.rundeckAppAuthorizer = Mock(AppAuthorizer) {
                1 * project(_, _) >> Mock(AuthorizingProject) {
                    1 * getResource() >> Stub(IRundeckProject){
                        getName()>>'test1'
                        getProperty('prop1') >>> [null, 'value1']
                    }
                    0*_(*_)
                }
                0*_(*_)
            }

            request.api_version = 11
            params.project = 'test1'
            params.keypath = 'prop1'
            request.setContent(input.bytes)
            request.format=format
            request.method='PUT'
        when:
            controller.apiProjectConfigKeyPut()
        then:
            assertEquals HttpServletResponse.SC_OK, response.status
            response.text==expected
            with(controller.apiService) {
                1 * requireApi(_, _) >> true
                1 * restoreUriPath(_, _) >> 'prop1'
                1 * extractResponseFormat(_, _, _) >> format
                (format == 'xml' ? 1 : 0) * renderSuccessXml(_, _, _ as Closure) >> {
                    it[1].writer << '<property key=\'prop1\' value=\'value1\' />'
                }
                (format!='text'?1:0) * parseJsonXmlWith(_,_,_)>>{
                    if(format=='json'){
                        it[2].get('json').call(it[0].JSON)
                    }else{

                        it[2].get('xml').call(it[0].XML)
                    }
                    true
                }
            }
        where:
            format |input                                   |expected
            'xml'  |'<property key="prop1" value="value1"/>'|"<property key='prop1' value='value1' />"
            'json' |'{"key":"prop1","value":"value1"}'      |'{"key":"prop1","value":"value1"}'
            'text' |'value1'                                |'value1'
    }



    void apiProjectConfigKeyDelete_success() {
        
        given:
            controller.apiService=Mock(ApiService)
            controller.frameworkService =Mock(FrameworkService){
                _*removeFrameworkProjectConfigProperties(_,['prop1'].toSet())>>[success:true]\
            }
            setupGetResource()
            params.project = 'test1'
            params.keypath = 'prop1'
            request.method='DELETE'
        when:
            controller.apiProjectConfigKeyDelete()

        then:
            assertEquals HttpServletResponse.SC_NO_CONTENT, response.status
            with(controller.apiService) {
                1 * requireApi(_, _) >> true
                1 * restoreUriPath(_, _) >> 'prop1'
            }
    }

    void "apiProjectExport"() {
        
        given:
            controller.apiService=Mock(ApiService){
                1 * requireApi(*_)>>true
            }
            controller.frameworkService = Mock(FrameworkService)
            controller.projectService=Mock(ProjectService){
                1 * exportProjectToOutputStream({ it.name=='test' },_,_,_,_,_)>>{
                    it[2]<<'some data'
                }
            }
            setupGetResource()

            request.api_version = 11
            params.project = 'test'
        when:
            controller.apiProjectExport()

        then:
            assertEquals HttpServletResponse.SC_OK, response.status
            assertEquals 'application/zip', response.contentType
            assertEquals 'some data', response.text
    }

    void apiProjectExport_scm_old_api_v() {

        given:
            controller.apiService=Mock(ApiService){
                1 * requireApi(*_)>>true
            }
            controller.frameworkService = Mock(FrameworkService)
            controller.projectService=Mock(ProjectService){
                1 * exportProjectToOutputStream( _,_,_,_,{
                    !it.scm
                },_)>> {
                    it[2]<<'some data'
                }
            }
            setupGetResource()
            request.api_version = 11
            params.project = 'test1'
            params.exportScm='true'
        when:
            controller.apiProjectExport()
        then:
            assertEquals HttpServletResponse.SC_OK, response.status
            assertEquals 'application/zip', response.contentType
            assertEquals 'some data', response.text
    }

    void apiProjectExport_scm_success_v28() {

        given:
            controller.apiService=Mock(ApiService){
                1 * requireApi(*_)>>true
            }
            controller.frameworkService = Mock(FrameworkService)
            controller.projectService=Mock(ProjectService){
                1 * exportProjectToOutputStream( _,_,_,_,{
                    it.scm
                },_)>> {
                    it[2]<<'some data'
                }
                0*_(*_)
            }
            setupGetResource()
            request.api_version = 28
            params.project = 'test1'
            params.exportScm='true'
        when:
            controller.apiProjectExport()
        then:

            assertEquals HttpServletResponse.SC_OK, response.status
            assertEquals 'application/zip', response.contentType
            assertEquals 'some data', response.text

    }

    void "apiProjectExport requires api"() {
        given:
            controller.apiService=Mock(ApiService)
            controller.rundeckAppAuthorizer=Mock(AppAuthorizer)
        when:
            params.project = 'test1'
            controller.apiProjectExport()
        then:

            1 * controller.apiService.requireApi(_,_)>>false
            0 * controller.rundeckAppAuthorizer._(*_)
    }


    void apiProjectImport_invalidFormat() {
        given:
            controller.apiService=Mock(ApiService)
            controller.frameworkService = Mock(FrameworkService)

            params.project = 'test1'
            request.format='blah'
            request.method='PUT'

            request.api_version=11
            setupGetResource()
        when:
            controller.apiProjectImport()
        then:
            1 * controller.apiService.requireApi(_,_)>>true
            1 * controller.apiService.requireRequestFormat(_,_,['application/zip'])>>false
    }

    @Unroll
    void "apiProjectImport #format failure"() {
        
        given:
            controller.frameworkService = Mock(FrameworkService)
            controller.projectService=Mock(ProjectService){
                1 * importToProject(_,_,_,_,_)>>{
                    [success:false,joberrors:['error1','error2']]
                }
            }
            params.project = 'test1'
            request.format='application/zip'
            response.format=format
            session.user='user1'
            request.method='PUT'
            request.api_version=11
            setupGetResource()
            setupImportApiService(format)
        when:
            controller.apiProjectImport(new ProjectArchiveParams(project:'test1'))

        then:
            assertEquals HttpServletResponse.SC_OK,response.status
            response.text==expect
        where:
            format | expect
            'xml'|'''<import status='failed' successful='false'>
  <errors count='2'>
    <error>error1</error>
    <error>error2</error>
  </errors>
</import>'''
            'json'|'{"import_status":"failed","successful":false,"errors":["error1","error2"]}'
    }

    private setupImportApiService(String format){
        controller.apiService=Mock(ApiService){
            1 * requireApi(_,_)>>true
            1 * extractResponseFormat(*_)>>format
            1 * requireRequestFormat(_,_,['application/zip'])>>true
            (format=='xml'?1:0) * renderSuccessXml(_,_,_ as Closure)>> {
                def mb = new MarkupBuilder(it[1].writer)
                it[2].delegate=mb
                it[2].resolveStrategy=Closure.DELEGATE_FIRST
                it[2].call()
            }
        }
    }

    @Unroll
    void "apiProjectImport #format success"() {
        
        given:
            setupImportApiService(format)
            controller.frameworkService = Mock(FrameworkService)
            controller.projectService=Mock(ProjectService){
                1 * importToProject(
                    _, _, _, _, {
                    it.importExecutions
                    it.jobUuidOption == 'preserve'
                    !it.importConfig
                    !it.importACL
                }
                ) >> [success: true]
                0*_(*_)
            }
            setupGetResource()
            request.api_version = 11
            params.project = 'test1'
            request.format='application/zip'
            response.format=format
            session.user='user1'
            request.method='PUT'
        when:
            controller.apiProjectImport()

        then:
            assertEquals HttpServletResponse.SC_OK,response.status
            response.text==expect
        where:
            format| expect
            'xml' | '<import status=\'successful\' successful=\'true\' />'
            'json'|'{"import_status":"successful","successful":true}'
    }



    @Unroll
    void "apiProjectImport #param #val"() {
        
        given:
            setupImportApiService('json')
            controller.frameworkService = Mock(FrameworkService)
            controller.projectService=Mock(ProjectService){
                1 * importToProject(
                    _, _, _, _, {
                    it."$param"==val
                }
                ) >> [success: true]
                0*_(*_)
            }
            setupGetResource()
            request.api_version = 11
            params.project = 'test1'
            params."$param"=val.toString()
            request.format='application/zip'
            session.user='user1'
            request.method='PUT'
        when:
            controller.apiProjectImport()

        then:
            assertEquals HttpServletResponse.SC_OK,response.status
        where:
            param              | val
            'importExecutions' | true
            'importExecutions' | false
            'jobUuidOption'    | 'preserve'
            'jobUuidOption'    | 'remove'
    }




    void apiProjectImport_jobUuidOption_invalidValue() {
        given:
            setupImportApiService('json')
            controller.frameworkService =Mock(FrameworkService)
            controller.projectService=Mock(ProjectService){
                0*_(*_)
            }
            setupGetResource()
            request.api_version = 11
            params.project = 'test1'
            params.jobUuidOption='blah'
            request.format='application/zip'
            response.format='json'
            session.user='user1'
            request.method='PUT'
        when:
            controller.apiProjectImport()
        then:
            1 * controller.apiService.renderErrorFormat(_, {
                it.status == HttpServletResponse.SC_BAD_REQUEST
                it.code== 'api.error.invalid.request'
                it.args==['Property [jobUuidOption] of class [class com.dtolabs.rundeck.app.support.ProjectArchiveParams] with value [blah] is not contained within the list [[preserve, remove]]']
            })
    }


    void apiProjectImport_importAcl_unauthorized() {
        
        given:
            setupImportApiService('json')
            controller.frameworkService = Mock(FrameworkService)
            controller.projectService=Mock(ProjectService)
            setupGetResource()
            controller.rundeckAuthContextProcessor = Mock(AppAuthContextProcessor){
                1 * authResourceForProjectAcl('test')>>[acl:true]
                0 *authorizeApplicationResourceAny(_, null, ['import', AuthConstants.ACTION_ADMIN, AuthConstants.ACTION_APP_ADMIN]) >> true
                1 * authorizeApplicationResourceAny(_, [acl:true], [AuthConstants.ACTION_CREATE, AuthConstants.ACTION_ADMIN, AuthConstants.ACTION_APP_ADMIN]) >> false
            }
            request.api_version = 11
            params.project = 'test1'
            params.importACL='true'
            request.format='application/zip'
            response.format='json'
            session.user='user1'
            request.method='PUT'
        when:
            controller.apiProjectImport()
        then:
            with(controller.projectService){
                0 * importToProject(*_)>>[success:true]
            }
            1 * controller.apiService.renderErrorFormat(_,
                [
                    status: HttpServletResponse.SC_FORBIDDEN,
                    code  : "api.error.item.unauthorized",
                    args  : [AuthConstants.ACTION_CREATE, "ACL for Project", 'test']
                ]
            )
    }


    void apiProjectImport_importAcl_authorized() {

        given:
            setupImportApiService('json')
            controller.frameworkService = Mock(FrameworkService)
            controller.projectService=Mock(ProjectService)
            setupGetResource()
            controller.rundeckAuthContextProcessor = Mock(AppAuthContextProcessor){
                1 * authResourceForProjectAcl('test')>>[acl:true]
                0 *authorizeApplicationResourceAny(_, null, ['import', AuthConstants.ACTION_ADMIN, AuthConstants.ACTION_APP_ADMIN]) >> true
                1 * authorizeApplicationResourceAny(_, [acl:true], [AuthConstants.ACTION_CREATE, AuthConstants.ACTION_ADMIN, AuthConstants.ACTION_APP_ADMIN]) >> true
            }
            request.api_version = 11
            params.project = 'test1'
            params.importACL='true'
            request.format='application/zip'
            response.format='json'
            session.user='user1'
            request.method='PUT'
        when:
            controller.apiProjectImport()
        then:

            with(controller.projectService){
                1 * importToProject(*_)>>[success:true]
            }
            assertEquals HttpServletResponse.SC_OK,response.status
            assertEquals( [
                                  import_status: 'successful',
                                  successful   : true
                          ],
                          response.json
            )
            assertEquals null,response.json.errors
    }


    void apiProjectImport_importScm_unauthorized() {
        given:
            setupImportApiService('json')
            controller.frameworkService = Mock(FrameworkService)
            controller.projectService=Mock(ProjectService)
            controller.rundeckAppAuthorizer = Mock(AppAuthorizer) {
                2 * project(_, _) >> Mock(AuthorizingProject) {
                    1 * getResource() >> {
                        Stub(IRundeckProject){
                            getName()>>'test'
                        }
                    }
                    1 * authorize(RundeckAccess.Project.APP_CONFIGURE)>>{
                        throw new UnauthorizedAccess('configure','project','test')
                    }
                    0*_(*_)
                }
                0*_(*_)
            }
            controller.rundeckAuthContextProcessor = Mock(AppAuthContextProcessor)

            request.api_version = 28
            params.project = 'test1'
            params.importScm='true'
            request.format='application/zip'
            response.format='json'
            session.user='user1'
            request.method='PUT'
        when:
            controller.apiProjectImport()
        then:
            1 * controller.rundeckExceptionHandler.handleException(_,_,_ as UnauthorizedAccess)
    }


    void apiProjectImport_importScm_authorized() {
        given:
            setupImportApiService('json')
            controller.frameworkService = Mock(FrameworkService)
            controller.projectService=Mock(ProjectService)
            controller.rundeckAppAuthorizer = Mock(AppAuthorizer) {
                2 * project(_, _) >> Mock(AuthorizingProject) {
                    1 * getResource() >> {
                        Stub(IRundeckProject){
                            getName()>>'test'
                        }
                    }
                    1 * authorize(RundeckAccess.Project.APP_CONFIGURE)
                    0*_(*_)
                }
                0*_(*_)
            }
            controller.rundeckAuthContextProcessor = Mock(AppAuthContextProcessor)

            request.api_version = 28
            params.project = 'test1'
            params.importScm='true'
            request.format='application/zip'
            response.format='json'
            session.user='user1'
            request.method='PUT'
        when:
            controller.apiProjectImport()

        then:
            with(controller.projectService){
                1 * importToProject(*_)>>[success:true]
            }
            assertEquals HttpServletResponse.SC_OK,response.status
            assertEquals( [
                    import_status: 'successful',
                    successful   : true
            ],
                    response.json
            )
            assertEquals null,response.json.errors
    }



    void apiProjectList_json_date_v33_creation_time(){
        when:
        def dbProjA = new Project(name: 'testproject')
        dbProjA.save()
        def prja = new MockFor(IRundeckProject)
        prja.demand.getName(1..3) { -> 'testproject'}
        prja.demand.getProjectProperties(1..2){ -> [:]}
        prja.demand.getConfigCreatedTime(){->new Date()}
        def dbProjB = new Project(name: 'testproject2')
        dbProjB.save()
        def prjb = new MockFor(IRundeckProject)
        prjb.demand.getName(1..3) { -> 'testproject2'}
        prjb.demand.getProjectProperties(1..2){ -> [:]}
        prjb.demand.getConfigCreatedTime(){->new Date()}
        controller.frameworkService = mockWith(FrameworkService) {
            projects(1..1) { auth ->
                [
                        prja.proxyInstance(),
                        prjb.proxyInstance(),
                ]
            }
        }
            controller.rundeckAuthContextProcessor = Mock(AppAuthContextProcessor)
        controller.apiService=mockWith(ApiService){
            requireApi(1..1){req,resp->true}
        }
        request.setAttribute('api_version', 33)
        response.format='json'
        controller.apiProjectList()
        def base='http://localhost:8080/api/'+ApiVersions.API_CURRENT_VERSION
        then:
        assert response.status == HttpServletResponse.SC_OK
        assert response.json.size()==2
        assert response.json[0].name=='testproject'
        assert response.json[0].description==''
        assert response.json[0].url==base+'/project/testproject'
        assert response.json[0].created != null
        assert response.json[1].name=='testproject2'
        assert response.json[1].description==''
        assert response.json[1].url==base+'/project/testproject2'
        assert response.json[1].created != null
    }
}
