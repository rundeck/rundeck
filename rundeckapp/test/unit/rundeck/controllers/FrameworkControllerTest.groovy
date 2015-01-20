/*
 Copyright 2014 SimplifyOps Inc, <http://simplifyops.com>

 Licensed under the Apache License, Version 2.0 (the "License");
 you may not use this file except in compliance with the License.
 You may obtain a copy of the License at

     http://www.apache.org/licenses/LICENSE-2.0

 Unless required by applicable law or agreed to in writing, software
 distributed under the License is distributed on an "AS IS" BASIS,
 WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 See the License for the specific language governing permissions and
 limitations under the License.
 */

package rundeck.controllers

import com.dtolabs.rundeck.app.support.ExtNodeFilters
import com.dtolabs.rundeck.app.support.PluginConfigParams
import com.dtolabs.rundeck.core.common.Framework
import com.dtolabs.rundeck.core.plugins.configuration.Description
import com.dtolabs.rundeck.core.plugins.configuration.Property
import com.dtolabs.rundeck.core.plugins.configuration.StringRenderingConstants
import com.dtolabs.rundeck.server.authorization.AuthConstants
import grails.test.mixin.Mock
import grails.test.mixin.TestFor
import org.codehaus.groovy.grails.web.json.JSONElement
import org.codehaus.groovy.grails.web.json.JSONObject
import org.codehaus.groovy.grails.web.servlet.mvc.SynchronizerTokensHolder
import rundeck.CommandExec
import rundeck.Execution
import rundeck.ScheduledExecution
import rundeck.Workflow
import rundeck.WorkflowStep
import rundeck.services.FrameworkService
import rundeck.services.PasswordFieldsService
import rundeck.services.PasswordFieldsServiceTests
import rundeck.services.UserService

/**
 * $INTERFACE is ...
 * User: greg
 * Date: 1/30/14
 * Time: 5:19 PM
 */
@TestFor(FrameworkController)
@Mock([ScheduledExecution, Workflow, WorkflowStep, CommandExec, Execution])
class FrameworkControllerTest {
    /**
     * utility method to mock a class
     */
    private <T> T mockWith(Class<T> clazz, Closure clos) {
        def mock = mockFor(clazz)
        mock.demand.with(clos)
        return mock.createMock()
    }

    public void testextractApiNodeFilterParamsEmpty(){
        def params = FrameworkController.extractApiNodeFilterParams([:])
        assertEquals(0,params.size())
    }
    public void testextractApiNodeFilterParamsLegacyFilters(){
        def params = FrameworkController.extractApiNodeFilterParams([
                'hostname':'host1',
                'tags':'tags1',
                'name':'name1',
                'os-name':'osname1',
                'os-arch':'osarch1',
                'os-version':'osvers1',
                'os-family':'osfam1',
        ])
        assertEquals(7,params.size())
        assertEquals([
                'nodeInclude': 'host1',
                'nodeIncludeTags': 'tags1',
                'nodeIncludeName': 'name1',
                'nodeIncludeOsName': 'osname1',
                'nodeIncludeOsArch': 'osarch1',
                'nodeIncludeOsVersion': 'osvers1',
                'nodeIncludeOsFamily': 'osfam1',
        ],params)
    }
    public void testextractApiNodeFilterParamsLegacyFiltersExclude(){
        def params = FrameworkController.extractApiNodeFilterParams([
                'exclude-hostname':'host1',
                'exclude-tags':'tags1',
                'exclude-name':'name1',
                'exclude-os-name':'osname1',
                'exclude-os-arch':'osarch1',
                'exclude-os-version':'osvers1',
                'exclude-os-family':'osfam1',
        ])
        assertEquals(7,params.size())
        assertEquals([
                'nodeExclude': 'host1',
                'nodeExcludeTags': 'tags1',
                'nodeExcludeName': 'name1',
                'nodeExcludeOsName': 'osname1',
                'nodeExcludeOsArch': 'osarch1',
                'nodeExcludeOsVersion': 'osvers1',
                'nodeExcludeOsFamily': 'osfam1',
        ],params)
    }
    public void testextractApiNodeFilterParamsLegacyFiltersPrecedenceWithFilter(){
        def params = FrameworkController.extractApiNodeFilterParams([
                'exclude-precedence':'true',
                'hostname':'boing'
        ])
        assertEquals(2,params.size())
        assertEquals([
                'nodeExcludePrecedence': true,
                'nodeInclude': 'boing',
        ],params)
    }
    public void testextractApiNodeFilterParamsLegacyFiltersPrecedenceWithoutFilter(){
        def params = FrameworkController.extractApiNodeFilterParams([
                'exclude-precedence':'true',
        ])
        assertEquals(0,params.size())
    }
    public void testextractApiNodeFilterParamsLegacyFiltersPrecedenceFalseWithFilter(){
        def params = FrameworkController.extractApiNodeFilterParams([
                'exclude-precedence':'false',
                'hostname':'boing'
        ])
        assertEquals(2,params.size())
        assertEquals([
                'nodeExcludePrecedence': false,
                'nodeInclude': 'boing',
        ],params)
    }
    public void testextractApiNodeFilterParamsLegacyFiltersPrecedenceFalseWithoutFilter(){
        def params = FrameworkController.extractApiNodeFilterParams([
                'exclude-precedence':'false',
        ])
        assertEquals(0,params.size())
    }
    public void testextractApiNodeFilterParamsFilterString(){
        def params = FrameworkController.extractApiNodeFilterParams([
                'filter':'mynode !tags: blah',
        ])
        assertEquals(1,params.size())
        assertEquals([
                'filter': 'mynode !tags: blah',
        ],params)
    }
    public void testAdhocRetryFailedExecId(){
        def exec = new Execution(
                user: "testuser", project: "testproj", loglevel: 'WARN',
                workflow: new Workflow(commands: [new CommandExec(adhocExecution: true,
                        adhocRemoteString: 'a remote string')]).save(),
                failedNodeList: "abc,xyz"
        )
        assertNotNull exec.save()
        params.retryFailedExecId=exec.id

        def fwkControl = mockFor(FrameworkService, true)
        fwkControl.demand.getRundeckFramework {-> return null }
        fwkControl.demand.getAuthContextForSubject { subject -> return null }
        fwkControl.demand.authorizeProjectExecutionAll {ctx,e,actions->
            assertEquals(exec,e)
            assertEquals([AuthConstants.ACTION_READ],actions)
            true
        }
        fwkControl.demand.projects { return [] }
        fwkControl.demand.authorizeProjectResource { framework, resource, actions, project ->
            assertEquals([type:'adhoc'],resource)
            assertEquals('run',actions)
            return true
        }
        fwkControl.demand.authorizeProjectResourceAll { framework, resource, actions, project -> return true }
        fwkControl.demand.authorizeProjectJobAll { framework, resource, actions, project -> return true }
        fwkControl.demand.getRundeckFramework {-> return null }
        fwkControl.demand.getRundeckFramework {-> return null }
        controller.frameworkService = fwkControl.createMock()

        def result=controller.adhoc(new ExtNodeFilters())
        assertNotNull(result.query)
        assertEquals("name: abc,xyz",result.query.filter)
        assertNotNull(result.runCommand)
        assertEquals("a remote string",result.runCommand)
    }
    public void testAdhocFromExecId_nodeDispatch(){
        def exec = new Execution(
                user: "testuser", project: "testproj", loglevel: 'WARN',
                workflow: new Workflow(commands: [new CommandExec(adhocExecution: true,
                        adhocRemoteString: 'a remote string')]).save(),
                doNodedispatch: true,
                nodeIncludeName: "abc",
                nodeIncludeTags: "xyz"
        )
        assertNotNull exec.save()
        params.fromExecId=exec.id

        def fwkControl = mockFor(FrameworkService, true)
        fwkControl.demand.getRundeckFramework {-> return null }
        fwkControl.demand.getAuthContextForSubject { subject -> return null }
        fwkControl.demand.authorizeProjectExecutionAll {ctx,e,actions->
            assertEquals(exec,e)
            assertEquals([AuthConstants.ACTION_READ],actions)
            true
        }
        fwkControl.demand.projects { return [] }
        fwkControl.demand.authorizeProjectResource { framework, resource, actions, project ->
            assertEquals([type: 'adhoc'], resource)
            assertEquals('run', actions)
            return true
        }
        fwkControl.demand.authorizeProjectResourceAll { framework, resource, actions, project -> return true }
        fwkControl.demand.authorizeProjectJobAll { framework, resource, actions, project -> return true }
        fwkControl.demand.getRundeckFramework {-> return null }
        fwkControl.demand.getRundeckFramework {-> return null }
        controller.frameworkService = fwkControl.createMock()

        def result=controller.adhoc(new ExtNodeFilters())
        assertNotNull(result.query)
        assertEquals("name: abc tags: xyz",result.query.filter)
        assertNotNull(result.runCommand)
        assertEquals("a remote string",result.runCommand)
    }
    public void testAdhocFromExecId_local(){
        def exec = new Execution(
                user: "testuser", project: "testproj", loglevel: 'WARN',
                workflow: new Workflow(commands: [new CommandExec(adhocExecution: true,
                        adhocRemoteString: 'a remote string')]).save(),
                doNodedispatch: false,
        )
        assertNotNull exec.save()
        params.fromExecId=exec.id

        def fwkControl = mockFor(FrameworkService, true)
        fwkControl.demand.getRundeckFramework {-> return null }
        fwkControl.demand.getAuthContextForSubject { subject -> return null }
        fwkControl.demand.authorizeProjectResource {ctx,resource,action,proj->
            assertEquals([type: 'adhoc'], resource)
            assertEquals('run', action)
            true
        }
        fwkControl.demand.authorizeProjectExecutionAll {ctx,e,actions->
            assertEquals(exec,e)
            assertEquals([AuthConstants.ACTION_READ],actions)
            true
        }
        fwkControl.demand.getFrameworkNodeName { -> return "monkey1" }
        controller.frameworkService = fwkControl.createMock()

        def result=controller.adhoc(new ExtNodeFilters())
        assertNotNull(result.query)
        assertEquals("name: monkey1",result.query.filter)
        assertNotNull(result.runCommand)
        assertEquals("a remote string",result.runCommand)
    }

    public void testEditProjectObscurePassword() {
        given:
        def fwk = mockFor(FrameworkService, true)

        fwk.demand.getAuthContextForSubject {subject -> return null}
        fwk.demand.authResourceForProject {project -> return null}
        fwk.demand.authorizeApplicationResourceAll {ctx, e, actions -> true }

        fwk.demand.listResourceModelConfigurations { project ->
            [
                    [
                            "type": "withPasswordDescription",
                            "props": PasswordFieldsServiceTests.props("simple=text", "password=secret", "textField=a test field")
                    ],
            ]
        }
        fwk.demand.listDescriptions { -> [[withPasswordFieldDescription], null, null] }
        fwk.demand.getDefaultNodeExecutorService { -> null }
        fwk.demand.getDefaultFileCopyService { -> null }
        fwk.demand.getNodeExecConfigurationForType { -> null }
        fwk.demand.getFileCopyConfigurationForType { -> null }

        controller.frameworkService = fwk.createMock()

        def resourcePFmck = mockFor(PasswordFieldsService)
        def execPFmck = mockFor(PasswordFieldsService)
        def fcopyPFmck = mockFor(PasswordFieldsService)

        resourcePFmck.demand.reset{ -> return null}
        resourcePFmck.demand.track{a, b -> return null}
        execPFmck.demand.reset{ -> return null}
        execPFmck.demand.track{a, b -> return null}
        fcopyPFmck.demand.reset{ -> return null}
        fcopyPFmck.demand.track{a, b -> return null}

        controller.resourcesPasswordFieldsService = resourcePFmck.createMock()
        controller.execPasswordFieldsService = execPFmck.createMock()
        controller.fcopyPasswordFieldsService = fcopyPFmck.createMock()

        def passwordFieldsService = new PasswordFieldsService()
        passwordFieldsService.fields.put("dummy", "stuff")

        controller.resourcesPasswordFieldsService = passwordFieldsService
        params.project = "edit_test_project"

        when:
        def model = controller.editProject()

        then:
        assertEquals("plugin", model["prefixKey"])
        assertEquals(model["project"], "edit_test_project")
        assertEquals(1, model["configs"].size())
        assertEquals(1, passwordFieldsService.fields.size())
    }


    static passwordField = [
            getName: { "password" },
            getRenderingOptions: {
                [
                        "displayType": StringRenderingConstants.DisplayType.PASSWORD,
                ]
            }
    ] as Property

    static textField = [
            getName: { "textField" },
            getRenderingOptions: {[:]}
    ] as Property

    static noPasswordFieldDescription = [
            getName: { "noPasswordDescription" },
            getTitle: { "No Password" },
            getDescription: {"No Password Description" },
            getProperties: { [textField] },
            getPropertyMapping: { [:] },
            getFwkPropertyMapping: { [:] }
    ] as Description

    static withPasswordFieldDescription = [
            getName: { "withPasswordDescription" },
            getTitle: { "With Password" },
            getDescription: {"With Password Description" },
            getProperties: { [textField, passwordField] },
            getPropertyMapping: { [:] },
            getFwkPropertyMapping: { [:] }
    ] as Description

    protected void setupFormTokens(FrameworkController sec) {
        def token = SynchronizerTokensHolder.store(session)
        sec.params[SynchronizerTokensHolder.TOKEN_KEY] = token.generateToken('/test')
        sec.params[SynchronizerTokensHolder.TOKEN_URI] = '/test'
    }
    public void testSaveProjectInvalidRequestToken() {
        request.method='POST'
        controller.saveProject()
        assertEquals('/common/error', view)
        assertEquals("request.error.invalidtoken.message", request.getAttribute('errorCode'))
    }

    public void testSaveProjectMissingProject() {
        setupFormTokens(controller)
        request.method='POST'
        controller.saveProject()
        assertEquals(view, "/common/error")
        assertNotNull(request.errorMessage)
    }

    public void testSaveProjectCancel() {
        setupFormTokens(controller)
        params.cancel = "Cancel"
        params.project = "TestSaveProjectCancel"
        controller.saveProject()
        assertNull(view)
        assertNull(request.error)

    }

    public void testSaveProjectNominal() {
        def fwk = mockFor(FrameworkService, true)

        fwk.demand.getAuthContextForSubject {subject -> return null}
        fwk.demand.authResourceForProject {project -> return null}
        fwk.demand.authorizeApplicationResourceAll {ctx, e, actions -> true }
        fwk.demand.listDescriptions { -> [null, null, null] }
        fwk.demand.getRundeckFramework { -> null }

        fwk.demand.getFrameworkProject { project -> [name:project] }
        fwk.demand.getNodeExecutorService { -> null }
        fwk.demand.validateServiceConfig { a, b, c, d -> [valid:true] }
        fwk.demand.getNodeExecutorService { -> null }
        fwk.demand.addProjectNodeExecutorPropertiesForType {type, props, config, remove ->
            props.setProperty("foobar", "barbaz")
        }

        fwk.demand.updateFrameworkProjectConfig { project, Properties props, removePrefixes ->
            ["success":props.size() != 0]
        }

        controller.frameworkService = fwk.createMock()

        def resourcePFmck = mockFor(PasswordFieldsService)
        def execPFmck = mockFor(PasswordFieldsService)
        def fcopyPFmck = mockFor(PasswordFieldsService)

        resourcePFmck.demand.adjust{a -> return null}
        resourcePFmck.demand.untrack{a, b -> return null}
        execPFmck.demand.untrack{a, b -> return null}
        fcopyPFmck.demand.untrack{a, b -> return null}

        controller.resourcesPasswordFieldsService = resourcePFmck.createMock()
        controller.execPasswordFieldsService = execPFmck.createMock()
        controller.fcopyPasswordFieldsService = fcopyPFmck.createMock()

        def us = mockFor(UserService)
        us.demand.storeFilterPref { -> true }
        controller.userService = us.createMock()

        request.method = "POST"

        params.project = "TestSaveProject"
        params.defaultNodeExec = 1
        params.nodeexec = [
                "1": [
                        type  : "foobar",
                        config: [
                                specialvalue1: "foobar",
                                specialvalue2: "barfoo",
                                specialvalue3: "fizbaz"
                        ]
                ]
        ]

        setupFormTokens(controller)
        controller.saveProject()

        assertNull(view)
        assertNull(request.error)
        assertEquals("Project TestSaveProject saved", flash.message)

    }

    public void skip_testSaveProjectPrefixKeyWrong() {
        def fwk = mockFor(FrameworkService, true)

        fwk.demand.getAuthContextForSubject { subject -> return null }
        fwk.demand.authResourceForProject { project -> return null }
        fwk.demand.authorizeApplicationResourceAll { ctx, e, actions -> true }
        fwk.demand.listDescriptions { -> [null, null, null] }
        fwk.demand.getRundeckFramework { -> null }
        fwk.demand.updateFrameworkProjectConfig { a, b, c -> ["success": true] }
        fwk.demand.getFrameworkProject { project -> [name: project] }
        fwk.demand.getNodeExecutorService { -> null }
        fwk.demand.validateServiceConfig { a, b, c, d -> [valid: true] }

        controller.frameworkService = fwk.createMock()

        def us = mockFor(UserService)
        us.demand.storeFilterPref { -> true }
        controller.userService = us.createMock()

        params.project = "TestSaveProject"
        request.method = "POST"
        // nodeexec
        params.defaultNodeExec = 1
        params["nodexec.1.type"] = "foobar"  // the key isn't correct <<<<<=======
        params["nodexec.1.config.specialvalue1"] = "foobar"

        controller.saveProject()
    }
    void testCheckResourceModelConfig_noType(){
        controller.frameworkService = mockWith(FrameworkService){
            getRundeckFramework {-> [getResourceModelSourceService:{->null}] }
        }

        def params = new PluginConfigParams()
        controller.checkResourceModelConfig(params)
        assertEquals(false,response.json.valid)
        assertEquals('Plugin provider type must be specified',response.json.error)
    }
    void testCheckResourceModelConfig_okType(){
        controller.frameworkService = mockWith(FrameworkService){
            getRundeckFramework {-> [getResourceModelSourceService:{->null}] }
            validateServiceConfig{String type,String prefix,Map params,service->
                assertEquals('abc',type)
                assertEquals('config.',prefix)
                [valid:true]
            }
        }

        def config = new PluginConfigParams()
        controller.params.type='abc'
        controller.checkResourceModelConfig(config)
        assertEquals(true,response.json.valid)
        assertTrue(response.json.error in [null, JSONObject.NULL])
    }
    void testCheckResourceModelConfig_revertUsesOrigPrefix(){
        controller.frameworkService = mockWith(FrameworkService){
            getRundeckFramework {-> [getResourceModelSourceService:{->null}] }
            validateServiceConfig{String type,String prefix,Map params,service->
                assertEquals('abc',type)
                assertEquals('orig.config.',prefix)
                [valid:true]
            }
        }

        def config = new PluginConfigParams()
        controller.params.type='abc'
        controller.params.revert='true'
        controller.checkResourceModelConfig(config)
        assertEquals(true,response.json.valid)
        assertTrue(response.json.error in [null, JSONObject.NULL])
    }

    void testViewResourceModelConfig_noType() {
        controller.frameworkService = mockWith(FrameworkService) {
            getRundeckFramework {-> [getResourceModelSourceService: {-> null }] }
        }

        def params = new PluginConfigParams()
        def model = controller.viewResourceModelConfig(params)
        assertEquals('Plugin provider type must be specified',model.error)
        assertEquals('',model.prefix)
        assertEquals(null,model.values)
        assertEquals(null,model.description)
        assertEquals(null,model.report)
        assertEquals(null,model.type)
        assertEquals(true,model.saved)
        assertEquals(true,model.includeFormFields)
    }
    void testViewResourceModelConfig_okType() {
        controller.frameworkService = mockWith(FrameworkService) {
            getRundeckFramework {-> [getResourceModelSourceService: {-> null }] }
            validateServiceConfig { String type, String prefix, Map params, service ->
                assertEquals('abc', type)
                assertEquals('config.', prefix)
                [props: new Properties(),desc:'desc1',report:'report']
            }
        }

        def params = new PluginConfigParams()
        controller.params.type = 'abc'
        def model = controller.viewResourceModelConfig(params)
        assertEquals('', model.prefix)
        assertNotNull(model.values)
        assertEquals('desc1', model.description)
        assertEquals('report', model.report)
        assertEquals(null, model.error)
        assertEquals('abc', model.type)
        assertEquals(true, model.saved)
        assertEquals(true, model.includeFormFields)
    }
    void testViewResourceModelConfig_revertUsesOrigPrefix() {
        controller.frameworkService = mockWith(FrameworkService) {
            getRundeckFramework {-> [getResourceModelSourceService: {-> null }] }
            validateServiceConfig { String type, String prefix, Map params, service ->
                assertEquals('abc', type)
                assertEquals('orig.config.', prefix)
                [props: new Properties(),desc:'desc1',report:'report']
            }
        }

        def params = new PluginConfigParams()
        controller.params.type = 'abc'
        controller.params.revert = 'true'
        def model = controller.viewResourceModelConfig(params)
        assertEquals('', model.prefix)
        assertNotNull(model.values)
        assertEquals('desc1', model.description)
        assertEquals('report', model.report)
        assertEquals(null, model.error)
        assertEquals('abc', model.type)
        assertEquals(true, model.saved)
        assertEquals(true, model.includeFormFields)
    }
}
