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

import com.dtolabs.rundeck.app.support.ExtNodeFilters
import com.dtolabs.rundeck.core.authorization.UserAndRolesAuthContext
import com.dtolabs.rundeck.core.common.Framework
import com.dtolabs.rundeck.core.common.IFramework
import com.dtolabs.rundeck.core.common.IProjectNodes
import com.dtolabs.rundeck.core.common.IRundeckProject
import com.dtolabs.rundeck.core.common.NodeEntryImpl
import com.dtolabs.rundeck.core.common.NodeSetImpl
import com.dtolabs.rundeck.core.execution.service.FileCopierService
import com.dtolabs.rundeck.core.execution.service.NodeExecutorService
import com.dtolabs.rundeck.core.plugins.configuration.Property
import com.dtolabs.rundeck.core.resources.ResourceModelSourceService
import com.dtolabs.rundeck.core.resources.format.ResourceFormatGeneratorService
import com.dtolabs.rundeck.core.resources.format.ResourceXMLFormatGenerator
import com.dtolabs.rundeck.core.resources.format.json.ResourceJsonFormatGenerator
import com.dtolabs.rundeck.server.authorization.AuthConstants
import grails.test.mixin.TestFor
import org.codehaus.groovy.grails.web.servlet.mvc.SynchronizerTokensHolder
import org.grails.plugins.metricsweb.MetricService
import rundeck.services.ApiService
import rundeck.services.AuthorizationService
import rundeck.services.FrameworkService
import rundeck.services.PasswordFieldsService
import rundeck.services.ScheduledExecutionService
import rundeck.services.StorageManager
import rundeck.services.UserService
import rundeck.services.authorization.PoliciesValidation
import rundeck.services.framework.RundeckProjectConfigurable
import spock.lang.Specification
import spock.lang.Unroll

import static com.dtolabs.rundeck.server.authorization.AuthConstants.ACTION_ADMIN
import static com.dtolabs.rundeck.server.authorization.AuthConstants.ACTION_CREATE
import static com.dtolabs.rundeck.server.authorization.AuthConstants.ACTION_DELETE
import static com.dtolabs.rundeck.server.authorization.AuthConstants.ACTION_READ
import static com.dtolabs.rundeck.server.authorization.AuthConstants.ACTION_UPDATE

/**
 * Created by greg on 7/28/15.
 */
@TestFor(FrameworkController)
class FrameworkControllerSpec extends Specification {
    def "system acls require api_version 14"(){
        setup:
        controller.apiService=Mock(ApiService){
            1 * requireVersion(_,_,14) >> {args->
                args[1].status=400
                false
            }
        }
        when:
        controller.apiSystemAcls()

        then:
        response.status==400
    }
    def "system acls not authorized"(){
        setup:
        controller.apiService=Mock(ApiService){
            1 * requireVersion(_,_,14) >> true
            1 * renderErrorFormat(_,[status:403,code:'api.error.item.unauthorized',args:[action,'Rundeck System ACLs','']]) >> {args->
                args[0].status=args[1].status
            }
        }
        controller.frameworkService=Mock(FrameworkService){
            1 * getAuthContextForSubject(_) >> null
            1 * authorizeApplicationResourceAny(null,AuthConstants.RESOURCE_TYPE_SYSTEM_ACL,[action,ACTION_ADMIN])>>false
        }
        when:
        params.project='monkey'
        request.method=method
        controller.apiSystemAcls()

        then:
        response.status==403

        where:
        method | action
        'GET' | ACTION_READ
        'POST' | ACTION_CREATE
        'PUT' | ACTION_UPDATE
        'DELETE' | ACTION_DELETE
    }
    def "system acls invalid path"(){
        setup:
        controller.apiService=Mock(ApiService){
            1 * requireVersion(_,_,14) >> true

            1 * extractResponseFormat(_,_,_,_) >> 'json'
            1 * renderErrorFormat(
                    _,
                    [
                            status: 400,
                            code: 'api.error.parameter.invalid',
                            args: ['elf', 'path', 'Must refer to a file ending in .aclpolicy'],
                            format: 'json'
                    ]
            ) >> { args ->
                args[0].status = args[1].status
            }
        }
        controller.frameworkService=Mock(FrameworkService){
            1 * getAuthContextForSubject(_) >> null
            1 * authorizeApplicationResourceAny(null,AuthConstants.RESOURCE_TYPE_SYSTEM_ACL,[ACTION_READ,ACTION_ADMIN])>>true
        }
        when:
        params.path='elf'
        params.project='monkey'
        controller.apiSystemAcls()

        then:
        response.status==400
    }
    def "system acls GET 404"(){
        setup:
        controller.configStorageService=Mock(StorageManager){
            existsFileResource(_) >> false
            existsDirResource(_) >> false
        }
        controller.frameworkService=Mock(FrameworkService){
            1 * getAuthContextForSubject(_) >> null
            1 * authorizeApplicationResourceAny(_,AuthConstants.RESOURCE_TYPE_SYSTEM_ACL,[ACTION_READ,ACTION_ADMIN]) >> true
        }
        controller.apiService=Mock(ApiService){
            1 * requireVersion(_,_,14) >> true
            1 * extractResponseFormat(_,_,_,_) >> 'json'
            1 * renderErrorFormat(_,_) >> {args->
                args[0].status=args[1].status
                null
            }
        }
        when:
        params.path='blah.aclpolicy'
        params.project="test"
        def result=controller.apiSystemAcls()

        then:
        response.status==404
    }
    def "system acls GET json"(){
        setup:
        controller.configStorageService=Mock(StorageManager){
            1 * existsFileResource(_) >> true
            1 * loadFileResource('acls/blah.aclpolicy',_) >> {args->
                args[1].write('blah'.bytes)
                4
            }
        }
        controller.frameworkService=Mock(FrameworkService){
            1 * getAuthContextForSubject(_) >> null
            1 * authorizeApplicationResourceAny(_,AuthConstants.RESOURCE_TYPE_SYSTEM_ACL,[ACTION_READ,ACTION_ADMIN]) >> true
        }
        controller.apiService=Mock(ApiService){
            1 * requireVersion(_,_,14) >> true
            1 * extractResponseFormat(_,_,_,_) >> 'json'
            1 * renderWrappedFileContents(_,_,_) >> { args ->
                args[2].success=true
            }
        }
        when:
        params.path='blah.aclpolicy'
        params.project="test"
        response.format='json'
        def result=controller.apiSystemAcls()

        then:
        response.status==200
        response.contentType.split(';').contains('application/json')
    }
    def "system acls GET xml"(){
        setup:
        controller.configStorageService=Mock(StorageManager){
            1 * existsFileResource(_) >> true
            1 * loadFileResource('acls/blah.aclpolicy',_) >> {args->
                args[1].write('blah'.bytes)
                4
            }
        }
        controller.frameworkService=Mock(FrameworkService){
            1 * getAuthContextForSubject(_) >> null
            1 * authorizeApplicationResourceAny(_,AuthConstants.RESOURCE_TYPE_SYSTEM_ACL,[ACTION_READ,ACTION_ADMIN]) >> true
        }
        controller.apiService=Mock(ApiService){
            1 * requireVersion(_,_,14) >> true
            1 * extractResponseFormat(_,_,_,_) >> 'xml'
            1 * renderWrappedFileContents('blah','xml',_)
        }
        when:
        params.path='blah.aclpolicy'
        params.project="test"
        response.format='xml'
        controller.apiSystemAcls()

        then:
        response.status==200
        response.contentType.split(';').contains('application/xml')
    }
    def "system acls GET text/yaml"(String respFormat, String contentType){
        setup:
        controller.configStorageService=Mock(StorageManager){
            1 * existsFileResource(_) >> true
            1 * loadFileResource('acls/blah.aclpolicy',_) >> {args->
                args[1].write('blah'.bytes)
                4
            }
        }
        controller.frameworkService=Mock(FrameworkService){
            1 * getAuthContextForSubject(_) >> null
            1 * authorizeApplicationResourceAny(_,AuthConstants.RESOURCE_TYPE_SYSTEM_ACL,[ACTION_READ,ACTION_ADMIN]) >> true
        }
        controller.apiService=Mock(ApiService){
            1 * requireVersion(_,_,14) >> true
            1 * extractResponseFormat(_,_,_,_) >> respFormat
        }
        when:
        params.path='blah.aclpolicy'
        params.project="test"
        def result=controller.apiSystemAcls()

        then:
        response.status==200
        response.contentType.split(';').contains(contentType)
        response.contentAsString=='blah'

        where:
        respFormat | contentType
        'text'     | 'text/plain'
        'yaml'     | 'application/yaml'
    }
    def "system acls GET dir JSON"(){
        setup:
        controller.configStorageService=Mock(StorageManager){
            1 * existsFileResource(_) >> false
            1 * existsDirResource('acls/') >> true
            1 * listDirPaths('acls/','.+\\.aclpolicy$') >> { args ->
                ['acls/blah.aclpolicy']
            }
            0*_(*_)
        }
        controller.frameworkService=Mock(FrameworkService){
            1 * getAuthContextForSubject(_) >> null
            1 * authorizeApplicationResourceAny(_,AuthConstants.RESOURCE_TYPE_SYSTEM_ACL,[ACTION_READ,ACTION_ADMIN]) >> true
            0*_(*_)
        }
        controller.apiService=Mock(ApiService){
            1 * requireVersion(_,_,14) >> true
            1 * extractResponseFormat(_,_,_,_) >> 'json'
            1 * jsonRenderDirlist('acls/',_,_,['acls/blah.aclpolicy'],_)>>{args-> args[4].success=true}
            0*_(*_)
        }
        when:
        params.path=''
        params.project="test"
        def result=controller.apiSystemAcls()

        then:
        response.status==200
        response.contentType.split(';').contains('application/json')
        response.json==[success:true]

    }
    def "system acls GET dir XML"(){
        setup:
        controller.configStorageService=Mock(StorageManager){
            1 * existsFileResource(_) >> false
            1 * existsDirResource('acls/') >> true
            1 * listDirPaths('acls/','.+\\.aclpolicy$') >> { args ->
                ['acls/blah.aclpolicy']
            }
            0*_(*_)
        }
        controller.frameworkService=Mock(FrameworkService){
            1 * getAuthContextForSubject(_) >> null
                         1 * authorizeApplicationResourceAny(_,AuthConstants.RESOURCE_TYPE_SYSTEM_ACL,[ACTION_READ,ACTION_ADMIN]) >> true
            0*_(*_)
        }
        controller.apiService=Mock(ApiService){
            1 * requireVersion(_,_,14) >> true
            1 * extractResponseFormat(_,_,_,_) >> 'xml'
            1 * xmlRenderDirList('acls/',_,_,['acls/blah.aclpolicy'],_)>>{args-> args[4].success(ok:true)}
            0*_(*_)
        }
        when:
        params.path=''
        params.project="test"
        response.format='xml'
        def result=controller.apiSystemAcls()

        then:
        response.status==200
        response.contentType.split(';').contains('application/xml')
//        response.contentAsString==''
        response.xml.'@ok'.text()=='true'

    }
    def "system acls POST text"(){
        setup:
        controller.configStorageService=Mock(StorageManager){
            1 * existsFileResource('acls/test.aclpolicy') >> false
            1 * writeFileResource('acls/test.aclpolicy',_,_) >> null
            1 * loadFileResource('acls/test.aclpolicy',_) >> {args->
                args[1].write('blah'.bytes)
                4
            }
        }
        controller.frameworkService=Mock(FrameworkService){
            1 * getAuthContextForSubject(_) >> null
            1 * authorizeApplicationResourceAny(_,AuthConstants.RESOURCE_TYPE_SYSTEM_ACL,[ACTION_CREATE,ACTION_ADMIN]) >> true
        }
        controller.apiService=Mock(ApiService){
            1 * requireVersion(_,_,14) >> true
            1 * extractResponseFormat(_,_,_,_) >> 'json'

            1 * renderWrappedFileContents(_,_,_) >> { args ->
                args[2].contents=args[0]
            }
        }
        controller.authorizationService=Stub(AuthorizationService){
            validateYamlPolicy('test.aclpolicy',_)>>Stub(PoliciesValidation){
                isValid()>>true
            }
        }
        when:
        params.path='test.aclpolicy'
        params.project="test"
        response.format='json'
        request.method='POST'
        request.contentType='application/yaml'
        request.content=('{ description: \'\', \n' +
                'context: { project: \'test\' }, \n' +
                'by: { username: \'test\' }, \n' +
                'for: { resource: [ { allow: \'x\' } ] } }').bytes
        def result=controller.apiSystemAcls()

        then:
        response.status==201
        response.contentType!=null
        response.contentType.split(';').contains('application/json')
        response.json==[contents:'blah']
    }
    def "system acls POST already exists"(){
        setup:
        controller.configStorageService=Mock(StorageManager){
            1 * existsFileResource('acls/test.aclpolicy') >> true
        }
        controller.frameworkService=Mock(FrameworkService){
            1 * getAuthContextForSubject(_) >> null
            1 * authorizeApplicationResourceAny(_,AuthConstants.RESOURCE_TYPE_SYSTEM_ACL,[ACTION_CREATE,ACTION_ADMIN]) >> true
        }
        controller.apiService=Mock(ApiService){
            1 * requireVersion(_,_,14) >> true
            1 * extractResponseFormat(_,_,_,_) >> 'json'

            1 * renderErrorFormat(_,[status:409,code:'api.error.item.alreadyexists',args:['System ACL Policy File','test.aclpolicy'],format:'json']) >> {args->
                args[0].status=args[1].status
            }
        }
        controller.authorizationService=Stub(AuthorizationService){
            validateYamlPolicy('test.aclpolicy',_)>>Stub(PoliciesValidation){
                isValid()>>true
            }
        }
        when:
        params.path='test.aclpolicy'
        params.project="test"
        response.format='json'
        request.method='POST'
        request.contentType='application/yaml'
        request.content=('{ description: \'\', \n' +
                'context: { project: \'test\' }, \n' +
                'by: { username: \'test\' }, \n' +
                'for: { resource: [ { allow: \'x\' } ] } }').bytes
        def result=controller.apiSystemAcls()

        then:
        response!=null
        response.status==409
    }
    def "system acls PUT doesn't exist"(){
        setup:
        controller.configStorageService=Mock(StorageManager){
            1 * existsFileResource('acls/test.aclpolicy') >> false
        }
        controller.frameworkService=Mock(FrameworkService){
            1 * getAuthContextForSubject(_) >> null
            1 * authorizeApplicationResourceAny(_,AuthConstants.RESOURCE_TYPE_SYSTEM_ACL,[ACTION_UPDATE,ACTION_ADMIN]) >> true
        }
        controller.apiService=Mock(ApiService){
            1 * requireVersion(_,_,14) >> true
            1 * extractResponseFormat(_,_,_,_) >> 'json'

            1 * renderErrorFormat(_,[status:404,code:'api.error.item.doesnotexist',args:['System ACL Policy File','test.aclpolicy'],format:'json']) >> {args->
                args[0].status=args[1].status
            }
        }
        controller.authorizationService=Stub(AuthorizationService){
            validateYamlPolicy('test.aclpolicy',_)>>Stub(PoliciesValidation){
                isValid()>>true
            }
        }
        when:
        params.path='test.aclpolicy'
        params.project="test"
        response.format='json'
        request.method='PUT'
        request.contentType='application/yaml'
        request.content=('{ description: \'\', \n' +
                'context: { project: \'test\' }, \n' +
                'by: { username: \'test\' }, \n' +
                'for: { resource: [ { allow: \'x\' } ] } }').bytes
        def result=controller.apiSystemAcls()

        then:
        response!=null
        response.status==404
    }

    def "system acls PUT text"(){
        setup:
        controller.configStorageService=Mock(StorageManager){
            1 * existsFileResource('acls/test.aclpolicy') >> true
            1 * writeFileResource('acls/test.aclpolicy',_,_) >> null
            1 * loadFileResource('acls/test.aclpolicy',_) >> {args->
                args[1].write('blah'.bytes)
                4
            }
        }
        controller.frameworkService=Mock(FrameworkService){
            1 * getAuthContextForSubject(_) >> null
            1 * authorizeApplicationResourceAny(_,AuthConstants.RESOURCE_TYPE_SYSTEM_ACL,[ACTION_UPDATE,ACTION_ADMIN]) >> true
        }
        controller.apiService=Mock(ApiService){
            1 * requireVersion(_,_,14) >> true
            1 * extractResponseFormat(_,_,_,_) >> 'json'

            1 * renderWrappedFileContents(_,_,_) >> { args ->
                args[2].contents=args[0]
            }
        }
        controller.authorizationService=Stub(AuthorizationService){
            validateYamlPolicy('test.aclpolicy',_)>>Stub(PoliciesValidation){
                isValid()>>true
            }
        }
        when:
        params.path='test.aclpolicy'
        params.project="test"
        response.format='json'
        request.method='PUT'
        request.contentType='application/yaml'
        request.content=('{ description: \'\', \n' +
                'context: { project: \'test\' }, \n' +
                'by: { username: \'test\' }, \n' +
                'for: { resource: [ { allow: \'x\' } ] } }').bytes
        def result=controller.apiSystemAcls()

        then:
        response.status==200
        response.contentType!=null
        response.contentType.split(';').contains('application/json')
        response.json==[contents:'blah']
    }
    def "system acls DELETE not found"(){
        setup:
        controller.configStorageService=Mock(StorageManager){
            1 * existsFileResource('acls/test.aclpolicy') >> false
        }
        controller.frameworkService=Mock(FrameworkService){
            1 * getAuthContextForSubject(_) >> null
            1 * authorizeApplicationResourceAny(_,AuthConstants.RESOURCE_TYPE_SYSTEM_ACL,[ACTION_DELETE,ACTION_ADMIN]) >> true
        }
        controller.apiService=Mock(ApiService){
            1 * requireVersion(_,_,14) >> true
            1 * extractResponseFormat(_,_,_,_) >> 'json'

            1 * renderErrorFormat(_,[status:404,code:'api.error.item.doesnotexist',args:['System ACL Policy File','test.aclpolicy'],format:'json']) >> {args->
                args[0].status=args[1].status
            }
        }
        when:
        params.path='test.aclpolicy'
        params.project="test"
        response.format='json'
        request.method='DELETE'
        def result=controller.apiSystemAcls()

        then:
        response.status==404
    }
    def "system acls DELETE ok"(){
        setup:
        controller.configStorageService=Mock(StorageManager){
            1 * existsFileResource('acls/test.aclpolicy') >> true
            1 * deleteFileResource('acls/test.aclpolicy') >> true
        }
        controller.frameworkService=Mock(FrameworkService){
            1 * getAuthContextForSubject(_) >> null
            1 * authorizeApplicationResourceAny(_,AuthConstants.RESOURCE_TYPE_SYSTEM_ACL,[ACTION_DELETE,ACTION_ADMIN]) >> true
        }
        controller.apiService=Mock(ApiService){
            1 * requireVersion(_,_,14) >> true
            1 * extractResponseFormat(_,_,_,_) >> 'json'

        }
        when:
        params.path='test.aclpolicy'
        params.project="test"
        response.format='json'
        request.method='DELETE'
        def result=controller.apiSystemAcls()

        then:
        response.status==204
    }
    /**
     * Policy validation failure
     * @return
     */
    def "system acls POST text invalid (json response)"(){
        setup:
        controller.configStorageService=Mock(StorageManager){
            1 * existsFileResource('acls/test.aclpolicy') >> false
        }
        controller.frameworkService=Mock(FrameworkService){
            1 * getAuthContextForSubject(_) >> null
            1 * authorizeApplicationResourceAny(_,AuthConstants.RESOURCE_TYPE_SYSTEM_ACL,[ACTION_CREATE,ACTION_ADMIN]) >> true
        }
        controller.apiService=Mock(ApiService){
            1 * requireVersion(_,_,14) >> true
            1 * extractResponseFormat(_,_,_,_) >> 'json'

            1 * renderJsonAclpolicyValidation(_,_)>>{args->
                args[1].contents='blahz'
            }
        }
        controller.authorizationService=Stub(AuthorizationService){
            validateYamlPolicy('test.aclpolicy',_)>>Stub(PoliciesValidation){
                isValid()>>false
            }
        }
        when:
        params.path='test.aclpolicy'
        params.project="test"
        response.format='json'
        request.method='POST'
        request.contentType='application/yaml'
        request.content=('{ description: \'\', \n' +
                'context: { project: \'test\' }, \n' +
                'by: { username: \'test\' }, \n' +
                'for: { resource: [ { allow: \'x\' } ] } }').bytes
        def result=controller.apiSystemAcls()

        then:
        response.status==400
        response.contentType.split(';').contains('application/json')
        response.json==[contents:'blahz']
    }
    /**
     * Policy validation failure
     * @return
     */
    def "system acls POST text invalid (xml response)"(){
        setup:
        controller.configStorageService=Mock(StorageManager){
            1 * existsFileResource('acls/test.aclpolicy') >> false
        }
        controller.frameworkService=Mock(FrameworkService){
            1 * getAuthContextForSubject(_) >> null
                         1 * authorizeApplicationResourceAny(_,AuthConstants.RESOURCE_TYPE_SYSTEM_ACL,[ACTION_CREATE,ACTION_ADMIN]) >> true
        }
        controller.apiService=Mock(ApiService){
            1 * requireVersion(_,_,14) >> true
            1 * extractResponseFormat(_,_,_,_) >> 'xml'

            1 * renderXmlAclpolicyValidation(_,_)>>{args->
                args[1].contents {
                    'data'('value')
                }
            }
        }
        controller.authorizationService=Stub(AuthorizationService){
            validateYamlPolicy('test.aclpolicy',_)>>Stub(PoliciesValidation){
                isValid()>>false
            }
        }
        when:
        params.path='test.aclpolicy'
        params.project="test"
        response.format='xml'
        request.method='POST'
        request.contentType='application/yaml'
        request.content=('{ description: \'\', \n' +
                'context: { project: \'test\' }, \n' +
                'by: { username: \'test\' }, \n' +
                'for: { resource: [ { allow: \'x\' } ] } }').bytes
        def result=controller.apiSystemAcls()

        then:
        response.status==400
        response.contentType.split(';').contains('application/xml')
        response.xml!=null
        response.xml.data.text()=='value'

    }

    def "save project with description"(){
        setup:
        def fwkService=Mock(FrameworkService)
        controller.frameworkService = fwkService
        controller.resourcesPasswordFieldsService = Mock(PasswordFieldsService)
        controller.fcopyPasswordFieldsService = Mock(PasswordFieldsService)
        controller.execPasswordFieldsService = Mock(PasswordFieldsService)
        controller.userService = Mock(UserService)
        controller.scheduledExecutionService = Mock(ScheduledExecutionService){
            isProjectExecutionEnabled(_) >> true
        }

        params.project = "TestSaveProject"
        params.description='abc'

        setupFormTokens(params)
        when:
        request.method = "POST"
        controller.saveProject()

        then:
        response.status==302
        request.errors == null
        1 * fwkService.authResourceForProject(_)
        1 * fwkService.getAuthContextForSubject(_)
        1 * fwkService.authorizeApplicationResourceAny(null,null,['configure','admin']) >> true
        1 * fwkService.listDescriptions() >> [null,null,null]
        1 * fwkService.updateFrameworkProjectConfig(_,{
            it['project.description'] == 'abc'
        },_) >> [success:true]
        1 * fwkService.validateProjectConfigurableInput(_,_,{!it.test('resourceModelSource')})>>[:]

    }
    def "save project with out description"(){
        setup:
        def fwkService=Mock(FrameworkService)
        controller.frameworkService = fwkService
        controller.resourcesPasswordFieldsService = Mock(PasswordFieldsService)
        controller.fcopyPasswordFieldsService = Mock(PasswordFieldsService)
        controller.execPasswordFieldsService = Mock(PasswordFieldsService)
        controller.userService = Mock(UserService)
        controller.scheduledExecutionService = Mock(ScheduledExecutionService){
            isProjectExecutionEnabled(_) >> true
        }

        params.project = "TestSaveProject"

        setupFormTokens(params)
        when:
        request.method = "POST"
        controller.saveProject()

        then:
        response.status==302
        request.errors == null
        1 * fwkService.authResourceForProject(_)
        1 * fwkService.getAuthContextForSubject(_)
        1 * fwkService.authorizeApplicationResourceAny(null,null,['configure','admin']) >> true
        1 * fwkService.listDescriptions() >> [null,null,null]
        1 * fwkService.updateFrameworkProjectConfig(_,{
            it['project.description'] == ''
        },_) >> [success:true]
        1 * fwkService.validateProjectConfigurableInput(_,_,{!it.test('resourceModelSource')})>>[:]

    }
    def "get project resources, project dne"(){
        setup:
        controller.frameworkService=Mock(FrameworkService){
            1 * getRundeckFramework()
            1 * existsFrameworkProject('test') >> false
            0 * _(*_)
        }
        controller.apiService=Mock(ApiService){
            1 * requireApi(_,_) >> true
            1 * renderErrorFormat(_,{map->
                map.status==404
            })>>'404result'
            0 * _(*_)
        }
        def query = new ExtNodeFilters(project: 'test')
        params.project="test"
        when:

        def result=controller.apiResources(query)

        then:
        result == '404result'
    }

    def "get project resources filters authorized nodes"() {
        setup:
        def authCtx = Mock(UserAndRolesAuthContext)
        def nodeSet = new NodeSetImpl()
        nodeSet.putNode(new NodeEntryImpl('monkey1'))
        nodeSet.putNode(new NodeEntryImpl('monkey2'))
        def authedNodes = new NodeSetImpl()
        authedNodes.putNode(new NodeEntryImpl('monkey1'))
        def projectName = 'testproj'
        controller.frameworkService = Mock(FrameworkService) {
            1 * getRundeckFramework() >> Mock(IFramework) {
                1 * getResourceFormatGeneratorService() >> Mock(ResourceFormatGeneratorService) {
                    getGeneratorForFormat('resourcexml') >> new ResourceXMLFormatGenerator()
                }
            }
            1 * existsFrameworkProject(projectName) >> true

            1 * getAuthContextForSubjectAndProject(_, projectName) >> authCtx

            1 * authorizeProjectResourceAll(authCtx, [type: 'resource', kind: 'node'], ['read'], projectName) >> true

            1 * getFrameworkProject(projectName) >> Mock(IRundeckProject) {
                1 * getNodeSet() >> nodeSet
            }
            1 * filterAuthorizedNodes(projectName, Collections.singleton('read'), !null, authCtx) >> authedNodes
            0 * _(*_)
        }
        controller.apiService = Mock(ApiService) {
            1 * requireApi(_, _) >> true
//            1 * requireVersion(_, _, 3) >> true

            0 * _(*_)
        }
        def query = new ExtNodeFilters(project: projectName)
        params.project = projectName
        when:
        response.format = 'xml'
        def result = controller.apiResources(query)

        then:
        response.contentType == 'text/xml;charset=UTF-8'
    }

    @Unroll
    def "get project resources default #mime for api_version #api_version"() {
        setup:
        def authCtx = Mock(UserAndRolesAuthContext)
        def nodeSet = new NodeSetImpl()
        nodeSet.putNode(new NodeEntryImpl('monkey1'))
        nodeSet.putNode(new NodeEntryImpl('monkey2'))
        def authedNodes = new NodeSetImpl()
        authedNodes.putNode(new NodeEntryImpl('monkey1'))
        def projectName = 'testproj'
        controller.frameworkService = Mock(FrameworkService) {
            1 * getRundeckFramework() >> Mock(IFramework) {
                1 * getResourceFormatGeneratorService() >> Mock(ResourceFormatGeneratorService) {
                    getGeneratorForFormat('resourcexml') >> new ResourceXMLFormatGenerator()
                    getGeneratorForFormat('resourcejson') >> new ResourceJsonFormatGenerator()
                }
            }
            1 * existsFrameworkProject(projectName) >> true

            1 * getAuthContextForSubjectAndProject(_, projectName) >> authCtx

            1 * authorizeProjectResourceAll(authCtx, [type: 'resource', kind: 'node'], ['read'], projectName) >> true

            1 * getFrameworkProject(projectName) >> Mock(IRundeckProject) {
                1 * getNodeSet() >> nodeSet
            }
            1 * filterAuthorizedNodes(projectName, Collections.singleton('read'), !null, authCtx) >> authedNodes
            0 * _(*_)
        }
        controller.apiService = Mock(ApiService) {
            1 * requireApi(_, _) >> true

            0 * _(*_)
        }
        def query = new ExtNodeFilters(project: projectName)
        params.project = projectName
        request.api_version = api_version
        when:
        response.format = 'all'
        def result = controller.apiResources(query)

        then:
        response.contentType == "$mime;charset=UTF-8"

        where:
        api_version | mime
        22          | 'text/xml'
        23          | 'application/json'
    }

    def "get single resource filters checks acl"() {
        setup:
        def authCtx = Mock(UserAndRolesAuthContext)
        def nodeSet = new NodeSetImpl()
        nodeSet.putNode(new NodeEntryImpl('monkey1'))
        nodeSet.putNode(new NodeEntryImpl('monkey2'))
        def authedNodes = new NodeSetImpl()
        authedNodes.putNode(new NodeEntryImpl('monkey1'))
        def projectName = 'testproj'
        controller.frameworkService = Mock(FrameworkService) {
            1 * getRundeckFramework() >> Mock(IFramework) {
                1 * getResourceFormatGeneratorService() >> Mock(ResourceFormatGeneratorService) {
                    getGeneratorForFormat('resourcexml') >> new ResourceXMLFormatGenerator()
                }
            }
            1 * existsFrameworkProject(projectName) >> true

            1 * getAuthContextForSubjectAndProject(_, projectName) >> authCtx

            1 * authorizeProjectResourceAll(authCtx, [type: 'resource', kind: 'node'], ['read'], projectName) >> true

            1 * getFrameworkProject(projectName) >> Mock(IRundeckProject) {
                1 * getNodeSet() >> nodeSet
            }
            1 * filterAuthorizedNodes(projectName, Collections.singleton('read'), !null, authCtx) >> authedNodes
            0 * _(*_)
        }
        controller.apiService = Mock(ApiService) {
            1 * requireApi(_, _) >> true

            0 * _(*_)
        }
        def query = new ExtNodeFilters(project: projectName)
        params.project = projectName
        params.name = 'monkey1'
        when:
        response.format = 'xml'
        def result = controller.apiResource()

        then:
        response.contentType == 'text/xml;charset=UTF-8'
        response.status == 200
    }

    @Unroll
    def "get single resource default #mime for api_version #api_version"() {
        setup:
        def authCtx = Mock(UserAndRolesAuthContext)
        def nodeSet = new NodeSetImpl()
        nodeSet.putNode(new NodeEntryImpl('monkey1'))
        nodeSet.putNode(new NodeEntryImpl('monkey2'))
        def authedNodes = new NodeSetImpl()
        authedNodes.putNode(new NodeEntryImpl('monkey1'))
        def projectName = 'testproj'
        controller.frameworkService = Mock(FrameworkService) {
            1 * getRundeckFramework() >> Mock(IFramework) {
                1 * getResourceFormatGeneratorService() >> Mock(ResourceFormatGeneratorService) {
                    getGeneratorForFormat('resourcexml') >> new ResourceXMLFormatGenerator()
                    getGeneratorForFormat('resourcejson') >> new ResourceJsonFormatGenerator()
                }
            }
            1 * existsFrameworkProject(projectName) >> true

            1 * getAuthContextForSubjectAndProject(_, projectName) >> authCtx

            1 * authorizeProjectResourceAll(authCtx, [type: 'resource', kind: 'node'], ['read'], projectName) >> true

            1 * getFrameworkProject(projectName) >> Mock(IRundeckProject) {
                1 * getNodeSet() >> nodeSet
            }
            1 * filterAuthorizedNodes(projectName, Collections.singleton('read'), !null, authCtx) >> authedNodes
            0 * _(*_)
        }
        controller.apiService = Mock(ApiService) {
            1 * requireApi(_, _) >> true

            0 * _(*_)
        }
        def query = new ExtNodeFilters(project: projectName)
        params.project = projectName
        params.name = 'monkey1'
        request.api_version = api_version
        when:
        response.format = 'all'
        def result = controller.apiResource()

        then:
        response.contentType == "$mime;charset=UTF-8"
        response.status == 200
        where:
        api_version | mime
        22          | 'text/xml'
        23          | 'application/json'
    }

    def "get single resource filters unauthorized"() {
        setup:
        def authCtx = Mock(UserAndRolesAuthContext)
        def nodeSet = new NodeSetImpl()
        nodeSet.putNode(new NodeEntryImpl('monkey1'))
        nodeSet.putNode(new NodeEntryImpl('monkey2'))
        def authedNodes = new NodeSetImpl()
        //empty authorization result
        def projectName = 'testproj'
        controller.frameworkService = Mock(FrameworkService) {
            1 * getRundeckFramework()
            1 * existsFrameworkProject(projectName) >> true

            1 * getAuthContextForSubjectAndProject(_, projectName) >> authCtx

            1 * authorizeProjectResourceAll(authCtx, [type: 'resource', kind: 'node'], ['read'], projectName) >> true

            1 * getFrameworkProject(projectName) >> Mock(IRundeckProject) {
                1 * getNodeSet() >> nodeSet
            }
            1 * filterAuthorizedNodes(projectName, Collections.singleton('read'), !null, authCtx) >> authedNodes
            0 * _(*_)
        }
        controller.apiService = Mock(ApiService) {
            1 * requireApi(_, _) >> true
            1 * renderErrorFormat(_, _) >> { it[0].status = it[1].status }
            0 * _(*_)
        }
        def query = new ExtNodeFilters(project: projectName)
        params.project = projectName
        params.name = 'monkey1'
        when:

        def result = controller.apiResource()

        then:
        response.status == 404
    }

    def "POST project source resources, not writeable, should result in 405 response"() {
        setup:
        controller.frameworkService = Mock(FrameworkService) {
            1 * existsFrameworkProject('test') >> true
            1 * getFrameworkProject('test')>>Mock(IRundeckProject){
                1 * getProjectNodes()>>Mock(IProjectNodes){
                    1 * getWriteableResourceModelSources()>>[]
                }
            }
            1 * getAuthContextForSubjectAndProject(_,'test')
            1 * authResourceForProject('test')
            1 * authorizeApplicationResourceAll(_,_,['configure','admin']) >> true
            0 * _(*_)
        }
        controller.apiService = Mock(ApiService) {
            1 * requireVersion(_, _, 23) >> true
            1 * requireParameters(_, _, ['project', 'index']) >> true
            1 * requireExists(_, _, ['project', 'test']) >> true
            1 * requireExists(_, 1, ['source index', '1']) >> true

            1 * requireAuthorized(_,_,['configure','Project','test']) >> true
            1 * renderErrorFormat(_, _) >> { it[0].status = it[1].status }
            0 * _(*_)
        }

        params.project = "test"
        params.index = "1"
        request.method = 'POST'
        request.json = [
            "testnode": [
                hostname: "testnode",
                nodename: "testnode",
                tags    : "test",
            ]
        ]
        when:

        def result = controller.apiSourceWriteContent()

        then:
        response.status == 405
    }
    protected void setupFormTokens(params) {
        def token = SynchronizerTokensHolder.store(session)
        params[SynchronizerTokensHolder.TOKEN_KEY] = token.generateToken('/test')
        params[SynchronizerTokensHolder.TOKEN_URI] = '/test'
    }

    static class TestConfigurableBean implements RundeckProjectConfigurable {

        Map<String, String> categories = [:]

        List<Property> projectConfigProperties = []

        Map<String, String> propertiesMapping = [:]
    }

    @Unroll
    def "save project updating passive mode"(){
        setup:
        defineBeans {
            testConfigurableBean(TestConfigurableBean) {
                projectConfigProperties = ScheduledExecutionService.ProjectConfigProperties
                propertiesMapping = ScheduledExecutionService.ConfigPropertiesMapping
            }
        }
        def fwkService = Mock(FrameworkService) {
            validateProjectConfigurableInput(_, _,_) >> [config: [testConfigurableBean: [
                    disableExecution: disableExecution,
                    disableSchedule : disableSchedule
            ]],props:[
                    'project.disable.executions':disableExecution,
                    'project.disable.schedule':disableSchedule
            ]]
        }
        controller.frameworkService = fwkService
        controller.resourcesPasswordFieldsService = Mock(PasswordFieldsService)
        controller.fcopyPasswordFieldsService = Mock(PasswordFieldsService)
        controller.execPasswordFieldsService = Mock(PasswordFieldsService)
        controller.userService = Mock(UserService)
        def sEService=Mock(ScheduledExecutionService){
            isProjectExecutionEnabled(_) >> !currentExecutionDisabled
            isProjectScheduledEnabled(_) >> !currentScheduleDisabled
        }
        controller.scheduledExecutionService = sEService

        params.project = "TestSaveProject"
        params.description='abc'
        params.extraConfig = [
                testConfigurableBean: [
                        disableExecution   : disableExecution,
                        disableSchedule: disableSchedule
                ]
        ]

        setupFormTokens(params)
        when:
        request.method = "POST"
        controller.saveProject()

        then:
        response.status==302
        request.errors == null
        1 * fwkService.authResourceForProject(_)
        1 * fwkService.getAuthContextForSubject(_)
        1 * fwkService.authorizeApplicationResourceAny(null,null,['configure','admin']) >> true
        1 * fwkService.listDescriptions() >> [null,null,null]
        1 * fwkService.updateFrameworkProjectConfig(_,{
            it['project.description'] == 'abc'
        },_) >> [success:true]
        if(shouldReSchedule){
            1 * sEService.rescheduleJobs(_,_)
        }else{
            0 * sEService.rescheduleJobs(_,_)
        }
        if(shouldUnSchedule){
            1 * sEService.unscheduleJobsForProject(_,_)
        }else{
            0 * sEService.unscheduleJobsForProject(_,_)
        }

        where:
        currentExecutionDisabled | currentScheduleDisabled | disableExecution | disableSchedule | shouldReSchedule | shouldUnSchedule
        false                    | false                   | 'false'          | 'false'         | false            | false
        false                    | false                   | 'true'           | 'false'         | false            | true
        false                    | false                   | 'false'          | 'true'          | false            | true
        false                    | false                   | 'true'           | 'true'          | false            | true
        true                     | false                   | 'false'          | 'false'         | true             | false
        true                     | false                   | 'true'           | 'false'         | false            | false
        true                     | false                   | 'false'          | 'true'          | false            | true
        true                     | false                   | 'true'           | 'true'          | false            | true
        false                    | true                    | 'false'          | 'false'         | true             | false
        false                    | true                    | 'true'           | 'false'         | false            | true
        false                    | true                    | 'false'          | 'true'          | false            | false
        false                    | true                    | 'true'           | 'true'          | false            | true
        true                     | true                    | 'false'          | 'false'         | true             | false
        true                     | true                    | 'true'           | 'false'         | false            | true
        true                     | true                    | 'false'          | 'true'          | false            | true
        true                     | true                    | 'true'           | 'true'          | false            | false

    }


    def "create project invalid name"(){
        setup:
        controller.metricService = Mock(MetricService)
        def rdframework=Mock(Framework){
            1 * getNodeExecutorService() >> Mock(NodeExecutorService)
            1 * getResourceModelSourceService() >> Mock(ResourceModelSourceService)
            1 * getFileCopierService() >> Mock(FileCopierService)
        }
        controller.frameworkService=Mock(FrameworkService){
            1 * getRundeckFramework() >> rdframework
            1 * getAuthContextForSubject(_) >> null
            1 * authorizeApplicationResourceTypeAll(null,'project',[AuthConstants.ACTION_CREATE])>>true
            1 * validateProjectConfigurableInput(_,_,_)>>[props:[:]]
        }


        def description = 'Project Desc'
        params.newproject = "Test SaveProject"
        params.description=description

        setupFormTokens(params)
        when:
        request.method = "POST"
        controller.createProjectPost()

        then:
        response.status==200
        request.errors == ['project.name.can.only.contain.these.characters']
        model.projectDescription == description

    }
}
