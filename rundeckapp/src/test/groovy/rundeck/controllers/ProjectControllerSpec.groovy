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
import com.dtolabs.rundeck.app.support.ProjectArchiveImportRequest
import com.dtolabs.rundeck.app.support.ProjectArchiveParams
import com.dtolabs.rundeck.core.authorization.RuleSetValidation
import com.dtolabs.rundeck.core.authorization.UserAndRolesAuthContext
import com.dtolabs.rundeck.core.common.IFramework
import com.dtolabs.rundeck.core.common.IRundeckProject
import grails.testing.gorm.DataTest
import grails.testing.web.controllers.ControllerUnitTest
import groovy.xml.MarkupBuilder
import org.grails.plugins.testing.GrailsMockMultipartFile
import org.grails.web.servlet.mvc.SynchronizerTokensHolder
import org.rundeck.app.acl.AppACLContext
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
import org.rundeck.core.auth.web.RdAuthorizeProject
import org.rundeck.core.auth.web.WebDefaultParameterNamesMapper
import rundeck.services.AclFileManagerService
import rundeck.services.ApiService
import rundeck.services.ArchiveOptions
import rundeck.services.ConfigurationService
import rundeck.services.FrameworkService
import rundeck.services.ImportResponse
import rundeck.services.ProgressSummary
import rundeck.services.ProjectService
import spock.lang.Specification
import spock.lang.Unroll
import webhooks.component.project.WebhooksProjectComponent
import webhooks.exporter.WebhooksProjectExporter
import webhooks.importer.WebhooksProjectImporter

import javax.security.auth.Subject
import java.lang.annotation.Annotation

import static org.rundeck.core.auth.AuthConstants.ACTION_CREATE
import static org.rundeck.core.auth.AuthConstants.ACTION_DELETE
import static org.rundeck.core.auth.AuthConstants.ACTION_READ
import static org.rundeck.core.auth.AuthConstants.ACTION_UPDATE

/**
 * Created by greg on 2/26/15.
 */
class ProjectControllerSpec extends Specification implements ControllerUnitTest<ProjectController>, DataTest {

    def setup(){
        session.subject = new Subject()
        controller.rundeckWebDefaultParameterNamesMapper=Mock(WebDefaultParameterNamesMapper)
        controller.rundeckExceptionHandler=Mock(WebExceptionHandler)
    }
    private void setupAuthExport(
        boolean auth = true,
        boolean found = true,
        String name = 'test',
        IRundeckProject rdproj=null,
        AuthActions actions = RundeckAccess.Project.APP_EXPORT
    ) {
        setupAuthAccess(auth, found, name, rdproj,actions)
    }
    private void setupAuthImport(
        boolean auth = true,
        boolean found = true,
        String name = 'test',
        IRundeckProject rdproj=null,
        AuthActions actions = RundeckAccess.Project.APP_IMPORT
    ) {
        setupAuthAccess(auth, found, name, rdproj,actions)
    }
    private void setupAuthConfigure(
        boolean auth = true,
        boolean found = true,
        String name = 'test',
        IRundeckProject project=null,
        AuthActions actions = RundeckAccess.Project.APP_CONFIGURE
    ) {
        setupAuthAccess(auth, found, name, project, actions)
    }
    private void setupAuthDelete(
        boolean auth = true,
        boolean found = true,
        IRundeckProject rdproj=null,
        String name = 'test'
    ) {
        controller.rundeckAuthContextProcessor=Mock(AppAuthContextProcessor)
        controller.rundeckAppAuthorizer = Mock(AppAuthorizer) {
            1 * project(_, _) >> Mock(AuthorizingProject) {
                _ * getAuthContext() >> Mock(UserAndRolesAuthContext)
                1 * getResource() >> {
                    if(!found) {
                        throw new NotFound('Project', name)
                    }
                    rdproj?:Stub(IRundeckProject){
                        getName()>>name
                    }
                }
                0*_(*_)
            }
            0*_(*_)
        }
    }
    private void setupAuthAccess(
        boolean auth = true,
        boolean found = true,
        String name = 'test',
        IRundeckProject rdproj=null,
        AuthActions actions = RundeckAccess.Project.APP_CONFIGURE
    ) {
        controller.rundeckAuthContextProcessor=Mock(AppAuthContextProcessor)
        controller.rundeckAppAuthorizer = Mock(AppAuthorizer) {
            _ * project(_, _) >> Mock(AuthorizingProject) {
                1 * access(actions) >> {
                    if(!auth) {
                        throw new UnauthorizedAccess(actions.description, 'Project', name)
                    }
                    if(!found) {
                        throw new NotFound('Project', name)
                    }
                    rdproj?:Stub(IRundeckProject){
                        getName()>>name
                    }
                }
                0*_(*_)
            }
            0*_(*_)
        }
    }
    @Unroll
    def "api project create description #inputDesc"(){
        given:
        controller.projectService=Mock(ProjectService)
        controller.apiService=Mock(ApiService)
        controller.frameworkService=Mock(FrameworkService)
        params.project='aproject'

        request.method='POST'
        request.format='json'
        request.json=[name:'aproject',description:inputDesc]
        when:

        def result=controller.apiProjectCreate()

        then:
        1 * controller.apiService.requireApi(_, _) >> true
        1 * controller.apiService.extractResponseFormat(*_) >> 'json'
        1 * controller.apiService.parseJsonXmlWith(*_) >> { args ->
            args[2].json.call(args[0].JSON)
            true
        }
        1 * controller.frameworkService.isFrameworkProjectDisabled('aproject')>>false
        1 * controller.frameworkService.existsFrameworkProject('aproject')>>false
        1 * controller.frameworkService.createFrameworkProject('aproject',{
            it['project.description']==inputDesc
        })>>[Mock(IRundeckProject){
            getName()>>'aproject'
        },[]]
        1 * controller.frameworkService.loadProjectProperties(*_)>>([:] as Properties)
        0 * controller.frameworkService._(*_)

        where:
        inputDesc       | _
        'a description' | _
        null            | _
    }

    @Unroll
    def "api project create with execution cleaner"() {
        given:
            controller.projectService = Mock(ProjectService)
            controller.apiService = Mock(ApiService)
            controller.frameworkService = Mock(FrameworkService)
            params.project = 'aproject'

            request.method = 'POST'
            request.format = 'json'
            def jsonData=[
                name                                                 : 'aproject',
                description                                          : 'a description',
                config: [
                    'project.execution.history.cleanup.enabled'          : 'true',
                    'project.execution.history.cleanup.retention.days'   : '1',
                    'project.execution.history.cleanup.retention.minimum': '2',
                    'project.execution.history.cleanup.batch'            : '3',
                    'project.execution.history.cleanup.schedule'         : 'crontab1',
                ]
            ]
            request.json = jsonData
        when:

            def result = controller.apiProjectCreate()

        then:
            1 * controller.apiService.requireApi(_, _) >> true
            1 * controller.apiService.extractResponseFormat(*_) >> 'json'
            1 * controller.apiService.parseJsonXmlWith(*_) >> { args ->
                args[2].json.call(jsonData)
                true
            }
            1 * controller.frameworkService.isFrameworkProjectDisabled('aproject') >> false
            1 * controller.frameworkService.existsFrameworkProject('aproject') >> false
            1 * controller.frameworkService.createFrameworkProject('aproject', _) >> [Mock(IRundeckProject) {
                getName() >> 'aproject'
            }, []]
            1 * controller.frameworkService.loadProjectProperties(*_) >> ([:] as Properties)
            1 * controller.frameworkService.scheduleCleanerExecutions(
                'aproject', {
                it.enabled && it.maxDaysToKeep == 1 &&
                it.cronExpression == 'crontab1' &&
                it.minimumExecutionToKeep == 2 &&
                it.maximumDeletionSize == 3
            }
            )
            0 * controller.frameworkService._(*_)
    }

    @Unroll
    def "api project create validate input json #inputJson"(){
        given:
        controller.projectService=Mock(ProjectService)
        controller.apiService=Mock(ApiService)
        controller.frameworkService=Mock(FrameworkService)
        params.project='aproject'

        request.method='POST'
        request.format='json'
        request.json=inputJson
        when:

        def result=controller.apiProjectCreate()

        then:
        1 * controller.apiService.requireApi(_, _) >> true
        1 * controller.apiService.extractResponseFormat(*_) >> 'json'
        1 * controller.apiService.parseJsonXmlWith(*_) >> { args ->
            args[2].json.call(args[0].JSON)
            true
        }
        1 * controller.apiService.renderErrorFormat(_, [status: 400, code:'api.error.invalid.request',args: [errMsg], format: 'json'])

        0 * controller.frameworkService._(*_)

        where:
        inputJson                                              | errMsg
        [name: 'aproject', description: 'xyz', config: 'blah'] | 'json: expected \'config\' to be a Map'
        [name: 'aproject', description: 12]                    | 'json: expected \'description\' to be a String'
        [name: [a: 'b'], description: null]                    | 'json: expected \'name\' to be a String'
        [description: 'monkey']                                | 'json: required \'name\' but it was not found'
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

    def "api export execution ids string"(){
        given:
        controller.projectService=Mock(ProjectService)
        controller.apiService=Mock(ApiService)
        controller.frameworkService=Mock(FrameworkService)

        setupGetResource()
        params.project='aproject'
        params.executionIds=eidparam

        when:
        def result=controller.apiProjectExport()

        then:
        1 * controller.apiService.requireApi(_,_) >> true
        1 * controller.projectService.exportProjectToOutputStream(_,_,_,_,{ ArchiveOptions opts ->
            opts.executionsOnly==true && opts.executionIds==(expectedset)
        },_)

        where:
        eidparam       | expectedset
        '123'          | ['123'] as Set
        '123,456'      | ['123', '456'] as Set
        ['123', '456'] | ['123', '456'] as Set
    }

    def "api export params"() {
        given:
        controller.projectService = Mock(ProjectService)
        controller.apiService = Mock(ApiService)
        controller.frameworkService = Mock(FrameworkService)
        setupGetResource()

        params.project = 'aproject'
        params.exportAll = all
        params.exportJobs = jobs
        params.exportExecutions = execs
        params.exportConfigs = configs
        params.exportReadmes = readmes
        params.exportAcls = acls

        when:
        def result = controller.apiProjectExport()

        then:
        1 * controller.apiService.requireApi(_, _) >> true
        1 * controller.projectService.exportProjectToOutputStream(_, _, _, _, { ArchiveOptions opts ->
            opts.executionsOnly == false &&
                    opts.all == all &&
                    opts.jobs == jobs &&
                    opts.executions == execs &&
                    opts.configs == configs &&
                    opts.readmes == readmes &&
                    opts.acls == acls
        },_
        )

        where:
        all  | jobs  | execs | configs | readmes | acls
        true | false | false | false   | false   | false
    }

    def "api v34 exportAll include webhooks auth tokens when whkIncludeAuthTokens is set to true"(){

        given:"a project to be exported"
        controller.projectService = Mock(ProjectService)
        controller.apiService = Mock(ApiService)
        controller.frameworkService = Mock(FrameworkService)
        setupGetResource()
        params.project = 'aproject'

        when:"exporting the project using the API"
        params.exportAll = "true"
        params.whkIncludeAuthTokens = "true"
        request.api_version = ApiVersions.V34
        controller.apiProjectExport()

        then:"webhooks auth tokens should be exported"
        response.status == 200
        1 * controller.apiService.requireApi(_, _) >> true
        1 * controller.projectService.exportProjectToOutputStream(_, _, _, _, { ArchiveOptions opts ->
                    opts.all == true &&
                    opts.exportOpts[WebhooksProjectComponent.COMPONENT_NAME]==[(WebhooksProjectExporter.INLUDE_AUTH_TOKENS):"true"]
        },_
        )
    }

    def "api export component params"() {
        given:
        controller.projectService = Mock(ProjectService)
        controller.apiService = Mock(ApiService)
        controller.frameworkService = Mock(FrameworkService)
        setupGetResource()

        params.project = 'aproject'
        params.exportAll = all
        params.exportJobs = jobs
        params.exportExecutions = execs
        params.exportConfigs = configs
        params.exportReadmes = readmes
        params.exportAcls = acls
        params.'exportComponents.testcomponent' = compBool.toString()
        params.'exportOpts.testcomponent.someoption'='avalue'
        request.api_version = 19

        when:
        def result = controller.apiProjectExport()

        then:
        1 * controller.apiService.requireApi(_, _) >> true
        1 * controller.projectService.exportProjectToOutputStream(_, _, _, _, { ArchiveOptions opts ->
            opts.executionsOnly == false &&
                    opts.all == all &&
                    opts.jobs == jobs &&
                    opts.executions == execs &&
                    opts.configs == configs &&
                    opts.readmes == readmes &&
                    opts.acls == acls &&
                    opts.exportComponents['testcomponent'] == compBool &&
                    opts.exportOpts['testcomponent']==[someoption:'avalue']
        },_
        )

        where:
        all  | jobs  | execs | configs | readmes | acls | compBool
        true | false | false | false   | false   | false | true
        true | false | false | false   | false   | false | false
    }

    @Unroll
    def "api export v34 compat webhook params"() {
        given:
        controller.projectService = Mock(ProjectService)
        controller.apiService = Mock(ApiService)
        controller.frameworkService = Mock(FrameworkService)
        setupGetResource()

        params.project = 'aproject'

        params.exportWebhooks=whenable.toString()
        params.whkIncludeAuthTokens=whinclude.toString()
        request.api_version = 34

        when:
        def result = controller.apiProjectExport()

        then:
        1 * controller.apiService.requireApi(_, _) >> true

        1 * controller.projectService.exportProjectToOutputStream(_, _, _, _, { ArchiveOptions opts ->
                    opts.exportComponents[WebhooksProjectComponent.COMPONENT_NAME] == whenable &&
                    opts.exportOpts[WebhooksProjectComponent.COMPONENT_NAME]==[(WebhooksProjectExporter.INLUDE_AUTH_TOKENS):whinclude.toString()]
        },_
        )

        where:
            whenable | whinclude
            true     | true
            true     | false
    }

    def "api project delete error"() {
        given:
        controller.projectService = Mock(ProjectService)
        controller.apiService = Mock(ApiService)
        controller.frameworkService = Mock(FrameworkService)
        setupAuthDelete()

        params.project = 'aproject'

        when:
        request.method = 'DELETE'
        def result = controller.apiProjectDelete()

        then:
        1 * controller.apiService.requireApi(_, _) >> true
        1 * controller.projectService.deleteProject(_, _, _, _, _) >> [success: false, error: 'message']
        1 * controller.apiService.renderErrorFormat(_, [
                status : 500,
                code   : 'api.error.unknown',
                message: 'message'
        ]
        )
    }

    def "api project delete deferred parameter behavior"() {
        given:
        controller.projectService = Mock(ProjectService)
        controller.apiService = Mock(ApiService)
        controller.frameworkService = Mock(FrameworkService)
        controller.configurationService = Mock(ConfigurationService) {
            getBoolean('projectService.deferredProjectDelete', _) >> configValue
        }
        setupAuthDelete()

        params.project = 'aproject'
        request.method = 'DELETE'
        request.api_version = apiVersion
        if(deferParamPresent) {
            params.deferred = deferredValue
        }

        when:
        def result = controller.apiProjectDelete()

        then:
        1 * controller.apiService.requireApi(_, _) >> true
        1 * controller.projectService.deleteProject(_, _, _, _, deferredResult) >> [success: true]

        where:
        apiVersion | deferParamPresent | deferredValue | configValue | deferredResult
        11         | false             | null          | true        | false
        11         | true              | "null"        | true        | false
        11         | true              | "false"       | true        | false
        11         | true              | "true"        | true        | false
        11         | false             | null          | false       | false
        11         | true              | "null"        | false       | false
        11         | true              | "false"       | false       | false
        11         | true              | "true"        | false       | false
        45         | false             | null          | true        | true
        45         | true              | "null"        | true        | false
        45         | true              | "false"       | true        | false
        45         | true              | "true"        | true        | true
        45         | false             | null          | false       | false
        45         | true              | "null"        | false       | false
        45         | true              | "false"       | false       | false
        45         | true              | "true"        | false       | true
    }

    def "export prepare"() {
        given:
        controller.projectService = Mock(ProjectService)
        controller.apiService = Mock(ApiService)
        controller.frameworkService = Mock(FrameworkService)
        controller.rundeckAppAuthorizer = Mock(AppAuthorizer)
        params.project = 'aproject'
        params.exportAll = all
        params.exportJobs = jobs
        params.exportExecutions = execs
        params.exportConfigs = configs
        params.exportReadmes = readmes
        params.exportAcls = acls
        session.subject = new Subject()
        when:
        def result = controller.exportPrepare()

        then:
        1 * controller.rundeckAppAuthorizer.project(_, _) >> Mock(AuthorizingProject) {
            _ * getAuthContext() >> Mock(UserAndRolesAuthContext) {
                _ * getUsername() >> 'auser'
            }
            1 * getResource() >> Stub(IRundeckProject){
                getName()>>'aproject'
            }
        }

        1 * controller.projectService.exportProjectToFileAsync(_, _, _, { ArchiveOptions opts ->
            opts.executionsOnly == false &&
                    opts.all == (all ?: false) &&
                    opts.jobs == (jobs ?: false) &&
                    opts.executions == (execs ?: false) &&
                    opts.configs == (configs ?: false) &&
                    opts.readmes == (readmes ?: false) &&
                    opts.acls == (acls ?: false)
        }, _
        ) >> 'dummytoken'
        1 * controller.projectService.validateAllProjectComponentExportOptions(_) >> [:]
        response.redirectedUrl ==  '/project/aproject/exportWait/dummytoken'

        where:
        all  | jobs  | execs | configs | readmes | acls
        true | false | false | false   | false   | false
        true | false | false | false   | false   | null
    }

    def "export wait response format json"() {
        given:
        controller.projectService = Mock(ProjectService)
        when:
        params.token = 'abc'
        response.format = 'json'
        controller.exportWait()
        then:
        1 * controller.projectService.hasPromise(_, 'abc') >> true
        1 * controller.projectService.promiseError(_, 'abc') >> null
        1 * controller.projectService.promiseReady(_, 'abc')
        1 * controller.projectService.promiseSummary(_, 'abc') >> Mock(ProgressSummary) {
            percent() >> 50
        }

        response.status == 200
        response.json == [token: 'abc', ready: false, percentage: 50]
    }

    def "export wait no token param format json"() {
        given:
        controller.projectService = Mock(ProjectService)
        when:
        params.token = ptoken
        response.format = 'json'
        controller.exportWait()
        then:


        response.status == 200
        response.json == [token: ptoken, errorMessage: 'token is required']

        where:
        ptoken | _
        null   | _

    }

    def "export wait missing token format json"() {
        given:
        controller.projectService = Mock(ProjectService)
        when:
        params.token = ptoken
        response.format = 'json'
        controller.exportWait()
        then:

        controller.projectService.hasPromise(_, ptoken) >> false

        response.status == 200
        response.json == [token: ptoken, notFound: true]

        where:
        ptoken | _
        'xyz'  | _

    }

    def "export wait error message format json"() {
        given:
        controller.projectService = Mock(ProjectService)
        when:
        params.token = ptoken
        response.format = 'json'
        controller.exportWait()
        then:

        controller.projectService.hasPromise(_, ptoken) >> true
        controller.projectService.promiseError(_, ptoken) >> new Exception("expected exception")

        response.status == 200
        response.json == [token: ptoken, errorMessage: 'Project export request failed: expected exception']

        where:
        ptoken | _
        'xyz'  | _

    }

    def "export wait remote error message format json"() {
        given:
        controller.projectService = Mock(ProjectService)
        when:
        params.token = ptoken
        params.instance = 'true'
        response.format = 'json'
        controller.exportWait()
        then:

        controller.projectService.hasPromise(_, ptoken) >> true
        controller.projectService.promiseError(_, ptoken) >> null
        controller.projectService.promiseResult(_, ptoken) >> new ImportResponse(
            ok: false,
            errors: ['a', 'b'],
            executionErrors: ['c', 'd'],
            aclErrors: ['e', 'f']
        )

        response.status == 200
        response.json == [token: ptoken, errors: ['a', 'b', 'c', 'd', 'e', 'f']]

        where:
        ptoken | _
        'xyz'  | _

    }

    def "api export execution ids async"() {
        given:
        controller.projectService = Mock(ProjectService)
        controller.apiService = Mock(ApiService)
        controller.frameworkService = Mock(FrameworkService)
        setupGetResource()
        params.project = 'aproject'
        params.executionIds = eidparam
        params.async = true

        when:
        def result = controller.apiProjectExport()

        then:
        1 * controller.apiService.requireApi(_, _) >> true
        1 * controller.apiService.requireApi(_, _, 19) >> true
        1 * controller.projectService.exportProjectToFileAsync(_, _, _, { ArchiveOptions opts ->
            opts.executionsOnly == true && opts.executionIds == (expectedset)
        },_
        ) >> 'atoken'
        1 * controller.projectService.promiseReady(_, 'atoken') >> null
        1 * controller.projectService.promiseSummary(_, 'atoken') >> Mock(ProgressSummary)

        where:
        eidparam | expectedset
        '123' | ['123'] as Set
        '123,456' | ['123','456'] as Set
        ['123','456'] | ['123','456'] as Set
    }

    def "api export async status"() {
        given:
        controller.projectService = Mock(ProjectService)
        controller.apiService = Mock(ApiService)
        controller.frameworkService = Mock(FrameworkService)
        params.project = 'aproject'
        params.token = 'atoken'
        params.async = true

        when:
        def result = controller.apiProjectExportAsyncStatus()

        then:
        1 * controller.apiService.requireApi(_, _, 19) >> true
        1 * controller.apiService.requireParameters(_, _, ['token']) >> true
        1 * controller.apiService.requireExists(_, true, ['Export Request Token', 'atoken']) >> true
        1 * controller.projectService.hasPromise(_, 'atoken') >> true
        1 * controller.projectService.promiseError(_, 'atoken') >> null
        1 * controller.projectService.promiseReady(_, 'atoken') >> null
        1 * controller.projectService.promiseSummary(_, 'atoken') >> Mock(ProgressSummary)

    }

    def "api export async download"() {
        given:
        controller.projectService = Mock(ProjectService)
        controller.apiService = Mock(ApiService)
        controller.frameworkService = Mock(FrameworkService)
        params.project = 'aproject'
        params.token = 'atoken'
        params.async = true
        def afile = File.createTempFile("project-async-export-test", "data")
        afile.text << 'test'


        when:
        def result = controller.apiProjectExportAsyncDownload()

        then:
        1 * controller.apiService.requireApi(_, _, 19) >> true
        1 * controller.apiService.requireParameters(_, _, ['token']) >> true
        1 * controller.apiService.requireExists(_, true, ['Export Request Token', 'atoken']) >> true
        1 * controller.projectService.hasPromise(_, 'atoken') >> true
        1 * controller.projectService.promiseReady(_, 'atoken') >> afile
        1 * controller.apiService.requireExists(_, afile, ['Export File for Token', 'atoken']) >> true
        1 * controller.projectService.promiseRequestStarted(_, 'atoken') >> new Date()
        1 * controller.projectService.releasePromise(_, 'atoken')
        response.getHeader('content-disposition') != null
        response.getContentType() == 'application/zip'

    }
    def "project file readme get not project param"(){
        given:
        params.filename="readme.md"
        controller.apiService=Mock(ApiService){
            1 * requireApi(_,_,13) >> true
            1 * requireApi(_,_) >> true
        }
        request.api_version=11

        when:
        def result=controller.apiProjectFileGet()

        then:
            1 * controller.rundeckExceptionHandler.handleException(_,_, _ as MissingParameter)
    }
    def "project file readme get project dne"(){
        given:
        params.filename="readme.md"
        params.project="test"
        controller.frameworkService=Mock(FrameworkService){
        }
        controller.apiService=Mock(ApiService){
            1 * requireApi(_,_,13) >> true
            1 * requireApi(_,_) >> true

        }
        setupAuthConfigure(true,false)
        request.api_version=11
        when:
        def result=controller.apiProjectFileGet()

        then:
            1 * controller.rundeckExceptionHandler.handleException(_,_, _ as NotFound)
    }
    def "project file readme get project not authorized"(){
        given:
        params.filename="readme.md"
        params.project="test"
        controller.frameworkService=Mock(FrameworkService){
        }
        controller.apiService=Mock(ApiService){
            1 * requireApi(_,_,13) >> true
            1 * requireApi(_,_) >> true
        }
        setupAuthConfigure(false)
        request.api_version=11
        when:
        def result=controller.apiProjectFileGet()

        then:
            1 * controller.rundeckExceptionHandler.handleException(_,_, _ as UnauthorizedAccess)
    }
    def "project file readme get project authorized wrong filename"(){
        given:
        params.filename="wrong.md"
        params.project="test"
        controller.frameworkService=Mock(FrameworkService)
        setupAuthConfigure()
        request.api_version=11
        controller.apiService=Mock(ApiService){
            1 * requireApi(_,_,13) >> true
            1 * requireApi(_,_) >> true
            1 * renderErrorFormat(_,{it.code=='api.error.item.doesnotexist' && it.args==['resource','wrong.md']})
        }
        when:
        def result=controller.apiProjectFileGet()

        then:
        null==result
    }
    def "project file readme get not found"(){
        given:
        params.filename="readme.md"
        params.project="test"
        controller.frameworkService=Mock(FrameworkService)
        setupAuthConfigure(true,true,'test',Mock(IRundeckProject){
            1 * existsFileResource('readme.md') >> false
        })
        request.api_version=11
        controller.apiService=Mock(ApiService){
            1 * requireApi(_,_,13) >> true
            1 * requireApi(_,_) >> true
            1 * renderErrorFormat(_,{it.code=='api.error.item.doesnotexist' && it.args==['resource','readme.md']})
        }
        when:
        def result=controller.apiProjectFileGet()

        then:
        null==result
    }
    def "project file GET text format"(){
        given:
        params.filename="readme.md"
        params.project="test"
        controller.frameworkService=Mock(FrameworkService)
        controller.apiService=Mock(ApiService){
            1 * requireApi(_,_,13) >> true
            1 * requireApi(_,_) >> true
            1 * extractResponseFormat(_,_,_,_) >> 'text'
        }
        setupAuthConfigure(true,true,'test',Mock(IRundeckProject){
            1 * existsFileResource('readme.md') >> true
            1 * loadFileResource('readme.md',!null)
        })
        request.api_version=11
        when:
        def result=controller.apiProjectFileGet()

        then:
        response.contentType=='text/plain'
    }
    def "project file GET xml format"(String filename,String text){
        given:
        controller.frameworkService=Mock(FrameworkService)

        controller.apiService=Mock(ApiService){
            1 * requireApi(_,_,13) >> true
            1 * requireApi(_,_) >> true
            1 * extractResponseFormat(_,_,_,_) >> 'xml'
            1 * renderSuccessXml(_,_,_) >> text
        }
        setupAuthConfigure(true,true,'test',Mock(IRundeckProject){
            1 * existsFileResource(filename) >> true
            1 * loadFileResource(filename,!null)
        })
        request.api_version=11
        when:
        params.filename=filename
        params.project="test"
        def result=controller.apiProjectFileGet()

        then:
        result==text

        where:
        filename    | text
        'readme.md' | 'test'
        'motd.md'   | 'test2'
    }
    def "project file GET json format"(String filename,String text){
        setup:
        controller.frameworkService=Mock(FrameworkService)

        controller.apiService=Mock(ApiService){
            1 * requireApi(_,_,13) >> true
            1 * requireApi(_,_) >> true
            1 * extractResponseFormat(_,_,_,_) >> 'json'
        }
        setupAuthConfigure(true,true,'test',Stub(IRundeckProject){
            existsFileResource(filename) >> true
            loadFileResource(filename,_) >> {args->
                args[1].write(text.bytes)
                text.length()
            }
        })
        request.api_version=11
        when:
        params.filename=filename
        params.project="test"
        def result=controller.apiProjectFileGet()

        then:
        response.contentType==~/^application\/json(;.+)?$/
        response.json==[contents:text]

        where:
        filename    | text
        'readme.md' | 'test'
        'motd.md'   | 'test2'
    }


    def "project file delete"(String filename){
        given:
        controller.frameworkService=Mock(FrameworkService)
        controller.apiService=Mock(ApiService){
            1 * requireApi(_,_,13) >> true
            1 * requireApi(_,_) >> true
            1 * extractResponseFormat(_,_,_) >> 'xml'
        }
        setupAuthConfigure(true,true,'test',Mock(IRundeckProject){
            1 * deleteFileResource(filename) >> true
        })
        request.api_version=11
        when:
        params.filename=filename
        params.project="test"
        request.method='DELETE'
        def result=controller.apiProjectFileDelete()

        then:
        response.status==204

        where:
        filename    | _
        'readme.md' | _
        'motd.md'   | _
    }


    def "project file delete wrong method"(String filename,String method){

        when:
        params.filename=filename
        params.project="test"
        request.method=method
        def result=controller.apiProjectFileDelete()

        then:
        response.status==405

        where:
        filename    | method
        'readme.md' | 'GET'
        'readme.md' | 'PUT'
        'readme.md' | 'POST'
        'motd.md'   | 'GET'
        'motd.md'   | 'PUT'
        'motd.md'   | 'POST'
    }
    private setupAuthProjectFilePut(String filename,String text){
        setupAuthConfigure(true,true,'test',Mock(IRundeckProject){
            1 * storeFileResource(filename,{args->
                byte[] bar=new byte[1024]
                def len=args.read(bar)
                text == new String(bar,0,len)
            }) >> text.length()

            1 * loadFileResource(filename,_) >> {args->
                args[1].write(text.bytes)
                text.length()
            }
        })
    }

    def "project file PUT json"(String filename,String text){
        given:
        controller.frameworkService=Mock(FrameworkService)
        controller.apiService=Mock(ApiService){
            1 * requireApi(_,_,13) >> true
            1 * requireApi(_,_) >> true
            1 * extractResponseFormat(*_) >> 'xml'
            1 * parseJsonXmlWith(*_) >> {args->
                args[2].json.call(args[0].JSON)
                true
            }
        }
        setupAuthProjectFilePut(filename,text)
        request.api_version=11
        when:
        params.filename=filename
        params.project="test"
        request.method='PUT'
        request.format='json'
        request.json=[contents:text]
        def result=controller.apiProjectFilePut()

        then:
        response.status==200

        where:
        filename    | text
        'readme.md' | 'test'
        'motd.md'   | 'test2'
    }
    def "project file PUT xml"(String filename,String text){
        given:
        controller.frameworkService=Mock(FrameworkService)
        controller.apiService=Mock(ApiService){
            1 * requireApi(_,_,13) >> true
            1 * requireApi(_,_) >> true
            1 * extractResponseFormat(*_) >> 'xml'
            1 * parseJsonXmlWith(_,_,_) >> {args->
                args[2].xml.call(args[0].XML)
                true
            }
        }
        setupAuthProjectFilePut(filename,text)
        request.api_version=11
        when:
        params.filename=filename
        params.project="test"
        request.method='PUT'
        request.format='xml'
        request.content=('<contents>'+text+'</contents>').bytes

        def result=controller.apiProjectFilePut()

        then:
        response.status==200

        where:
        filename    | text
        'readme.md' | 'test'
        'motd.md'   | 'test2'
    }
    def "project file PUT text"(String filename,String text){
        given:
        controller.frameworkService=Mock(FrameworkService)
        controller.apiService=Mock(ApiService){
            1 * requireApi(_,_,13) >> true
            1 * requireApi(_,_) >> true
            1 * extractResponseFormat(*_) >> 'xml'
            1 * renderSuccessXml(*_)

        }
        setupAuthProjectFilePut(filename,text)
        request.api_version=11
        when:
        params.filename=filename
        params.project="test"
        request.method='PUT'
        request.format='text'
        request.content=text.bytes
        def result=controller.apiProjectFilePut()

        then:
        response.status==200

        where:
        filename    | text
        'readme.md' | 'test'
        'motd.md'   | 'test2'
    }


    def "project acls require api_version 14"(){
        setup:
        controller.apiService=Mock(ApiService){
            1 * requireApi(_,_,14) >> {args->
                args[1].status=400
                false
            }
        }
        when:
        controller.apiProjectAcls()

        then:
        response.status==400
    }
    def "project acls require project parameter"(){
        setup:
        controller.apiService=Mock(ApiService){
            1 * requireApi(_,_,14) >> true
            1 * requireApi(_,_) >> true
            1 * renderErrorFormat(_,[status:400,code:'api.error.parameter.required',args:['project']]) >> {args->
                args[0].status=args[1].status
            }
        }
        when:
        controller.apiProjectAcls()

        then:
        response.status==400
    }
    def "project acls project not found"(){
        setup:
        controller.apiService=Mock(ApiService){
            1 * requireApi(_,_,14) >> true
            1 * requireApi(_,_) >> true
            1 * renderErrorFormat(_,[status:404,code:'api.error.item.doesnotexist',args:['Project','monkey']]) >> {args->
                args[0].status=args[1].status
            }
        }
        controller.frameworkService=Mock(FrameworkService){
            1 * existsFrameworkProject('monkey') >> false
        }
        when:
        params.project='monkey'
        controller.apiProjectAcls()

        then:
        response.status==404
    }
    @Unroll
    def "project acls not authorized"(){
        setup:
        controller.apiService=Mock(ApiService){
            1 * requireApi(_,_,14) >> true
            1 * requireApi(_,_) >> true
            1 * renderErrorFormat(_,[status:403,code:'api.error.item.unauthorized',args:[action,'ACL for Project', 'monkey']]) >> {args->
                args[0].status=args[1].status
            }
        }
            controller.rundeckAuthContextProcessor = Mock(AppAuthContextProcessor){
                1 * getAuthContextForSubject(_) >> null
                1 * authResourceForProjectAcl('monkey') >> [type:'project_acl',name:'monkey']
                1 * authorizeApplicationResourceAny(null,[type:'project_acl',name:'monkey'],[action,AuthConstants.ACTION_ADMIN, AuthConstants.ACTION_APP_ADMIN])>>false
            }
        controller.frameworkService=Mock(FrameworkService){
            1 * existsFrameworkProject('monkey') >> true
        }
        when:
        params.project='monkey'
        request.method=method
        controller.apiProjectAcls()

        then:
        response.status==403

        where:
        method | action
        'GET' | ACTION_READ
        'POST' | ACTION_CREATE
        'PUT' | ACTION_UPDATE
        'DELETE' | ACTION_DELETE
    }
    def "project acls invalid path"(){
        setup:
        controller.apiService=Mock(ApiService){
            1 * requireApi(_,_,14) >> true
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

            controller.rundeckAuthContextProcessor = Mock(AppAuthContextProcessor){
                1 * getAuthContextForSubject(_) >> null

                1 * authResourceForProjectAcl('monkey') >> null
                1 * authorizeApplicationResourceAny(null,null,[ACTION_READ,AuthConstants.ACTION_ADMIN, AuthConstants.ACTION_APP_ADMIN])>>true
            }
        controller.frameworkService=Mock(FrameworkService){
            1 * existsFrameworkProject('monkey') >> true
            1 * getFrameworkProject('monkey') >> Stub(IRundeckProject)
        }
        when:
        params.path='elf'
        params.project='monkey'
        controller.apiProjectAcls()

        then:
        response.status==400
    }
    def "project acls GET 404"(){
        setup:
            controller.rundeckAuthContextProcessor = Mock(AppAuthContextProcessor){
                1 * getAuthContextForSubject(_) >> null
                1 * authResourceForProjectAcl('test') >> null
                1 * authorizeApplicationResourceAny(_,_,[ACTION_READ,AuthConstants.ACTION_ADMIN, AuthConstants.ACTION_APP_ADMIN]) >> true
            }
        controller.frameworkService=Mock(FrameworkService){
            1 * existsFrameworkProject('test') >> true
            1 * getFrameworkProject('test') >> Stub(IRundeckProject){
                existsFileResource(_) >> false
                existsDirResource(_) >> false

            }
        }
        def ctx = AppACLContext.project('test')
        controller.aclFileManagerService=Mock(AclFileManagerService){
            _* existsPolicyFile(ctx,_)>>false
        }
        controller.apiService=Mock(ApiService){
            1 * requireApi(_,_,14) >> true
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
        def result=controller.apiProjectAcls()

        then:
        response.status==404
    }
    def "project acls GET json"(){
        setup:
            controller.rundeckAuthContextProcessor = Mock(AppAuthContextProcessor){
                1 * getAuthContextForSubject(_) >> null
                1 * authResourceForProjectAcl('test') >> null
                1 * authorizeApplicationResourceAny(_,_,[AuthConstants.ACTION_READ, AuthConstants.ACTION_ADMIN, AuthConstants.ACTION_APP_ADMIN]) >> true
            }
            controller.frameworkService=Mock(FrameworkService){
                1 * existsFrameworkProject('test') >> true
                1 * getFrameworkProject('test') >> Mock(IRundeckProject){
                    _* getName()>>'test'
                    0 * _(*_)
                }
            }
            def ctx = AppACLContext.project('test')
            controller.aclFileManagerService=Mock(AclFileManagerService){
                1* existsPolicyFile(ctx,'blah.aclpolicy')>>true
                1 * loadPolicyFileContents(ctx,'blah.aclpolicy',_)>>{args->
                    args[2].write('blah'.bytes)
                    4
                }
            }
        controller.apiService=Mock(ApiService){
            1 * requireApi(_,_,14) >> true
            1 * requireApi(_,_) >> true
            1 * extractResponseFormat(_,_,_,_) >> 'json'
        }
        when:
        params.path='blah.aclpolicy'
        params.project="test"
        response.format='json'
        def result=controller.apiProjectAcls()

        then:
        response.status==200
        response.contentType?.split(';')?.contains('application/json')
        response.json==[contents:"blah"]
    }
    def "project acls GET unsupported format"(){
        setup:
            controller.rundeckAuthContextProcessor = Mock(AppAuthContextProcessor){
                1 * getAuthContextForSubject(_) >> null
                1 * authResourceForProjectAcl('test') >> null
                1 * authorizeApplicationResourceAny(_,_,[AuthConstants.ACTION_READ, AuthConstants.ACTION_ADMIN, AuthConstants.ACTION_APP_ADMIN]) >> true
            }
            controller.frameworkService=Mock(FrameworkService){
                1 * existsFrameworkProject('test') >> true
                1 * getFrameworkProject('test') >> Mock(IRundeckProject){
                    _* getName()>>'test'
                    0 * _(*_)
                }
            }
            def ctx = AppACLContext.project('test')
            controller.aclFileManagerService=Mock(AclFileManagerService){
                1* existsPolicyFile(ctx,'blah.aclpolicy')>>true
                0 * loadPolicyFileContents(ctx,'blah.aclpolicy',_)
            }
        controller.apiService=Mock(ApiService){
            1 * requireApi(_,_,14) >> true
            1 * requireApi(_,_) >> true
            1 * extractResponseFormat(_,_,_,_) >> {it[3]}
            1 * renderErrorFormat(_,[status:406,code:'api.error.resource.format.unsupported',args:['jambajuice']])>>{it[0].status=it[1].status}
            0 * _(*_)
        }
        when:
        params.path='blah.aclpolicy'
        params.project="test"
        response.format='jambajuice'
        def result=controller.apiProjectAcls()

        then:
        response.status==406
    }
    def "project acls GET default format"(){
        setup:
            controller.rundeckAuthContextProcessor = Mock(AppAuthContextProcessor){
                1 * getAuthContextForSubject(_) >> null
                1 * authResourceForProjectAcl('test') >> null
                1 * authorizeApplicationResourceAny(_,_,[AuthConstants.ACTION_READ, AuthConstants.ACTION_ADMIN, AuthConstants.ACTION_APP_ADMIN]) >> true
            }
            controller.frameworkService=Mock(FrameworkService){
                1 * existsFrameworkProject('test') >> true
                1 * getFrameworkProject('test') >> Mock(IRundeckProject){
                    _* getName()>>'test'
                    0 * _(*_)
                }
            }
            def ctx = AppACLContext.project('test')
            controller.aclFileManagerService=Mock(AclFileManagerService){
                1* existsPolicyFile(ctx,'blah.aclpolicy')>>true
                1 * loadPolicyFileContents(ctx,'blah.aclpolicy',_)>>{args->
                    args[2].write('blah'.bytes)
                    4
                }
            }
        controller.apiService=Mock(ApiService){
            1 * requireApi(_,_,14) >> true
            1 * requireApi(_,_) >> true
            1 * extractResponseFormat(_,_,_,_) >> {it[3]}
            0 * _(*_)
        }
        when:
        params.path='blah.aclpolicy'
        params.project="test"
        def result=controller.apiProjectAcls()

        then:
        response.status==200
        response.contentType.split(';').contains('application/json')
        response.json==[contents:"blah"]
    }
    def "project acls GET xml"(){
        setup:
            controller.rundeckAuthContextProcessor = Mock(AppAuthContextProcessor){
                1 * getAuthContextForSubject(_) >> null
                1 * authResourceForProjectAcl('test') >> null
                1 * authorizeApplicationResourceAny(_,_,[AuthConstants.ACTION_READ, AuthConstants.ACTION_ADMIN, AuthConstants.ACTION_APP_ADMIN]) >> true
            }
            controller.frameworkService=Mock(FrameworkService){
                1 * existsFrameworkProject('test') >> true
                1 * getFrameworkProject('test') >> Mock(IRundeckProject){
                    _* getName()>>'test'
                    0 * _(*_)
                }
            }
            def ctx = AppACLContext.project('test')
            controller.aclFileManagerService=Mock(AclFileManagerService){
                1* existsPolicyFile(ctx,'blah.aclpolicy')>>true
                1 * loadPolicyFileContents(ctx,'blah.aclpolicy',_)>>{args->
                    args[2].write('blah'.bytes)
                    4
                }
            }
        controller.apiService=Mock(ApiService){
            1 * requireApi(_,_,14) >> true
            1 * requireApi(_,_) >> true
            1 * extractResponseFormat(_,_,_,_) >> 'xml'
            1 * renderWrappedFileContentsXml('blah','xml',_) >> {args-> args[2]}
        }
        when:
        params.path='blah.aclpolicy'
        params.project="test"
        response.format='xml'
        controller.apiProjectAcls()

        then:
        response.status==200
        response.contentType.split(';').contains('application/xml')
    }
    def "project acls GET text/yaml"(String respFormat, String contentType){
        setup:
            controller.rundeckAuthContextProcessor = Mock(AppAuthContextProcessor){
                1 * getAuthContextForSubject(_) >> null
                1 * authResourceForProjectAcl('test') >> null
                1 * authorizeApplicationResourceAny(_,_,[AuthConstants.ACTION_READ, AuthConstants.ACTION_ADMIN, AuthConstants.ACTION_APP_ADMIN]) >> true
            }
            controller.frameworkService=Mock(FrameworkService){
                1 * existsFrameworkProject('test') >> true
                1 * getFrameworkProject('test') >> Mock(IRundeckProject){
                    _* getName()>>'test'
                    0 * _(*_)
                }
            }
            def ctx = AppACLContext.project('test')
            controller.aclFileManagerService=Mock(AclFileManagerService){
                1* existsPolicyFile(ctx,'blah.aclpolicy')>>true
                1 * loadPolicyFileContents(ctx,'blah.aclpolicy',_)>>{args->
                    args[2].write('blah'.bytes)
                    4
                }
            }
        controller.apiService=Mock(ApiService){
            1 * requireApi(_,_,14) >> true
            1 * requireApi(_,_) >> true
            1 * extractResponseFormat(_,_,_,_) >> respFormat
        }
        when:
        params.path='blah.aclpolicy'
        params.project="test"
        def result=controller.apiProjectAcls()

        then:
        response.status==200
        response.contentType.split(';').contains(contentType)
        response.contentAsString=='blah'

        where:
        respFormat | contentType
        'text'     | 'text/plain'
        'yaml'     | 'application/yaml'
    }
    def "project acls GET dir JSON"(){
        setup:
            controller.rundeckAuthContextProcessor = Mock(AppAuthContextProcessor){
                1 * getAuthContextForSubject(_) >> null
                1 * authResourceForProjectAcl('test') >> null
                1 * authorizeApplicationResourceAny(_,_,[AuthConstants.ACTION_READ, AuthConstants.ACTION_ADMIN, AuthConstants.ACTION_APP_ADMIN]) >> true
            }
            controller.frameworkService=Mock(FrameworkService){
                1 * existsFrameworkProject('test') >> true
                1 * getFrameworkProject('test') >> Mock(IRundeckProject){
                    _* getName()>>'test'
                    0 * _(*_)
                }
            }
            def ctx = AppACLContext.project('test')
            controller.aclFileManagerService=Mock(AclFileManagerService){
                1* listStoredPolicyFiles(ctx)>>['blah.aclpolicy']
            }
            controller.apiService=Mock(ApiService){
                1 * requireApi(_,_,14) >> true
                1 * requireApi(_,_) >> true
                1 * jsonRenderDirlist('',_,_,['blah.aclpolicy']) >> {args->
                    [success: true]
                }
                0*_(*_)

            }
        when:
        params.path=''
        params.project="test"
        response.format='json'
        def result=controller.apiProjectAcls()

        then:
        response.status==200
        response.contentType.split(';').contains('application/json')
        response.json==[success:true]

    }
    def "project acls GET dir XML"(){
        setup:
            controller.rundeckAuthContextProcessor = Mock(AppAuthContextProcessor){
                1 * getAuthContextForSubject(_) >> null
                1 * authResourceForProjectAcl('test') >> null
                1 * authorizeApplicationResourceAny(_,_,[AuthConstants.ACTION_READ, AuthConstants.ACTION_ADMIN, AuthConstants.ACTION_APP_ADMIN]) >> true
            }
            controller.frameworkService=Mock(FrameworkService){
                1 * existsFrameworkProject('test') >> true
                1 * getFrameworkProject('test') >> Mock(IRundeckProject){
                    _* getName()>>'test'
                    0 * _(*_)
                }
            }
            def ctx = AppACLContext.project('test')
            controller.aclFileManagerService=Mock(AclFileManagerService){
                1* listStoredPolicyFiles(ctx)>>['blah.aclpolicy']
            }
        controller.apiService=Mock(ApiService){
            1 * requireApi(_,_,14) >> true
            1 * requireApi(_,_) >> true
            1 * xmlRenderDirList('',_,_,['blah.aclpolicy'],_)
            0*_(*_)
        }
        when:
        params.path=''
        params.project="test"
        response.format='xml'
        def result=controller.apiProjectAcls()

        then:
        response.status==200
        response.contentType.split(';').contains('application/xml')
    }
    def "project acls POST text"(){
        setup:
            controller.rundeckAuthContextProcessor = Mock(AppAuthContextProcessor){
                1 * getAuthContextForSubject(_) >> null
                1 * authResourceForProjectAcl('test') >> null
                1 * authorizeApplicationResourceAny(_,_,[AuthConstants.ACTION_CREATE, AuthConstants.ACTION_ADMIN, AuthConstants.ACTION_APP_ADMIN]) >> true
            }

            controller.frameworkService=Mock(FrameworkService){
                1 * existsFrameworkProject('test') >> true
                1 * getFrameworkProject('test') >> Mock(IRundeckProject){
                    _* getName()>>'test'
                    0 * _(*_)
                }
            }
            def ctx = AppACLContext.project('test')
            controller.aclFileManagerService=Mock(AclFileManagerService){
                1* existsPolicyFile(ctx,'test.aclpolicy')>>false
                1* storePolicyFileContents(ctx,'test.aclpolicy',_)>>4

                1 * loadPolicyFileContents(ctx,'test.aclpolicy',_)>>{args->
                    args[2].write('blah'.bytes)
                    4
                }
                1 * validateYamlPolicy(ctx, 'test.aclpolicy', _)>>Stub(RuleSetValidation){
                    isValid()>>true
                }
                0*_(*_)

            }
        controller.apiService=Mock(ApiService){
            1 * requireApi(_,_,14) >> true
            1 * requireApi(_,_) >> true
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
        def result=controller.apiProjectAcls()

        then:
        response.status==201
        response.contentType.split(';').contains('application/json')
        response.json==[contents:'blah']


    }
    def "project acls POST text, invalid policy, json response"(){
        setup:
            controller.rundeckAuthContextProcessor = Mock(AppAuthContextProcessor){
                1 * getAuthContextForSubject(_) >> null
                1 * authResourceForProjectAcl('test') >> null
                1 * authorizeApplicationResourceAny(_,_,[AuthConstants.ACTION_CREATE, AuthConstants.ACTION_ADMIN, AuthConstants.ACTION_APP_ADMIN]) >> true
            }
            controller.apiService=Mock(ApiService){
                1 * requireApi(_,_,14) >> true
                1 * requireApi(_,_) >> true
                1 * extractResponseFormat(_,_,_,_) >> 'json'
                1 * renderJsonAclpolicyValidation(_)>>{args-> [contents: 'blah']}
            }

            controller.frameworkService=Mock(FrameworkService){
                1 * existsFrameworkProject('test') >> true
                1 * getFrameworkProject('test') >> Mock(IRundeckProject){
                    _* getName()>>'test'
                    0 * _(*_)
                }
            }
            def ctx = AppACLContext.project('test')
            controller.aclFileManagerService=Mock(AclFileManagerService){
                1* existsPolicyFile(ctx,'test.aclpolicy')>>false
                0* storePolicyFileContents(ctx,'test.aclpolicy','blah')
                1 * validateYamlPolicy(ctx, 'test.aclpolicy', _)>>Stub(RuleSetValidation){
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
        def result=controller.apiProjectAcls()

        then:
        response.status==400
        response.contentType.split(';').contains('application/json')
        response.json==[contents:'blah']


    }
    def "project acls POST text, invalid policy, xml response"(){
        setup:
            controller.rundeckAuthContextProcessor = Mock(AppAuthContextProcessor){
                1 * getAuthContextForSubject(_) >> null
                1 * authResourceForProjectAcl('test') >> null
                1 * authorizeApplicationResourceAny(_,_,[AuthConstants.ACTION_CREATE, AuthConstants.ACTION_ADMIN, AuthConstants.ACTION_APP_ADMIN]) >> true
            }
            controller.apiService=Mock(ApiService){
                1 * requireApi(_,_,14) >> true
                1 * requireApi(_,_) >> true
                1 * extractResponseFormat(_,_,_,_) >> 'xml'
                1 * renderXmlAclpolicyValidation(_,_)>>{args->args[1].contents('data')}
            }

            controller.frameworkService=Mock(FrameworkService){
                1 * existsFrameworkProject('test') >> true
                1 * getFrameworkProject('test') >> Mock(IRundeckProject){
                    _* getName()>>'test'
                    0 * _(*_)
                }
            }
            def ctx = AppACLContext.project('test')
            controller.aclFileManagerService=Mock(AclFileManagerService){
                1* existsPolicyFile(ctx,'test.aclpolicy')>>false
                0* storePolicyFileContents(ctx,'test.aclpolicy','blah')
                1 * validateYamlPolicy(ctx, 'test.aclpolicy', _)>>Stub(RuleSetValidation){
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
        def result=controller.apiProjectAcls()

        then:
        response.status==400
        response.contentType.split(';').contains('application/xml')
        response.xml!=null
        response.xml.text()=='data'


    }
    def "project acls PUT not found"(){
        setup:
            controller.rundeckAuthContextProcessor = Mock(AppAuthContextProcessor){
                1 * getAuthContextForSubject(_) >> null
                1 * authResourceForProjectAcl('test') >> null
                1 * authorizeApplicationResourceAny(_,_,[AuthConstants.ACTION_UPDATE, AuthConstants.ACTION_ADMIN, AuthConstants.ACTION_APP_ADMIN]) >> true
            }
        controller.apiService=Mock(ApiService){
            1 * requireApi(_,_,14) >> true
            1 * requireApi(_,_) >> true
            1 * extractResponseFormat(_,_,_,_) >> 'json'
            1 * renderErrorFormat(
                    _,
                    [status: 404,
                     code: 'api.error.item.doesnotexist',
                     args: ['Project ACL Policy File', 'test.aclpolicy for project test'],
                     format: 'json'] ) >> { args ->
                args[0].status = args[1].status
            }
        }


            controller.frameworkService=Mock(FrameworkService){
                1 * existsFrameworkProject('test') >> true
                1 * getFrameworkProject('test') >> Mock(IRundeckProject){
                    _* getName()>>'test'
                    0 * _(*_)
                }
            }
            def ctx = AppACLContext.project('test')
            controller.aclFileManagerService=Mock(AclFileManagerService){
                1* existsPolicyFile(ctx,'test.aclpolicy')>>false
                0* storePolicyFileContents(ctx,'test.aclpolicy','blah')
                0 * getValidator()
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
        def result=controller.apiProjectAcls()

        then:
        response.status==404

    }

    def "project acls PUT text ok"(){
        setup:
            controller.rundeckAuthContextProcessor = Mock(AppAuthContextProcessor){
                1 * getAuthContextForSubject(_) >> null
                1 * authResourceForProjectAcl('test') >> null
                1 * authorizeApplicationResourceAny(_,_,[AuthConstants.ACTION_UPDATE, AuthConstants.ACTION_ADMIN, AuthConstants.ACTION_APP_ADMIN]) >> true
            }
            controller.apiService=Mock(ApiService){
                1 * requireApi(_,_,14) >> true
                1 * requireApi(_,_) >> true
                1 * extractResponseFormat(_,_,_,_) >> 'json'
            }



            controller.frameworkService=Mock(FrameworkService){
                1 * existsFrameworkProject('test') >> true
                1 * getFrameworkProject('test') >> Mock(IRundeckProject){
                    _* getName()>>'test'
                    0 * _(*_)
                }
            }
            def ctx = AppACLContext.project('test')
            controller.aclFileManagerService=Mock(AclFileManagerService){
                1* existsPolicyFile(ctx,'test.aclpolicy')>>true
                1* storePolicyFileContents(ctx,'test.aclpolicy',_)>>1L

                1 * loadPolicyFileContents(ctx,'test.aclpolicy',_)>>{args->
                    args[2].write('blah'.bytes)
                    4
                }
                1 * validateYamlPolicy(ctx, 'test.aclpolicy', _)>>Stub(RuleSetValidation){
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
        def result=controller.apiProjectAcls()

        then:
        response.status==200
        response.contentType.split(';').contains('application/json')
        response.json==[contents:'blah']


    }
    def "project acls DELETE not found"(){
        setup:
            controller.rundeckAuthContextProcessor = Mock(AppAuthContextProcessor){
                1 * getAuthContextForSubject(_) >> null
                1 * authResourceForProjectAcl('test') >> null
                1 * authorizeApplicationResourceAny(_,_,[AuthConstants.ACTION_DELETE, AuthConstants.ACTION_ADMIN, AuthConstants.ACTION_APP_ADMIN]) >> true
            }

            controller.frameworkService=Mock(FrameworkService){
                1 * existsFrameworkProject('test') >> true
                1 * getFrameworkProject('test') >> Mock(IRundeckProject){
                    _* getName()>>'test'
                    0 * _(*_)
                }
            }
            def ctx = AppACLContext.project('test')
            controller.aclFileManagerService=Mock(AclFileManagerService){
                1* existsPolicyFile(ctx,'test.aclpolicy')>>false
                0* deletePolicyFile(ctx,'test.aclpolicy')
            }
        controller.apiService=Mock(ApiService){
            1 * requireApi(_,_,14) >> true
            1 * requireApi(_,_) >> true
            1 * extractResponseFormat(_,_,_,_) >> 'json'
            1 * renderErrorFormat(
                    _,
                    [status: 404,
                     code: 'api.error.item.doesnotexist',
                     args: ['Project ACL Policy File', 'test.aclpolicy for project test'],
                     format: 'json'] ) >> { args ->
                args[0].status = args[1].status
            }
        }

        when:
        params.path='test.aclpolicy'
        params.project="test"
        response.format='json'
        request.method='DELETE'
        def result=controller.apiProjectAcls()

        then:
        response.status==404

    }
    def "project acls DELETE ok"(){
        setup:
            controller.rundeckAuthContextProcessor = Mock(AppAuthContextProcessor){
                1 * getAuthContextForSubject(_) >> null
                1 * authResourceForProjectAcl('test') >> null
                1 * authorizeApplicationResourceAny(_,_,[AuthConstants.ACTION_DELETE, AuthConstants.ACTION_ADMIN, AuthConstants.ACTION_APP_ADMIN]) >> true
            }
            controller.frameworkService=Mock(FrameworkService){
                1 * existsFrameworkProject('test') >> true
                1 * getFrameworkProject('test') >> Mock(IRundeckProject){
                    _* getName()>>'test'
                    0 * _(*_)
                }
            }
            def ctx = AppACLContext.project('test')
            controller.aclFileManagerService=Mock(AclFileManagerService){
                1* existsPolicyFile(ctx,'test.aclpolicy')>>true
                1* deletePolicyFile(ctx,'test.aclpolicy')>>true
            }
        controller.apiService=Mock(ApiService){
            1 * requireApi(_,_,14) >> true
            1 * requireApi(_,_) >> true
            1 * extractResponseFormat(_,_,_,_) >> 'json'

        }

        when:
        params.path='test.aclpolicy'
        params.project="test"
        response.format='json'
        request.method='DELETE'
        def result=controller.apiProjectAcls()

        then:
        response.status==204

    }

    def "api v35 import archive webhooks error has detail response json"(){
        setup:
            setupGetResource()
            controller.rundeckAuthContextProcessor = Mock(AppAuthContextProcessor){
                1 * getAuthContextForSubjectAndProject(_,'test') >> null

            }
            controller.frameworkService=Mock(FrameworkService){
                1 * getRundeckFramework() >> null

                0 * _(*_)
            }
            controller.projectService=Mock(ProjectService){
                1*importToProject(_,_,_,_, {
                    it.importComponents == [(WebhooksProjectComponent.COMPONENT_NAME): true]
                }
                ) >> [success: false, importerErrors: ['err1', 'err2']]

                0 * _(*_)
            }
            controller.apiService=Mock(ApiService){
                1 * requireApi(_, _) >> true
                1 * requireRequestFormat(_, _, _) >> true
                1 * extractResponseFormat(_, _, _, _) >> 'json'
            }

            params.project="test"
            params.importWebhooks='true'
            response.format='json'
            request.method='PUT'

            request.content='test'.bytes
            request.api_version=35
        when:

            def result=controller.apiProjectImport()

        then:
            response.contentType.contains 'application/json'
            response.status==200
            response.json.import_status=='failed'
            response.json.successful==false
            response.json.other_errors==['err1','err2']
    }

    def "api v35 import archive webhooks error has detail response xml"(){
        setup:

            setupGetResource()
            controller.rundeckAuthContextProcessor = Mock(AppAuthContextProcessor){
                1 * getAuthContextForSubjectAndProject(_,'test') >> null
            }
            controller.frameworkService=Mock(FrameworkService){
                1 * getRundeckFramework() >> null

                0 * _(*_)
            }
            controller.projectService=Mock(ProjectService){
                1*importToProject(_,_,_,_, {
                    it.importComponents == [(WebhooksProjectComponent.COMPONENT_NAME): true]
                }
                ) >> [success: false, importerErrors: ['err1', 'err2'], joberrors:[]]

                0 * _(*_)
            }
            controller.apiService=Mock(ApiService){
                1 * requireApi(_, _) >> true
                1 * requireRequestFormat(_, _, _) >> true
                1 * extractResponseFormat(_, _, _, _) >> 'xml'
                1 * renderSuccessXml(_, _, _) >> { args ->
                    def writer = new StringWriter()
                    def xml = new MarkupBuilder(writer)
                    def response = args[1]
                    def recall = args[2]
                    xml.with {
                        recall.delegate = delegate
                        recall.resolveStrategy = Closure.DELEGATE_FIRST
                        recall()
                    }
                    def xmlstr = writer.toString()
                    response.setContentType('application/xml')
                    response.setCharacterEncoding('UTF-8')
                    def out = response.outputStream
                    out << xmlstr
                    out.flush()
                }
            }

            params.project="test"
            params.importWebhooks='true'
            response.format='xml'
            request.method='PUT'

            request.content='test'.bytes
            request.api_version=35
        when:

            def result=controller.apiProjectImport()

        then:
            response.status==200
            response.contentType.contains 'application/xml'
            response.xml.@status=='failed'
            response.xml.@successful==false
            response.xml.otherErrors.@count=='2'
            response.xml.otherErrors.size()==1
            response.xml.otherErrors[0].error.size()==2
            response.xml.otherErrors[0].error[0].text()=='err1'
            response.xml.otherErrors[0].error[1].text()=='err2'
    }


    def "import archive importACL"(){
        setup:
            controller.rundeckAuthContextProcessor = Mock(AppAuthContextProcessor){

                1 * authResourceForProjectAcl('test') >> null
                1 * authorizeApplicationResourceAny(_,_,[AuthConstants.ACTION_CREATE, AuthConstants.ACTION_ADMIN, AuthConstants.ACTION_APP_ADMIN]) >> true
            }
            controller.rundeckAppAuthorizer=Mock(AppAuthorizer){
                1 * project(_,_)>>Mock(AuthorizingProject){
                    1 * getResource()>>Stub(IRundeckProject){
                        getName()>>'test'
                    }
                }
            }
        controller.frameworkService=Mock(FrameworkService){

            1 * getFrameworkProject('test') >> null
            1 * getRundeckFramework() >> null

            0 * _(*_)
        }
        controller.projectService=Mock(ProjectService){
            1*importToProject(null,null,null,!null, {
                it.jobUuidOption== 'preserve'
                it.importExecutions== true
                it.importConfig== false
                it.importACL== true
            })>>[success:true]
            1 * validateAllProjectComponentImportOptions(_) >> [:]
            0 * _(*_)
        }

        session.subject= new Subject()
        when:

        setupFormTokens()

        params.project="test"
        params.importACL='true'
        response.format='json'
        request.method='POST'
        def file = new GrailsMockMultipartFile('zipFile', 'data'.bytes)
        request.addFile file
        def result=controller.importArchive()

        then:
        response.redirectedUrl=='/project/test/import'
        flash.message=='archive.successfully.imported'
        response.status==302
    }
    private def setupFormTokens(){
        def tokenHolder = SynchronizerTokensHolder.store(session)

        params[SynchronizerTokensHolder.TOKEN_URI] = '/controller/handleForm'
        params[SynchronizerTokensHolder.TOKEN_KEY] = tokenHolder.generateToken(params[SynchronizerTokensHolder.TOKEN_URI])
    }
    
    def "import errors flash the user with the actual details"(){
        //if something fails during the import, the flash will show the user what went wrong
        setup:
        // Setting up connection props
        controller.rundeckAuthContextProcessor = Mock(AppAuthContextProcessor){

            1 * authResourceForProjectAcl('test') >> null
            1 * authorizeApplicationResourceAny(_,_,[AuthConstants.ACTION_CREATE, AuthConstants.ACTION_ADMIN, AuthConstants.ACTION_APP_ADMIN]) >> true
        }
        controller.rundeckAppAuthorizer=Mock(AppAuthorizer){
            1 * project(_,_)>>Mock(AuthorizingProject){
                1 * getResource()>>Stub(IRundeckProject){
                    getName()>>'test'
                }
            }
        }
        controller.frameworkService=Mock(FrameworkService){

            1 * getFrameworkProject('test') >> null
            1 * getRundeckFramework() >> null

            0 * _(*_)
        }
        controller.projectService=Mock(ProjectService){
            1*importToProject(null,null,null,!null, {
                it.jobUuidOption== 'preserve'
                it.importExecutions== true
                it.importConfig== false
                it.importACL== true
            })>>[joberrors:"There was an error during the import project"]
            1 * validateAllProjectComponentImportOptions(_) >> [:]
            0 * _(*_)
        }
        //Building session
        session.subject= new Subject()
        when:
        // Setting Authorization to the API
        setupFormTokens()
        // Builing the request to upload the archive
        params.project="test"
        params.importACL='true'
        response.format='json'
        request.method='POST'
        def file = new GrailsMockMultipartFile('zipFile', 'data'.bytes)
        request.addFile file
        // Importing a file (with previous context of having an error)
        def result=controller.importArchive()

        then:
        //The flash generated by the "importArchive" method of the controller contains the message of the error.
        flash.joberrors == "There was an error during the import project"

    }

    def "If there's a hint, it displays to the user or not displays at all"(){
        setup:
        controller.rundeckAuthContextProcessor = Mock(AppAuthContextProcessor){

            1 * authResourceForProjectAcl('test') >> null
            1 * authorizeApplicationResourceAny(_,_,[AuthConstants.ACTION_CREATE, AuthConstants.ACTION_ADMIN, AuthConstants.ACTION_APP_ADMIN]) >> true
        }
        controller.rundeckAppAuthorizer=Mock(AppAuthorizer){
            1 * project(_,_)>>Mock(AuthorizingProject){
                1 * getResource()>>Stub(IRundeckProject){
                    getName()>>'test'
                }
            }
        }
        controller.frameworkService=Mock(FrameworkService){

            1 * getFrameworkProject('test') >> null
            1 * getRundeckFramework() >> null

            0 * _(*_)
        }
        controller.projectService=Mock(ProjectService){
            1*importToProject(null,null,null,!null, {
                it.jobUuidOption== 'preserve'
                it.importExecutions== true
                it.importConfig== false
                it.importACL== true
            })>> { throw new Exception(exceptionString) }
            1 * validateAllProjectComponentImportOptions(_) >> [:]
            0 * _(*_)
        }
        session.subject= new Subject()
        when:
        setupFormTokens()
        params.project="test"
        params.importACL='true'
        response.format='json'
        request.method='POST'
        def file = new GrailsMockMultipartFile('zipFile', 'data'.bytes)
        request.addFile file
        def result=controller.importArchive()

        then:
        flash.warn == flashWarning

        where:
        exceptionString                      |     flashWarning
        'Data too long for column \'data\''  |     "Some of the imported content was too large, this may be caused by a node source definition or other components that exceeds the supported size."
        'Other exception'                    |     null

    }
    
    def "When a exception is thrown during project's import process, the user is flashed with errors"(){
        setup:
        controller.rundeckAuthContextProcessor = Mock(AppAuthContextProcessor){

            1 * authResourceForProjectAcl('test') >> null
            1 * authorizeApplicationResourceAny(_,_,[AuthConstants.ACTION_CREATE, AuthConstants.ACTION_ADMIN, AuthConstants.ACTION_APP_ADMIN]) >> true
        }
        controller.rundeckAppAuthorizer=Mock(AppAuthorizer){
            1 * project(_,_)>>Mock(AuthorizingProject){
                1 * getResource()>>Stub(IRundeckProject){
                    getName()>>'test'
                }
            }
        }
        controller.frameworkService=Mock(FrameworkService){

            1 * getFrameworkProject('test') >> null
            1 * getRundeckFramework() >> null

            0 * _(*_)
        }
        controller.projectService=Mock(ProjectService){
            1*importToProject(null,null,null,!null, {
                it.jobUuidOption== 'preserve'
                it.importExecutions== true
                it.importConfig== false
                it.importACL== true
            })>>new Exception("expected exception")
            1 * validateAllProjectComponentImportOptions(_) >> [:]
            0 * _(*_)
        }
        session.subject= new Subject()

        when:
        setupFormTokens()
        params.project="test"
        params.importACL='true'
        response.format='json'
        request.method='POST'
        def file = new GrailsMockMultipartFile('zipFile', 'data'.bytes)
        request.addFile file
        def result=controller.importArchive()

        then:
        flash.error == 'There was some errors in the import process: [ No such property: success for class: java.lang.Exception ]'
    }

    def "import archive no importACL"(){
        setup:
            controller.rundeckAuthContextProcessor = Mock(AppAuthContextProcessor){

                0 * authResourceForProjectAcl('test') >> null
                0 * authorizeApplicationResourceAny(_,_,[AuthConstants.ACTION_CREATE, AuthConstants.ACTION_ADMIN, AuthConstants.ACTION_APP_ADMIN]) >> true
            }
            controller.rundeckAppAuthorizer=Mock(AppAuthorizer){
                1 * project(_,_)>>Mock(AuthorizingProject){
                    1 * getResource()>>Stub(IRundeckProject){
                        getName()>>'test'
                    }
                }
            }
        controller.frameworkService=Mock(FrameworkService){
            1 * getFrameworkProject('test') >> null
            1 * getRundeckFramework() >> null

            0 * _(*_)
        }
        controller.projectService=Mock(ProjectService){
            1*importToProject(null,null,null,!null,{
                it.jobUuidOption== 'preserve'
                it.importExecutions== true
                it.importConfig== false
                it.importACL== false
            })>>[success:true]
            1 * validateAllProjectComponentImportOptions(_) >> [:]

            0 * _(*_)
        }
        session.subject=new Subject()

        when:

        setupFormTokens()

        params.project="test"
        params.importACL='false'
        response.format='json'
        request.method='POST'
        def file = new GrailsMockMultipartFile('zipFile', 'data'.bytes)
        request.addFile file
        def result=controller.importArchive()

        then:
        response.redirectedUrl=='/project/test/import'
        flash.message=='archive.successfully.imported'
        response.status==302
    }
    def "import archive importACL unauthorized"(){
        setup:
            controller.rundeckAuthContextProcessor = Mock(AppAuthContextProcessor){
                1 * authResourceForProjectAcl('test') >> null
                1 * authorizeApplicationResourceAny(_,_,[AuthConstants.ACTION_CREATE, AuthConstants.ACTION_ADMIN, AuthConstants.ACTION_APP_ADMIN]) >> false
            }
            controller.rundeckAppAuthorizer=Mock(AppAuthorizer){
                1 * project(_,_)>>Mock(AuthorizingProject){
                    1 * getResource()>>Stub(IRundeckProject){
                        getName()>>'test'
                    }
                }
            }
        controller.frameworkService=Mock(FrameworkService){


            0 * _(*_)
        }
        controller.projectService=Mock(ProjectService){

            0 * _(*_)
        }
        session.subject=new Subject()


        when:

        def tokenHolder = SynchronizerTokensHolder.store(session)

        params[SynchronizerTokensHolder.TOKEN_URI] = '/controller/handleForm'
        params[SynchronizerTokensHolder.TOKEN_KEY] = tokenHolder.generateToken(params[SynchronizerTokensHolder.TOKEN_URI])

        params.project="test"
        params.importACL='true'
        response.format='json'
        request.method='POST'
        def file = new GrailsMockMultipartFile('zipFile', 'data'.bytes)
        request.addFile file
        def result=controller.importArchive()

        then:
        view == '/common/error'
        request.errorCode == 'request.error.unauthorized.message'
        request.errorArgs == [ACTION_CREATE, 'ACL for Project', 'test']

    }
    def "import archive token failure"(){
        setup:
        controller.frameworkService=Mock(FrameworkService){
            0 * _(*_)
        }
        controller.projectService=Mock(ProjectService){

            0 * _(*_)
        }

        when:

        def tokenHolder = SynchronizerTokensHolder.store(session)

        params[SynchronizerTokensHolder.TOKEN_URI] = '/controller/handleForm'
        params[SynchronizerTokensHolder.TOKEN_KEY] = 'xxx'//tokenHolder.generateToken(params[SynchronizerTokensHolder.TOKEN_URI])

        params.project="test"
        params.importACL='true'
        response.format='json'
        request.method='POST'
        def file = new GrailsMockMultipartFile('zipFile', 'data'.bytes)
        request.addFile file
        def result=controller.importArchive()

        then:
        response.redirectedUrl=='/project/test/import'
        flash.error=='request.error.invalidtoken.message'

    }

    def "export Instance Prepare"() {
        given:
        controller.projectService = Mock(ProjectService)
        controller.apiService = Mock(ApiService)
        controller.frameworkService = Mock(FrameworkService)
        controller.rundeckAppAuthorizer = Mock(AppAuthorizer)
                params.project = 'aproject'
        params.exportAll = true
        params.exportJobs = true
        params.exportExecutions = true
        params.exportConfigs = true
        params.exportReadmes = true
        params.exportAcls = true
        params.url = url
        params.apitoken = token
        params.targetproject = target
        params.preserveuuid = preserveuuid

        session.subject = new Subject()
        when:
        def result = controller.exportInstancePrepare()

        then:
        1 * controller.rundeckAppAuthorizer.project(_, _) >> Mock(AuthorizingProject) {
            _ * getAuthContext() >> Mock(UserAndRolesAuthContext) {
                _ * getUsername() >> 'auser'
            }
            1 * getResource() >> Stub(IRundeckProject){
                getName()>>'aproject'
            }
        }
        1 * controller.projectService.exportProjectToInstanceAsync(_, _, _, { ProjectArchiveParams opts ->
                    opts.exportAll == true &&
                    opts.exportJobs == true &&
                    opts.exportExecutions == true &&
                    opts.exportConfigs == true &&
                    opts.exportReadmes == true &&
                    opts.exportAcls == true &&
                    opts.preserveuuid == preserveuuid
        }, _ ) >> 'dummytoken'
        1 * controller.projectService.validateAllProjectComponentExportOptions(_) >> [:]
        response.redirectedUrl == '/project/aproject/exportWait/dummytoken?instance=' + url + '&iproject=' + target

        where:
        url      | token  | target      | preserveuuid
        'url1'   | '123'  | 'proj1'     | null
        'url2'   | '456'  | 'proj2'     | true

    }

    def "export Instance Prepare With missing properties"() {
        given:
        controller.projectService = Mock(ProjectService)
        controller.apiService = Mock(ApiService)
        controller.frameworkService = Mock(FrameworkService)
        params.project = 'aproject'
        params.exportAll = true
        params.exportJobs = true
        params.exportExecutions = true
        params.exportConfigs = true
        params.exportReadmes = true
        params.exportAcls = true
        params.url = url
        params.apitoken = token
        params.targetproject = target
        params.preserveuuid = preserveuuid


        when:
        def result = controller.exportInstancePrepare()

        then:
        0 * controller.frameworkService.existsFrameworkProject('aproject') >> true
        0 * controller.projectService.exportProjectToInstanceAsync(_, _, _, _, { ArchiveOptions opts ->
            opts.executionsOnly == false &&
                    opts.all == true &&
                    opts.jobs == true &&
                    opts.executions == true &&
                    opts.configs == true &&
                    opts.readmes == true &&
                    opts.acls == true
        },_,_,_,preserveuuid?:false
        ) >> 'dummytoken'
        flash.error
        response.redirectedUrl == '/project/aproject/export'

        where:
        url      | token  | target      | preserveuuid
        null     | '123'  | 'proj1'     | null
        'url1'   | null   | 'proj2'     | true
        'url2'   | '456'  | null        | true

    }


    def "export Instance with scm config"() {
        given:
        controller.projectService = Mock(ProjectService)
        controller.apiService = Mock(ApiService)
        controller.frameworkService = Mock(FrameworkService)
        controller.rundeckAppAuthorizer = Mock(AppAuthorizer)
                params.project = 'aproject'
        params.exportAll = true
        params.exportJobs = true
        params.exportExecutions = true
        params.exportConfigs = true
        params.exportReadmes = true
        params.exportAcls = true
        params.exportScm = true
        params.url = url
        params.apitoken = token
        params.targetproject = target
        params.preserveuuid = preserveuuid
        session.subject = new Subject()

        when:
        def result = controller.exportInstancePrepare()

        then:
        1 * controller.rundeckAppAuthorizer.project(_, _) >> Mock(AuthorizingProject) {
            _ * getAuthContext() >> Mock(UserAndRolesAuthContext) {
                _ * getUsername() >> 'auser'
            }
            1 * getResource() >> Stub(IRundeckProject){
                getName()>>'aproject'
            }

        }

        1 * controller.projectService.exportProjectToInstanceAsync(_, _, _, { ProjectArchiveParams opts ->
            opts.exportAll == true &&
                    opts.exportJobs == true &&
                    opts.exportExecutions == true &&
                    opts.exportConfigs == true &&
                    opts.exportReadmes == true &&
                    opts.exportAcls == true &&
                    opts.exportScm == true &&
                    opts.preserveuuid == preserveuuid
        }, _ ) >> 'dummytoken'
        1 * controller.projectService.validateAllProjectComponentExportOptions(_) >> [:]
        response.redirectedUrl == '/project/aproject/exportWait/dummytoken?instance=' + url + '&iproject=' + target

        where:
        url      | token  | target      | preserveuuid
        'url1'   | '123'  | 'proj1'     | null
        'url2'   | '456'  | 'proj2'     | true

    }

    def "api import api 34 webhook params"() {
        given: "api v34 request params for webhooks options are used"
            def aparams = new ProjectArchiveParams()
            request.method = 'PUT'
            request.api_version = 34
            params.project = 'test'
            controller.apiService = Mock(ApiService)
            controller.frameworkService = Mock(FrameworkService)
            def project = Mock(IRundeckProject)
            setupGetResource(project)
            controller.projectService = Mock(ProjectService)
            request.content = 'test'.bytes
            params.importWebhooks='true'
            params.whkRegenAuthTokens='true'
        when: "import project via api"
            controller.apiProjectImport(aparams)
        then: "webhook component import options are set"
            response.status == 200
            1 * controller.apiService.requireApi(_, _) >> true
            1 * controller.apiService.requireRequestFormat(_,_,['application/zip'])>>true
            1 * controller.apiService.extractResponseFormat(_, _, ['xml', 'json'], 'xml') >> 'json'
            1 * controller.frameworkService.getRundeckFramework()>>Mock(IFramework)
            1 * controller.projectService.importToProject(project,_,_,_,{ ProjectArchiveImportRequest req->
                req.importComponents == [(WebhooksProjectComponent.COMPONENT_NAME): true]
                req.importOpts == [(WebhooksProjectComponent.COMPONENT_NAME): [(WebhooksProjectImporter.WHK_REGEN_AUTH_TOKENS): 'true']]
            }) >> [success:true]
    }

    def "api import component options"() {
        given: "api request params for components"
            request.method = 'PUT'
            request.api_version = 34
            params.project = 'test'
            controller.apiService = Mock(ApiService)
            controller.frameworkService = Mock(FrameworkService)
            controller.projectService = Mock(ProjectService)
            def auth = Mock(UserAndRolesAuthContext)
            def project = Mock(IRundeckProject)
            setupGetResource(project)
            request.content = 'test'.bytes
            params.'importComponents.mycomponent'='true'
            params.'importOpts.mycomponent.someoption'='avalue'
        when: "import project via api"
            controller.apiProjectImport()
        then: "mycomponent import options are set"
            response.status == 200
            1 * controller.apiService.requireApi(_, _) >> true
            1 * controller.rundeckAuthContextProcessor.getAuthContextForSubject(_) >> auth
            1 * controller.rundeckAuthContextProcessor.getAuthContextForSubjectAndProject(_, 'test') >> auth
            1 * controller.apiService.requireRequestFormat(_, _, ['application/zip']) >> true
            1 * controller.apiService.extractResponseFormat(_, _, ['xml', 'json'], 'xml') >> 'json'
            1 * controller.frameworkService.getRundeckFramework() >> Mock(IFramework)
            1 * controller.projectService.importToProject(
                project, _, auth, _, { ProjectArchiveImportRequest req ->
                req.importComponents.mycomponent
                req.importOpts.mycomponent?.someoption == 'avalue'
            } ) >> [success: true]
            0 * controller.projectService._(*_)
    }
    private <T extends Annotation> T getControllerMethodAnnotation(String name, Class<T> clazz) {
        artefactInstance.getClass().getDeclaredMethods().find { it.name == name }.getAnnotation(clazz)
    }

    @Unroll
    def "RdAuthorizeProject annotation required for endpoint #endpoint"() {
        when:
            def result = getControllerMethodAnnotation(endpoint, RdAuthorizeProject)
        then:
            result.value() == access
        where:
            endpoint | access
            'export' | RundeckAccess.Project.AUTH_APP_EXPORT
            'delete' | RundeckAccess.General.AUTH_APP_DELETE
    }

    def "delete project"(){
        given:
            def aparams = new ProjectArchiveParams()
            aparams.project='aproject'
            controller.rundeckAppAuthorizer = Mock(AppAuthorizer) {

                1 * project(_, _) >> Mock(AuthorizingProject) {
                    _*getAuthContext()>>Mock(UserAndRolesAuthContext){
                        _*getUsername()>>'auser'
                    }
                    1 * getResource() >> Stub(IRundeckProject){
                        getName()>>'aproject'
                    }
                }
            }
            controller.frameworkService = Mock(FrameworkService)
            session.subject = new Subject()
            controller.projectService = Mock(ProjectService){
                1 * deleteProject(_, _, _, _) >> [success: true]
            }

            params.project='aproject'
        when:
            setupFormTokens()
            request.method='POST'
            controller.delete(aparams)
        then:
        response.status==302
        response.redirectedUrl=='/menu/home'
    }
}
