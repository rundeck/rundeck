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
import com.dtolabs.rundeck.core.common.*
import com.dtolabs.rundeck.core.config.Features
import com.dtolabs.rundeck.core.execution.service.FileCopierService
import com.dtolabs.rundeck.core.execution.service.NodeExecutorService
import com.dtolabs.rundeck.core.plugins.DescribedPlugin
import com.dtolabs.rundeck.core.plugins.ValidatedPlugin
import com.dtolabs.rundeck.core.plugins.configuration.Property
import com.dtolabs.rundeck.core.plugins.configuration.Validator
import com.dtolabs.rundeck.core.resources.ResourceModelSourceService
import com.dtolabs.rundeck.core.resources.WriteableModelSource
import com.dtolabs.rundeck.core.resources.format.ResourceFormatGeneratorService
import com.dtolabs.rundeck.core.resources.format.ResourceXMLFormatGenerator
import com.dtolabs.rundeck.core.resources.format.json.ResourceJsonFormatGenerator
import grails.test.hibernate.HibernateSpec
import grails.test.mixin.Mock
import grails.test.mixin.TestFor
import grails.test.mixin.TestMixin
import grails.test.mixin.web.GroovyPageUnitTestMixin
import grails.testing.services.ServiceUnitTest
import grails.testing.web.controllers.ControllerUnitTest
import org.grails.plugins.metricsweb.MetricService
import org.grails.web.servlet.mvc.SynchronizerTokensHolder
import org.rundeck.app.acl.ACLManager
import org.rundeck.core.projects.ProjectConfigurable
import rundeck.NodeFilter
import rundeck.Project
import rundeck.User
import rundeck.UtilityTagLib
import rundeck.services.*
import rundeck.services.authorization.PoliciesValidation
import rundeck.services.feature.FeatureService
import spock.lang.Specification
import spock.lang.Unroll

import static org.rundeck.core.auth.AuthConstants.*

/**
 * Created by greg on 7/28/15.
 */
class FrameworkControllerSpec extends HibernateSpec implements ControllerUnitTest<FrameworkController> {

    List<Class> getDomainClasses() { [NodeFilter, User] }

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
            1 * authorizeApplicationResourceAny(null,RESOURCE_TYPE_SYSTEM_ACL,[action,ACTION_ADMIN])>>false
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
            1 * authorizeApplicationResourceAny(null,RESOURCE_TYPE_SYSTEM_ACL,[ACTION_READ,ACTION_ADMIN])>>true
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
        controller.aclManagerService=Mock(ACLManager){
            existsPolicyFile(_) >> false
        }
        controller.frameworkService=Mock(FrameworkService){
            1 * getAuthContextForSubject(_) >> null
            1 * authorizeApplicationResourceAny(_,RESOURCE_TYPE_SYSTEM_ACL,[ACTION_READ,ACTION_ADMIN]) >> true
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
        controller.aclManagerService=Mock(ACLManager){
            1 * existsPolicyFile(_) >> true
            1 * loadPolicyFileContents('blah.aclpolicy',_) >> {args->
                args[1].write('blah'.bytes)
                4
            }
        }
        controller.frameworkService=Mock(FrameworkService){
            1 * getAuthContextForSubject(_) >> null
            1 * authorizeApplicationResourceAny(_,RESOURCE_TYPE_SYSTEM_ACL,[ACTION_READ,ACTION_ADMIN]) >> true
        }
        controller.apiService=Mock(ApiService){
            1 * requireVersion(_,_,14) >> true
            1 * extractResponseFormat(_,_,_,_) >> 'json'
        }
        when:
        params.path='blah.aclpolicy'
        params.project="test"
        response.format='json'
        def result=controller.apiSystemAcls()

        then:
        response.status==200
        response.contentType.split(';').contains('application/json')
        response.json == [contents:"blah"]
    }
    def "system acls GET xml"(){
        setup:
        controller.aclManagerService=Mock(ACLManager){
            1 * existsPolicyFile(_) >> true
            1 * loadPolicyFileContents('blah.aclpolicy',_) >> {args->
                args[1].write('blah'.bytes)
                4
            }
        }
        controller.frameworkService=Mock(FrameworkService){
            1 * getAuthContextForSubject(_) >> null
            1 * authorizeApplicationResourceAny(_,RESOURCE_TYPE_SYSTEM_ACL,[ACTION_READ,ACTION_ADMIN]) >> true
        }
        controller.apiService=Mock(ApiService){
            1 * requireVersion(_,_,14) >> true
            1 * extractResponseFormat(_,_,_,_) >> 'xml'
            1 * renderWrappedFileContentsXml('blah','xml',_)
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
        controller.aclManagerService=Mock(ACLManager){
            1 * existsPolicyFile(_) >> true
            1 * loadPolicyFileContents('blah.aclpolicy',_) >> {args->
                args[1].write('blah'.bytes)
                4
            }
        }
        controller.frameworkService=Mock(FrameworkService){
            1 * getAuthContextForSubject(_) >> null
            1 * authorizeApplicationResourceAny(_,RESOURCE_TYPE_SYSTEM_ACL,[ACTION_READ,ACTION_ADMIN]) >> true
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
        controller.aclManagerService=Mock(ACLManager){
            1 * listStoredPolicyFiles() >> ['blah.aclpolicy']
            0*_(*_)
        }
        controller.frameworkService=Mock(FrameworkService){
            1 * getAuthContextForSubject(_) >> null
            1 * authorizeApplicationResourceAny(_, RESOURCE_TYPE_SYSTEM_ACL,[ACTION_READ, ACTION_ADMIN]) >> true
            0*_(*_)
        }
        controller.apiService=Mock(ApiService){
            1 * requireVersion(_,_,14) >> true
            1 * extractResponseFormat(_,_,_,_) >> 'json'
            1 * jsonRenderDirlist('',_,_,['blah.aclpolicy'])>>{args-> [success: true] }
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
            controller.aclManagerService=Mock(ACLManager){
            1 * listStoredPolicyFiles() >> ['blah.aclpolicy']

            0*_(*_)
        }
        controller.frameworkService=Mock(FrameworkService){
            1 * getAuthContextForSubject(_) >> null
                         1 * authorizeApplicationResourceAny(_,RESOURCE_TYPE_SYSTEM_ACL,[ACTION_READ,ACTION_ADMIN]) >> true
            0*_(*_)
        }
        controller.apiService=Mock(ApiService){
            1 * requireVersion(_,_,14) >> true
            1 * extractResponseFormat(_,_,_,_) >> 'xml'
            1 * xmlRenderDirList('',_,_,['blah.aclpolicy'],_)>>{args-> args[4].success(ok:true)}
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
        controller.aclManagerService=Mock(ACLManager){
            1 * existsPolicyFile('test.aclpolicy') >> false
            1 * storePolicyFileContents('test.aclpolicy',_) >> 4
            1 * loadPolicyFileContents('test.aclpolicy',_) >> {args->
                args[1].write('blah'.bytes)
                4
            }
            1 * validateYamlPolicy('test.aclpolicy',_)>>Stub(PoliciesValidation){
                isValid()>>true
            }
        }
        controller.frameworkService=Mock(FrameworkService){
            1 * getAuthContextForSubject(_) >> null
            1 * authorizeApplicationResourceAny(_,RESOURCE_TYPE_SYSTEM_ACL,[ACTION_CREATE,ACTION_ADMIN]) >> true
        }
        controller.apiService=Mock(ApiService){
            1 * requireVersion(_,_,14) >> true
            1 * extractResponseFormat(_,_,_,_) >> 'json'
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
        controller.aclManagerService=Mock(ACLManager){
            1 * existsPolicyFile('test.aclpolicy') >> true
            0 * validateYamlPolicy('test.aclpolicy',_)>>Stub(PoliciesValidation){
                isValid()>>true
            }
        }
        controller.frameworkService=Mock(FrameworkService){
            1 * getAuthContextForSubject(_) >> null
            1 * authorizeApplicationResourceAny(_,RESOURCE_TYPE_SYSTEM_ACL,[ACTION_CREATE,ACTION_ADMIN]) >> true
        }
        controller.apiService=Mock(ApiService){
            1 * requireVersion(_,_,14) >> true
            1 * extractResponseFormat(_,_,_,_) >> 'json'

            1 * renderErrorFormat(_,[status:409,code:'api.error.item.alreadyexists',args:['System ACL Policy File','test.aclpolicy'],format:'json']) >> {args->
                args[0].status=args[1].status
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
        controller.aclManagerService=Mock(ACLManager){
            1 * existsPolicyFile('test.aclpolicy') >> false
            0 * validateYamlPolicy('test.aclpolicy',_)>>Stub(PoliciesValidation){
                isValid()>>true
            }
        }
        controller.frameworkService=Mock(FrameworkService){
            1 * getAuthContextForSubject(_) >> null
            1 * authorizeApplicationResourceAny(_,RESOURCE_TYPE_SYSTEM_ACL,[ACTION_UPDATE,ACTION_ADMIN]) >> true
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
        controller.aclManagerService=Mock(ACLManager){
            1 * existsPolicyFile('test.aclpolicy') >> true

            1 * storePolicyFileContents('test.aclpolicy',_) >> 4
            1 * loadPolicyFileContents('test.aclpolicy',_) >> {args->
                args[1].write('blah'.bytes)
                4
            }
            1 * validateYamlPolicy('test.aclpolicy',_)>>Stub(PoliciesValidation){
                isValid()>>true
            }
        }
        controller.frameworkService=Mock(FrameworkService){
            1 * getAuthContextForSubject(_) >> null
            1 * authorizeApplicationResourceAny(_,RESOURCE_TYPE_SYSTEM_ACL,[ACTION_UPDATE,ACTION_ADMIN]) >> true
        }
        controller.apiService=Mock(ApiService){
            1 * requireVersion(_,_,14) >> true
            1 * extractResponseFormat(_,_,_,_) >> 'json'
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
        controller.aclManagerService=Mock(ACLManager){
            1 * existsPolicyFile('test.aclpolicy') >> false
        }
        controller.frameworkService=Mock(FrameworkService){
            1 * getAuthContextForSubject(_) >> null
            1 * authorizeApplicationResourceAny(_,RESOURCE_TYPE_SYSTEM_ACL,[ACTION_DELETE,ACTION_ADMIN]) >> true
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
        controller.aclManagerService=Mock(ACLManager){
            1 * existsPolicyFile('test.aclpolicy') >> true
            1 * deletePolicyFile('test.aclpolicy') >> true
        }
        controller.frameworkService=Mock(FrameworkService){
            1 * getAuthContextForSubject(_) >> null
            1 * authorizeApplicationResourceAny(_,RESOURCE_TYPE_SYSTEM_ACL,[ACTION_DELETE,ACTION_ADMIN]) >> true
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
        controller.aclManagerService=Mock(ACLManager){
            1 * existsPolicyFile('test.aclpolicy') >> false
            1 * validateYamlPolicy('test.aclpolicy',_)>>Stub(PoliciesValidation){
                isValid()>>false
            }
        }
        controller.frameworkService=Mock(FrameworkService){
            1 * getAuthContextForSubject(_) >> null
            1 * authorizeApplicationResourceAny(_,RESOURCE_TYPE_SYSTEM_ACL,[ACTION_CREATE,ACTION_ADMIN]) >> true
        }
        controller.apiService=Mock(ApiService){
            1 * requireVersion(_,_,14) >> true
            1 * extractResponseFormat(_,_,_,_) >> 'json'

            1 * renderJsonAclpolicyValidation(_)>>{args->
                [contents:'blahz']
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
        controller.aclManagerService=Mock(ACLManager){
            1 * existsPolicyFile('test.aclpolicy') >> false
            1 * validateYamlPolicy('test.aclpolicy',_)>>Stub(PoliciesValidation){
                isValid()>>false
            }
        }
        controller.frameworkService=Mock(FrameworkService){
            1 * getAuthContextForSubject(_) >> null
                         1 * authorizeApplicationResourceAny(_,RESOURCE_TYPE_SYSTEM_ACL,[ACTION_CREATE,ACTION_ADMIN]) >> true
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
        controller.featureService = Mock(FeatureService)
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

    def "save project change label updates session"() {
        setup:
            def fwkService = Mock(FrameworkService)
            controller.frameworkService = fwkService
            controller.resourcesPasswordFieldsService = Mock(PasswordFieldsService)
            controller.fcopyPasswordFieldsService = Mock(PasswordFieldsService)
            controller.execPasswordFieldsService = Mock(PasswordFieldsService)
            controller.userService = Mock(UserService)
            controller.featureService = Mock(FeatureService)
            controller.scheduledExecutionService = Mock(ScheduledExecutionService) {
                isProjectExecutionEnabled(_) >> true
            }

            params.project = "TestSaveProject"
            params.label = 'A Label'

            setupFormTokens(params)
        when:
            request.method = "POST"
            controller.saveProject()

        then:
            response.status == 302
            request.errors == null
            1 * fwkService.updateFrameworkProjectConfig(
                _, {
                it['project.label'] == 'A Label'
            }, _
            ) >> [success: true]

            1 * fwkService.authResourceForProject(_)
            1 * fwkService.getAuthContextForSubject(_)
            1 * fwkService.authorizeApplicationResourceAny(null, null, ['configure', 'admin']) >> true
            1 * fwkService.listDescriptions() >> [null, null, null]
            1 * fwkService.validateProjectConfigurableInput(_, _, { !it.test('resourceModelSource') }) >> [:]
            1 * fwkService.getRundeckFramework()
            1 * fwkService.isClusterModeEnabled()

            1 * fwkService.refreshSessionProjects(_,_)
            1 * fwkService.loadSessionProjectLabel(_, 'TestSaveProject', 'A Label')
            0 * fwkService._(*_)
    }
    def "save project with out description"(){
        setup:
        def fwkService=Mock(FrameworkService)
        controller.frameworkService = fwkService
        controller.resourcesPasswordFieldsService = Mock(PasswordFieldsService)
        controller.fcopyPasswordFieldsService = Mock(PasswordFieldsService)
        controller.execPasswordFieldsService = Mock(PasswordFieldsService)
        controller.userService = Mock(UserService)
        controller.featureService = Mock(FeatureService)
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

    def "save project with execution cleaner"() {
        setup:
            def fwkService = Mock(FrameworkService)
            controller.frameworkService = fwkService
            controller.resourcesPasswordFieldsService = Mock(PasswordFieldsService)
            controller.fcopyPasswordFieldsService = Mock(PasswordFieldsService)
            controller.execPasswordFieldsService = Mock(PasswordFieldsService)
            controller.userService = Mock(UserService)
            controller.featureService = Mock(FeatureService){
                featurePresent(Features.CLEAN_EXECUTIONS_HISTORY, _) >> true
            }
            controller.scheduledExecutionService = Mock(ScheduledExecutionService) {
                isProjectExecutionEnabled(_) >> true
            }

            params.project = "TestSaveProject"
            params.cleanerHistory = 'on'
            params.cleanperiod = '1'
            params.minimumtokeep = '2'
            params.maximumdeletionsize = '3'
            params.crontabString = 'crontab1'

            setupFormTokens(params)
        when:
            request.method = "POST"
            controller.saveProject()

        then:
            response.status == 302
            request.errors == null
            1 * controller.frameworkService.scheduleCleanerExecutions('TestSaveProject',{
                it.enabled && it.maxDaysToKeep==1 && it.cronExpression=='crontab1' && it.minimumExecutionToKeep==2 && it.maximumDeletionSize==3
            })
            1 * fwkService.authResourceForProject(_)
            1 * fwkService.getAuthContextForSubject(_)
            1 * fwkService.authorizeApplicationResourceAny(null, null, ['configure', 'admin']) >> true
            1 * fwkService.listDescriptions() >> [null, null, null]
            1 * fwkService.updateFrameworkProjectConfig(
                _, {
                it['project.description'] == ''
            }, _
            ) >> [success: true]
            1 * fwkService.validateProjectConfigurableInput(_, _, { !it.test('resourceModelSource') }) >> [:]

    }

    @Unroll
    def "save project default file copier"() {
        setup:
            def fwkService = Mock(FrameworkService)
            controller.frameworkService = fwkService
            controller.resourcesPasswordFieldsService = Mock(PasswordFieldsService)
            controller.fcopyPasswordFieldsService = Mock(PasswordFieldsService)
            controller.execPasswordFieldsService = Mock(PasswordFieldsService)
            controller.userService = Mock(UserService)
            controller.featureService = Mock(FeatureService)
            controller.scheduledExecutionService = Mock(ScheduledExecutionService) {
                isProjectExecutionEnabled(_) >> true
            }

            params.project = "TestSaveProject"
            params."default_${service}" = 'blah'
            def defaultsConfig = [
                    specialvalue1: "foobar",
                    specialvalue2: "barfoo",
                    specialvalue3: "fizbaz"
            ]
            params."$prefix" = [
                    "default": [
                            type  : type,
                            config: defaultsConfig
                    ]
            ]

            setupFormTokens(params)
        when:
            request.method = "POST"
            controller.saveProject()

        then:
            response.status == 302
            request.errors == null
            1 * fwkService.authResourceForProject(_)
            1 * fwkService.getAuthContextForSubject(_)
            1 * fwkService.authorizeApplicationResourceAny(null, null, ['configure', 'admin']) >> true
            1 * fwkService.validateServiceConfig('foobar', "${prefix}.default.config.", _, _) >> [valid: true]
            if (service == 'FileCopier') {
                controller.fcopyPasswordFieldsService.untrack(
                        [[config: [type: type, props: defaultsConfig], index: 0]],
                        _
                )
                1 * fwkService.addProjectFileCopierPropertiesForType(type, _, defaultsConfig, _)
            } else {
                1 * controller.execPasswordFieldsService.untrack(
                        [[config: [type: type, props: defaultsConfig], index: 0]],
                        _
                )
                1 * fwkService.addProjectNodeExecutorPropertiesForType(type, _, defaultsConfig, _)
            }
            1 * fwkService.listDescriptions() >> [null, null, null]
            1 * fwkService.updateFrameworkProjectConfig(
                    _, {
                it['project.description'] == ''
            }, _
            ) >> [success: true]
            1 * fwkService.validateProjectConfigurableInput(_, _, { !it.test('resourceModelSource') }) >> [:]

        where:
            service        | type     | prefix
            'FileCopier'   | 'foobar' | 'fcopy'
            'NodeExecutor' | 'foobar' | 'nodeexec'
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
            1 * authorizeApplicationResourceAny(_,_,['configure','admin']) >> true
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

    def "POST project source resources,  writeable, catch IO Exception"() {
        setup:
            def source =Mock(WriteableModelSource){
                1 * writeData(_)>>{
                    throw new IOException("expected error")
                }
                1 * getSyntaxMimeType()>>'application/json'
                0 * _(*_)
            }
            controller.frameworkService = Mock(FrameworkService) {
                1 * existsFrameworkProject('test') >> true
                1 * getRundeckFramework()
                1 * getFrameworkProject('test')>>Mock(IRundeckProject){
                    1 * getProjectNodes()>>Mock(IProjectNodes){
                        1 * getWriteableResourceModelSources()>>[
                            Mock(IProjectNodes.WriteableProjectNodes){
                                getWriteableSource()>>source
                                getIndex()>>1
                                getType()>>'monkey'
                            }
                        ]
                    }
                }
                1 * getAuthContextForSubjectAndProject(_,'test')
                1 * authResourceForProject('test')
                1 * authorizeApplicationResourceAny(_,_,['configure','admin']) >> true
                0 * _(*_)
            }
            controller.apiService = Mock(ApiService) {
                1 * requireVersion(_, _, 23) >> true
                1 * requireParameters(_, _, ['project', 'index']) >> true
                1 * requireExists(_, _, ['project', 'test']) >> true
                1 * requireExists(_, 1, ['source index', '1']) >> true

                1 * requireAuthorized(_,_,['configure','Project','test']) >> true
                1 * renderErrorFormat(_,{it.status==500 && it.args==['expected error']})>>{
                    it[0].status=it[1].status
                }
                0 * _(*_)
            }

            params.project = "test"
            params.index = "1"
            request.method = 'POST'
            request.contentType=contentType
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
            response.status == 500
        where:
            contentType<<[
                'application/json',
                'application/json; charset=utf8'
            ]
    }
    protected void setupFormTokens(params) {
        def token = SynchronizerTokensHolder.store(session)
        params[SynchronizerTokensHolder.TOKEN_KEY] = token.generateToken('/test')
        params[SynchronizerTokensHolder.TOKEN_URI] = '/test'
    }

    static class TestConfigurableBean implements ProjectConfigurable {

        Map<String, String> categories = [:]

        List<Property> projectConfigProperties = []

        Map<String, String> propertiesMapping = [:]
    }

    @Unroll
    def "save project updating passive mode"(){
        setup:
        controller.featureService = Mock(FeatureService)
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

    @Unroll
    def "save project config updating passive mode"(){
        setup:
        controller.featureService = Mock(FeatureService)
        controller.frameworkService = Mock(FrameworkService)
        controller.resourcesPasswordFieldsService = Mock(PasswordFieldsService)
        controller.fcopyPasswordFieldsService = Mock(PasswordFieldsService)
        controller.execPasswordFieldsService = Mock(PasswordFieldsService)
        controller.pluginsPasswordFieldsService = Mock(PasswordFieldsService)
        controller.userService = Mock(UserService)
        def project = "TestSaveProject"
        controller.scheduledExecutionService = Mock(ScheduledExecutionService)

        params.project = project
        params.description='abc'
        params.projectConfig = """
${ScheduledExecutionService.CONF_PROJECT_DISABLE_EXECUTION}=${disableExecution}
${ScheduledExecutionService.CONF_PROJECT_DISABLE_SCHEDULE}=${disableSchedule}
"""

        setupFormTokens(params)
        when:
        request.method = "POST"
        controller.saveProjectConfig()

        then:
        response.status==302
        request.errors == null
        1 * controller.frameworkService.authResourceForProject(_)
        1 * controller.frameworkService.getAuthContextForSubject(_)
        1 * controller.frameworkService.authorizeApplicationResourceAny(null,null,['configure','admin']) >> true
        1 * controller.frameworkService.listDescriptions() >> [null,null,null]
        2 * controller.frameworkService.validateServiceConfig(*_) >> [valid:true]
        1 * controller.frameworkService.setFrameworkProjectConfig(project,{
            it[ScheduledExecutionService.CONF_PROJECT_DISABLE_EXECUTION] == disableExecution
            it['project.disable.schedule'] == disableSchedule
        }) >> [success:true]

        1 * controller.scheduledExecutionService.isProjectExecutionEnabled(project) >> !currentExecutionDisabled
        1 * controller.scheduledExecutionService.isProjectScheduledEnabled(project) >> !currentScheduleDisabled

        (shouldReSchedule?1:0) * controller.scheduledExecutionService.rescheduleJobs(null,project)

        (shouldUnSchedule?1:0) * controller.scheduledExecutionService.unscheduleJobsForProject(_,_)

          0 * controller.scheduledExecutionService._(*_)

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

    @Unroll
    def "save project config update label refreshes session data"() {
        setup:
            controller.featureService = Mock(FeatureService)
            controller.frameworkService = Mock(FrameworkService)
            controller.resourcesPasswordFieldsService = Mock(PasswordFieldsService)
            controller.fcopyPasswordFieldsService = Mock(PasswordFieldsService)
            controller.execPasswordFieldsService = Mock(PasswordFieldsService)
            controller.pluginsPasswordFieldsService = Mock(PasswordFieldsService)
            controller.userService = Mock(UserService)
            def project = "TestSaveProject"
            controller.scheduledExecutionService = Mock(ScheduledExecutionService)

            params.project = project
            params.projectConfig = """
project.label=A Label
"""

            setupFormTokens(params)
        when:
            request.method = "POST"
            controller.saveProjectConfig()

        then:
            response.status == 302
            request.errors == null
            1 * controller.frameworkService.authResourceForProject(_)
            1 * controller.frameworkService.getAuthContextForSubject(_)
            1 * controller.frameworkService.getRundeckFramework()
            1 * controller.frameworkService.authorizeApplicationResourceAny(null, null, ['configure', 'admin']) >> true
            1 * controller.frameworkService.listDescriptions() >> [null, null, null]
            2 * controller.frameworkService.validateServiceConfig(*_) >> [valid: true]

            1 * controller.scheduledExecutionService.isProjectExecutionEnabled(project) >> true
            1 * controller.scheduledExecutionService.isProjectScheduledEnabled(project) >> true

            1 * controller.frameworkService.setFrameworkProjectConfig(
                project, {
                it['project.label'] == 'A Label'
            }
            ) >> [success: true]
            1 * controller.frameworkService.loadSessionProjectLabel(_, project, 'A Label')

            0 * controller.scheduledExecutionService._(*_)
    }


    def "create project invalid name"(){
        setup:
        controller.metricService = Mock(MetricService)
        controller.featureService = Mock(FeatureService)
        def rdframework=Mock(Framework){
        }
        controller.frameworkService=Mock(FrameworkService){
            getRundeckFramework() >> rdframework
            1 * getAuthContextForSubject(_) >> null
            1 * authorizeApplicationResourceTypeAll(null,'project',[ACTION_CREATE])>>true
            1 * validateProjectConfigurableInput(_,_,_)>>[props:[:]]
            listDescriptions()>>[Mock(ResourceModelSourceService),Mock(NodeExecutorService),Mock(FileCopierService)]
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

    def "create project ok with exec cleaner"() {
        setup:
            controller.featureService = Mock(FeatureService) {
                featurePresent(Features.CLEAN_EXECUTIONS_HISTORY, _) >> true
            }
            setupNewProjectWithDescriptionOkTest()

            def description = 'something'
            params.newproject = "TestSaveProject"
            params.description = description

            setupFormTokens(params)
            params.cleanerHistory = 'on'
            params.cleanperiod = '1'
            params.minimumtokeep = '2'
            params.maximumdeletionsize = '3'
            params.crontabString = 'crontab1'
        when:
            request.method = "POST"
            controller.createProjectPost()

        then:
            1 * controller.frameworkService.scheduleCleanerExecutions('TestSaveProject',{
                it.enabled && it.maxDaysToKeep==1 && it.cronExpression=='crontab1' && it.minimumExecutionToKeep==2 && it.maximumDeletionSize==3
            })
            response.status == 302
            request.errors == null
            response.redirectedUrl == "/project/projName/nodes/sources"
    }

    def "create project description empty"(){
        setup:
        controller.featureService = Mock(FeatureService)
        setupNewProjectWithDescriptionOkTest()

        def description = ''
        params.newproject = "TestSaveProject"
        params.description=description

        setupFormTokens(params)
        when:
        request.method = "POST"
        controller.createProjectPost()

        then:
        response.status==302
        request.errors == null
        response.redirectedUrl == "/project/projName/nodes/sources"
    }

    def "create project description name with invalid characters"(){
        setup:
        controller.featureService = Mock(FeatureService)
        controller.metricService = Mock(MetricService)
        def rdframework=Mock(Framework){
        }
        controller.frameworkService=Mock(FrameworkService){
            getRundeckFramework() >> rdframework
            1 * getAuthContextForSubject(_) >> null
            1 * authorizeApplicationResourceTypeAll(null,'project',[ACTION_CREATE])>>true
            1 * validateProjectConfigurableInput(_,_,_)>>[props:[:]]
            listDescriptions()>>[Mock(ResourceModelSourceService),Mock(NodeExecutorService),Mock(FileCopierService)]
        }


        def description = '<b>Project Desc</b>'
        params.newproject = "TestSaveProject"
        params.description=description

        setupFormTokens(params)
        when:
        request.method = "POST"
        controller.createProjectPost()

        then:
        response.status==200
        request.errors == ['project.description.can.only.contain.these.characters']
        model.projectDescription == description

    }

    def "create project description name starting with space"(){
        setup:
        controller.featureService = Mock(FeatureService)
        setupNewProjectWithDescriptionOkTest()

        def description = ' Project Desc'
        params.newproject = "TestSaveProject"
        params.description=description

        setupFormTokens(params)
        when:
        request.method = "POST"
        controller.createProjectPost()

        then:
        response.status==302
        request.errors == null
        response.redirectedUrl == "/project/projName/nodes/sources"
    }

    def "create project description name starting with numbers"(){
        setup:
        controller.featureService = Mock(FeatureService)
        setupNewProjectWithDescriptionOkTest()

        def description = '1 Project Desc'
        params.newproject = "TestSaveProject"
        params.description=description

        setupFormTokens(params)
        when:
        request.method = "POST"
        controller.createProjectPost()

        then:
        response.status==302
        request.errors == null
        response.redirectedUrl == "/project/projName/nodes/sources"

    }

    def "create project description name starting with parenthesis"(){
        setup:
        controller.featureService = Mock(FeatureService)
        setupNewProjectWithDescriptionOkTest()


        def description = '(1) Project Desc'
        params.newproject = "TestSaveProject"
        params.description=description

        setupFormTokens(params)
        when:
        request.method = "POST"
        controller.createProjectPost()

        then:
        response.status==302
        request.errors == null
        response.redirectedUrl == "/project/projName/nodes/sources"

    }

    def "create project description that includes unicode characters"(){
        setup:
        controller.featureService = Mock(FeatureService)
        setupNewProjectWithDescriptionOkTest()


        def description = 'Project Desc À'
        params.newproject = "TestSaveProject"
        params.description=description

        setupFormTokens(params)
        when:
        request.method = "POST"
        controller.createProjectPost()

        then:
        response.status==302
        request.errors == null
        response.redirectedUrl == "/project/projName/nodes/sources"

    }

    def "node summary ajax lists filters"() {
        given:
        def project = 'testProj'
        controller.userService = Mock(UserService)
        controller.frameworkService = Mock(FrameworkService)
        def testUser = new User(login: 'auser').save()
        [
            new NodeFilter(user: testUser, filter: 'abc', name: 'filter1', project: project),
            new NodeFilter(user: testUser, filter: 'tags:xyz', name: 'filter2', project: project),
            new NodeFilter(user: testUser, filter: 'tags:basdf', name: 'filter3', project: 'otherProject'),

        ]*.save(flush: true)
        when:
        controller.nodeSummaryAjax(project)
        then:
        1 * controller.frameworkService.authorizeProjectResourceAll(*_) >> true
        1 * controller.userService.findOrCreateUser(_) >> testUser
        1 * controller.frameworkService.getFrameworkProject(project) >> Mock(IRundeckProject) {
            getNodeSet() >> new NodeSetImpl()
        }
        1 * controller.frameworkService.summarizeTags(_) >> [asdf: 1]

        response.json.filters == [
            [filter: 'tags:xyz', name: 'filter2', project: project],
            [filter: 'abc', name: 'filter1', project: project],
        ]
    }


    def "node page with filter param doesn't redirect"() {
        given:
            def project = 'testProj'
            controller.userService = Mock(UserService)
            controller.frameworkService = Mock(FrameworkService)
            def testUser = new User(login: 'auser').save()
            [
                    new NodeFilter(user: testUser, filter: 'abc', name: 'filter1', project: project),
                    new NodeFilter(user: testUser, filter: 'tags:xyz', name: 'filter2', project: project),
                    new NodeFilter(user: testUser, filter: 'tags:basdf', name: 'filter3', project: 'otherProject'),

            ]*.save(flush: true)
            def query = new ExtNodeFilters(project: project, filter: 'tags:abc')
        when:
            params.project = project
            def result = controller.nodes(query)
        then:
            1 * controller.userService.findOrCreateUser(_) >> testUser
            1 * controller.userService.getFilterPref(_) >> [nodes: 'filter1']
            response.status == 200
            result.filter == 'tags:abc'
    }

    def "save project node sources"() {
        given:
        controller.frameworkService = Mock(FrameworkService)
            controller.featureService = Mock(FeatureService)

        setupFormTokens(params)
        def project = 'testProj'

        when:
        request.method = "POST"
        params.project = project
        def result = controller.saveProjectNodeSources()
        then:
        1 * controller.frameworkService.getAuthContextForSubject(*_)
        1 * controller.frameworkService.authResourceForProject(project)
        1 * controller.frameworkService.getRundeckFramework()
//        1 * controller.frameworkService.listResourceModelSourceDescriptions()
        1 * controller.frameworkService.authorizeApplicationResourceAny(*_) >> true
        1 * controller.frameworkService.validateProjectConfigurableInput(*_) >> [props: [:], remove: []]
        1 * controller.frameworkService.updateFrameworkProjectConfig(project, _, _) >> [success: true]
        0 * controller.frameworkService._(*_)
//        1 * controller.resourcesPasswordFieldsService.reset()

        response.redirectedUrl == "/project/$project/nodes/sources"
        flash.message == "Project ${project} Node Sources saved"
        result == null
    }


    def setupNewProjectWithDescriptionOkTest(){
        controller.metricService = Mock(MetricService)
        def project = Mock(Project){
            getName()>>"projName"
        }
        def projectManager = Mock(ProjectManager){
            existsFrameworkProject( )>>false
        }
        def rdframework=Mock(Framework){
            1 * getFrameworkProjectMgr()>>projectManager
        }
        controller.frameworkService=Mock(FrameworkService){
            getRundeckFramework() >> rdframework
            1 * getAuthContextForSubject(_) >> null
            1 * authorizeApplicationResourceTypeAll(null,'project',[ACTION_CREATE])>>true
            1 * validateProjectConfigurableInput(_,_,_)>>[props:[:]]
            1 * createFrameworkProject(_,_)>>[project, null]
            1 * refreshSessionProjects(_,_)>>null
            listDescriptions()>>[Mock(ResourceModelSourceService),Mock(NodeExecutorService),Mock(FileCopierService)]
        }
    }

    def "projectPluginsAjax"() {
        given:
            def project = "aProject"
            def serviceName = "SomeService"
            def configPrefix = "xyz"
            controller.frameworkService = Mock(FrameworkService)
            controller.pluginService = Mock(PluginService)
            controller.obscurePasswordFieldsService = Mock(PasswordFieldsService)
        when:
            controller.projectPluginsAjax(project, serviceName, configPrefix)
        then:
            1 * controller.frameworkService.authorizeApplicationResourceAll(_, _, ['configure', 'admin']) >> true
            1 * controller.frameworkService.getAuthContextForSubject(_)
            1 * controller.frameworkService.authResourceForProject(project)
            1 * controller.frameworkService.getRundeckFramework() >> Mock(IFramework) {
                getFrameworkProjectMgr() >> Mock(ProjectManager) {
                    loadProjectConfig(project) >> Mock(IRundeckProjectConfig) {
                        getProjectProperties() >> [
                                'xyz.1.type'          : 'atype1',
                                'xyz.1.config.blah'   : 'blahval',
                                'xyz.1.mongo'         : 'extramongo',
                                'xyz.2.type'          : 'atype2',
                                'xyz.2.config.zeeblah': 'zblahval',
                                'xyz.2.blango'        : 'extrablango',
                        ]
                    }
                }
            }


            response.status == 200
            response.json == [
                    project: project,
                    plugins: [
                            [
                                    type   : 'atype1',
                                    service: serviceName,
                                    config : [blah: 'blahval'],
                                    extra  : [mongo: 'extramongo']
                            ],
                            [
                                    type   : 'atype2',
                                    service: serviceName,
                                    config : [zeeblah: 'zblahval'],
                                    extra  : [blango: 'extrablango']
                            ]
                    ]
            ]
    }

    def "save project plugins ajax ok"() {
        given:

            grailsApplication.config.clear()
            grailsApplication.config.rundeck.security.useHMacRequestTokens = 'false'
            def utilTagLib = mockTagLib(UtilityTagLib)
            def project = "aProject"
            def serviceName = "SomeService"
            def configPrefix = "xyz"
            controller.frameworkService = Mock(FrameworkService)
            controller.pluginService = Mock(PluginService)
            controller.obscurePasswordFieldsService = Mock(PasswordFieldsService)
            controller.featureService = Mock(FeatureService)
            def expectData = [
                    plugins: [
                            [type   : '1type',
                             config : [bongo: 'asdf'],
                             extra  : [:],
                             service: 'SomeService'
                            ],

                            [type   : '2type',
                             config : [zingo: 'azsdf'],
                             extra  : [asdf: 'jfkdjkf', zjiji: 'dkdkd'],
                             service: 'SomeService'
                            ]
                    ]
            ]
            def inputData = [
                    plugins: [
                            [type     : '1type',
                             config   : [bongo: 'asdf'],
                             origIndex: 1
                            ],

                            [type     : '2type',
                             config   : [zingo: 'azsdf'],
                             extra    : [asdf: 'jfkdjkf', zjiji: 'dkdkd'],
                             origIndex: 2
                            ]
                    ]
            ]
            request.json = inputData
            request.method = 'POST'

            setupFormTokens(params)
        when:
            controller.saveProjectPluginsAjax(project, serviceName, configPrefix)

        then:
            response.status == 200
            1 * controller.frameworkService.authorizeApplicationResourceAll(_, _, ['configure', 'admin']) >> true
            1 * controller.frameworkService.getAuthContextForSubject(_)
            1 * controller.frameworkService.authResourceForProject(project)
            1 * controller.pluginService.getPluginDescriptor('1type', serviceName) >>
            new DescribedPlugin(null, null, '1type')
            1 * controller.pluginService.validatePluginConfig(serviceName, '1type', [bongo: 'asdf']) >>
            new ValidatedPlugin(valid: true)
            1 * controller.pluginService.getPluginDescriptor('2type', serviceName) >>
            new DescribedPlugin(null, null, '2type')
            1 * controller.pluginService.validatePluginConfig(serviceName, '2type', [zingo: 'azsdf']) >>
            new ValidatedPlugin(valid: true)

            1 * controller.obscurePasswordFieldsService.untrack('aProject/SomeService/xyz', {
                it.size()==2 &&
                        it[0].type=='1type' &&
                        it[0].index==1 &&
                        (it[0].props==[bongo:'asdf']) &&
                        it[1].type=='2type' &&
                        it[1].index==2 &&
                        it[1].props==[zingo:'azsdf']
            }, _)
            1 * controller.obscurePasswordFieldsService.resetTrack('aProject/SomeService/xyz', _, _)
            1 * controller.frameworkService.updateFrameworkProjectConfig(project, _, ['xyz.'].toSet()) >>
            [success: true]

            response.json == ([project: project] + expectData)
    }

    def "save project plugins ajax error  type name"() {
        given:

            grailsApplication.config.clear()
            grailsApplication.config.rundeck.security.useHMacRequestTokens = 'false'
            def utilTagLib = mockTagLib(UtilityTagLib)
            def project = "aProject"
            def serviceName = "SomeService"
            def configPrefix = "xyz"
            controller.frameworkService = Mock(FrameworkService)
            controller.pluginService = Mock(PluginService)
            controller.obscurePasswordFieldsService = Mock(PasswordFieldsService)
            def inputData = [
                    plugins: [
                            [
                                    type  : type,
                                    config: [bongo: 'asdf']
                            ],

                            [type  : '2type',
                             config: [zingo: 'azsdf'],
                             extra : [asdf: 'jfkdjkf', zjiji: 'dkdkd']
                            ]
                    ]
            ]
            request.json = inputData
            request.method = 'POST'

            setupFormTokens(params)
        when:
            controller.saveProjectPluginsAjax(project, serviceName, configPrefix)

        then:
            response.status == 422
            1 * controller.frameworkService.authorizeApplicationResourceAll(_, _, ['configure', 'admin']) >> true
            1 * controller.frameworkService.getAuthContextForSubject(_)
            1 * controller.frameworkService.authResourceForProject(project)

            0 * controller.pluginService.getPluginDescriptor('1type', serviceName) >>
            new DescribedPlugin(null, null, '1type')
            0 * controller.pluginService.validatePluginConfig(serviceName, '1type', [bongo: 'asdf'])

            1 * controller.pluginService.getPluginDescriptor('2type', serviceName) >>
            new DescribedPlugin(null, null, '2type')
            1 * controller.pluginService.validatePluginConfig(serviceName, '2type', [zingo: 'azsdf']) >>
            new ValidatedPlugin(valid: true)

            0 * controller.frameworkService.updateFrameworkProjectConfig(project, _, ['xyz.'].toSet()) >>
            [success: true]

            response.json == [
                    reports: [:],
                    errors : [
                            msg
                    ]
            ]

        where:
            type        | msg
            null        | '[0]: missing type'
            'asdf asdf' | '[0]: Invalid provider type name'
    }

    def "save project plugins ajax error missing plugin"() {
        given:

            grailsApplication.config.clear()
            grailsApplication.config.rundeck.security.useHMacRequestTokens = 'false'
            def utilTagLib = mockTagLib(UtilityTagLib)
            def project = "aProject"
            def serviceName = "SomeService"
            def configPrefix = "xyz"
            controller.frameworkService = Mock(FrameworkService)
            controller.pluginService = Mock(PluginService)
            controller.obscurePasswordFieldsService = Mock(PasswordFieldsService)
            def inputData = [
                    plugins: [
                            [
                                    type  : '1type',
                                    config: [bongo: 'asdf']
                            ],

                            [type  : '2type',
                             config: [zingo: 'azsdf'],
                             extra : [asdf: 'jfkdjkf', zjiji: 'dkdkd']
                            ]
                    ]
            ]
            request.json = inputData
            request.method = 'POST'

            setupFormTokens(params)
        when:
            controller.saveProjectPluginsAjax(project, serviceName, configPrefix)

        then:
            response.status == 422
            1 * controller.frameworkService.authorizeApplicationResourceAll(_, _, ['configure', 'admin']) >> true
            1 * controller.frameworkService.getAuthContextForSubject(_)
            1 * controller.frameworkService.authResourceForProject(project)

            1 * controller.pluginService.getPluginDescriptor('1type', serviceName) >> null
            0 * controller.pluginService.validatePluginConfig(serviceName, '1type', [bongo: 'asdf'])

            1 * controller.pluginService.getPluginDescriptor('2type', serviceName) >>
            new DescribedPlugin(null, null, '2type')
            1 * controller.pluginService.validatePluginConfig(serviceName, '2type', [zingo: 'azsdf']) >>
            new ValidatedPlugin(valid: true)

            0 * controller.frameworkService.updateFrameworkProjectConfig(project, _, ['xyz.'].toSet()) >>
            [success: true]

            response.json == [
                    reports: [:],
                    errors : [
                            msg
                    ]
            ]

        where:
            msg = '[0]: SomeService provider was not found: 1type'
    }

    def "save project plugins ajax error plugin validation"() {
        given:

            grailsApplication.config.clear()
            grailsApplication.config.rundeck.security.useHMacRequestTokens = 'false'
            def utilTagLib = mockTagLib(UtilityTagLib)
            def project = "aProject"
            def serviceName = "SomeService"
            def configPrefix = "xyz"
            controller.frameworkService = Mock(FrameworkService)
            controller.pluginService = Mock(PluginService)
            controller.obscurePasswordFieldsService = Mock(PasswordFieldsService)
            def inputData = [
                    plugins: [
                            [
                                    type  : '1type',
                                    config: [bongo: 'asdf']
                            ],

                            [type  : '2type',
                             config: [zingo: 'azsdf'],
                             extra : [asdf: 'jfkdjkf', zjiji: 'dkdkd']
                            ]
                    ]
            ]
            request.json = inputData
            request.method = 'POST'
            def report = new Validator.Report()
            report.errors['bongo'] = 'Invalid value'


            setupFormTokens(params)
        when:
            controller.saveProjectPluginsAjax(project, serviceName, configPrefix)

        then:
            response.status == 422
            1 * controller.frameworkService.authorizeApplicationResourceAll(_, _, ['configure', 'admin']) >> true
            1 * controller.frameworkService.getAuthContextForSubject(_)
            1 * controller.frameworkService.authResourceForProject(project)

            1 * controller.pluginService.getPluginDescriptor('1type', serviceName) >>
            new DescribedPlugin(null, null, '1type')
            1 * controller.pluginService.validatePluginConfig(serviceName, '1type', [bongo: 'asdf']) >>
            new ValidatedPlugin(valid: false, report: report)

            1 * controller.pluginService.getPluginDescriptor('2type', serviceName) >>
            new DescribedPlugin(null, null, '2type')
            1 * controller.pluginService.validatePluginConfig(serviceName, '2type', [zingo: 'azsdf']) >>
            new ValidatedPlugin(valid: true)

            0 * controller.frameworkService.updateFrameworkProjectConfig(project, _, ['xyz.'].toSet()) >>
            [success: true]

            response.json == [
                    reports: ['0': [bongo: 'Invalid value']],
                    errors : [
                            msg
                    ]
            ]

        where:
            msg = '[0]: configuration was invalid: Property validation FAILED. errors={bongo=Invalid value}'
    }

    def "save project plugins ajax error saving config"() {
        given:

            grailsApplication.config.clear()
            grailsApplication.config.rundeck.security.useHMacRequestTokens = 'false'
            def utilTagLib = mockTagLib(UtilityTagLib)
            def project = "aProject"
            def serviceName = "SomeService"
            def configPrefix = "xyz"
            controller.frameworkService = Mock(FrameworkService)
            controller.pluginService = Mock(PluginService)
            controller.obscurePasswordFieldsService = Mock(PasswordFieldsService)
            def inputData = [
                    plugins: [
                            [
                                    type  : '1type',
                                    config: [bongo: 'asdf']
                            ],

                            [type  : '2type',
                             config: [zingo: 'azsdf'],
                             extra : [asdf: 'jfkdjkf', zjiji: 'dkdkd']
                            ]
                    ]
            ]
            request.json = inputData
            request.method = 'POST'
            def report = new Validator.Report()
            report.errors['bongo'] = 'Invalid value'


            setupFormTokens(params)
        when:
            controller.saveProjectPluginsAjax(project, serviceName, configPrefix)

        then:
            response.status == 422
            1 * controller.frameworkService.authorizeApplicationResourceAll(_, _, ['configure', 'admin']) >> true
            1 * controller.frameworkService.getAuthContextForSubject(_)
            1 * controller.frameworkService.authResourceForProject(project)

            1 * controller.pluginService.getPluginDescriptor('1type', serviceName) >>
            new DescribedPlugin(null, null, '1type')
            1 * controller.pluginService.validatePluginConfig(serviceName, '1type', [bongo: 'asdf']) >>
            new ValidatedPlugin(valid: true)

            1 * controller.pluginService.getPluginDescriptor('2type', serviceName) >>
            new DescribedPlugin(null, null, '2type')
            1 * controller.pluginService.validatePluginConfig(serviceName, '2type', [zingo: 'azsdf']) >>
            new ValidatedPlugin(valid: true)

            1 * controller.frameworkService.updateFrameworkProjectConfig(project, _, ['xyz.'].toSet()) >>
            [success: false, error: 'Project does not exist']

            response.json == [
                    reports: [:],
                    errors : [
                            'Project does not exist'
                    ]
            ]

    }
    def "save node source file, catch IOException"() {

        setup:
            def source = Mock(WriteableModelSource) {
                1 * writeData(_) >> {
                    throw new IOException("expected error")
                }
                2 * getSyntaxMimeType() >> 'application/json'
                1 * getSourceDescription()>>'x'
                0 * _(*_)
            }
            controller.frameworkService = Mock(FrameworkService) {
                1 * getRundeckFramework()>>Mock(IFramework){
                    1 * getResourceModelSourceService()
                }
                1 * getFrameworkProject('test') >> Mock(IRundeckProject) {
                    1 * getProjectNodes() >> Mock(IProjectNodes) {
                        1 * getWriteableResourceModelSources() >> [
                            Mock(IProjectNodes.WriteableProjectNodes) {
                                getWriteableSource() >> source
                                getIndex() >> 1
                                getType() >> 'monkey'
                            }
                        ]
                    }
                }
                1 * getAuthContextForSubject(_)
                1 * authResourceForProject('test')
                1 * authorizeApplicationResourceAny(_, _, ['configure', 'admin']) >> true
                0 * _(*_)
            }
            controller.pluginService=Mock(PluginService){
                1 * getPluginDescriptor('monkey',_)
            }

            params.project = "test"
            params.index = "1"
            request.method = 'POST'
            params.fileText = 'some text'
        when:

            setupFormTokens(params)
            def result = controller.saveProjectNodeSourceFile()

        then:
            view=='/framework/editProjectNodeSourceFile'
            model.saveError=='expected error'
    }
}
