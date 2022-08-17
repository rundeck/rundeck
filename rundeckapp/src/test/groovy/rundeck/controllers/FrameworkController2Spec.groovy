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
import com.dtolabs.rundeck.core.authorization.AuthContextProvider
import com.dtolabs.rundeck.core.common.IRundeckProject
import com.dtolabs.rundeck.core.execution.service.FileCopier
import com.dtolabs.rundeck.core.execution.service.NodeExecutor
import com.dtolabs.rundeck.core.plugins.DescribedPlugin
import com.dtolabs.rundeck.core.plugins.configuration.Description
import com.dtolabs.rundeck.core.plugins.configuration.Property
import com.dtolabs.rundeck.core.plugins.configuration.StringRenderingConstants
import com.dtolabs.rundeck.plugins.ServiceNameConstants
import grails.test.hibernate.HibernateSpec
import grails.testing.web.controllers.ControllerUnitTest
import groovy.mock.interceptor.MockFor
import org.grails.web.servlet.mvc.SynchronizerTokensHolder
import org.rundeck.app.authorization.AppAuthContextEvaluator
import org.rundeck.app.authorization.AppAuthContextProcessor
import org.rundeck.core.auth.AuthConstants
import rundeck.*
import rundeck.services.*
import rundeck.services.feature.FeatureService
import spock.lang.Unroll
import testhelper.RundeckHibernateSpec

import static org.junit.Assert.*

/**
 * $INTERFACE is ...
 * User: greg
 * Date: 1/30/14
 * Time: 5:19 PM
 */
class FrameworkController2Spec extends RundeckHibernateSpec implements ControllerUnitTest<FrameworkController> {

    List<Class> getDomainClasses() { [ScheduledExecution, Workflow, WorkflowStep, CommandExec, Execution, Project]}

    /**
     * utility method to mock a class
     */
    private <T> T mockWith(Class<T> clazz, Closure clos) {
        def mock = new MockFor(clazz)
        mock.demand.with(clos)
        return mock.proxyInstance()
    }

    public void testextractApiNodeFilterParamsEmpty(){
        when:
        def params = FrameworkController.extractApiNodeFilterParams([:])
        then:
        assertEquals(0,params.size())
    }
    public void testextractApiNodeFilterParamsLegacyFilters(){
        when:
        def params = FrameworkController.extractApiNodeFilterParams([
                'hostname':'host1',
                'tags':'tags1',
                'name':'name1',
                'os-name':'osname1',
                'os-arch':'osarch1',
                'os-version':'osvers1',
                'os-family':'osfam1',
        ])

        then:
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

        when:
        def params = FrameworkController.extractApiNodeFilterParams([
                'exclude-hostname':'host1',
                'exclude-tags':'tags1',
                'exclude-name':'name1',
                'exclude-os-name':'osname1',
                'exclude-os-arch':'osarch1',
                'exclude-os-version':'osvers1',
                'exclude-os-family':'osfam1',
        ])

        then:
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

        when:
        def params = FrameworkController.extractApiNodeFilterParams([
                'exclude-precedence':'true',
                'hostname':'boing'
        ])

        then:
        assertEquals(2,params.size())
        assertEquals([
                'nodeExcludePrecedence': true,
                'nodeInclude': 'boing',
        ],params)
    }
    public void testextractApiNodeFilterParamsLegacyFiltersPrecedenceWithoutFilter(){

        when:
        def params = FrameworkController.extractApiNodeFilterParams([
                'exclude-precedence':'true',
        ])

        then:
        assertEquals(0,params.size())
    }
    public void testextractApiNodeFilterParamsLegacyFiltersPrecedenceFalseWithFilter(){

        when:
        def params = FrameworkController.extractApiNodeFilterParams([
                'exclude-precedence':'false',
                'hostname':'boing'
        ])

        then:
        assertEquals(2,params.size())
        assertEquals([
                'nodeExcludePrecedence': false,
                'nodeInclude': 'boing',
        ],params)
    }
    public void testextractApiNodeFilterParamsLegacyFiltersPrecedenceFalseWithoutFilter(){
        when:
        def params = FrameworkController.extractApiNodeFilterParams([
                'exclude-precedence':'false',
        ])

        then:
        assertEquals(0,params.size())
    }
    public void testextractApiNodeFilterParamsFilterString(){
        when:
        def params = FrameworkController.extractApiNodeFilterParams([
                'filter':'mynode !tags: blah',
        ])

        then:
        assertEquals(1,params.size())
        assertEquals([
                'filter': 'mynode !tags: blah',
        ],params)
    }
    public void testAdhocRetryFailedExecId(){
        given:
        def exec = new Execution(
                user: "testuser", project: "testproj", loglevel: 'WARN',
                workflow: new Workflow(commands: [new CommandExec(adhocExecution: true,
                        adhocRemoteString: 'a remote string')]).save(),
                failedNodeList: "abc,xyz"
        )
        assertNotNull exec.save()
        params.retryFailedExecId=exec.id

        def fwkControl = new MockFor(FrameworkService, true)

        fwkControl.demand.getRundeckFramework {-> return null }
        fwkControl.demand.projects { return [] }
        fwkControl.demand.getRundeckFramework {-> return null }
        fwkControl.demand.getRundeckFramework {-> return null }
        controller.frameworkService = fwkControl.proxyInstance()

        def scheduledExecutionServiceMock = new MockFor(ScheduledExecutionService, true)
        scheduledExecutionServiceMock.demand.getMatchedNodesMaxCount {-> return null}
        controller.scheduledExecutionService = scheduledExecutionServiceMock.proxyInstance()

        controller.rundeckAuthContextProcessor=Mock(AppAuthContextProcessor){
            1 * authorizeProjectResource(_,  [type:'adhoc'], 'run', _)>>true
            1 * authorizeProjectExecutionAny(_,exec,[AuthConstants.ACTION_READ,AuthConstants.ACTION_VIEW])>>true
        }
        when:
        def result=controller.adhoc(new ExtNodeFilters())

        then:
        assertNotNull(result.query)
        assertEquals("name: abc,xyz",result.query.filter)
        assertNotNull(result.runCommand)
        assertEquals("a remote string",result.runCommand)
    }
    public void testAdhocFromExecId_nodeDispatch(){
        when:
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

        def fwkControl = new MockFor(FrameworkService, true)
        fwkControl.demand.getRundeckFramework {-> return null }
        fwkControl.demand.projects { return [] }
        fwkControl.demand.getRundeckFramework {-> return null }
        fwkControl.demand.getRundeckFramework {-> return null }
        controller.frameworkService = fwkControl.proxyInstance()
        def scheduledExecutionServiceMock = new MockFor(ScheduledExecutionService, true)
        scheduledExecutionServiceMock.demand.getMatchedNodesMaxCount {-> return null}
        controller.scheduledExecutionService = scheduledExecutionServiceMock.proxyInstance()

                        controller.rundeckAuthContextProcessor=Mock(AppAuthContextProcessor){
                1 * authorizeProjectResource(_,  [type:'adhoc'], 'run', _)>>true
                1 * authorizeProjectExecutionAny(_,exec,[AuthConstants.ACTION_READ,AuthConstants.ACTION_VIEW])>>true
            }

        def result=controller.adhoc(new ExtNodeFilters())

        then:
        assertNotNull(result.query)
        assertEquals("name: abc tags: xyz",result.query.filter)
        assertNotNull(result.runCommand)
        assertEquals("a remote string",result.runCommand)
    }
    public void testAdhocFromExecId_local(){

        when:
        def exec = new Execution(
                user: "testuser", project: "testproj", loglevel: 'WARN',
                workflow: new Workflow(commands: [new CommandExec(adhocExecution: true,
                        adhocRemoteString: 'a remote string')]).save(),
                doNodedispatch: false,
        )
        assertNotNull exec.save()
        params.fromExecId=exec.id

        def fwkControl = new MockFor(FrameworkService, true)
        fwkControl.demand.getFrameworkNodeName { -> return "monkey1" }
        fwkControl.demand.getRundeckFramework {-> return null }
        controller.frameworkService = fwkControl.proxyInstance()

                        controller.rundeckAuthContextProcessor=Mock(AppAuthContextProcessor){
                1 * authorizeProjectResource(_,  [type:'adhoc'], 'run', _)>>true
                1 * authorizeProjectExecutionAny(_,exec,[AuthConstants.ACTION_READ,AuthConstants.ACTION_VIEW])>>true
            }
        def scheduledExecutionServiceMock = new MockFor(ScheduledExecutionService, true)
        scheduledExecutionServiceMock.demand.getMatchedNodesMaxCount {-> return null}
        controller.scheduledExecutionService = scheduledExecutionServiceMock.proxyInstance()
        def result=controller.adhoc(new ExtNodeFilters())

        then:
        assertNotNull(result.query)
        assertEquals("name: monkey1",result.query.filter)
        assertNotNull(result.runCommand)
        assertEquals("a remote string",result.runCommand)
    }

    public void testEditProjectObscurePassword() {
        given:
        def fwk = new MockFor(FrameworkService)
        def proj = new MockFor(IRundeckProject)


        fwk.demand.getFrameworkProject { name-> proj.proxyInstance() }

        fwk.demand.listDescriptions { -> [[withPasswordFieldDescription], null, null] }
        fwk.demand.getDefaultNodeExecutorService { project -> null }
        fwk.demand.getDefaultFileCopyService { project -> null }
        fwk.demand.getNodeExecConfigurationForType { defaultNodeExec, project -> null }
        fwk.demand.getFileCopyConfigurationForType { defaultFileCopy, project -> null }
        fwk.demand.listPluginGroupDescriptions{null}
        fwk.demand.loadProjectConfigurableInput {prefix,props -> [:] }
        fwk.demand.listResourceModelConfigurations { project ->
            [
                    [
                            "type": "withPasswordDescription",
                            "props": PasswordFieldsServiceTests.props("simple=text", "password=secret", "textField=a test field")
                    ],
            ]
        }
        fwk.demand.listWriteableResourceModelSources { project -> [] }

        proj.demand.getProjectProperties(1..8){-> [:]}

        fwk.demand.getAuthContextForSubjectAndProject { subject,pr -> return null}

        controller.frameworkService = fwk.proxyInstance()

        def execPFmck = new MockFor(PasswordFieldsService)
        def fcopyPFmck = new MockFor(PasswordFieldsService)
        def pluginPFmck = new MockFor(PasswordFieldsService)

        pluginPFmck.demand.reset{ -> return null}
        pluginPFmck.demand.track{a, b -> return null}
        execPFmck.demand.reset{ -> return null}
        execPFmck.demand.track{a, b -> return null}
        fcopyPFmck.demand.reset{ -> return null}
        fcopyPFmck.demand.track{a, b -> return null}


        controller.execPasswordFieldsService = execPFmck.proxyInstance()
        controller.fcopyPasswordFieldsService = fcopyPFmck.proxyInstance()
        controller.pluginGroupPasswordFieldsService = pluginPFmck.proxyInstance()


        def passwordFieldsService = new PasswordFieldsService()
        passwordFieldsService.fields.put("dummy", "stuff")

        controller.resourcesPasswordFieldsService = passwordFieldsService
        params.project = "edit_test_project"

        controller.rundeckAuthContextProcessor=Mock(AppAuthContextProcessor){
            1 * authorizeProjectConfigure(*_)>>true
        }
        when:
        def model = controller.editProject()

        then:
        assertEquals("plugin", model["prefixKey"])
        assertEquals(model["project"], "edit_test_project")
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
        when:
        request.method='POST'
        controller.saveProject()
        then:
        assertEquals('/common/error', view)
        assertEquals("request.error.invalidtoken.message", request.getAttribute('errorCode'))
    }

    public void testSaveProjectMissingProject() {
        when:
        setupFormTokens(controller)
        request.method='POST'
        controller.saveProject()
        then:
        assertEquals(view, "/common/error")
        assertNotNull(request.errorMessage)
    }

    public void testSaveProjectCancel() {
        when:
        setupFormTokens(controller)
        params.cancel = "Cancel"
        params.project = "TestSaveProjectCancel"
        request.method = 'POST'
        controller.saveProject()
        then:
        response.redirectUrl=='/?project=TestSaveProjectCancel'
        assertNull(request.error)

    }

    public void testSaveProjectNominal() {
        given:

        controller.frameworkService = Mock(FrameworkService){

            1*getRundeckFramework()
            1*listDescriptions()>>[null,null,null]

            1*getNodeExecutorService()
            1*validateServiceConfig(_,_,_,_)>> [valid:true]
            2*addProjectNodeExecutorPropertiesForType(_,_,_,_)>> {
                it[1].setProperty("foobar", "barbaz")
            }
            1*validateProjectConfigurableInput (_,_,_) >> [:]

            1*updateFrameworkProjectConfig(_,_,_) >>{
                ["success":it[1].size() != 0]
            }
            1*scheduleCleanerExecutions(_,_)
            1*refreshSessionProjects(_,_)>>['TestSaveProject']
            1*loadSessionProjectLabel(_,_,_)

        }
        controller.featureService = Mock(FeatureService){
            3 * featurePresent(_,_) >> true
        }


        controller.execPasswordFieldsService = Mock(PasswordFieldsService){
            2 * untrack(_,_)
            1 * reset(*_)
        }
        controller.pluginGroupPasswordFieldsService = Mock(PasswordFieldsService){
            1 * reset(*_)
        }
        controller.fcopyPasswordFieldsService = Mock(PasswordFieldsService){
            1 * reset(*_)
        }

        controller.scheduledExecutionService = Mock(ScheduledExecutionService) {
            _ * isProjectExecutionEnabled(_) >> true
            _ * isProjectScheduledEnabled(_) >> true
        }
        controller.rundeckAuthContextProcessor=Mock(AppAuthContextProcessor){
            1 * getAuthContextForSubject(_)
            1 * authorizeProjectConfigure(*_)>>true
        }
        when:
        request.method = "POST"

        params.project = "TestSaveProject"
        params.default_NodeExecutor = 'foobar'
        params.nodeexec = [
                "default": [
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

        then:
        response.redirectUrl=='/?project=TestSaveProject'
        assertNull(request.error)
        assertEquals("Project TestSaveProject saved", flash.message.toString())

    }

    public void skip_testSaveProjectPrefixKeyWrong() {
        def fwk = new MockFor(FrameworkService, true)

        fwk.demand.getAuthContextForSubjectAndProject { subject,proj -> return null }
        fwk.demand.authResourceForProject { project -> return null }
        fwk.demand.authorizeApplicationResourceAll { ctx, e, actions -> true }
        fwk.demand.listDescriptions { -> [null, null, null] }
        fwk.demand.getRundeckFramework { -> null }
        fwk.demand.updateFrameworkProjectConfig { a, b, c -> ["success": true] }
        fwk.demand.getFrameworkProject { project -> [name: project] }
        fwk.demand.getNodeExecutorService { -> null }
        fwk.demand.validateServiceConfig { a, b, c, d -> [valid: true] }

        controller.frameworkService = fwk.proxyInstance()

        def us = new MockFor(UserService)
        us.demand.storeFilterPref { -> true }
        controller.userService = us.proxyInstance()

        params.project = "TestSaveProject"
        request.method = "POST"
        // nodeexec
        params.defaultNodeExec = 1
        params["nodexec.1.type"] = "foobar"  // the key isn't correct <<<<<=======
        params["nodexec.1.config.specialvalue1"] = "foobar"

        controller.saveProject()
    }
    void testCheckResourceModelConfig_noType(){
        when:
        controller.frameworkService = mockWith(FrameworkService){
            getRundeckFramework {-> [getResourceModelSourceService:{->null}] }
        }

        def params = new PluginConfigParams()
        controller.checkResourceModelConfig(params)
        then:
        assertEquals(false,response.json.valid)
        assertEquals('Plugin provider type must be specified',response.json.error)
    }
    void testCheckResourceModelConfig_okType(){
        when:
        controller.frameworkService = mockWith(FrameworkService){
            getRundeckFramework {-> [getResourceModelSourceService:{->null}] }
            validateServiceConfig{String type,String prefix,Map params,service->
                assertEquals('abc',type)
                assertEquals('config.',prefix)
                [valid:true]
            }
        }

        controller.pluginService = mockWith(PluginService) {
            validatePluginConfig{type,svc,props->

                assertEquals('abc', type)
                assertEquals('data2', props.test1)

                [props: new Properties(),desc:'desc1',report:'report',valid:true]
            }
        }

        def config = new PluginConfigParams()
        controller.params.type='abc'
        controller.params['orig.config.test1']='data1'
        controller.params['config.test1']='data2'
        controller.checkResourceModelConfig(config)
        then:
        assertEquals(true,response.json.valid)
        assertNull(response.json.error)
    }
    void testCheckResourceModelConfig_revertUsesOrigPrefix(){
        when:
        controller.frameworkService = mockWith(FrameworkService){
            getRundeckFramework {-> [getResourceModelSourceService:{->null}] }
            validateServiceConfig{String type,String prefix,Map params,service->
                assertEquals('abc',type)
                assertEquals('orig.config.',prefix)
                [valid:true]
            }
        }

        controller.pluginService = mockWith(PluginService) {
            validatePluginConfig{type,svc,props->

                assertEquals('abc', type)
                assertEquals('data1', props.test1)

                [props: new Properties(),desc:'desc1',report:'report',valid:true]
            }
        }

        def config = new PluginConfigParams()
        controller.params.type='abc'
        controller.params.revert='true'
        controller.params['orig.config.test1']='data1'
        controller.checkResourceModelConfig(config)
        then:
        assertEquals(true,response.json.valid)
        assertNull(response.json.error)
    }

    void testViewResourceModelConfig_noType() {
        when:
        controller.frameworkService = mockWith(FrameworkService) {
            getRundeckFramework {-> [getResourceModelSourceService: {-> null }] }
        }

        def params = new PluginConfigParams()
        def model = controller.viewResourceModelConfig(params)
        then:
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
        when:
        controller.frameworkService = mockWith(FrameworkService) {
            getRundeckFramework {-> [getResourceModelSourceService: {-> null }] }
            validateServiceConfig { String type, String prefix, Map params, service ->
                assertEquals('abc', type)
                assertEquals('config.', prefix)
                [props: new Properties(),desc:'desc1',report:'report']
            }
        }

        controller.pluginService = mockWith(PluginService) {
            getPluginDescriptor {type,svc-> [description:'desc1'] }
            validatePluginConfig{type,svc,props->

                assertEquals('abc', type)

                [props: new Properties(),desc:'desc1',report:'report']
            }
        }

        def params = new PluginConfigParams()
        controller.params.type = 'abc'
        def model = controller.viewResourceModelConfig(params)

        then:
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
        when:
        controller.frameworkService = mockWith(FrameworkService) {
            getRundeckFramework {-> [getResourceModelSourceService: {-> null }] }
            validateServiceConfig { String type, String prefix, Map params, service ->
                assertEquals('abc', type)
                assertEquals('orig.config.', prefix)
                [props: new Properties(),desc:'desc1',report:'report']
            }
        }
        controller.pluginService = mockWith(PluginService) {
            getPluginDescriptor {type,svc-> [description:'desc1'] }
            validatePluginConfig{type,svc,props->

                assertEquals('abc', type)
                assertEquals('data1', props.test1)

                [props: new Properties(),desc:'desc1',report:'report']
            }
        }

        def params = new PluginConfigParams()
        controller.params.type = 'abc'
        controller.params.revert = 'true'
        controller.params['orig.config.test1'] = 'data1'
        def model = controller.viewResourceModelConfig(params)
        then:
        assertEquals('', model.prefix)
        assertNotNull(model.values)
        assertEquals('desc1', model.description)
        assertEquals('report', model.report)
        assertEquals(null, model.error)
        assertEquals('abc', model.type)
        assertEquals(true, model.saved)
        assertEquals(true, model.includeFormFields)
    }

    public void testSaveProjectLabel() {
        given:

            controller.frameworkService = Mock(FrameworkService){

                1*getRundeckFramework()
                1*listDescriptions()>>[null,null,null]

                1*getNodeExecutorService()
                1*validateServiceConfig('foobar', 'nodeexec.default.config.',_,_)>> [valid:true]
                1*validateServiceConfig('barbar', 'fcopy.default.config.',_,_)>> [valid:true]
                2*addProjectNodeExecutorPropertiesForType(_,_,_,_)>> {
                    it[1].setProperty("foobar", "barbaz")
                }
                1*validateProjectConfigurableInput (_,_,_) >> [:]

                1*updateFrameworkProjectConfig(_,_,_) >>{
                    assertEquals('Label----',it[1].getProperty('project.label'))
                    ["success":it[1].size() != 0]
                }
                1*scheduleCleanerExecutions(_,_)
                1*refreshSessionProjects(_,_)>>['TestSaveProject']
                1*loadSessionProjectLabel(_,_,_)

            }
            controller.featureService = Mock(FeatureService){
                3 * featurePresent(_,_) >> true
            }

            def execPasswordFieldsService = Mock(PasswordFieldsService){
                4 * untrack(_,_)
                1 * reset(*_)
            }
            controller.execPasswordFieldsService = execPasswordFieldsService

            def fcopyPasswordFieldsService = Mock(PasswordFieldsService){
                1 * reset(*_)
            }
            controller.pluginGroupPasswordFieldsService = Mock(PasswordFieldsService){
                1 * reset(*_)
            }
            controller.fcopyPasswordFieldsService = fcopyPasswordFieldsService

            controller.scheduledExecutionService = Mock(ScheduledExecutionService) {
                _ * isProjectExecutionEnabled(_) >> true
                _ * isProjectScheduledEnabled(_) >> true
            }
            controller.rundeckAuthContextProcessor=Mock(AppAuthContextProcessor){
                1 * getAuthContextForSubject(_)
                1 * authorizeProjectConfigure(*_)>>true
            }
        when:

        request.method = "POST"

        params.project = "TestSaveProject"
        params.default_FileCopier = 'barbar'
        params.fcopy = [
            "default": [
                type  : "barbar",
                config: [
                        specialvalue1: "foobar",
                        specialvalue2: "barfoo",
                        specialvalue3: "fizbaz"
                ]
            ],
        ]
        params.default_NodeExecutor = 'foobar'
        params.orig = [
                "nodeexec": [
                    "default": [
                        type  : "foobar",
                        config: [
                            specialvalue1: "foobar",
                            specialvalue2: "barfoo",
                            specialvalue3: "fizbaz"
                        ]
                    ]
                ],
                "fcopy": [
                        "default": [
                                type  : "barbar",
                                config: [
                                        specialvalue1: "foobar",
                                        specialvalue2: "barfoo",
                                        specialvalue3: "fizbaz"
                                ]
                        ]
                ]
        ]
        params.nodeexec = [
                "default": [
                        type  : "foobar",
                        config: [
                                specialvalue1: "foobar1",
                                specialvalue2: "barfoo",
                                specialvalue3: "fizbaz1",
                                specialvalue4: "rembar1"
                        ]
                ]
        ]
        params.label = 'Label----'

        setupFormTokens(controller)
        def r = controller.saveProject()

        then:
        response.redirectUrl=='/?project=TestSaveProject'
        assertNull(request.error)
        assertEquals("Project TestSaveProject saved", flash.message.toString())

    }


    public void testSaveDefaultServiceProperties() {
        given:

            controller.frameworkService = Mock(FrameworkService){

                1*getRundeckFramework()
                1*listDescriptions()>>[null,null,null]

                1*getNodeExecutorService()
                1*validateServiceConfig('foobar', 'nodeexec.default.config.',_,_)>> [valid:true]
                1*validateServiceConfig('barbar', 'fcopy.default.config.',_,_)>> [valid:true]
                _*addProjectFileCopierPropertiesForType(_,_,_,_)>> {
                    it[1].putAll(it[2])
                }
                _*addProjectNodeExecutorPropertiesForType(_,_,_,_)>> {
                    it[1].putAll(it[2])
                }
                1*validateProjectConfigurableInput (_,_,_) >> [:]

                1*updateFrameworkProjectConfig(_,_,_) >>{
                    assertEquals("foobar1",it[1].getProperty('specialvalue1'))
                    assertEquals("barfoo",it[1].getProperty('specialvalue2'))
                    assertEquals("fizbaz1",it[1].getProperty('specialvalue3'))
                    assertEquals("rembar1",it[1].getProperty('specialvalue4'))
                    ["success":it[1].size() != 0]
                }
                1*scheduleCleanerExecutions(_,_)
                1*refreshSessionProjects(_,_)>>['TestSaveProject']
                1*loadSessionProjectLabel(_,_,_)

            }
            controller.featureService = Mock(FeatureService){
                3 * featurePresent(_,_) >> true
            }

            def execPasswordFieldsService = Mock(PasswordFieldsService){
                4 * untrack(_,_)
                1 * reset(*_)
            }
            controller.execPasswordFieldsService = execPasswordFieldsService

            def fcopyPasswordFieldsService = Mock(PasswordFieldsService){
                1 * reset(*_)
            }
            controller.fcopyPasswordFieldsService = fcopyPasswordFieldsService
            controller.pluginGroupPasswordFieldsService = Mock(PasswordFieldsService){
                1 * reset(*_)
            }

            controller.scheduledExecutionService = Mock(ScheduledExecutionService) {
                _ * isProjectExecutionEnabled(_) >> true
                _ * isProjectScheduledEnabled(_) >> true
            }
            controller.rundeckAuthContextProcessor=Mock(AppAuthContextProcessor){
                1 * getAuthContextForSubject(_)
                1 * authorizeProjectConfigure(*_)>>true
            }

        when:
        request.method = "POST"

        params.project = "TestSaveProject"
        params.default_FileCopier = 'barbar'
        params.fcopy = [
            "default": [
                type  : "barbar",
                config: [
                        specialvalue1: "foobar",
                        specialvalue2: "barfoo",
                        specialvalue3: "fizbaz"
                ]
            ],
        ]
        params.default_NodeExecutor = 'foobar'
        params.orig = [
                "nodeexec": [
                    "default": [
                        type  : "foobar",
                        config: [
                            specialvalue1: "foobar",
                            specialvalue2: "barfoo",
                            specialvalue3: "fizbaz"
                        ]
                    ]
                ],
                "fcopy": [
                        "default": [
                                type  : "barbar",
                                config: [
                                        specialvalue1: "foobar",
                                        specialvalue2: "barfoo",
                                        specialvalue3: "fizbaz"
                                ]
                        ]
                ]
        ]
        params.nodeexec = [
                "default": [
                        type  : "foobar",
                        config: [
                                specialvalue1: "foobar1",
                                specialvalue2: "barfoo",
                                specialvalue3: "fizbaz1",
                                specialvalue4: "rembar1"
                        ]
                ]
        ]

        setupFormTokens(controller)
        controller.saveProject()

        then:
        response.redirectUrl=='/?project=TestSaveProject'
        assertNull(request.error)
        assertEquals("Project TestSaveProject saved", flash.message.toString())

    }

    public void testEditProjectLabel() {
        given:
        def label = "Label for project"
        controller.frameworkService = Mock(FrameworkService){
            _*getFrameworkProject(_)>>Mock(IRundeckProject){
                _*getProjectProperties()>>["project.label":label]
            }
            1*listDescriptions()>>[[withPasswordFieldDescription],null,null]

            _*addProjectFileCopierPropertiesForType(_,_,_,_)>> {
                it[1].putAll(it[2])
            }
            _*addProjectNodeExecutorPropertiesForType(_,_,_,_)>> {
                it[1].putAll(it[2])
            }

            _*updateFrameworkProjectConfig(_,_,_) >>{
                ["success":it[1].size() != 0]
            }
            0*scheduleCleanerExecutions(_,_)
            _*loadProjectConfigurableInput(_,_)>>[:]
        }


        controller.execPasswordFieldsService = Mock(PasswordFieldsService){
            _*reset()
            _*track(*_)
        }
        controller.fcopyPasswordFieldsService = Mock(PasswordFieldsService){
            _*reset()
            _*track(*_)
        }

        controller.resourcesPasswordFieldsService = Mock(PasswordFieldsService){
            _*reset()
            _*track(*_)
        }
        controller.pluginGroupPasswordFieldsService = Mock(PasswordFieldsService){
            1 * reset(*_)
        }

            controller.rundeckAuthContextProcessor=Mock(AppAuthContextProcessor){
                1 * getAuthContextForSubject(_)
                1 * authorizeProjectConfigure(*_)>>true
            }
        params.project = "edit_test_project"

        when:
        def model = controller.editProject()

        then:
        assertEquals(model["project"], "edit_test_project")
        assertEquals(label,model["projectLabel"])

    }

    @Unroll
    def editProject_NodeExecutorOrFileCopier_PluginNotFound() {
        given:
        def label = "Label for project"

        controller.rundeckAuthContextProcessor=Mock(AppAuthContextProcessor){
            1 * authorizeProjectConfigure(*_)>>true
            1 * getAuthContextForSubject(_)
        }

        def proj = Mock(IRundeckProject){
            _*getProjectProperties()>>["project.label":label]
        }

        def fwk = Mock(FrameworkService) {
            1*getFrameworkProject (_)>> proj
            1*listDescriptions()>>  [[withPasswordFieldDescription], null, null]

            1*getDefaultNodeExecutorService (_)>> "TestPluginsNodeExecutor"
            1*getDefaultFileCopyService (_)>>"WinRMcpPython"

            1*getNodeExecConfigurationForType (_,_)
            1*getFileCopyConfigurationForType (_,_)>> [:]
            1*loadProjectConfigurableInput (_,_)>>[:]
        }
        controller.frameworkService = fwk

        def execPFmck = Mock(PasswordFieldsService)
        def fcopyPFmck = Mock(PasswordFieldsService)
        def pluginPFmck = Mock(PasswordFieldsService)


        controller.execPasswordFieldsService = execPFmck
        controller.fcopyPasswordFieldsService = fcopyPFmck
        controller.pluginGroupPasswordFieldsService = pluginPFmck


        controller.resourcesPasswordFieldsService = Mock(PasswordFieldsService)
        controller.pluginService = Mock(PluginService){
            1 * getPluginDescriptor('TestPluginsNodeExecutor', ServiceNameConstants.NodeExecutor)>>{
                if(foundNodeExec){
                    return new DescribedPlugin<NodeExecutor>(Mock(NodeExecutor),null,'TestPluginsNodeExecutor', null, null)
                }
                null
            }
            1 * getPluginDescriptor('WinRMcpPython', ServiceNameConstants.FileCopier)>>{
                if(foundFileCopier){
                    return new DescribedPlugin<FileCopier>(Mock(FileCopier), null, 'WinRMcpPython', null, null)
                }
                null
            }
        }

        params.project = "edit_test_project"

        when:
        def model = controller.editProject()

        then:
            request.errors==errors

        where:
            foundNodeExec | foundFileCopier | errors
            true          | true            | null
            false         | true            | ['domain.project.edit.plugin.missing.message']
            true         | false            | ['domain.project.edit.plugin.missing.message']
            false         | false            | ['domain.project.edit.plugin.missing.message','domain.project.edit.plugin.missing.message']
    }


}
