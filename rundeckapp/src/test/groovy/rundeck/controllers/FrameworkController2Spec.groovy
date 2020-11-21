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
import com.dtolabs.rundeck.core.plugins.configuration.Description
import com.dtolabs.rundeck.core.plugins.configuration.Property
import com.dtolabs.rundeck.core.plugins.configuration.StringRenderingConstants
import grails.test.hibernate.HibernateSpec
import grails.testing.web.controllers.ControllerUnitTest
import groovy.mock.interceptor.MockFor
import org.grails.web.servlet.mvc.SynchronizerTokensHolder
import org.rundeck.app.authorization.AppAuthContextEvaluator
import org.rundeck.core.auth.AuthConstants
import rundeck.*
import rundeck.services.*
import rundeck.services.feature.FeatureService

import static org.junit.Assert.*

/**
 * $INTERFACE is ...
 * User: greg
 * Date: 1/30/14
 * Time: 5:19 PM
 */
class FrameworkController2Spec extends HibernateSpec implements ControllerUnitTest<FrameworkController> {

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

        controller.rundeckAuthContextProvider=Mock(AuthContextProvider)
        controller.rundeckAuthContextEvaluator=Mock(AppAuthContextEvaluator){
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

            controller.rundeckAuthContextProvider=Mock(AuthContextProvider)
            controller.rundeckAuthContextEvaluator=Mock(AppAuthContextEvaluator){
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

            controller.rundeckAuthContextProvider=Mock(AuthContextProvider)
            controller.rundeckAuthContextEvaluator=Mock(AppAuthContextEvaluator){
                1 * authorizeProjectResource(_,  [type:'adhoc'], 'run', _)>>true
                1 * authorizeProjectExecutionAny(_,exec,[AuthConstants.ACTION_READ,AuthConstants.ACTION_VIEW])>>true
            }
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

        execPFmck.demand.reset{ -> return null}
        execPFmck.demand.track{a, b -> return null}
        fcopyPFmck.demand.reset{ -> return null}
        fcopyPFmck.demand.track{a, b -> return null}


        controller.execPasswordFieldsService = execPFmck.proxyInstance()
        controller.fcopyPasswordFieldsService = fcopyPFmck.proxyInstance()


        def passwordFieldsService = new PasswordFieldsService()
        passwordFieldsService.fields.put("dummy", "stuff")

        controller.resourcesPasswordFieldsService = passwordFieldsService
        params.project = "edit_test_project"

            controller.rundeckAuthContextProvider=Mock(AuthContextProvider)
            controller.rundeckAuthContextEvaluator=Mock(AppAuthContextEvaluator){
                1 * authorizeApplicationResourceAny(*_)>>true
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
        when:
        def fwk = new MockFor(FrameworkService, true)
        def featureServiceMock = new MockFor(FeatureService, true)

        fwk.demand.getRundeckFramework { -> null }
        fwk.demand.listDescriptions { -> [null, null, null] }

        fwk.demand.getNodeExecutorService { -> null }
        fwk.demand.validateServiceConfig { a, b, c, d -> [valid:true] }
        fwk.demand.addProjectNodeExecutorPropertiesForType(1..2) {type, props, config, remove ->
            props.setProperty("foobar", "barbaz")
        }
// REVIEW: Disabled at grails3 merge
//        fwk.demand.validateProjectConfigurableInput {data,prefix -> [:] }
        fwk.demand.validateProjectConfigurableInput {data,prefix,pred -> [:] }

        fwk.demand.updateFrameworkProjectConfig { project, Properties props, removePrefixes ->
            ["success":props.size() != 0]
        }
        fwk.demand.scheduleCleanerExecutions{project, config->null}
        fwk.demand.refreshSessionProjects{auth,session->['TestSaveProject']}
        fwk.demand.loadSessionProjectLabel(1){a,b,c->}

        featureServiceMock.demand.featurePresent(1..3){a,b->true}


        controller.frameworkService = fwk.proxyInstance()
        controller.featureService = featureServiceMock.proxyInstance()

        def execPFmck = new MockFor(PasswordFieldsService)
        def fcopyPFmck = new MockFor(PasswordFieldsService)


        controller.execPasswordFieldsService = mockWith(PasswordFieldsService){
            untrack{a, b -> return null}
            untrack{a, b -> return null}
            reset{ -> }
        }
        controller.fcopyPasswordFieldsService = mockWith(PasswordFieldsService){
            reset{ -> }
        }

        controller.userService = mockWith(UserService){
            storeFilterPref { -> true }
        }

        def seServiceControl = new MockFor(ScheduledExecutionService)
        seServiceControl.demand.isProjectExecutionEnabled{ project -> true
        }
        seServiceControl.demand.isProjectScheduledEnabled{ project -> true}
        controller.scheduledExecutionService = seServiceControl.proxyInstance()

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

            controller.rundeckAuthContextProvider=Mock(AuthContextProvider)
            controller.rundeckAuthContextEvaluator=Mock(AppAuthContextEvaluator){
                1 * authorizeApplicationResourceAny(*_)>>true
            }
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
        when:
        def fwk = new MockFor(FrameworkService, true)
        def featureServiceMock = new MockFor(FeatureService, true)



            controller.rundeckAuthContextProvider=Mock(AuthContextProvider)
            controller.rundeckAuthContextEvaluator=Mock(AppAuthContextEvaluator){
                1 * authorizeApplicationResourceAny(*_)>>true
            }
        fwk.demand.getRundeckFramework { -> null }
        fwk.demand.listDescriptions { -> [null, null, null] }

        //fwk.demand.getFrameworkProject { project -> [name:project] }
        fwk.demand.getFileCopierService(1..2) { -> null }
        fwk.demand.validateServiceConfig { a, b, c, d -> [valid:true] }
        fwk.demand.addProjectFileCopierPropertiesForType(1..2) {type, props, config, remove ->
            props.putAll(config)
        }
        fwk.demand.getNodeExecutorService { -> null }
        fwk.demand.validateServiceConfig { a, b, c, d -> [valid:true] }
        //fwk.demand.getNodeExecutorService { -> null }
        fwk.demand.addProjectNodeExecutorPropertiesForType(1..2) {type, props, config, remove ->
            props.putAll(config)
        }
        fwk.demand.validateProjectConfigurableInput {data,prefix,pred -> [:] }

        fwk.demand.updateFrameworkProjectConfig { project, Properties props, removePrefixes ->
            assertEquals('Label----',props.getProperty('project.label'))
            ["success":props.size() != 0]
        }
        fwk.demand.scheduleCleanerExecutions{project, config->null}
        fwk.demand.refreshSessionProjects{auth,session->['TestSaveProject']}
        fwk.demand.loadSessionProjectLabel(1){a,b,c->}

        featureServiceMock.demand.featurePresent(1..3){a,b->true}

        controller.frameworkService = fwk.proxyInstance()
        controller.featureService = featureServiceMock.proxyInstance()

        controller.execPasswordFieldsService = mockWith(PasswordFieldsService){
            untrack(1..4){a, b -> return null}
            reset{ -> }
        }
        controller.fcopyPasswordFieldsService = mockWith(PasswordFieldsService){
            reset{ -> }
        }

        controller.userService = mockWith(UserService){
            storeFilterPref { -> true }
        }

        def seServiceControl = new MockFor(ScheduledExecutionService, true)
        seServiceControl.demand.isProjectExecutionEnabled{ project -> true
        }
        seServiceControl.demand.isProjectScheduledEnabled{ project -> true}
        controller.scheduledExecutionService = seServiceControl.proxyInstance()

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
        when:
        def fwk = new MockFor(FrameworkService, true)
        def featureServiceMock = new MockFor(FeatureService, true)


            controller.rundeckAuthContextProvider=Mock(AuthContextProvider)
            controller.rundeckAuthContextEvaluator=Mock(AppAuthContextEvaluator){
                1 * authorizeApplicationResourceAny(*_)>>true
            }
        fwk.demand.getRundeckFramework { -> null }
        fwk.demand.listDescriptions { -> [null, null, null] }

        fwk.demand.getFileCopierService(1..2) { -> null }
        fwk.demand.validateServiceConfig { a, b, c, d -> [valid:true] }
        fwk.demand.addProjectFileCopierPropertiesForType(1..2) {type, props, config, remove ->
            props.putAll(config)
        }
        fwk.demand.getNodeExecutorService { -> null }
        fwk.demand.validateServiceConfig { a, b, c, d -> [valid:true] }
        fwk.demand.addProjectNodeExecutorPropertiesForType(1..2) {type, props, config, remove ->
            props.putAll(config)
        }
        fwk.demand.validateProjectConfigurableInput {data,prefix,pred -> [:] }

        fwk.demand.updateFrameworkProjectConfig { project, Properties props, removePrefixes ->
            assertEquals("foobar1",props.getProperty('specialvalue1'))
            assertEquals("barfoo",props.getProperty('specialvalue2'))
            assertEquals("fizbaz1",props.getProperty('specialvalue3'))
            assertEquals("rembar1",props.getProperty('specialvalue4'))
            ["success":props.size() != 0]
        }
        fwk.demand.scheduleCleanerExecutions{project, config->null}
        fwk.demand.refreshSessionProjects{auth,session->['TestSaveProject']}
        fwk.demand.loadSessionProjectLabel(1){a,b,c->}

        featureServiceMock.demand.featurePresent(1..3){a,b->true}

        controller.frameworkService = fwk.proxyInstance()
        controller.featureService = featureServiceMock.proxyInstance()

        controller.execPasswordFieldsService = mockWith(PasswordFieldsService){
            untrack(1..4){a, b -> return null}
            reset{ -> }
        }
        controller.fcopyPasswordFieldsService = mockWith(PasswordFieldsService){
            reset{ -> }
        }

        controller.userService = mockWith(UserService){
            storeFilterPref { -> true }
        }

        def seServiceControl = new MockFor(ScheduledExecutionService, true)
        seServiceControl.demand.isProjectExecutionEnabled{ project -> true
        }
        seServiceControl.demand.isProjectScheduledEnabled{ project -> true}
        controller.scheduledExecutionService = seServiceControl.proxyInstance()

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
        def fwk = new MockFor(FrameworkService, true)


            controller.rundeckAuthContextProvider=Mock(AuthContextProvider)
            controller.rundeckAuthContextEvaluator=Mock(AppAuthContextEvaluator){
                1 * authorizeApplicationResourceAny(*_)>>true
            }

        def proj = new MockFor(IRundeckProject)
        proj.demand.getProjectProperties(1..8){-> ["project.label":label]}

        fwk.demand.getFrameworkProject { name-> proj.proxyInstance() }
        fwk.demand.listDescriptions { -> [[withPasswordFieldDescription], null, null] }
        fwk.demand.getDefaultNodeExecutorService { prj -> null }
        fwk.demand.getDefaultFileCopyService { prj -> null }
        fwk.demand.getNodeExecConfigurationForType { nodeExec,prj -> null }
        fwk.demand.getFileCopyConfigurationForType { fcpy,prj -> null }
        fwk.demand.loadProjectConfigurableInput {prefix,props -> [:] }

        controller.frameworkService = fwk.proxyInstance()

        def execPFmck = new MockFor(PasswordFieldsService,true)
        def fcopyPFmck = new MockFor(PasswordFieldsService,true)

        execPFmck.demand.reset{ -> return null}
        execPFmck.demand.track{a, b -> return null}
        fcopyPFmck.demand.reset{ -> return null}
        fcopyPFmck.demand.track{a, b -> return null}


        controller.execPasswordFieldsService = execPFmck.proxyInstance()
        controller.fcopyPasswordFieldsService = fcopyPFmck.proxyInstance()


        def passwordFieldsService = new PasswordFieldsService()
        passwordFieldsService.fields.put("dummy", "stuff")

        controller.resourcesPasswordFieldsService = passwordFieldsService
        params.project = "edit_test_project"

        when:
        def model = controller.editProject()

        then:
        assertEquals(model["project"], "edit_test_project")
        assertEquals(label,model["projectLabel"])

    }

    public void editProjectNodeExecutorPluginNotFound() {
        given:
        def label = "Label for project"
        def fwk = new MockFor(FrameworkService, true)


            controller.rundeckAuthContextProvider=Mock(AuthContextProvider)
            controller.rundeckAuthContextEvaluator=Mock(AppAuthContextEvaluator){
                1 * authorizeApplicationResourceAny(*_)>>true
            }

        def proj = new MockFor(IRundeckProject)
        proj.demand.getProjectProperties(1..8){-> ["project.label":label]}

        fwk.demand.getFrameworkProject { name-> proj.proxyInstance() }
        fwk.demand.listDescriptions { -> [[withPasswordFieldDescription], null, null] }

        fwk.demand.getDefaultNodeExecutorService { prj -> "TestPluginsNodeExecutor" }
        fwk.demand.getDefaultFileCopyService { prj -> "WinRMcpPython" }

        fwk.demand.getNodeExecConfigurationForType { nodeExec,prj -> null }
        fwk.demand.getFileCopyConfigurationForType { fcpy,prj -> "WinRMcpPython" }
        fwk.demand.loadProjectConfigurableInput {prefix,props -> [:] }

        controller.frameworkService = fwk.proxyInstance()

        def execPFmck = new MockFor(PasswordFieldsService,true)
        def fcopyPFmck = new MockFor(PasswordFieldsService,true)

        execPFmck.demand.reset{ -> return null}
        execPFmck.demand.track{a, b -> return null}
        fcopyPFmck.demand.reset{ -> return null}
        fcopyPFmck.demand.track{a, b -> return null}

        controller.execPasswordFieldsService = execPFmck.proxyInstance()
        controller.fcopyPasswordFieldsService = fcopyPFmck.proxyInstance()

        def passwordFieldsService = new PasswordFieldsService()
        passwordFieldsService.fields.put("dummy", "stuff")

        controller.resourcesPasswordFieldsService = passwordFieldsService

        params.project = "edit_test_project"

        when:
        def model = controller.editProject()

        then:
        assertNotNull(request.errors)

    }

    public void editProjectFileCopyPluginNotFound() {
        given:
        def label = "Label for project"
        def fwk = new MockFor(FrameworkService, true)


            controller.rundeckAuthContextProvider=Mock(AuthContextProvider)
            controller.rundeckAuthContextEvaluator=Mock(AppAuthContextEvaluator){
                1 * authorizeApplicationResourceAny(*_)>>true
            }

        def proj = new MockFor(IRundeckProject)
        proj.demand.getProjectProperties(1..8){-> ["project.label":label]}

        fwk.demand.getFrameworkProject { name-> proj.proxyInstance() }
        fwk.demand.listDescriptions { -> [[withPasswordFieldDescription], null, null] }

        fwk.demand.getDefaultNodeExecutorService { prj -> "ssh-exec" }
        fwk.demand.getDefaultFileCopyService { prj -> "TestPluginsFileCopy" }

        fwk.demand.getNodeExecConfigurationForType { nodeExec,prj -> "ssh-exec" }
        fwk.demand.getFileCopyConfigurationForType { fcpy,prj -> null }
        fwk.demand.loadProjectConfigurableInput {prefix,props -> [:] }

        controller.frameworkService = fwk.proxyInstance()

        def execPFmck = new MockFor(PasswordFieldsService,true)
        def fcopyPFmck = new MockFor(PasswordFieldsService,true)

        execPFmck.demand.reset{ -> return null}
        execPFmck.demand.track{a, b -> return null}
        fcopyPFmck.demand.reset{ -> return null}
        fcopyPFmck.demand.track{a, b -> return null}

        controller.execPasswordFieldsService = execPFmck.proxyInstance()
        controller.fcopyPasswordFieldsService = fcopyPFmck.proxyInstance()

        def passwordFieldsService = new PasswordFieldsService()
        passwordFieldsService.fields.put("dummy", "stuff")

        controller.resourcesPasswordFieldsService = passwordFieldsService

        params.project = "edit_test_project"

        when:
        def model = controller.editProject()

        then:
        assertNotNull(request.errors)

    }

}
