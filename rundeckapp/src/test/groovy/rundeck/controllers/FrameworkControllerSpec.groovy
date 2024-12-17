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

import rundeck.support.filters.ExtNodeFilters
import com.dtolabs.rundeck.core.authorization.RuleSetValidation
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
import grails.testing.gorm.DataTest
import grails.testing.web.controllers.ControllerUnitTest
import org.grails.plugins.metricsweb.MetricService
import org.grails.web.servlet.mvc.SynchronizerTokensHolder
import org.rundeck.app.acl.AppACLContext
import org.rundeck.app.acl.ContextACLManager
import org.rundeck.app.authorization.AppAuthContextProcessor
import org.rundeck.core.auth.AuthConstants
import org.rundeck.core.projects.ProjectConfigurable
import org.rundeck.storage.api.StorageException
import rundeck.Project
import rundeck.User
import rundeck.UtilityTagLib
import rundeck.services.*
import rundeck.services.feature.FeatureService
import spock.lang.Specification
import spock.lang.Unroll

import static org.rundeck.core.auth.AuthConstants.*

/**
 * Created by greg on 7/28/15.
 */
class FrameworkControllerSpec extends Specification implements ControllerUnitTest<FrameworkController>, DataTest {

    def setupSpec() { mockDomains User }

    def setup() {
        grailsApplication.config.clear()
        grailsApplication.config.rundeck.security.useHMacRequestTokens = 'false'
        controller.featureService = Mock(FeatureService)

        defineBeans {
            configurationService(ConfigurationService) {
                grailsApplication = grailsApplication
            }
        }
    }

    def "system acls require api_version 14"(){
        setup:
        controller.apiService=Mock(ApiService){
            1 * requireApi(_,_) >> {args->
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
            1 * requireApi(_,_) >> true
            1 * renderErrorFormat(_,[status:403,code:'api.error.item.unauthorized',args:[action,'Rundeck System ACLs','']]) >> {args->
                args[0].status=args[1].status
            }
        }
        controller.frameworkService=Mock(FrameworkService){

        }

            controller.rundeckAuthContextProcessor=Mock(AppAuthContextProcessor) {
                1 * getAuthContextForSubject(_) >> null
                1 * authorizeApplicationResourceAny(null,RESOURCE_TYPE_SYSTEM_ACL,[action, AuthConstants.ACTION_ADMIN, AuthConstants.ACTION_APP_ADMIN])>> false
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
            1 * requireApi(_,_) >> true

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
         }

            controller.rundeckAuthContextProcessor=Mock(AppAuthContextProcessor) {
                1 * getAuthContextForSubject(_) >> null
                1 * authorizeApplicationResourceAny(null,RESOURCE_TYPE_SYSTEM_ACL,[ACTION_READ,AuthConstants.ACTION_ADMIN, AuthConstants.ACTION_APP_ADMIN])>>true
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
        controller.aclFileManagerService=Mock(ContextACLManager){
            existsPolicyFile(AppACLContext.system(),_) >> false
        }
        controller.frameworkService=Mock(FrameworkService){
        }
            controller.rundeckAuthContextProcessor=Mock(AppAuthContextProcessor) {
                1 * getAuthContextForSubject(_) >> null
                1 * authorizeApplicationResourceAny(_,RESOURCE_TYPE_SYSTEM_ACL,[ACTION_READ,AuthConstants.ACTION_ADMIN, AuthConstants.ACTION_APP_ADMIN])>>true
            }
        controller.apiService=Mock(ApiService){
            1 * requireApi(_,_) >> true
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
        controller.aclFileManagerService=Mock(ContextACLManager){
            1 * existsPolicyFile(AppACLContext.system(), _) >> true
            1 * loadPolicyFileContents(AppACLContext.system(), 'blah.aclpolicy',_) >> {args->
                args[2].write('blah'.bytes)
                4
            }
        }
        controller.frameworkService=Mock(FrameworkService){
        }

            controller.rundeckAuthContextProcessor=Mock(AppAuthContextProcessor) {
                1 * getAuthContextForSubject(_) >> null
                1 * authorizeApplicationResourceAny(_,RESOURCE_TYPE_SYSTEM_ACL,[ACTION_READ,AuthConstants.ACTION_ADMIN, AuthConstants.ACTION_APP_ADMIN]) >> true
            }
        controller.apiService=Mock(ApiService){
            1 * requireApi(_,_) >> true
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
    def "system acls GET text/yaml"(String respFormat, String contentType){
        setup:
        controller.aclFileManagerService=Mock(ContextACLManager){
            1 * existsPolicyFile(AppACLContext.system(), _) >> true
            1 * loadPolicyFileContents(AppACLContext.system(), 'blah.aclpolicy',_) >> {args->
                args[2].write('blah'.bytes)
                4
            }
        }
        controller.frameworkService=Mock(FrameworkService)
        controller.rundeckAuthContextProcessor=Mock(AppAuthContextProcessor){
            1 * authorizeApplicationResourceAny(_,RESOURCE_TYPE_SYSTEM_ACL,[ACTION_READ,AuthConstants.ACTION_ADMIN, AuthConstants.ACTION_APP_ADMIN]) >> true
            1 * getAuthContextForSubject(_) >> null
        }
        controller.apiService=Mock(ApiService){
            1 * requireApi(_,_) >> true
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
        controller.aclFileManagerService=Mock(ContextACLManager){
            1 * listStoredPolicyFiles(AppACLContext.system()) >> ['blah.aclpolicy']
            0*_(*_)
        }
        controller.frameworkService=Mock(FrameworkService){
            0*_(*_)
        }

            controller.rundeckAuthContextProcessor=Mock(AppAuthContextProcessor) {
                1 * getAuthContextForSubject(_) >> null
                1 * authorizeApplicationResourceAny(_,RESOURCE_TYPE_SYSTEM_ACL,[ACTION_READ,AuthConstants.ACTION_ADMIN, AuthConstants.ACTION_APP_ADMIN]) >> true
            }
        controller.apiService=Mock(ApiService){
            1 * requireApi(_,_) >> true
            1 * extractResponseFormat(_,_,_,_) >> 'json'
            1 * jsonRenderDirlist('',_,_,['blah.aclpolicy'])>>{args-> [success: true] }
            0*_(*_)
        }
        when:
        params.path=''
        params.project="test"
        controller.response.format = "json"

        def result=controller.apiSystemAcls()

        then:
        response.status==200
        response.contentType.split(';').contains('application/json')
        response.json==[success:true]

    }
    def "system acls POST text"(){
        setup:
        controller.aclFileManagerService=Mock(ContextACLManager){
            1 * existsPolicyFile(AppACLContext.system(), 'test.aclpolicy') >> false
            1 * storePolicyFileContents(AppACLContext.system(), 'test.aclpolicy',_) >> 4
            1 * loadPolicyFileContents(AppACLContext.system(), 'test.aclpolicy',_) >> {args->
                args[2].write('blah'.bytes)
                4
            }
            1 * validateYamlPolicy(AppACLContext.system(), 'test.aclpolicy', _) >> Stub(RuleSetValidation) {
                isValid() >> true
            }
        }
        controller.frameworkService=Mock(FrameworkService)
        controller.rundeckAuthContextProcessor=Mock(AppAuthContextProcessor) {
            1 * getAuthContextForSubject(_) >> null

            1 * authorizeApplicationResourceAny(_,RESOURCE_TYPE_SYSTEM_ACL,[ACTION_CREATE,AuthConstants.ACTION_ADMIN, AuthConstants.ACTION_APP_ADMIN]) >> true
        }
        controller.apiService=Mock(ApiService){
            1 * requireApi(_,_) >> true
            1 * extractResponseFormat(_,_,_,_) >> 'json'
        }
        when:
        params.path='test.aclpolicy'
        params.project="test"
        response.format='json'
        request.format='yaml'
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
        controller.aclFileManagerService=Mock(ContextACLManager){
            1 * existsPolicyFile(AppACLContext.system(), 'test.aclpolicy') >> true
            0 * getValidator()
        }
        controller.frameworkService=Mock(FrameworkService)
        controller.rundeckAuthContextProcessor=Mock(AppAuthContextProcessor) {
            1 * getAuthContextForSubject(_) >> null

            1 * authorizeApplicationResourceAny(_,RESOURCE_TYPE_SYSTEM_ACL,[ACTION_CREATE,AuthConstants.ACTION_ADMIN, AuthConstants.ACTION_APP_ADMIN]) >> true
        }
        controller.apiService=Mock(ApiService){
            1 * requireApi(_,_) >> true
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
        controller.aclFileManagerService=Mock(ContextACLManager){
            1 * existsPolicyFile(AppACLContext.system(), 'test.aclpolicy') >> false

            0 * getValidator()
        }
            controller.frameworkService=Mock(FrameworkService)
            controller.rundeckAuthContextProcessor=Mock(AppAuthContextProcessor) {
                1 * getAuthContextForSubject(_) >> null
                1 * authorizeApplicationResourceAny(_,RESOURCE_TYPE_SYSTEM_ACL,[ACTION_UPDATE,AuthConstants.ACTION_ADMIN, AuthConstants.ACTION_APP_ADMIN]) >> true
            }
        controller.apiService=Mock(ApiService){
            1 * requireApi(_,_) >> true
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
        controller.aclFileManagerService=Mock(ContextACLManager){
            1 * existsPolicyFile(AppACLContext.system(), 'test.aclpolicy') >> true

            1 * storePolicyFileContents(AppACLContext.system(), 'test.aclpolicy',_) >> 4
            1 * loadPolicyFileContents(AppACLContext.system(), 'test.aclpolicy',_) >> {args->
                args[2].write('blah'.bytes)
                4
            }
            1 * validateYamlPolicy(AppACLContext.system(), 'test.aclpolicy', _) >> Stub(RuleSetValidation) {
                isValid() >> true
            }
        }
            controller.frameworkService=Mock(FrameworkService)
            controller.rundeckAuthContextProcessor=Mock(AppAuthContextProcessor) {
                1 * getAuthContextForSubject(_) >> null
                1 * authorizeApplicationResourceAny(_,RESOURCE_TYPE_SYSTEM_ACL,[ACTION_UPDATE,AuthConstants.ACTION_ADMIN, AuthConstants.ACTION_APP_ADMIN]) >> true
            }
        controller.apiService=Mock(ApiService){
            1 * requireApi(_,_) >> true
            1 * extractResponseFormat(_,_,_,_) >> 'json'
        }
        when:
        params.path='test.aclpolicy'
        params.project="test"
        response.format='json'
        request.format='yaml'
        request.method='PUT'
        request.contentType='application/yaml'
        request.content=('{ description: \'\', \n' +
                'context: { project: \'test\' }, \n' +
                'by: { username: \'test\' }, \n' +
                'for: { resource: [ { allow: \'x\' } ] } }').bytes
        controller.response.format = "json"
        def result=controller.apiSystemAcls()

        then:
        response.status==200
        response.contentType!=null
        response.contentType.split(';').contains('application/json')
        response.json==[contents:'blah']
    }
    def "system acls DELETE not found"(){
        setup:
        controller.aclFileManagerService=Mock(ContextACLManager){
            1 * existsPolicyFile(AppACLContext.system(), 'test.aclpolicy') >> false
        }
            controller.frameworkService=Mock(FrameworkService)
            controller.rundeckAuthContextProcessor=Mock(AppAuthContextProcessor) {
                1 * getAuthContextForSubject(_) >> null
                1 * authorizeApplicationResourceAny(_,RESOURCE_TYPE_SYSTEM_ACL,[ACTION_DELETE,AuthConstants.ACTION_ADMIN, AuthConstants.ACTION_APP_ADMIN]) >> true
            }
        controller.apiService=Mock(ApiService){
            1 * requireApi(_,_) >> true
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
        controller.aclFileManagerService=Mock(ContextACLManager){
            1 * existsPolicyFile(AppACLContext.system(), 'test.aclpolicy') >> true
            1 * deletePolicyFile(AppACLContext.system(), 'test.aclpolicy') >> true
        }
            controller.frameworkService=Mock(FrameworkService)
            controller.rundeckAuthContextProcessor=Mock(AppAuthContextProcessor) {
                1 * getAuthContextForSubject(_) >> null
                1 * authorizeApplicationResourceAny(_,RESOURCE_TYPE_SYSTEM_ACL,[ACTION_DELETE,AuthConstants.ACTION_ADMIN, AuthConstants.ACTION_APP_ADMIN]) >> true
            }
        controller.apiService=Mock(ApiService){
            1 * requireApi(_,_) >> true
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
        controller.aclFileManagerService=Mock(ContextACLManager){
            1 * existsPolicyFile(AppACLContext.system(), 'test.aclpolicy') >> false

            1 * validateYamlPolicy(AppACLContext.system(), 'test.aclpolicy', _) >> Stub(RuleSetValidation) {
                isValid() >> false
            }
        }
            controller.frameworkService=Mock(FrameworkService)
            controller.rundeckAuthContextProcessor=Mock(AppAuthContextProcessor) {
                1 * getAuthContextForSubject(_) >> null
                1 * authorizeApplicationResourceAny(_,RESOURCE_TYPE_SYSTEM_ACL,[ACTION_CREATE,AuthConstants.ACTION_ADMIN, AuthConstants.ACTION_APP_ADMIN]) >> true
            }
        controller.apiService=Mock(ApiService){
            1 * requireApi(_,_) >> true
            1 * extractResponseFormat(_,_,_,_) >> 'json'

            1 * renderJsonAclpolicyValidation(_)>>{args->
                [contents:'blahz']
            }
        }
        when:
        params.path='test.aclpolicy'
        params.project="test"
        response.format='json'
        request.format='yaml'
        request.method='POST'
        request.contentType='application/yaml'
        request.content=('{ description: \'\', \n' +
                'context: { project: \'test\' }, \n' +
                'by: { username: \'test\' }, \n' +
                'for: { resource: [ { allow: \'x\' } ] } }').bytes
        controller.response.format = "json"
        def result=controller.apiSystemAcls()

        then:
        response.status==400
        response.contentType.split(';').contains('application/json')
        response.json==[contents:'blahz']
    }

    def "save project with description"(){
        setup:
        def fwkService=Mock(FrameworkService)
        controller.frameworkService = fwkService
        controller.resourcesPasswordFieldsService = Mock(PasswordFieldsService)
        controller.fcopyPasswordFieldsService = Mock(PasswordFieldsService)
        controller.execPasswordFieldsService = Mock(PasswordFieldsService)
        controller.pluginGroupPasswordFieldsService = Mock(PasswordFieldsService)
        controller.userService = Mock(UserService)
        controller.featureService = Mock(FeatureService)
        controller.scheduledExecutionService = Mock(ScheduledExecutionService){
            isProjectExecutionEnabled(_) >> true
        }
            controller.rundeckAuthContextProcessor=Mock(AppAuthContextProcessor)


        params.project = "TestSaveProject"
        params.description='abc'

        setupFormTokens(params)
        when:
        request.method = "POST"
        controller.saveProject()

        then:
        response.status==302
        request.errors == null

        1 * controller.rundeckAuthContextProcessor.getAuthContextForSubject(_)
        1 * controller.rundeckAuthContextProcessor.authorizeProjectConfigure(_,'TestSaveProject') >> true
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
            controller.pluginGroupPasswordFieldsService = Mock(PasswordFieldsService)
            controller.userService = Mock(UserService)
            controller.featureService = Mock(FeatureService)
            controller.scheduledExecutionService = Mock(ScheduledExecutionService) {
                isProjectExecutionEnabled(_) >> true
            }
            controller.rundeckAuthContextProcessor=Mock(AppAuthContextProcessor)


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


            1 * controller.rundeckAuthContextProcessor.getAuthContextForSubject(_)
            1 * controller.rundeckAuthContextProcessor.authorizeProjectConfigure(_,'TestSaveProject') >> true
            1 * fwkService.listDescriptions() >> [null, null, null]
            1 * fwkService.validateProjectConfigurableInput(_, _, { !it.test('resourceModelSource') }) >> [:]
            1 * fwkService.getRundeckFramework()
            1 * fwkService.handleProjectSchedulingEnabledChange(_,_,_,_,_)
            1 * fwkService.refreshSessionProjects(_,_)
            1 * fwkService.loadSessionProjectLabel(_, 'TestSaveProject', 'A Label')
            1 * fwkService.discoverScopedConfiguration(_, 'project.plugin')
            0 * fwkService._(*_)
    }
    def "save project plugin groups"() {
        setup:
            def fwkService = Mock(FrameworkService)
            controller.frameworkService = fwkService
            controller.resourcesPasswordFieldsService = Mock(PasswordFieldsService)
            controller.fcopyPasswordFieldsService = Mock(PasswordFieldsService)
            controller.execPasswordFieldsService = Mock(PasswordFieldsService)
            controller.pluginGroupPasswordFieldsService = Mock(PasswordFieldsService)
            controller.userService = Mock(UserService)
            controller.featureService = Mock(FeatureService){
                (pgEnabled ? 1 : 0) * featurePresent(Features.PLUGIN_GROUPS) >> pgEnabled
                _ * featurePresent(Features.CLEAN_EXECUTIONS_HISTORY,_) >> false
            }
            controller.scheduledExecutionService = Mock(ScheduledExecutionService) {
                isProjectExecutionEnabled(_) >> true
            }
            controller.rundeckAuthContextProcessor=Mock(AppAuthContextProcessor)


            params.project = "TestSaveProject"
            params.label = 'A Label'
            if(json){
                params['pluginValues.PluginGroup.json']=json
            }
            setupFormTokens(params)
        when:
            request.method = "POST"
            controller.saveProject()

        then:
            response.status == 302
            request.errors == null
            1 * fwkService.updateFrameworkProjectConfig(
                _,
                { it.subMap(expected.keySet())==expected && it.getProperty("project.plugin.PluginGroup.aplugin.c") == null},
                { it.containsAll(rmPrefixes) }
            ) >> [success: true]


            1 * controller.rundeckAuthContextProcessor.getAuthContextForSubject(_)
            1 * controller.rundeckAuthContextProcessor.authorizeProjectConfigure(_,'TestSaveProject') >> true
            1 * fwkService.listDescriptions() >> [null, null, null]
            1 * fwkService.validateProjectConfigurableInput(_, _, { !it.test('resourceModelSource') }) >> [:]
            1 * fwkService.getRundeckFramework()
            (pgEnabled?1:0) * fwkService.listPluginGroupDescriptions() >> null
            1 * fwkService.handleProjectSchedulingEnabledChange(_,_,_,_,_)
            1 * fwkService.refreshSessionProjects(_,_)
            1 * fwkService.loadSessionProjectLabel(_, 'TestSaveProject', 'A Label')
            1 * fwkService.discoverScopedConfiguration(_, 'project.plugin')
            0 * fwkService._(*_)
            1 * controller.pluginGroupPasswordFieldsService.reset()
            (expected?1:0) * controller.pluginGroupPasswordFieldsService.untrack([[config: [type: 'aplugin', props: [a:'b', 'c':null]], type: 'aplugin', index: 0]],_)
        where:
            pgEnabled | rmPrefixes                                              | json |expected
            true      | ['project.plugin.PluginGroup.', 'project.PluginGroup.'] | ''   |[:]
            true      | ['project.plugin.PluginGroup.', 'project.PluginGroup.'] | '[]' |[:]
            true      | ['project.plugin.PluginGroup.', 'project.PluginGroup.'] | '[{"type":"aplugin","config":{"a":"b", "c":null}}]'|['project.PluginGroup.aplugin.enabled':'true','project.plugin.PluginGroup.aplugin.a':'b']
    }
    def "save project with out description"(){
        setup:
        def fwkService=Mock(FrameworkService)
        controller.frameworkService = fwkService
        controller.resourcesPasswordFieldsService = Mock(PasswordFieldsService)
        controller.fcopyPasswordFieldsService = Mock(PasswordFieldsService)
        controller.pluginGroupPasswordFieldsService = Mock(PasswordFieldsService)
        controller.execPasswordFieldsService = Mock(PasswordFieldsService)
        controller.userService = Mock(UserService)
        controller.featureService = Mock(FeatureService)
        controller.scheduledExecutionService = Mock(ScheduledExecutionService){
            isProjectExecutionEnabled(_) >> true
        }
            controller.rundeckAuthContextProcessor=Mock(AppAuthContextProcessor)


        params.project = "TestSaveProject"

        setupFormTokens(params)
        when:
        request.method = "POST"
        controller.saveProject()

        then:
        response.status==302
        request.errors == null

        1 * controller.rundeckAuthContextProcessor.getAuthContextForSubject(_)
        1 * controller.rundeckAuthContextProcessor.authorizeProjectConfigure(_,'TestSaveProject') >> true
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
            controller.pluginGroupPasswordFieldsService = Mock(PasswordFieldsService)
            controller.userService = Mock(UserService)
            controller.featureService = Mock(FeatureService){
                featurePresent(Features.CLEAN_EXECUTIONS_HISTORY, _) >> true
            }
            controller.scheduledExecutionService = Mock(ScheduledExecutionService) {
                isProjectExecutionEnabled(_) >> true
            }
            controller.rundeckAuthContextProcessor=Mock(AppAuthContextProcessor)


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

            1 * controller.rundeckAuthContextProcessor.getAuthContextForSubject(_)
            1 * controller.rundeckAuthContextProcessor.authorizeProjectConfigure(_,'TestSaveProject') >> true
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
            controller.pluginGroupPasswordFieldsService = Mock(PasswordFieldsService)
            controller.userService = Mock(UserService)
            controller.featureService = Mock(FeatureService)
            controller.scheduledExecutionService = Mock(ScheduledExecutionService) {
                isProjectExecutionEnabled(_) >> true
            }
            controller.rundeckAuthContextProcessor=Mock(AppAuthContextProcessor)


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

            1 * controller.rundeckAuthContextProcessor.getAuthContextForSubject(_)
            1 * controller.rundeckAuthContextProcessor.authorizeProjectConfigure(_,'TestSaveProject') >> true
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
            1 * requireApi(_, _) >> true
            1 * renderErrorFormat(_,{map->
                map.status==404
            })>>{it[0].status=it[1].status}
        }
        def query = new ExtNodeFilters(project: 'test')
        params.project="test"
        when:

        def result=controller.apiResourcesv2(query)

        then:
        response.status==404
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



            1 * getFrameworkProject(projectName) >> Mock(IRundeckProject) {
                1 * getNodeSet() >> nodeSet
            }
            0 * _(*_)
        }


            controller.rundeckAuthContextProcessor=Mock(AppAuthContextProcessor){
                1 * filterAuthorizedNodes(projectName, Collections.singleton('read'), !null, authCtx) >> authedNodes
                1 * authorizeProjectResource(authCtx, [type: 'resource', kind: 'node'], 'read', projectName) >> true


                1 * getAuthContextForSubjectAndProject(_, projectName) >> authCtx
            }
        controller.apiService = Mock(ApiService) {
            1 * requireApi(_, _) >> true
        }
        def query = new ExtNodeFilters(project: projectName)
        params.project = projectName
        when:
        response.format = 'xml'
        def result = controller.apiResourcesv2(query)

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


            1 * getFrameworkProject(projectName) >> Mock(IRundeckProject) {
                1 * getNodeSet() >> nodeSet
            }
            0 * _(*_)
        }

            controller.rundeckAuthContextProcessor=Mock(AppAuthContextProcessor){
                1 * filterAuthorizedNodes(projectName, Collections.singleton('read'), !null, authCtx) >> authedNodes
                1 * authorizeProjectResource(authCtx, [type: 'resource', kind: 'node'], 'read', projectName) >> true

                1 * getAuthContextForSubjectAndProject(_, projectName) >> authCtx
            }
        controller.apiService = Mock(ApiService) {
            1 * requireApi(_, _) >> true
        }
        def query = new ExtNodeFilters(project: projectName)
        params.project = projectName
        request.api_version = api_version
        when:
        response.format = 'all'
        def result = controller.apiResourcesv2(query)

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


            1 * getFrameworkProject(projectName) >> Mock(IRundeckProject) {
                1 * getNodeSet() >> nodeSet
            }
            0 * _(*_)
        }

            controller.rundeckAuthContextProcessor=Mock(AppAuthContextProcessor){
                1 * filterAuthorizedNodes(projectName, Collections.singleton('read'), !null, authCtx) >> authedNodes

                1 * authorizeProjectResource(authCtx, [type: 'resource', kind: 'node'], 'read', projectName) >> true
                1 * getAuthContextForSubjectAndProject(_, projectName) >> authCtx
            }
        controller.apiService = Mock(ApiService) {
            1 * requireApi(_,_) >> true
        }
        def query = new ExtNodeFilters(project: projectName)
        params.project = projectName
        params.name = 'monkey1'
        when:
        response.format = 'xml'
        def result = controller.apiResourcev14()

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



            1 * getFrameworkProject(projectName) >> Mock(IRundeckProject) {
                1 * getNodeSet() >> nodeSet
            }
            0 * _(*_)
        }

            controller.rundeckAuthContextProcessor=Mock(AppAuthContextProcessor){
                1 * filterAuthorizedNodes(projectName, Collections.singleton('read'), !null, authCtx) >> authedNodes
                1 * authorizeProjectResource(authCtx, [type: 'resource', kind: 'node'], 'read', projectName) >> true
                1 * getAuthContextForSubjectAndProject(_, projectName) >> authCtx
            }
        controller.apiService = Mock(ApiService) {
            1 * requireApi(_,_) >> true
        }
        def query = new ExtNodeFilters(project: projectName)
        params.project = projectName
        params.name = 'monkey1'
        request.api_version = api_version
        when:
        response.format = 'all'
        def result = controller.apiResourcev14()

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



            1 * getFrameworkProject(projectName) >> Mock(IRundeckProject) {
                1 * getNodeSet() >> nodeSet
            }
            0 * _(*_)
        }

            controller.rundeckAuthContextProcessor=Mock(AppAuthContextProcessor){
                1 * filterAuthorizedNodes(projectName, Collections.singleton('read'), !null, authCtx) >> authedNodes
                1 * authorizeProjectResource(authCtx, [type: 'resource', kind: 'node'], 'read', projectName) >> true

                1 * getAuthContextForSubjectAndProject(_, projectName) >> authCtx
            }
        controller.apiService = Mock(ApiService) {
            1 * requireApi(_,_) >> true
            1 * renderErrorFormat(_, _) >> { it[0].status = it[1].status }
        }
        def query = new ExtNodeFilters(project: projectName)
        params.project = projectName
        params.name = 'monkey1'
        when:

        def result = controller.apiResourcev14()

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
            0 * _(*_)
        }

            controller.rundeckAuthContextProcessor=Mock(AppAuthContextProcessor){
                1 * authorizeProjectConfigure(_,'test') >> true
                1 * getAuthContextForSubject(_)
            }
        controller.apiService = Mock(ApiService) {
            1 * requireApi(_, _, 23) >> true
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
                0 * _(*_)
            }

            controller.rundeckAuthContextProcessor=Mock(AppAuthContextProcessor){
                1 * authorizeProjectConfigure(_,'test') >> true
                1 * getAuthContextForSubject(_)
            }
            controller.apiService = Mock(ApiService) {
                1 * requireApi(_, _, 23) >> true
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
        controller.pluginGroupPasswordFieldsService = Mock(PasswordFieldsService)
        controller.execPasswordFieldsService = Mock(PasswordFieldsService)
        controller.userService = Mock(UserService)
        def sEService=Mock(ScheduledExecutionService){
            isProjectExecutionEnabled(_) >> !currentExecutionDisabled
            isProjectScheduledEnabled(_) >> !currentScheduleDisabled
        }
        controller.scheduledExecutionService = sEService


        controller.rundeckAuthContextProcessor=Mock(AppAuthContextProcessor)

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

        1 * controller.rundeckAuthContextProcessor.getAuthContextForSubject(_)
        1 * controller.rundeckAuthContextProcessor.authorizeProjectConfigure(_,'TestSaveProject') >> true
        1 * fwkService.listDescriptions() >> [null,null,null]
        1 * fwkService.updateFrameworkProjectConfig(_,{
            it['project.description'] == 'abc'
        },_) >> [success:true]
        if(shouldChangeScheduling){
            1 * fwkService.handleProjectSchedulingEnabledChange(_,_,_,_,_)
        }else{
            0 * fwkService.handleProjectSchedulingEnabledChange(_,_,_,_,_)
        }

        where:
        currentExecutionDisabled | currentScheduleDisabled | disableExecution | disableSchedule | shouldChangeScheduling
        false                    | false                   | 'false'          | 'false'         | false
        false                    | false                   | 'true'           | 'false'         | true
        false                    | false                   | 'false'          | 'true'          | true
        false                    | false                   | 'true'           | 'true'          | true
        true                     | false                   | 'false'          | 'false'         | true
        true                     | false                   | 'true'           | 'false'         | false
        true                     | false                   | 'false'          | 'true'          | true
        true                     | false                   | 'true'           | 'true'          | true
        false                    | true                    | 'false'          | 'false'         | true
        false                    | true                    | 'true'           | 'false'         | true
        false                    | true                    | 'false'          | 'true'          | false
        false                    | true                    | 'true'           | 'true'          | true
        true                     | true                    | 'false'          | 'false'         | true
        true                     | true                    | 'true'           | 'false'         | true
        true                     | true                    | 'false'          | 'true'          | true
        true                     | true                    | 'true'           | 'true'          | false

    }

    @Unroll
    def "save project config updating passive mode"(){
        setup:
        controller.featureService = Mock(FeatureService)
        controller.frameworkService = Mock(FrameworkService)
        controller.rundeckAuthContextProcessor=Mock(AppAuthContextProcessor)

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

        1 * controller.rundeckAuthContextProcessor.getAuthContextForSubject(_)
        1 * controller.rundeckAuthContextProcessor.authorizeProjectConfigure(_,'TestSaveProject') >> true
        1 * controller.frameworkService.listDescriptions() >> [null,null,null]
        2 * controller.frameworkService.validateServiceConfig(*_) >> [valid:true]
        1 * controller.frameworkService.setFrameworkProjectConfig(project,{
            it[ScheduledExecutionService.CONF_PROJECT_DISABLE_EXECUTION] == disableExecution
            it['project.disable.schedule'] == disableSchedule
        }) >> [success:true]

        1 * controller.scheduledExecutionService.isProjectExecutionEnabled(project) >> !currentExecutionDisabled
        1 * controller.scheduledExecutionService.isProjectScheduledEnabled(project) >> !currentScheduleDisabled

        (shouldChangeScheduling?1:0) * controller.frameworkService.handleProjectSchedulingEnabledChange(_,_,_,_,_)


          0 * controller.scheduledExecutionService._(*_)

        where:
        currentExecutionDisabled | currentScheduleDisabled | disableExecution | disableSchedule | shouldChangeScheduling
        false                    | false                   | 'false'          | 'false'         | false
        false                    | false                   | 'true'           | 'false'         | true
        false                    | false                   | 'false'          | 'true'          | true
        false                    | false                   | 'true'           | 'true'          | true
        true                     | false                   | 'false'          | 'false'         | true
        true                     | false                   | 'true'           | 'false'         | false
        true                     | false                   | 'false'          | 'true'          | true
        true                     | false                   | 'true'           | 'true'          | true
        false                    | true                    | 'false'          | 'false'         | true
        false                    | true                    | 'true'           | 'false'         | true
        false                    | true                    | 'false'          | 'true'          | false
        false                    | true                    | 'true'           | 'true'          | true
        true                     | true                    | 'false'          | 'false'         | true
        true                     | true                    | 'true'           | 'false'         | true
        true                     | true                    | 'false'          | 'true'          | true
        true                     | true                    | 'true'           | 'true'          | false

    }

    @Unroll
    def "save project config update label refreshes session data"() {
        setup:
            controller.featureService = Mock(FeatureService)
            controller.frameworkService = Mock(FrameworkService)
            controller.rundeckAuthContextProcessor=Mock(AppAuthContextProcessor)

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

            1 * controller.rundeckAuthContextProcessor.getAuthContextForSubject(_)
            1 * controller.frameworkService.getRundeckFramework()
            1 * controller.rundeckAuthContextProcessor.authorizeProjectConfigure(_,'TestSaveProject') >> true
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
            1 * validateProjectConfigurableInput(_,_,_)>>[props:[:]]
            listDescriptions()>>[Mock(ResourceModelSourceService),Mock(NodeExecutorService),Mock(FileCopierService)]
        }


            controller.rundeckAuthContextProcessor=Mock(AppAuthContextProcessor){
                1 * authorizeApplicationResourceTypeAll(null,'project',[ACTION_CREATE])>>true
                1 * getAuthContextForSubject(_) >> null
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
            1 * validateProjectConfigurableInput(_,_,_)>>[props:[:]]
            listDescriptions()>>[Mock(ResourceModelSourceService),Mock(NodeExecutorService),Mock(FileCopierService)]
        }


            controller.rundeckAuthContextProcessor=Mock(AppAuthContextProcessor){
                1 * authorizeApplicationResourceTypeAll(null,'project',[ACTION_CREATE])>>true
                1 * getAuthContextForSubject(_) >> null
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

    def "create project plugin groups don't save null"(){
        setup:
        controller.featureService = Mock(FeatureService)
        controller.metricService = Mock(MetricService)
        controller.rundeckAuthContextProcessor=Mock(AppAuthContextProcessor){
            1 * authorizeApplicationResourceTypeAll(null,'project',[ACTION_CREATE])>>true

            1 * getAuthContextForSubject(_) >> null
        }
        def fwkService = Mock(FrameworkService)
        controller.frameworkService = fwkService
        def project = Mock(Project){
            getName() >> "TestSaveProject"
        }
        def projectManager = Mock(ProjectManager){
            existsFrameworkProject( )>>false
        }
        def rdframework=Mock(Framework){
            1 * getFrameworkProjectMgr()>>projectManager
        }

        params.newproject = "TestSaveProject"
        if(json){
            params['pluginValues.PluginGroup.json']=json
        }

        setupFormTokens(params)
        when:
        request.method = "POST"
        controller.createProjectPost()

        then:
        response.status==302
        request.errors == null

        1 * fwkService.validateProjectConfigurableInput(_,_,_)>>[props:[:]]
        1 * fwkService.getRundeckFramework() >> rdframework
        1 * fwkService.createFrameworkProject(
                _,
                { it.subMap(expected.keySet())==expected && it.getProperty("project.plugin.PluginGroup.aplugin.a") == null}) >> [project, null]

        where:
         json                                                  |expected
         '[{"type":"aplugin","config":{"a":null, "b":"good"}}]'|['project.PluginGroup.aplugin.enabled':'true',"project.plugin.PluginGroup.aplugin.b":"good"]
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

    def "save project node sources"() {
        given:
        controller.frameworkService = Mock(FrameworkService)
            controller.rundeckAuthContextProcessor=Mock(AppAuthContextProcessor)

            controller.featureService = Mock(FeatureService)

        setupFormTokens(params)
        def project = 'testProj'

        when:
        request.method = "POST"
        params.project = project
        def result = controller.saveProjectNodeSources()
        then:
        1 * controller.rundeckAuthContextProcessor.getAuthContextForSubject(_)
        1 * controller.frameworkService.getRundeckFramework()
//        1 * controller.frameworkService.listResourceModelSourceDescriptions()
        1 * controller.rundeckAuthContextProcessor.authorizeProjectConfigure(_,project) >> true
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
            1 * validateProjectConfigurableInput(_,_,_)>>[props:[:]]
            1 * createFrameworkProject(_,_)>>[project, null]
            1 * refreshSessionProjects(_,_)>>null
            listDescriptions()>>[Mock(ResourceModelSourceService),Mock(NodeExecutorService),Mock(FileCopierService)]
        }
        controller.rundeckAuthContextProcessor=Mock(AppAuthContextProcessor){
            1 * authorizeApplicationResourceTypeAll(null,'project',[ACTION_CREATE])>>true

            1 * getAuthContextForSubject(_) >> null
        }
    }

    def "projectPluginsAjax"() {
        given:
            def project = "aProject"
            def serviceName = "SomeService"
            def configPrefix = "xyz"
            controller.frameworkService = Mock(FrameworkService)
            controller.rundeckAuthContextProcessor=Mock(AppAuthContextProcessor)

            controller.pluginService = Mock(PluginService)
            controller.obscurePasswordFieldsService = Mock(PasswordFieldsService)
        when:
            controller.projectPluginsAjax(project, serviceName, configPrefix)
        then:
            1 * controller.rundeckAuthContextProcessor.authorizeProjectConfigure(_,project) >> true
            1 * controller.rundeckAuthContextProcessor.getAuthContextForSubject(_)
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
            controller.rundeckAuthContextProcessor=Mock(AppAuthContextProcessor)

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
            1 * controller.rundeckAuthContextProcessor.authorizeProjectConfigure(_, project) >> true
            1 * controller.rundeckAuthContextProcessor.getAuthContextForSubject(_)
            1 * controller.pluginService.getPluginDescriptor('1type', serviceName) >>
            new DescribedPlugin(null, null, '1type', null, null)
            1 * controller.pluginService.validatePluginConfig(serviceName, '1type', [bongo: 'asdf']) >>
            new ValidatedPlugin(valid: true)
            1 * controller.pluginService.getPluginDescriptor('2type', serviceName) >>
            new DescribedPlugin(null, null, '2type', null, null)
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
            controller.rundeckAuthContextProcessor=Mock(AppAuthContextProcessor)

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
            1 * controller.rundeckAuthContextProcessor.authorizeProjectConfigure(_, project) >> true
            1 * controller.rundeckAuthContextProcessor.getAuthContextForSubject(_)

            0 * controller.pluginService.getPluginDescriptor('1type', serviceName) >>
            new DescribedPlugin(null, null, '1type', null, null)
            0 * controller.pluginService.validatePluginConfig(serviceName, '1type', [bongo: 'asdf'])

            1 * controller.pluginService.getPluginDescriptor('2type', serviceName) >>
            new DescribedPlugin(null, null, '2type', null, null)
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
            controller.rundeckAuthContextProcessor=Mock(AppAuthContextProcessor)

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
            1 * controller.rundeckAuthContextProcessor.authorizeProjectConfigure(_, project) >> true
            1 * controller.rundeckAuthContextProcessor.getAuthContextForSubject(_)

            1 * controller.pluginService.getPluginDescriptor('1type', serviceName) >> null
            0 * controller.pluginService.validatePluginConfig(serviceName, '1type', [bongo: 'asdf'])

            1 * controller.pluginService.getPluginDescriptor('2type', serviceName) >>
            new DescribedPlugin(null, null, '2type', null, null)
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
            controller.rundeckAuthContextProcessor=Mock(AppAuthContextProcessor)

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
            1 * controller.rundeckAuthContextProcessor.authorizeProjectConfigure(_, project) >> true
            1 * controller.rundeckAuthContextProcessor.getAuthContextForSubject(_)

            1 * controller.pluginService.getPluginDescriptor('1type', serviceName) >>
            new DescribedPlugin(null, null, '1type', null, null)
            1 * controller.pluginService.validatePluginConfig(serviceName, '1type', [bongo: 'asdf']) >>
            new ValidatedPlugin(valid: false, report: report)

            1 * controller.pluginService.getPluginDescriptor('2type', serviceName) >>
            new DescribedPlugin(null, null, '2type', null, null)
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
            controller.rundeckAuthContextProcessor=Mock(AppAuthContextProcessor)

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
            1 * controller.rundeckAuthContextProcessor.authorizeProjectConfigure(_, project) >> true
            1 * controller.rundeckAuthContextProcessor.getAuthContextForSubject(_)

            1 * controller.pluginService.getPluginDescriptor('1type', serviceName) >>
            new DescribedPlugin(null, null, '1type', null, null)
            1 * controller.pluginService.validatePluginConfig(serviceName, '1type', [bongo: 'asdf']) >>
            new ValidatedPlugin(valid: true)

            1 * controller.pluginService.getPluginDescriptor('2type', serviceName) >>
            new DescribedPlugin(null, null, '2type', null, null)
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
            1 * getSyntaxMimeType() >> 'application/json'
            0 * _(*_)
        }
        controller.frameworkService = Mock(FrameworkService) {
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
            0 * _(*_)
        }

        controller.rundeckAuthContextProcessor=Mock(AppAuthContextProcessor){
            1 * authorizeProjectConfigure(_, 'test') >> true
            1 * getAuthContextForSubject(_)
        }

        params.project = "test"
        params.index = "1"
        request.method = 'POST'
        params.fileText = 'some text'
        when:

        setupFormTokens(params)
        def result = controller.saveProjectNodeSourceFile()

        then:
        view=='/framework/saveProjectNodeSourceFile.gsp'
        flash.error == "archive.import.importNodesSource.failed.message"
    }

    def "save node source file, catch exception"() {

        setup:
        def source = Mock(WriteableModelSource) {
            1 * writeData(_) >> {
                throw new StorageException()
            }
            1 * getSyntaxMimeType() >> 'application/json'
            0 * _(*_)
        }
        controller.frameworkService = Mock(FrameworkService) {
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
            0 * _(*_)
        }

        controller.rundeckAuthContextProcessor=Mock(AppAuthContextProcessor){
            1 * authorizeProjectConfigure(_, 'test') >> true
            1 * getAuthContextForSubject(_)
        }

        params.project = "test"
        params.index = "1"
        request.method = 'POST'
        params.fileText = 'some text'
        when:

        setupFormTokens(params)
        def result = controller.saveProjectNodeSourceFile()

        then:
        view=='/framework/saveProjectNodeSourceFile.gsp'
        flash.error == "archive.import.importNodesSource.failed.message"
    }

}
