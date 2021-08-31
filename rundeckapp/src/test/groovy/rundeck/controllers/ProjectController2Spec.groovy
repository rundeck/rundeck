
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
import com.dtolabs.rundeck.app.support.ProjectArchiveExportRequest
import com.dtolabs.rundeck.app.support.ProjectArchiveImportRequest
import com.dtolabs.rundeck.app.support.ProjectArchiveParams
import com.dtolabs.rundeck.core.authentication.Group
import com.dtolabs.rundeck.core.authentication.Username
import com.dtolabs.rundeck.core.authorization.UserAndRolesAuthContext
import com.dtolabs.rundeck.core.common.IRundeckProject
import grails.test.hibernate.HibernateSpec
import grails.testing.web.controllers.ControllerUnitTest
import groovy.mock.interceptor.MockFor
import groovy.mock.interceptor.StubFor
import org.grails.plugins.codecs.JSONCodec
import org.grails.plugins.testing.GrailsMockHttpServletResponse
import org.rundeck.app.authorization.AppAuthContextProcessor
import org.rundeck.core.auth.AuthConstants
import org.springframework.context.MessageSource
import rundeck.Project
import rundeck.services.ApiService
import rundeck.services.FrameworkService
import rundeck.services.ProjectService

import javax.security.auth.Subject
import javax.servlet.http.HttpServletResponse

import static org.junit.Assert.*

class ProjectController2Spec extends HibernateSpec implements ControllerUnitTest<ProjectController> {

    List<Class> getDomainClasses() { [Project] }

    def setup(){
        controller.apiService = new ApiService()
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
        when:
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

    void apiProjectGet_missingProjectParam(){
        when:
        controller.frameworkService = mockWith(FrameworkService){
        }
            controller.rundeckAuthContextProcessor = Mock(AppAuthContextProcessor)

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
        then:
        assert response.status==HttpServletResponse.SC_BAD_REQUEST
    }

    void apiProjectGet_unauthorized(){
        when:
        controller.frameworkService = mockWith(FrameworkService){
        }
            controller.rundeckAuthContextProcessor = Mock(AppAuthContextProcessor){
                1 * authResourceForProject('test1')
                1 * authorizeApplicationResourceAny(_,_,[AuthConstants.ACTION_READ,AuthConstants.ACTION_ADMIN])>>false
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
        then:
        assert response.status==HttpServletResponse.SC_FORBIDDEN
    }

    void apiProjectGet_notfound(){
        when:
        controller.frameworkService = mockWith(FrameworkService){
            existsFrameworkProject(1..1) { proj ->
                assertEquals('test1',proj)
                false
            }
        }
            controller.rundeckAuthContextProcessor = Mock(AppAuthContextProcessor){
                1 * authResourceForProject('test1')
                1 * authorizeApplicationResourceAny(_,_,[AuthConstants.ACTION_READ,AuthConstants.ACTION_ADMIN])>>true
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
        then:
        assert response.status==HttpServletResponse.SC_NOT_FOUND
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

    void apiProjectGet_xml_noconfig_v11(){
        when:
        defineBeans {
            apiService(ApiService)
        }
        controller.frameworkService = createFrameworkService(false, 'test1')

            controller.rundeckAuthContextProcessor = Mock(AppAuthContextProcessor){
                2 * authResourceForProject('test1')
                1 * authorizeApplicationResourceAny(_,_,[AuthConstants.ACTION_READ,AuthConstants.ACTION_ADMIN])>>true
                1 * authorizeApplicationResourceAny(_,_,[AuthConstants.ACTION_CONFIGURE,AuthConstants.ACTION_ADMIN])>>false
            }

        response.format='xml'
        params.project='test1'
        request.setAttribute('api_version', 11) //do not include <result> wrapper
        controller.apiProjectGet()
        then:
        assert response.status==HttpServletResponse.SC_OK

        //XML result has no wrapper
        assertEquals 'project', response.xml.name()
        assertEquals 0, response.xml.result.size()
        assertEquals 0, response.xml.projects.size()

        when:
        def project=response.xml

        //test project element
        then:
        assertEquals 'test1', project.name.text()
        assertEquals '', project.description.text()
        assertEquals 0, project.config.size()
    }
    /**
     * apiversion {@literal <} 11 will result in no {@literal <config>} element, even if authorized
     */
    /**
     * apiversion {@literal >=} 11 will result in {@literal <config>} element, if authorized
     */
    void apiProjectGet_xml_withconfig_v11(){

        when:
        defineBeans {
            apiService(ApiService)
        }
        controller.frameworkService = createFrameworkService(true, 'test1', ["test.property": "value1", "test.property2": "value2"])

            controller.rundeckAuthContextProcessor = Mock(AppAuthContextProcessor){
                2 * authResourceForProject('test1')
                1 * authorizeApplicationResourceAny(_,_,[AuthConstants.ACTION_READ,AuthConstants.ACTION_ADMIN])>>true
                1 * authorizeApplicationResourceAny(_,_,[AuthConstants.ACTION_CONFIGURE,AuthConstants.ACTION_ADMIN])>>true
            }
        response.format='xml'
        params.project='test1'
        request.setAttribute('api_version', 11) //do not include <result> wrapper
        controller.apiProjectGet()

        then:
        assert response.status==HttpServletResponse.SC_OK

        //XML result has wrapper
        assertEquals 'project', response.xml.name()

        assertEquals 0, response.xml.projects.size()
        assertEquals 0, response.xml.project.size()

        when:
        def project = response.xml

        then:
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

    void apiProjectGet_json_noconfig() {
        when:
        defineBeans {
            apiService(ApiService)
        }
        controller.frameworkService = createFrameworkService(false, 'test1')

            controller.rundeckAuthContextProcessor = Mock(AppAuthContextProcessor){
                2 * authResourceForProject('test1')
                1 * authorizeApplicationResourceAny(_,_,[AuthConstants.ACTION_READ,AuthConstants.ACTION_ADMIN])>>true
                1 * authorizeApplicationResourceAny(_,_,[AuthConstants.ACTION_CONFIGURE,AuthConstants.ACTION_ADMIN])>>false
            }
        response.format = 'json'
        params.project = 'test1'
        request.setAttribute('api_version', 11) // trigger xml <result> wrapper
        controller.apiProjectGet()

        then:
        assert response.status == HttpServletResponse.SC_OK

        when:
        def project = response.json

        then:
        //test project element
        assertEquals 'test1', project.name
        assertEquals '', project.description
        assertEquals null, project.config
    }

    void apiProjectGet_json_withconfig() {
        when:
        defineBeans {
            apiService(ApiService)
        }
        controller.frameworkService = createFrameworkService(true, 'test1',['test.property':'value1',
                'test.property2':'value2'])

            controller.rundeckAuthContextProcessor = Mock(AppAuthContextProcessor){
                2 * authResourceForProject('test1')
                1 * authorizeApplicationResourceAny(_,_,[AuthConstants.ACTION_READ,AuthConstants.ACTION_ADMIN])>>true
                1 * authorizeApplicationResourceAny(_,_,[AuthConstants.ACTION_CONFIGURE,AuthConstants.ACTION_ADMIN])>>true
            }
        response.format = 'json'
        params.project = 'test1'
        request.setAttribute('api_version', 11) // trigger xml <result> wrapper
        controller.apiProjectGet()
        then:
        assert response.status == HttpServletResponse.SC_OK

        when:
        def project = response.json

        then:
        //test project element
        assertEquals 'test1', project.name
        assertEquals '', project.description
        assertEquals(['test.property': 'value1', 'test.property2': 'value2'], project.config)
    }

    void apiProjectCreate_xml_unauthorized() {
        when:
        defineBeans {
            apiService(ApiService)
        }
        controller.apiService.messageSource= mockWith(MessageSource) {
            getMessage{code,args,defval,locale->
                code
            }
        }
        setupProjectCreate(controller, false, true, null, null, null)

        request.xml='<project><name>test1</name></project>'
        request.method='POST'
        response.format = 'xml'
        request.setAttribute('api_version', 11) // require version 11
        controller.apiProjectCreate()

        then:
        assert response.status == HttpServletResponse.SC_FORBIDDEN

        when:
        def result = response.xml

        then:
        //test project element
        assertEquals 'true', result.'@error'.text()
        assertEquals 'api.error.item.unauthorized', result.error.'@code'.text()
        assertEquals 'api.error.item.unauthorized', result.error.message.text()
    }

    void apiProjectCreate_json_unauthorized() {
        when:
        defineBeans {
            apiService(ApiService)
        }
        mockCodec(JSONCodec)
        controller.apiService.messageSource= mockWith(MessageSource) {
            getMessage{code,args,defval,locale->
                code
            }
        }
        setupProjectCreate(controller, false, true, null, null, null)

        request.json='{"name":"test1"}'
        request.method='POST'
        response.format = 'json'
        request.setAttribute('api_version', 11) // require version 11
        controller.apiProjectCreate()
        then:
        assert response.status == HttpServletResponse.SC_FORBIDDEN

        when:
        def result = response.json

        then:
        //test project element
        assertEquals true, result.error
        assertEquals 'api.error.item.unauthorized', result.errorCode
        assertEquals 'api.error.item.unauthorized', result.message
    }
    /**
     * Missing project name element
     */
    void apiProjectCreate_xml_invalid() {
        when:
        defineBeans {
            apiService(ApiService)
        }
        controller.apiService.messageSource= mockWith(MessageSource) {
            getMessage{code,args,defval,locale->
                code
            }
        }
        controller.rundeckAuthContextProcessor = Mock(AppAuthContextProcessor){
            _ * authResourceForProject('test1')
            1 * authorizeApplicationResourceTypeAll(_,'project',[AuthConstants.ACTION_CREATE])>>true
        }
        request.xml='<project><namex>test1</namex></project>'
        request.method='POST'
        response.format = 'xml'
        request.setAttribute('api_version', 11) // require version 11
        controller.apiProjectCreate()
        then:
        assert response.status == HttpServletResponse.SC_BAD_REQUEST

        when:
        def result = response.xml

        then:
        //test project element
        assertEquals 'true', result.'@error'.text()
        assertEquals 'api.error.invalid.request', result.error.'@code'.text()
        assertEquals 'api.error.invalid.request', result.error.message.text()
    }/**
     * Missing project name element
     */
    void apiProjectCreate_json_invalid() {
        when:
        controller.apiService = new ApiService()
        mockCodec(JSONCodec)
        controller.apiService.messageSource= mockWith(MessageSource) {
            getMessage{code,args,defval,locale->
                code
            }
        }

            controller.rundeckAuthContextProcessor = Mock(AppAuthContextProcessor){
                _ * authResourceForProject('test1')
                1 * authorizeApplicationResourceTypeAll(_,'project',[AuthConstants.ACTION_CREATE])>>true
            }
        request.json='{"blame":"monkey"}'
        request.method='POST'
        response.format = 'json'
        request.setAttribute('api_version', 11) // require version 11
        controller.apiProjectCreate()
        assert response.status == HttpServletResponse.SC_BAD_REQUEST

        def result = response.json

        then:
        //test project element
        assertEquals true, result.error
        assertEquals 'api.error.invalid.request', result.errorCode
        assertEquals 'api.error.invalid.request', result.message
    }
    /**
     * project already exists
     */
    void apiProjectCreate_xml_projectExists() {
        when:
        controller.apiService = new ApiService()
        controller.apiService.messageSource= mockWith(MessageSource){
            getMessage{code,args,defval,locale->
                code
            }
        }

        setupProjectCreate(controller, true, true, null, null, null)
        request.xml='<project><name>test1</name></project>'
        request.method='POST'
        response.format = 'xml'
        request.setAttribute('api_version', 11) // require version 11
        controller.apiProjectCreate()
        assert response.status == HttpServletResponse.SC_CONFLICT

        def result = response.xml

        //test project element
        then:
        assertEquals 'true', result.'@error'.text()
        assertEquals 'api.error.item.alreadyexists', result.error.'@code'.text()
        assertEquals 'api.error.item.alreadyexists', result.error.message.text()
    }
    /**
     * project already exists
     */
    void apiProjectCreate_json_projectExists() {
        when:
        controller.apiService = new ApiService()
        mockCodec(JSONCodec)
        controller.apiService.messageSource= mockWith(MessageSource){
            getMessage{code,args,defval,locale->
                code
            }
        }

        setupProjectCreate(controller, true, true, null, null, null)
        request.json = '{"name":"test1"}'
        request.method='POST'
        response.format = 'json'
        request.setAttribute('api_version', 11) // require version 11
        controller.apiProjectCreate()
        assert response.status == HttpServletResponse.SC_CONFLICT

        def result = response.json

        //test project element
        then:
        assertEquals true, result.error
        assertEquals 'api.error.item.alreadyexists', result.errorCode
        assertEquals 'api.error.item.alreadyexists', result.message
    }
    /**
     * Failure to create project
     */
    void apiProjectCreate_xml_withErrors() {
        when:
        controller.apiService = new ApiService()
        controller.apiService.messageSource= mockWith(MessageSource){
            getMessage{code,args,defval,locale->
                code
            }
        }
        setupProjectCreate(controller, true, false, ['error1', 'error2'], [:], null)
        request.xml='<project><name>test1</name></project>'
        request.method='POST'
        response.format = 'xml'
        request.setAttribute('api_version', 11) // require version 11
        controller.apiProjectCreate()
        assert response.status == HttpServletResponse.SC_INTERNAL_SERVER_ERROR

        def result = response.xml

        //test project element
        then:
        assertEquals 'true', result.'@error'.text()
        assertEquals 1,result.error.'@code'.size()
        assertEquals 'api.error.unknown',result.error.'@code'.text()
        assertEquals 'error1; error2', result.error.message.text()
    }
    /**
     * Failure to create project
     */
    void apiProjectCreate_json_withErrors() {
        when:
        controller.apiService = new ApiService()
        mockCodec(JSONCodec)
        controller.apiService.messageSource= mockWith(MessageSource){
            getMessage{code,args,defval,locale->
                code
            }
        }
        setupProjectCreate(controller, true, false, ['error1', 'error2'], [:], null)
        request.json = '{"name":"test1"}'
        request.method='POST'
        response.format = 'json'
        request.setAttribute('api_version', 11) // require version 11
        controller.apiProjectCreate()
        assert response.status == HttpServletResponse.SC_INTERNAL_SERVER_ERROR

        def result = response.json

        //test project element
        then:
        assertEquals true, result.error
        assertEquals 'api.error.unknown', result.errorCode
        assertEquals 'error1; error2', result.message
    }
    /**
     * Successful
     */
    void apiProjectCreate_xml_success() {
        when:
        controller.apiService = new ApiService()
        controller.apiService.messageSource= mockWith(MessageSource){
            getMessage{code,args,defval,locale->
                code
            }
        }
        setupProjectCreate(controller, true, false, [], [:], ['prop1': 'value1', 'prop2': 'value2'])
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
        then:
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
    void apiProjectCreate_json_success() {
        when:
        controller.apiService = new ApiService()
        mockCodec(JSONCodec)
        controller.apiService.messageSource= mockWith(MessageSource){
            getMessage{code,args,defval,locale->
                code
            }
        }
        setupProjectCreate(controller, true, false, [],
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

        then:
        assertEquals "test1",project.name
        assertEquals 2, project.config.size()
        assertEquals 'value1', project.config['prop1']
        assertEquals 'value2', project.config['prop2']
    }
    /**
     * Create project with input config
     */
    void apiProjectCreate_xml_withconfig() {
        when:
        controller.apiService = new ApiService()
        controller.apiService.messageSource= mockWith(MessageSource){
            getMessage{code,args,defval,locale->
                code
            }
        }
        setupProjectCreate(controller, true, false, [],
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

        then:
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
    void apiProjectCreate_json_withconfig() {
        when:
        controller.apiService = new ApiService()
        mockCodec(JSONCodec)
        controller.apiService.messageSource= mockWith(MessageSource){ getMessage {code,args,locale-> code } }
        setupProjectCreate(controller, true, false, [],
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
        then:
        assertEquals "test1", project.name
        assertEquals 2, project.config.size()
        assertEquals 'value1', project.config['prop1']
        assertEquals 'value2', project.config['prop2']
    }

    private def setupProjectCreate(ProjectController controller, boolean authorized, boolean exists, List createErrors,
                                   Map<String, String> inputProps,
                                   Map<String, String> configProps) {

        controller.rundeckAuthContextProcessor = Mock(AppAuthContextProcessor){
            _ * authResourceForProject('test1')
            1 * authorizeApplicationResourceTypeAll(_,'project',[AuthConstants.ACTION_CREATE])>>authorized
        }
        def prja = Stub(IRundeckProject) {
            getName()>>'test1'
            getProjectProperties()>>[:]
        }
        controller.frameworkService=Mock(FrameworkService)

        if(authorized){
            1 * controller.frameworkService.existsFrameworkProject('test1')>>exists


            (exists?0:1)*controller.frameworkService.createFrameworkProject('test1',inputProps)>> [createErrors?.size() > 0 ? null: prja, createErrors]
            if(!exists){
                (createErrors?.size() > 0?0:1)*controller.frameworkService.loadProjectProperties(prja)>>configProps
            }
        }

    }

    void deleteProject_xml_missingparam(){
        given:
            controller.apiService = Mock(ApiService)
        when:

            request.method = 'DELETE'
            controller.apiProjectDelete()
        then:
            1 * controller.apiService.requireApi(*_) >> true
            1 * controller.apiService.renderErrorFormat(_,[
                status: HttpServletResponse.SC_BAD_REQUEST,
                code: "api.error.parameter.required",
                args: ['project']
            ])

    }

    void deleteProject_json_missingparam(){
        when:
        controller.apiService = new ApiService()
        controller.apiService.messageSource = mockWith(MessageSource) { getMessage { code, args,defval, locale -> code } }
        mockCodec(JSONCodec)
        request.method = 'DELETE'
        response.format='json'
        request.setAttribute('api_version', 11) // require version 11
        controller.apiProjectDelete()
        then:
        assert response.status == HttpServletResponse.SC_BAD_REQUEST
        assertEquals true, response.json.error
        assertEquals "api.error.parameter.required", response.json.errorCode
    }

    void deleteProject_xml_notfound(){
        when:
        controller.apiService = new ApiService()
        controller.apiService.messageSource = mockWith(MessageSource) { getMessage { code, args,defval, locale -> code } }
        mockCodec(JSONCodec)
        controller.frameworkService=mockFrameworkServiceForProjectDelete(false, false)
        request.method = 'DELETE'
        response.format='xml'
        params.project='test1'
        request.setAttribute('api_version', 11) // require version 11
        controller.apiProjectDelete()
        then:
        assert response.status == HttpServletResponse.SC_NOT_FOUND
        assertEquals "true", response.xml.'@error'.text()
        assertEquals "api.error.item.doesnotexist", response.xml.error.'@code'.text()
    }

    void deleteProject_json_notfound(){
        controller.apiService = new ApiService()
        controller.apiService.messageSource = mockWith(MessageSource) { getMessage { code, args,defval, locale -> code } }
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

    void deleteProject_xml_unauthorized(){
        when:

        controller.apiService = new ApiService()
        controller.apiService.messageSource = mockWith(MessageSource) { getMessage { code, args,defval, locale -> code } }
        mockCodec(JSONCodec)
        controller.frameworkService=mockFrameworkServiceForProjectDelete(true, false)
            controller.rundeckAuthContextProcessor = Mock(AppAuthContextProcessor){
                1 * authResourceForProject('test1')
                1 * authorizeApplicationResourceAny(_,_,[AuthConstants.ACTION_DELETE,AuthConstants.ACTION_ADMIN])>>false
            }
        request.method = 'DELETE'
        response.format='xml'
        params.project='test1'
        request.setAttribute('api_version', 11) // require version 11
        controller.apiProjectDelete()
        then:
        assert response.status == HttpServletResponse.SC_FORBIDDEN
        assertEquals "true", response.xml.'@error'.text()
        assertEquals "api.error.item.unauthorized", response.xml.error.'@code'.text()
    }

    void deleteProject_json_unauthorized(){
        when:
        controller.apiService = new ApiService()
        controller.apiService.messageSource = mockWith(MessageSource) { getMessage { code, args,defval, locale -> code } }
        mockCodec(JSONCodec)
        controller.frameworkService=mockFrameworkServiceForProjectDelete(true, false)
            controller.rundeckAuthContextProcessor = Mock(AppAuthContextProcessor){
                1 * authResourceForProject('test1')
                1 * authorizeApplicationResourceAny(_,_,[AuthConstants.ACTION_DELETE,AuthConstants.ACTION_ADMIN])>>false
            }
        request.method = 'DELETE'
        response.format='json'
        params.project='test1'
        request.setAttribute('api_version', 11) // require version 11
        controller.apiProjectDelete()
        then:
        assert response.status == HttpServletResponse.SC_FORBIDDEN
        assertEquals true, response.json.error
        assertEquals "api.error.item.unauthorized", response.json.errorCode
    }

    void deleteProject_xml_haserrors(){
        when:
        controller.apiService = new ApiService()
        controller.apiService.messageSource = mockWith(MessageSource) { getMessage { code, args,defval, locale -> code } }
        mockCodec(JSONCodec)
        controller.frameworkService=mockFrameworkServiceForProjectDelete(true, true)
        controller.projectService=mockProjectServiceForProjectDelete(false, 'deleteProjectFailed')
        controller.rundeckAuthContextProcessor = Mock(AppAuthContextProcessor){
            authorizeApplicationResourceAny(_,_,[AuthConstants.ACTION_DELETE,AuthConstants.ACTION_ADMIN])>>true
        }

            request.method = 'DELETE'
        response.format='xml'
        params.project='test1'
        request.setAttribute('api_version', 11) // require version 11
        controller.apiProjectDelete()
        then:
        assert response.status == HttpServletResponse.SC_INTERNAL_SERVER_ERROR
        assertEquals "true", response.xml.'@error'.text()
        assertEquals "deleteProjectFailed", response.xml.error.message.text()
    }



    void deleteProject_json_haserrors(){
        when:
        controller.apiService = new ApiService()
        controller.apiService.messageSource = mockWith(MessageSource) { getMessage { code, args,defval, locale -> code } }
        mockCodec(JSONCodec)
        controller.frameworkService=mockFrameworkServiceForProjectDelete(true, true)
        controller.projectService = mockProjectServiceForProjectDelete(false, 'deleteProjectFailed')
        controller.rundeckAuthContextProcessor = Mock(AppAuthContextProcessor){
            authorizeApplicationResourceAny(_,_,[AuthConstants.ACTION_DELETE,AuthConstants.ACTION_ADMIN])>>true
        }

            request.method = 'DELETE'
        response.format='json'
        params.project='test1'
        request.setAttribute('api_version', 11) // require version 11
        controller.apiProjectDelete()
        then:
        assert response.status == HttpServletResponse.SC_INTERNAL_SERVER_ERROR
        assertEquals true, response.json.error
        assertEquals "deleteProjectFailed", response.json.message
    }

    void deleteProject_xml_success(){
        when:
        controller.apiService = new ApiService()
        controller.apiService.messageSource = mockWith(MessageSource) { getMessage { code, args,defval, locale -> code } }
        mockCodec(JSONCodec)
        controller.frameworkService=mockFrameworkServiceForProjectDelete(true, true)
        controller.projectService=mockProjectServiceForProjectDelete(true, null)
        controller.rundeckAuthContextProcessor = Mock(AppAuthContextProcessor){
                authorizeApplicationResourceAny(_,_,[AuthConstants.ACTION_DELETE,AuthConstants.ACTION_ADMIN])>>true
            }

            request.method = 'DELETE'
        response.format='xml'
        params.project='test1'
        request.setAttribute('api_version', 11) // require version 11
        controller.apiProjectDelete()
        then:
        assert response.status == HttpServletResponse.SC_NO_CONTENT
    }



    void deleteProject_json_success(){
        when:
        controller.apiService = new ApiService()
        controller.apiService.messageSource = mockWith(MessageSource) { getMessage { code, args,defval, locale -> code } }
        mockCodec(JSONCodec)
        controller.frameworkService=mockFrameworkServiceForProjectDelete(true, true)
        controller.projectService = mockProjectServiceForProjectDelete(true, null)
        controller.rundeckAuthContextProcessor = Mock(AppAuthContextProcessor){
                authorizeApplicationResourceAny(_,_,[AuthConstants.ACTION_DELETE,AuthConstants.ACTION_ADMIN])>>true
            }

            request.method = 'DELETE'
        response.format='json'
        params.project='test1'
        request.setAttribute('api_version', 11) // require version 11
        controller.apiProjectDelete()
        then:
        assert response.status == HttpServletResponse.SC_NO_CONTENT
    }

    def mockProjectServiceForProjectDelete(boolean success, String errorMessage) {
        Mock(ProjectService) {
            1 * deleteProject (*_)>>[success: success, error: errorMessage]
        }
    }

    private def mockFrameworkServiceForProjectDelete(boolean exists, boolean authorized){
        Mock(FrameworkService){

            _*existsFrameworkProject(_)>>exists
            if(!authorized){
                return
            }
            getFrameworkProject('test1')>>Mock(IRundeckProject){
                getName()>>'test1'
            }
        }
    }
    private def mockFrameworkServiceForProjectConfigGet(boolean exists, boolean authorized, String action,
                                                        LinkedHashMap props, String textformat=null){
        Mock(FrameworkService){
            1 * existsFrameworkProject(_)>>exists
            if(!exists){
                return
            }
            if(!authorized){
                return
            }
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
        mockWith(FrameworkService){
            Map newConfigProps = [:]
            existsFrameworkProject{String name->
                exists
            }
            if(!exists){
                return
            }

            if(!authorized){
                return
            }
            getFrameworkProject{name->
                assertEquals('test1',name)
                Mock(IRundeckProject){
                    getName()>>'test1'
                    getProjectProperties()>>currentProps
                }
            }
            setFrameworkProjectConfig{proj,configProps->
//                assertEquals props,configProps
                newConfigProps = configProps
                [success: success,error: errorMessage]
            }
            if(handleScheduling) {
                handleProjectSchedulingEnabledChange { String proj, isExecutionDisabledNow, isScheduleDisabledNow, newExecutionDisabledStatus, newScheduleDisabledStatus ->

                }
            }
            if(!success){
                return
            }
            loadProjectProperties { proj ->
                newConfigProps
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
    private def mockFrameworkServiceForProjectExport(boolean exists, boolean authorized, String action,
                                                     boolean isacl=false,boolean aclauth=false,
                                                     boolean isscm=false, boolean scmauth=false){
        mockWith(FrameworkService){
            existsFrameworkProject{String name->
                exists
            }
            if(!exists){
                return
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



            getAuthContextForSubjectAndProject{subj,proj->
                null
            }
        }
    }
    private def mockFrameworkServiceForProjectImport(boolean exists, boolean authorized, String action,
                                                     boolean isacl=false,boolean aclauth=false,
                                                     boolean isscm=false,boolean scmauth=false){
        mockWith(FrameworkService){
            existsFrameworkProject{String name->
                exists
            }
            if(!exists){
                return
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
            getAuthContextForSubject{subj->null}


            if(isscm){
                authResourceForProject{ name ->
                    assertEquals("test1", name)
                    [admin: true]
                }
                authorizeApplicationResourceAll(1..1){ctx,resource,actions->
                    aassertTrue(AuthConstants.ACTION_CONFIGURE in actions)
                    assertTrue(AuthConstants.ACTION_ADMIN in actions)
                    scmauth
                }
            }
            getAuthContextForSubjectAndProject{subj,proj->
                null
            }
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

            1 * controller.apiService.requireApi(_,_)>>true
            1 * controller.apiService.renderErrorFormat(response,
                                         [
                                             status: HttpServletResponse.SC_BAD_REQUEST,
                                             code: "api.error.parameter.required",
                                             args: ['project']
                                         ])
    }

    private void assertXmlError(GrailsMockHttpServletResponse response, int status, String code) {
        assertEquals status, response.status
        assertEquals "true", response.xml.'@error'.text()
        assertEquals code, response.xml.error.message.text()
    }


    void apiProjectConfigGet_json_missingparam(){
        when:
        controller.apiService = new ApiService()
        mockCodec(JSONCodec)
        controller.apiService.messageSource = mockWith(MessageSource) { getMessage { code, args,defval, locale -> code } }
        controller.frameworkService= Mock(FrameworkService)
        request.api_version = 11
        response.format='json'
        controller.apiProjectConfigGet()
        then:
        assertEquals HttpServletResponse.SC_BAD_REQUEST, response.status
        assertEquals true, response.json.error
        assertEquals "api.error.parameter.required", response.json.errorCode
    }

    void apiProjectConfigGet_xml_notfound(){
        given:
            controller.apiService = Mock(ApiService)
        when:
        controller.frameworkService= mockFrameworkServiceForProjectConfigGet(false, false, 'read', [:])
        request.api_version = 11
        params.project='test1'
        controller.apiProjectConfigGet()
        then:

            1 * controller.apiService.requireApi(_,_)>>true
            1 * controller.apiService.renderErrorFormat(response,
                                                        [
                                                            status: HttpServletResponse.SC_NOT_FOUND,
                                                            code: "api.error.item.doesnotexist",
                                                            args: ['Project', 'test1']
                                                        ])
    }

    void apiProjectConfigGet_json_notfound(){
        when:
        controller.apiService = new ApiService()
        mockCodec(JSONCodec)
        controller.apiService.messageSource = mockWith(MessageSource) { getMessage { code, args,defval, locale -> code } }
        controller.frameworkService= mockFrameworkServiceForProjectConfigGet(false, false, 'read', [:])
        request.api_version = 11
        params.project = 'test1'
        response.format='json'
        controller.apiProjectConfigGet()
        then:
        assertEquals HttpServletResponse.SC_NOT_FOUND, response.status
        assertEquals true, response.json.error
        assertEquals "api.error.item.doesnotexist", response.json.errorCode
    }

    void apiProjectConfigGet_xml_unauthorized(){
        given:
            controller.apiService=Mock(ApiService)
        when:
        controller.apiService.messageSource = mockWith(MessageSource) { getMessage { code, args,defval, locale -> code } }
        controller.frameworkService= mockFrameworkServiceForProjectConfigGet(true, false, 'configure', [:])

            controller.rundeckAuthContextProcessor = Mock(AppAuthContextProcessor){
                1 * authResourceForProject('test1')
                1 * authorizeApplicationResourceAny(_,_,[AuthConstants.ACTION_CONFIGURE,AuthConstants.ACTION_ADMIN])>>false

            }
        request.api_version = 11
        params.project='test1'
        controller.apiProjectConfigGet()
        then:
            1 * controller.apiService.requireApi(_,_)>>true
            1 * controller.apiService.renderErrorFormat(_,
                                             [
                                                 status: HttpServletResponse.SC_FORBIDDEN,
                                                 code: "api.error.item.unauthorized",
                                                 args: ['configure', "Project", 'test1']
                                             ])
    }

    void apiProjectConfigGet_json_unauthorized(){
        when:
        controller.apiService = new ApiService()
        mockCodec(JSONCodec)
        controller.apiService.messageSource = mockWith(MessageSource) { getMessage { code, args,defval, locale -> code } }
        controller.frameworkService= mockFrameworkServiceForProjectConfigGet(true, false, 'configure', [:])

            controller.rundeckAuthContextProcessor = Mock(AppAuthContextProcessor){
                1 * authResourceForProject('test1')
                1 * authorizeApplicationResourceAny(_,_,[AuthConstants.ACTION_CONFIGURE,AuthConstants.ACTION_ADMIN])>>false

            }
        request.api_version = 11
        params.project = 'test1'
        response.format='json'
        controller.apiProjectConfigGet()
        then:
        assertEquals HttpServletResponse.SC_FORBIDDEN, response.status
        assertEquals true, response.json.error
        assertEquals "api.error.item.unauthorized", response.json.errorCode
    }

    void apiProjectConfigGet_xml_success(){
        when:
        controller.apiService = new ApiService()
        controller.apiService.messageSource = mockWith(MessageSource) { getMessage { code, args,defval, locale -> code } }
        controller.frameworkService= mockFrameworkServiceForProjectConfigGet(true, true, 'configure', ["prop1": "value1", "prop2": "value2"])

            controller.rundeckAuthContextProcessor = Mock(AppAuthContextProcessor){
                authorizeApplicationResourceAny(_, _, ['configure', AuthConstants.ACTION_ADMIN]) >> true
            }
        request.api_version = 11
        params.project='test1'
        controller.apiProjectConfigGet()
        then:
        assertEquals HttpServletResponse.SC_OK, response.status
        assertEquals "config",response.xml.name()
        assertEquals 2,response.xml.property.size()
        assertEquals 'prop1',response.xml.property[0].'@key'.text()
        assertEquals 'value1',response.xml.property[0].'@value'.text()
        assertEquals 'prop2',response.xml.property[1].'@key'.text()
        assertEquals 'value2',response.xml.property[1].'@value'.text()
    }

    void apiProjectConfigGet_json_success(){
        when:
        controller.apiService = new ApiService()
        mockCodec(JSONCodec)
        controller.apiService.messageSource = mockWith(MessageSource) { getMessage { code, args,defval, locale -> code } }
        controller.frameworkService= mockFrameworkServiceForProjectConfigGet(true, true, 'configure', ["prop1": "value1", "prop2": "value2"])
            controller.rundeckAuthContextProcessor = Mock(AppAuthContextProcessor){
                authorizeApplicationResourceAny(_, _, ['configure', AuthConstants.ACTION_ADMIN]) >> true
            }
        request.api_version = 11
        params.project = 'test1'
        response.format='json'
        controller.apiProjectConfigGet()
        then:
        assertEquals HttpServletResponse.SC_OK, response.status
        assertEquals "value1",response.json.prop1
        assertEquals "value2",response.json.prop2
    }

    void apiProjectConfigGet_text_success(){
        when:
        controller.apiService = new ApiService()
        mockCodec(JSONCodec)
        controller.apiService.messageSource = mockWith(MessageSource) { getMessage { code, args,defval, locale -> code } }
        controller.frameworkService= mockFrameworkServiceForProjectConfigGet(true, true, 'configure', [:],
                "text format for properties")
            controller.rundeckAuthContextProcessor = Mock(AppAuthContextProcessor){
                authorizeApplicationResourceAny(_, _, ['configure', AuthConstants.ACTION_ADMIN]) >> true
            }
        request.api_version = 11
        params.project = 'test1'
        response.format='text'
        controller.apiProjectConfigGet()
        then:
        assertEquals HttpServletResponse.SC_OK, response.status
        assertTrue response.text.startsWith("#\n#")
    }


    void apiProjectConfigPut_xml_success(){
        when:
        //controller.apiService.messageSource = mockWith(MessageSource) { getMessage { code, args,defval, locale -> code } }
        controller.apiService = new ApiService()
        controller.frameworkService= mockFrameworkServiceForProjectConfigPut(true, true, 'configure', ['prop1': 'value1',
                prop2: 'value2'], true, null, 'text')
            controller.rundeckAuthContextProcessor = Mock(AppAuthContextProcessor){
                authorizeApplicationResourceAny(_, _, ['configure', AuthConstants.ACTION_ADMIN]) >> true
            }
        request.api_version = 11
        params.project = 'test1'
        request.method='PUT'
        request.xml='<config><property key="prop1" value="value1"/><property key="prop2" value="value2"/></config>'
        controller.apiProjectConfigPut()
        then:
        assertEquals HttpServletResponse.SC_OK, response.status
        assertEquals "config", response.xml.name()
        assertEquals 2, response.xml.property.size()
        assertEquals 'prop2', response.xml.property[0].'@key'.text()
        assertEquals 'value2', response.xml.property[0].'@value'.text()
        assertEquals 'prop1', response.xml.property[1].'@key'.text()
        assertEquals 'value1', response.xml.property[1].'@value'.text()
    }

    void apiProjectConfigPut_xml_disabling_execution_success(){
        when:
        //controller.apiService.messageSource = mockWith(MessageSource) { getMessage { code, args,defval, locale -> code } }
        controller.apiService = new ApiService()
        controller.frameworkService= mockFrameworkServiceForProjectConfigPut(true, true, 'configure',
                ['project.disable.executions': 'false'], true, null, 'text', true)
            controller.rundeckAuthContextProcessor = Mock(AppAuthContextProcessor){
                authorizeApplicationResourceAny(_, _, ['configure', AuthConstants.ACTION_ADMIN]) >> true
            }
        request.api_version = 11
        params.project = 'test1'
        request.method='PUT'
        request.xml='<config><property key="project.disable.executions" value="true"/></config>'
        controller.apiProjectConfigPut()
        then:
        assertEquals HttpServletResponse.SC_OK, response.status
        assertEquals "config", response.xml.name()
        assertEquals 1, response.xml.property.size()
        assertEquals 'project.disable.executions', response.xml.property[0].'@key'.text()
        assertEquals 'true', response.xml.property[0].'@value'.text()
    }

    void apiProjectConfigPut_xml_disabling_schedule_success(){
        when:
        //controller.apiService.messageSource = mockWith(MessageSource) { getMessage { code, args,defval, locale -> code } }
        controller.apiService = new ApiService()
        controller.frameworkService= mockFrameworkServiceForProjectConfigPut(true, true, 'configure',
                ['project.disable.schedule': 'false'], true, null, 'text', true)
            controller.rundeckAuthContextProcessor = Mock(AppAuthContextProcessor){
                authorizeApplicationResourceAny(_, _, ['configure', AuthConstants.ACTION_ADMIN]) >> true
            }
        request.api_version = 11
        params.project = 'test1'
        request.method='PUT'
        request.xml='<config><property key="project.disable.schedule" value="true"/></config>'
        controller.apiProjectConfigPut()
        then:
        assertEquals HttpServletResponse.SC_OK, response.status
        assertEquals "config", response.xml.name()
        assertEquals 1, response.xml.property.size()
        assertEquals 'project.disable.schedule', response.xml.property[0].'@key'.text()
        assertEquals 'true', response.xml.property[0].'@value'.text()
    }

    void apiProjectConfigPut_json_success(){
        when:
        controller.apiService = new ApiService()
        mockCodec(JSONCodec)
        controller.apiService.messageSource = mockWith(MessageSource) { getMessage { code, args,defval, locale -> code } }
        controller.frameworkService= mockFrameworkServiceForProjectConfigPut(true, true, 'configure', ['prop1': 'value1',
                prop2: 'value2'], true, null, 'text')
            controller.rundeckAuthContextProcessor = Mock(AppAuthContextProcessor){
                authorizeApplicationResourceAny(_, _, ['configure', AuthConstants.ACTION_ADMIN]) >> true
            }
        request.api_version = 11
        params.project = 'test1'
        request.json='{"prop1" :"value1","prop2":"value2"}'
        request.method='PUT'
        controller.apiProjectConfigPut()
        then:
        assertEquals HttpServletResponse.SC_OK, response.status
        assertEquals 'value1', response.json.prop1
        assertEquals 'value2', response.json.prop2
    }

    void apiProjectConfigPut_json_disabling_executions_success(){
        when:
        controller.apiService = new ApiService()
        mockCodec(JSONCodec)
        controller.apiService.messageSource = mockWith(MessageSource) { getMessage { code, args,defval, locale -> code } }
        controller.frameworkService= mockFrameworkServiceForProjectConfigPut(true, true, 'configure',
                ['project.disable.executions': 'true'], true, null, 'text', true)
            controller.rundeckAuthContextProcessor = Mock(AppAuthContextProcessor){
                authorizeApplicationResourceAny(_, _, ['configure', AuthConstants.ACTION_ADMIN]) >> true
            }
        request.api_version = 11
        params.project = 'test1'
        request.json='{"project.disable.executions":"false"}'
        request.method='PUT'
        controller.apiProjectConfigPut()
        then:
        assertEquals HttpServletResponse.SC_OK, response.status
        assertEquals 'false', response.json.getAt('project.disable.executions')
    }

    void apiProjectConfigPut_json_disabling_schedule_success(){
        when:
        controller.apiService = new ApiService()
        mockCodec(JSONCodec)
        controller.apiService.messageSource = mockWith(MessageSource) { getMessage { code, args,defval, locale -> code } }
        controller.frameworkService= mockFrameworkServiceForProjectConfigPut(true, true, 'configure',
                ['project.disable.schedule': 'true'], true, null, 'text', true)
            controller.rundeckAuthContextProcessor = Mock(AppAuthContextProcessor){
                authorizeApplicationResourceAny(_, _, ['configure', AuthConstants.ACTION_ADMIN]) >> true
            }
        request.api_version = 11
        params.project = 'test1'
        request.json='{"project.disable.schedule":"false"}'
        request.method='PUT'
        controller.apiProjectConfigPut()
        then:
        assertEquals HttpServletResponse.SC_OK, response.status
        assertEquals 'false', response.json.getAt('project.disable.schedule')
    }



    void apiProjectConfigKeyGet_xml_success() {
        when:
        controller.apiService = new ApiService()
        controller.apiService.messageSource = mockWith(MessageSource) { getMessage { code, args,defval, locale -> code } }
        controller.frameworkService = mockFrameworkServiceForProjectConfigGet(true, true, 'configure', ["prop1": "value1", "prop2": "value2"])
            controller.rundeckAuthContextProcessor = Mock(AppAuthContextProcessor){
                authorizeApplicationResourceAny(_, _, ['configure', AuthConstants.ACTION_ADMIN]) >> true
            }
        request.api_version = 11
        params.project = 'test1'
        params.keypath = 'prop1'
        response.format='xml'
        controller.apiProjectConfigKeyGet()

        then:
        assertEquals HttpServletResponse.SC_OK, response.status
        assertEquals "property", response.xml.name()
        assertEquals 'prop1', response.xml.'@key'.text()
        assertEquals 'value1', response.xml.'@value'.text()
    }


    void apiProjectConfigKeyGet_json_success() {
        when:
        controller.apiService = new ApiService()
        mockCodec(JSONCodec)
        controller.apiService.messageSource = mockWith(MessageSource) { getMessage { code, args,defval, locale -> code } }
        controller.frameworkService = mockFrameworkServiceForProjectConfigGet(true, true, 'configure', ["prop1": "value1", "prop2": "value2"])
            controller.rundeckAuthContextProcessor = Mock(AppAuthContextProcessor){
                authorizeApplicationResourceAny(_, _, ['configure', AuthConstants.ACTION_ADMIN]) >> true
            }
        request.api_version = 11
        params.project = 'test1'
        params.keypath = 'prop1'
        response.format='json'
        controller.apiProjectConfigKeyGet()
        then:
        assertEquals HttpServletResponse.SC_OK, response.status
        assertEquals 'prop1', response.json.key
        assertEquals 'value1', response.json.value
    }

    void apiProjectConfigKeyGet_text_success() {
        when:
        controller.apiService = new ApiService()
        controller.apiService.messageSource = mockWith(MessageSource) { getMessage { code, args,defval, locale -> code } }
        controller.frameworkService = mockFrameworkServiceForProjectConfigGet(true, true, 'configure', ["prop1": "value1", "prop2": "value2"])
            controller.rundeckAuthContextProcessor = Mock(AppAuthContextProcessor){
                authorizeApplicationResourceAny(_, _, ['configure', AuthConstants.ACTION_ADMIN]) >> true
            }
        request.api_version = 11
        params.project = 'test1'
        params.keypath = 'prop1'
        response.format='text'
        controller.apiProjectConfigKeyGet()
        then:
        assertEquals HttpServletResponse.SC_OK, response.status
        assertEquals 'value1', response.text
    }



    void apiProjectConfigKeyPut_xml_success() {
        when:
        controller.apiService = new ApiService()
        controller.apiService.messageSource = mockWith(MessageSource) { getMessage { code, args,defval, locale -> code } }
        controller.frameworkService = mockFrameworkServiceForProjectConfigKeyPut(true, true, 'configure',
                ["prop1": "value1"],true,null,null)
            controller.rundeckAuthContextProcessor = Mock(AppAuthContextProcessor){
                authorizeApplicationResourceAny(_, _, ['configure', AuthConstants.ACTION_ADMIN]) >> true
            }
        request.api_version = 11
        params.project = 'test1'
        params.keypath = 'prop1'
        request.xml='<property key="prop1" value="value1"/>'
        request.method='PUT'
        controller.apiProjectConfigKeyPut()
        then:
        assertEquals HttpServletResponse.SC_OK, response.status
        assertEquals "property", response.xml.name()
        assertEquals 'prop1', response.xml.'@key'.text()
        assertEquals 'value1', response.xml.'@value'.text()
    }


    void apiProjectConfigKeyPut_json_success() {
        when:
        controller.apiService = new ApiService()
        mockCodec(JSONCodec)
        controller.apiService.messageSource = mockWith(MessageSource) { getMessage { code, args,defval, locale -> code } }
        controller.frameworkService = mockFrameworkServiceForProjectConfigKeyPut(true, true, 'configure',
                ["prop1": "value1"],true,null,null)
            controller.rundeckAuthContextProcessor = Mock(AppAuthContextProcessor){
                authorizeApplicationResourceAny(_, _, ['configure', AuthConstants.ACTION_ADMIN]) >> true
            }
        request.api_version = 11
        params.project = 'test1'
        params.keypath = 'prop1'
        request.json='{"key":"prop1","value":"value1"}'
        request.method='PUT'
        controller.apiProjectConfigKeyPut()
        then:
        assertEquals HttpServletResponse.SC_OK, response.status
        assertEquals 'prop1', response.json.key
        assertEquals 'value1', response.json.value
    }

    void apiProjectConfigKeyPut_text_success() {
        when:
        controller.apiService = new ApiService()
        mockCodec(JSONCodec)
        controller.apiService.messageSource = mockWith(MessageSource) { getMessage { code, args,defval, locale -> code } }
        controller.frameworkService = mockFrameworkServiceForProjectConfigKeyPut(true, true, 'configure',
                ["prop1": "value1"],true,null,null)
            controller.rundeckAuthContextProcessor = Mock(AppAuthContextProcessor){
                authorizeApplicationResourceAny(_, _, ['configure', AuthConstants.ACTION_ADMIN]) >> true
            }
        request.api_version = 11
        params.project = 'test1'
        params.keypath = 'prop1'
        request.content='value1'
        request.contentType='text/plain'
        request.method='PUT'
        controller.apiProjectConfigKeyPut()
        then:
        assertEquals HttpServletResponse.SC_OK, response.status
        assertEquals 'value1', response.text
    }


    void apiProjectConfigKeyDelete_success() {
        when:
        controller.apiService = new ApiService()
        controller.apiService.messageSource = mockWith(MessageSource) { getMessage { code, args,defval, locale -> code } }
        controller.frameworkService = mockFrameworkServiceForProjectConfigKeyDelete(true, true, 'configure',
                'prop1', true, null, null)
            controller.rundeckAuthContextProcessor = Mock(AppAuthContextProcessor){
                authorizeApplicationResourceAny(_, _, ['configure', AuthConstants.ACTION_ADMIN]) >> true
            }
        request.api_version = 11
        params.project = 'test1'
        params.keypath = 'prop1'
        request.method='DELETE'
        controller.apiProjectConfigKeyDelete()

        then:
        assertEquals HttpServletResponse.SC_NO_CONTENT, response.status
    }

    void apiProjectExport_success() {
        when:
        controller.apiService = new ApiService()
        controller.apiService.messageSource = mockWith(MessageSource) { getMessage { code, args,defval, locale -> code } }
        controller.frameworkService = mockFrameworkServiceForProjectExport(true, true, 'export',true,true, true, true)
        controller.projectService=mockWith(ProjectService){
            exportProjectToOutputStream{project,fwk,stream,l,ProjectArchiveExportRequest opts,auth->
                assertEquals 'test1',project.name
                stream<<'some data'
            }
        }
            controller.rundeckAuthContextProcessor = Mock(AppAuthContextProcessor){
                authorizeApplicationResourceAny(_, _, ['export', AuthConstants.ACTION_ADMIN]) >> true
            }
        request.api_version = 11
        params.project = 'test1'
        controller.apiProjectExport()

        then:
        assertEquals HttpServletResponse.SC_OK, response.status
        assertEquals 'application/zip', response.contentType
        assertEquals 'some data', response.text

    }

    void "apiProjectExport requires api"() {
        given:
            controller.apiService=Mock(ApiService)
        when:
        controller.apiService.messageSource = mockWith(MessageSource) { getMessage { code, args,defval, locale -> code } }
        params.project = 'test1'
        controller.apiProjectExport()
        then:

            1 * controller.apiService.requireApi(_,_)>>false
    }

    void apiProjectExport_notfound() {
        given:
            controller.apiService=Mock(ApiService)
        when:
        params.project = 'test1'
        controller.frameworkService=Mock(FrameworkService){
            existsFrameworkProject('test1')>>false
        }
        controller.apiProjectExport()
        then:

            1 * controller.apiService.requireApi(_,_)>>true
            1 * controller.apiService.renderErrorFormat(_,
                                                        [
                                                            status: HttpServletResponse.SC_NOT_FOUND,
                                                            code: "api.error.item.doesnotexist",
                                                            args: ['Project', 'test1']
                                                        ])
    }

    void apiProjectExport_unauthorized() {
        given:
            controller.apiService=Mock(ApiService)
        when:
        controller.frameworkService = Mock(FrameworkService){
            existsFrameworkProject('test1')>>true
        }
        controller.projectService=mockWith(ProjectService){
            exportProjectToOutputStream{project,fwk,stream->
                assertEquals 'test1',project.name
                stream<<'some data'
            }
        }
            controller.rundeckAuthContextProcessor = Mock(AppAuthContextProcessor){
                authorizeApplicationResourceAny(_, _, ['export', AuthConstants.ACTION_ADMIN]) >> false
            }
        params.project = 'test1'
        controller.apiProjectExport()
        then:

            1 * controller.apiService.requireApi(_,_)>>true
            1 * controller.apiService.renderErrorFormat(_,
                                                        [
                                                            status: HttpServletResponse.SC_FORBIDDEN,
                                                            code: "api.error.item.unauthorized",
                                                            args: ['export', "Project", 'test1']
                                                        ])
    }

    void apiProjectImport_notfound() {
        given:
            controller.apiService=Mock(ApiService)
        when:
        controller.frameworkService = Mock(FrameworkService){
            existsFrameworkProject('test1')>>false
        }
        params.project = 'test1'
        request.format='blah'
        request.method='PUT'
        controller.apiProjectImport()
        then:
            2 * controller.apiService.requireApi(_,_)>>true
            1 * controller.apiService.renderErrorFormat(_,
                                                        [
                                                            status: HttpServletResponse.SC_NOT_FOUND,
                                                            code: "api.error.item.doesnotexist",
                                                            args: ['Project', 'test1']
                                                        ])
    }

    void apiProjectImport_unauthorized() {
        given:
            controller.apiService=Mock(ApiService)
        when:
        controller.frameworkService = Mock(FrameworkService){
            1 * existsFrameworkProject('test1')>>true
        }
            controller.rundeckAuthContextProcessor = Mock(AppAuthContextProcessor){
                authorizeApplicationResourceAny(_, _, ['import', AuthConstants.ACTION_ADMIN]) >> false
            }
        params.project = 'test1'
        request.format='blah'
        request.method='PUT'
        controller.apiProjectImport()
        then:
            2 * controller.apiService.requireApi(_,_)>>true
            1 * controller.apiService.renderErrorFormat(_,
                                                        [
                                                            status: HttpServletResponse.SC_FORBIDDEN,
                                                            code: "api.error.item.unauthorized",
                                                            args: ['import', "Project", 'test1']
                                                        ])
    }

    void apiProjectImport_invalidFormat() {
        given:
            controller.apiService=Mock(ApiService)
        when:
        controller.frameworkService = mockFrameworkServiceForProjectImport(true, true, 'import')
            controller.rundeckAuthContextProcessor = Mock(AppAuthContextProcessor){
                authorizeApplicationResourceAny(_, _, ['import', AuthConstants.ACTION_ADMIN]) >> true
            }
        params.project = 'test1'
        request.format='blah'
        request.method='PUT'
        controller.apiProjectImport()
        then:
            2 * controller.apiService.requireApi(_,_)>>true
            1 * controller.apiService.requireRequestFormat(_,_,['application/zip'])>>false
    }

    void apiProjectImport_xml_failure() {
        when:
        controller.apiService = new ApiService()
        controller.apiService.messageSource = mockWith(MessageSource) { getMessage { code, args,defval, locale -> code } }
        controller.frameworkService = mockFrameworkServiceForProjectImport(true, true, 'import')
        controller.projectService=mockWith(ProjectService){
            importToProject{  project,  framework,
                             UserAndRolesAuthContext authContext,  InputStream stream, ProjectArchiveImportRequest options->


                [success:false,joberrors:['error1','error2']]
            }
        }
            controller.rundeckAuthContextProcessor = Mock(AppAuthContextProcessor){
                authorizeApplicationResourceAny(_, _, ['import', AuthConstants.ACTION_ADMIN]) >> true
            }
        request.api_version = 11
        params.project = 'test1'
        request.format='application/zip'
        response.format='xml'
        request.subject = new Subject(false,[new Username('user1'),new Group('groupa'), new Group('groupb')] as Set,
                [] as Set, [] as Set)
        session.user='user1'
        request.method='PUT'
        controller.apiProjectImport(new ProjectArchiveParams(project:'test1'))

        then:
        assertEquals HttpServletResponse.SC_OK,response.status
        assertEquals 'failed',response.xml.'@status'.text()
        assertEquals '2',response.xml.errors.'@count'.text()
        assertEquals 2,response.xml.errors.error.size()
        assertEquals 'error1',response.xml.errors.error[0].text()
        assertEquals 'error2',response.xml.errors.error[1].text()
    }

    void apiProjectImport_json_failure() {
        when:
        controller.apiService = new ApiService()
        controller.apiService.messageSource = mockWith(MessageSource) { getMessage { code, args,defval, locale -> code } }
        controller.frameworkService = mockFrameworkServiceForProjectImport(true, true, 'import')
        controller.projectService=mockWith(ProjectService){
            importToProject{  project,  framework,
                             UserAndRolesAuthContext authContext,  InputStream stream, ProjectArchiveImportRequest options->


                [success:false,joberrors:['error1','error2']]
            }
        }
            controller.rundeckAuthContextProcessor = Mock(AppAuthContextProcessor){
                authorizeApplicationResourceAny(_, _, ['import', AuthConstants.ACTION_ADMIN]) >> true
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

        then:
        assertEquals HttpServletResponse.SC_OK,response.status
        assertEquals 'failed',response.json.import_status
        assertEquals 2,response.json.errors.size()
        assertEquals 'error1',response.json.errors[0]
        assertEquals 'error2',response.json.errors[1]
    }

    void apiProjectImport_xml_success() {
        when:
        controller.apiService = new ApiService()
        controller.apiService.messageSource = mockWith(MessageSource) { getMessage { code, args,defval, locale -> code } }
        controller.frameworkService = mockFrameworkServiceForProjectImport(true, true, 'import')
        controller.projectService=mockWith(ProjectService){
            importToProject{  project,  framework,
                             UserAndRolesAuthContext authContext,  InputStream stream, ProjectArchiveImportRequest options->


                assertEquals(true, options.importExecutions)
                assertEquals('preserve', options.jobUuidOption)
                assertEquals(false, options.importConfig)
                assertEquals(false, options.importACL)
                [success:true]
            }
        }
            controller.rundeckAuthContextProcessor = Mock(AppAuthContextProcessor){
                authorizeApplicationResourceAny(_, _, ['import', AuthConstants.ACTION_ADMIN]) >> true
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

        then:
        assertEquals HttpServletResponse.SC_OK,response.status
        assertEquals 'successful',response.xml.'@status'.text()
        assertEquals 0,response.xml.errors.size()
    }

    void apiProjectImport_importExecutionsFalse() {
        when:
        controller.apiService = new ApiService()
        controller.apiService.messageSource = mockWith(MessageSource) { getMessage { code, args,defval, locale -> code } }
        controller.frameworkService = mockFrameworkServiceForProjectImport(true, true, 'import')
        controller.projectService=mockWith(ProjectService){
            importToProject{  project,  framework,
                             UserAndRolesAuthContext authContext,  InputStream stream, ProjectArchiveImportRequest options->


                assertEquals(false, options.importExecutions)
                assertEquals('preserve', options.jobUuidOption)
                assertEquals(false, options.importConfig)
                assertEquals(false, options.importACL)
                [success:true]
            }
        }
            controller.rundeckAuthContextProcessor = Mock(AppAuthContextProcessor){
                authorizeApplicationResourceAny(_, _, ['import', AuthConstants.ACTION_ADMIN]) >> true
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

        then:
        assertEquals HttpServletResponse.SC_OK,response.status
    }

    void apiProjectImport_importExecutionsTrue() {
        when:
        controller.apiService = new ApiService()
        controller.apiService.messageSource = mockWith(MessageSource) { getMessage { code, args,defval, locale -> code } }
        controller.frameworkService = mockFrameworkServiceForProjectImport(true, true, 'import')
        controller.projectService=mockWith(ProjectService){
            importToProject{  project, framework,
                             UserAndRolesAuthContext authContext,  InputStream stream, ProjectArchiveImportRequest options->


                assertEquals(true, options.importExecutions)
                assertEquals('preserve', options.jobUuidOption)
                assertEquals(false, options.importConfig)
                assertEquals(false, options.importACL)
                [success:true]
            }
        }
            controller.rundeckAuthContextProcessor = Mock(AppAuthContextProcessor){
                authorizeApplicationResourceAny(_, _, ['import', AuthConstants.ACTION_ADMIN]) >> true
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
        then:
        assertEquals HttpServletResponse.SC_OK,response.status
    }

    void apiProjectImport_jobUuidOptionPreserve() {
        when:
        controller.apiService = new ApiService()
        controller.apiService.messageSource = mockWith(MessageSource) { getMessage { code, args,defval, locale -> code } }
        controller.frameworkService = mockFrameworkServiceForProjectImport(true, true, 'import')
        controller.projectService=mockWith(ProjectService){
            importToProject{  project,  framework,
                             UserAndRolesAuthContext authContext,  InputStream stream, ProjectArchiveImportRequest options->


                assertEquals(true, options.importExecutions)
                assertEquals('preserve', options.jobUuidOption)
                assertEquals(false, options.importConfig)
                assertEquals(false, options.importACL)
                [success:true]
            }
        }
            controller.rundeckAuthContextProcessor = Mock(AppAuthContextProcessor){
                authorizeApplicationResourceAny(_, _, ['import', AuthConstants.ACTION_ADMIN]) >> true
            }
        request.api_version = 11
        params.project = 'test1'
        params.jobUuidOption='preserve'
        request.format='application/zip'
        request.subject = new Subject(false,[new Username('user1'),new Group('groupa'), new Group('groupb')] as Set,
                [] as Set, [] as Set)
        session.user='user1'
        request.method='PUT'
        controller.apiProjectImport()
        then:
        assertEquals HttpServletResponse.SC_OK,response.status
    }

    void apiProjectImport_jobUuidOptionRemove() {
        controller.apiService = new ApiService()
        controller.apiService.messageSource = mockWith(MessageSource) { getMessage { code, args,defval, locale -> code } }
        controller.frameworkService = mockFrameworkServiceForProjectImport(true, true, 'import')
        controller.projectService=mockWith(ProjectService){
            importToProject{  project,  framework,
                             UserAndRolesAuthContext authContext,  InputStream stream, ProjectArchiveImportRequest options->


                assertEquals(true, options.importExecutions)
                assertEquals('remove', options.jobUuidOption)
                assertEquals(false, options.importConfig)
                assertEquals(false, options.importACL)
                [success:true]
            }
        }
        request.api_version = 11
        params.project = 'test1'
        params.jobUuidOption='remove'
        request.format='application/zip'
        request.subject = new Subject(false,[new Username('user1'),new Group('groupa'), new Group('groupb')] as Set,
                [] as Set, [] as Set)
        session.user='user1'
        request.method='PUT'
        controller.apiProjectImport()
        assertEquals ("expected 200, ${response.contentAsString}",HttpServletResponse.SC_OK,response.status)
    }

    void apiProjectImport_jobUuidOption_invalidValue() {
        controller.apiService = new ApiService()
        controller.apiService.messageSource = mockWith(MessageSource) { getMessage { code, args, defval, locale -> code+';'+args.join(';') } }
        controller.frameworkService = mockFrameworkServiceForProjectImport(true, true, 'import')
        controller.projectService=mockWith(ProjectService){
            importToProject{  project,  framework,
                             UserAndRolesAuthContext authContext,  InputStream stream, ProjectArchiveImportRequest options->


                assertEquals([importExecutions: true, jobUuidOption: 'remove',importConfig:false, importACL:false], options)
                [success:true]
            }
        }
        request.api_version = 11
        params.project = 'test1'
        params.jobUuidOption='blah'
        request.format='application/zip'
        response.format='json'
        request.subject = new Subject(false,[new Username('user1'),new Group('groupa'), new Group('groupb')] as Set,
                [] as Set, [] as Set)
        session.user='user1'
        request.method='PUT'
        controller.apiProjectImport()
        assertEquals ("expected 200, ${response.contentAsString}",HttpServletResponse.SC_BAD_REQUEST,response.status)
        assertEquals(
                ["message": "api.error.invalid.request;Property [jobUuidOption] of class [class com.dtolabs.rundeck.app.support.ProjectArchiveParams] with value [blah] is not contained within the list [[preserve, remove]]",
                 "error": true,
                 "errorCode": "api.error.invalid.request",
                 "apiversion": ApiVersions.API_CURRENT_VERSION],
                response.json
        )
    }

    void apiProjectImport_json_success() {
        controller.apiService = new ApiService()
        controller.apiService.messageSource = mockWith(MessageSource) { getMessage { code, args,defval, locale -> code } }
        controller.frameworkService = mockFrameworkServiceForProjectImport(true, true, 'import')
        controller.projectService=mockWith(ProjectService){
            importToProject{  project,  framework,
                             UserAndRolesAuthContext authContext,  InputStream stream, ProjectArchiveImportRequest options->


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



    void apiProjectImport_importAcl_unauthorized() {
        when:
        controller.apiService = new ApiService()
        controller.apiService.messageSource = mockWith(MessageSource) { getMessage { code, args, defval, locale -> code+';'+args } }
        controller.frameworkService = mockFrameworkServiceForProjectImport(true, true, 'import',true,false)
        controller.projectService=mockWith(ProjectService){
            importToProject{  project,  framework,
                              UserAndRolesAuthContext authContext,  InputStream stream, ProjectArchiveImportRequest options->


                [success:true]
            }
        }
            controller.rundeckAuthContextProcessor = Mock(AppAuthContextProcessor){
                authResourceForProjectAcl('test1')>>[acl:true]
                authorizeApplicationResourceAny(_, null, ['import', AuthConstants.ACTION_ADMIN]) >> true
                authorizeApplicationResourceAny(_, [acl:true], [AuthConstants.ACTION_CREATE, AuthConstants.ACTION_ADMIN]) >> false
            }
        request.api_version = 11
        params.project = 'test1'
        params.importACL='true'
        request.format='application/zip'
        response.format='json'
        request.subject = new Subject(false,[new Username('user1'),new Group('groupa'), new Group('groupb')] as Set,
                                      [] as Set, [] as Set)
        session.user='user1'
        request.method='PUT'
        controller.apiProjectImport()
        then:
        assertEquals HttpServletResponse.SC_FORBIDDEN,response.status
        assertEquals( [
                              message:"api.error.item.unauthorized;[create, ACL for Project, [name:test1]]",
                              error: true,
                              errorCode : "api.error.item.unauthorized",
                              apiversion: ApiVersions.API_CURRENT_VERSION
                      ],
                      response.json
        )
        assertEquals null,response.json.errors
    }


    void apiProjectImport_importAcl_authorized() {
        when:
        controller.apiService = new ApiService()
        controller.apiService.messageSource = mockWith(MessageSource) { getMessage { code, args, defval, locale -> code+';'+args } }
        controller.frameworkService = mockFrameworkServiceForProjectImport(true, true, 'import',true,true)
        controller.projectService=mockWith(ProjectService){
            importToProject{  project,  framework,
                              UserAndRolesAuthContext authContext,  InputStream stream, ProjectArchiveImportRequest options->


                assertTrue(options.importACL)
                [success:true]
            }
        }
            controller.rundeckAuthContextProcessor = Mock(AppAuthContextProcessor){
                authResourceForProjectAcl('test1')>>[acl:true]
                authorizeApplicationResourceAny(_, null, ['import', AuthConstants.ACTION_ADMIN]) >> true
                authorizeApplicationResourceAny(_, [acl:true], [AuthConstants.ACTION_CREATE, AuthConstants.ACTION_ADMIN]) >> true
            }
        request.api_version = 28
        params.project = 'test1'
        params.importACL='true'
        request.format='application/zip'
        response.format='json'
        request.subject = new Subject(false,[new Username('user1'),new Group('groupa'), new Group('groupb')] as Set,
                                      [] as Set, [] as Set)
        session.user='user1'
        request.method='PUT'
        controller.apiProjectImport()
        then:
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
        when:
        controller.apiService = new ApiService()
        controller.apiService.messageSource = mockWith(MessageSource) { getMessage { code, args, defval, locale -> code+';'+args } }
        controller.frameworkService = mockFrameworkServiceForProjectImport(true, true, 'import',false,false,true,false)
        controller.projectService=mockWith(ProjectService){
            importToProject{  project,  framework,
                              UserAndRolesAuthContext authContext,  InputStream stream, ProjectArchiveImportRequest options->


                [success:true]
            }
        }
            controller.rundeckAuthContextProcessor = Mock(AppAuthContextProcessor){
                authorizeApplicationResourceAny(_, _, ['import', AuthConstants.ACTION_ADMIN]) >> true
                authorizeApplicationResourceAny(_, _, [AuthConstants.ACTION_CONFIGURE, AuthConstants.ACTION_ADMIN]) >> false
            }
        request.api_version = 28
        params.project = 'test1'
        params.importScm='true'
        request.format='application/zip'
        response.format='json'
        request.subject = new Subject(false,[new Username('user1'),new Group('groupa'), new Group('groupb')] as Set,
                [] as Set, [] as Set)
        session.user='user1'
        request.method='PUT'
        controller.apiProjectImport()
        then:
        assertEquals HttpServletResponse.SC_FORBIDDEN,response.status
        assertEquals( [
                message:"api.error.item.unauthorized;[configure, SCM for Project, [name:test1]]",
                error: true,
                errorCode : "api.error.item.unauthorized",
                apiversion: ApiVersions.API_CURRENT_VERSION
        ],
                response.json
        )
        assertEquals null,response.json.errors
    }


    void apiProjectImport_importScm_authorized() {
        when:
        controller.apiService = new ApiService()
        controller.apiService.messageSource = mockWith(MessageSource) { getMessage { code, args, defval, locale -> code+';'+args } }
        controller.frameworkService = mockFrameworkServiceForProjectImport(true, true, 'import',false,true,true,true)
        controller.projectService=mockWith(ProjectService){
            importToProject{  project,  framework,
                              UserAndRolesAuthContext authContext,  InputStream stream, ProjectArchiveImportRequest options->


                assertTrue(options.importScm)
                [success:true]
            }
        }
            controller.rundeckAuthContextProcessor = Mock(AppAuthContextProcessor){
                _ * getAuthContextForSubject(_)
                _ * getAuthContextForSubjectAndProject(_,_)
                _ * authResourceForProject('test1')
                authorizeApplicationResourceAny(_, _, ['import', AuthConstants.ACTION_ADMIN]) >> true
                authorizeApplicationResourceAny(_, _, [AuthConstants.ACTION_CONFIGURE, AuthConstants.ACTION_ADMIN]) >> true
                0*_(*_)
            }
        request.api_version = 28
        params.project = 'test1'
        params.importScm='true'
        request.format='application/zip'
        response.format='json'
        request.subject = new Subject(false,[new Username('user1'),new Group('groupa'), new Group('groupb')] as Set,
                [] as Set, [] as Set)
        session.user='user1'
        request.method='PUT'
        controller.apiProjectImport()

        then:
        assertEquals HttpServletResponse.SC_OK,response.status
        assertEquals( [
                import_status: 'successful',
                successful   : true
        ],
                response.json
        )
        assertEquals null,response.json.errors
    }


    void apiProjectExport_scm_old_api_v() {
        when:
        controller.apiService = new ApiService()
        controller.apiService.messageSource = mockWith(MessageSource) { getMessage { code, args,defval, locale -> code } }
        controller.frameworkService = mockFrameworkServiceForProjectExport(true, true, 'export',true,true,true,true)
        controller.projectService=mockWith(ProjectService){
            exportProjectToOutputStream{ project, fwk, stream, l, ProjectArchiveExportRequest opts, auth->
                assertEquals 'test1',project.name
                assertFalse opts.scm
                stream<<'some data'
            }
        }
            controller.rundeckAuthContextProcessor = Mock(AppAuthContextProcessor){
                authResourceForProjectAcl('test1')>>[acl:true]
                authorizeApplicationResourceAny(_, null, ['export', AuthConstants.ACTION_ADMIN]) >> true
                authorizeApplicationResourceAny(_, null, [AuthConstants.ACTION_CONFIGURE, AuthConstants.ACTION_ADMIN]) >> true
                authorizeApplicationResourceAny(_, [acl:true], [AuthConstants.ACTION_CONFIGURE, AuthConstants.ACTION_ADMIN]) >> true
            }
        request.api_version = 11
        params.project = 'test1'
        params.exportScm='true'
        controller.apiProjectExport()
        then:
        assertEquals HttpServletResponse.SC_OK, response.status
        assertEquals 'application/zip', response.contentType
        assertEquals 'some data', response.text
    }

    void apiProjectExport_scm_success_v28() {
        when:
        controller.apiService = new ApiService()
        controller.apiService.messageSource = mockWith(MessageSource) { getMessage { code, args,defval, locale -> code } }
        controller.frameworkService = mockFrameworkServiceForProjectExport(true, true, 'export',true,true,true,true)
        controller.projectService=mockWith(ProjectService){
            exportProjectToOutputStream{project,fwk,stream,l,ProjectArchiveExportRequest opts,scmperms->
                assertEquals 'test1',project.name

                assertTrue opts.scm
                stream<<'some data'
            }
        }
            controller.rundeckAuthContextProcessor = Mock(AppAuthContextProcessor){
                authResourceForProjectAcl('test1')>>[acl:true]
                authorizeApplicationResourceAny(_, null, ['export', AuthConstants.ACTION_ADMIN]) >> true
                authorizeApplicationResourceAny(_, null, [AuthConstants.ACTION_CONFIGURE, AuthConstants.ACTION_ADMIN]) >> true
                authorizeApplicationResourceAny(_, [acl:true], [AuthConstants.ACTION_CONFIGURE, AuthConstants.ACTION_ADMIN]) >> true
            }
        request.api_version = 28
        params.project = 'test1'
        params.exportScm='true'
        controller.apiProjectExport()
        then:
        assertEquals HttpServletResponse.SC_OK, response.status
        assertEquals 'application/zip', response.contentType
        assertEquals 'some data', response.text

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
