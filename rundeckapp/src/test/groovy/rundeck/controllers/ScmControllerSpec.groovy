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

import com.dtolabs.rundeck.core.authorization.UserAndRolesAuthContext
import com.dtolabs.rundeck.core.plugins.views.BasicInputView
import com.dtolabs.rundeck.plugins.scm.JobStateImpl
import com.dtolabs.rundeck.plugins.scm.ScmImportTrackedItem
import com.dtolabs.rundeck.plugins.scm.ScmImportTrackedItemBuilder
import com.dtolabs.rundeck.plugins.scm.SynchState
import grails.test.mixin.Mock
import grails.test.mixin.TestFor
import rundeck.CommandExec
import rundeck.ScheduledExecution
import rundeck.Workflow
import rundeck.services.ApiService
import rundeck.services.FrameworkService
import rundeck.services.ScmService
import spock.lang.Specification
import spock.lang.Unroll

/**
 * See the API for {@link grails.test.mixin.web.ControllerUnitTestMixin} for usage instructions
 */
@TestFor(ScmController)
@Mock([ScheduledExecution, Workflow, CommandExec])
class ScmControllerSpec extends Specification {


    void "scm action cancel redirects to jobs page"() {
        given:
        controller.frameworkService = Mock(FrameworkService)
        controller.scmService = Mock(ScmService)

        when:
        request.method = 'POST'
        params.cancel = 'Cancel'
        controller.performActionSubmit('export', 'test1', 'asdf')

        then:
        1 * controller.frameworkService.authorizeApplicationResourceAny(*_) >> true
        1 * controller.scmService.projectHasConfiguredPlugin(*_) >> true

        response.status == 302
        response.redirectedUrl == '/project/test1/job/index'
    }

    def 'api export action project perform'() {
        given:
        def projectName = 'testproj'
        def actionName = 'testAction'
        params.actionId = actionName
        params.project = projectName
        params.integration = integration

        controller.frameworkService = Mock(FrameworkService) {
            1 * existsFrameworkProject(projectName) >> true
            1 * authResourceForProject(projectName)
            1 * authorizeApplicationResourceAny(_, _, _)
            getAuthContextForSubjectAndProject(_, projectName) >> Mock(UserAndRolesAuthContext)
            0 * _(*_)
        }
        controller.apiService = Mock(ApiService) {
            1 * requireAuthorized(_, _, _) >> true
            1 * parseJsonXmlWith(_, _, _) >> { args ->
                args[2].json(args[0].JSON)
                true
            }
            1 * requireExists(_, _, _, "no.scm.integration.plugin.configured") >> true
            1 * requireExists(_, _, _, "scm.not.a.valid.action.actionid") >> true
            0 * _(*_)
        }
        controller.scmService = Mock(ScmService) {

            1 * projectHasConfiguredPlugin(integration, projectName) >> true
            1 * getInputView(_, integration, projectName, actionName) >> Mock(BasicInputView)
            1 * exportStatusForJobs([]) >> [:]
            1 * exportFilePathsMapForJobs([]) >> [:]
            1 * getRenamedJobPathsForProject(projectName) >> [:]
            1 * performExportAction(actionName, _, projectName, _, _, _) >>
            [valid: true, nextAction: [id: 'someAction']]
            0 * _(*_)
        }

        response.format = 'json'
        request.method = 'POST'
        request.contentType = 'application/json'
        request.content = '{"input":null}'.bytes

        when:
        controller.apiProjectActionPerform()

        then:
        response.json == [
            success         : true,
            validationErrors: null,
            nextAction      : 'someAction',
            message         : 'api.scm.action.integration.success.message'
        ]
        response.status == 200

        where:
        integration | _
        'export'    | _
    }

    static final String exportJsonReqString = '''{"input":
{"message":"blah"},
"jobs":[
"job1",
],
"items":[
"item2"
],
"deleted":[
"del1",
"del2"
]}'''
    static final String exportXmlReqString = '''<scmAction>
    <input>
        <entry key="message">blah</entry>
    </input>
    <jobs>
        <job jobId="job1"/>
    </jobs>
    <items>
        <item itemId="item2"/>
    </items>
    <deleted>
        <item itemId="del1"/>
        <item itemId="del2"/>
    </deleted>
</scmAction>'''

    private Map createJobParams(Map overrides = [:]) {
        [
            jobName       : 'blue',
            project       : 'testproj',
            groupPath     : 'some/where',
            description   : 'a job',
            argString     : '-a b -c d',
            workflow      : new Workflow(
                keepgoing: true,
                commands: [new CommandExec([adhocRemoteString: 'test buddy'])]
            ),
            serverNodeUUID: null,
            scheduled     : true
        ] + overrides
    }

    private Map defineJobs(String... ids) {

        def jobs = [:]
        ids.collectEntries { id ->
            ScheduledExecution job = new ScheduledExecution(createJobParams(uuid: id, jobName: "job " + id)).save()
            [id, job]
        }
        jobs
    }

    @Unroll
    def 'api export action project perform with all inputs'() {
        given:
        def projectName = 'testproj'
        def actionName = 'testAction'
        params.actionId = actionName
        params.project = projectName
        params.integration = integration

        def definedJobs = defineJobs('job1', 'job2', 'job3')

        def job1 = definedJobs.job1
        def job2 = definedJobs.job2
        def job3 = definedJobs.job3


        controller.frameworkService = Mock(FrameworkService) {
            1 * existsFrameworkProject(projectName) >> true
            1 * authResourceForProject(projectName)
            1 * authorizeApplicationResourceAny(_, _, _)
            getAuthContextForSubjectAndProject(_, projectName) >> Mock(UserAndRolesAuthContext)
            0 * _(*_)
        }
        controller.apiService = Mock(ApiService) {
            1 * requireAuthorized(_, _, _) >> true
            1 * parseJsonXmlWith(_, _, _) >> { args ->
                if (ctype == 'application/json') {
                    args[2].json(args[0].JSON)
                } else {
                    args[2].xml(args[0].XML)
                }
                true
            }
            1 * requireExists(_, _, _, "no.scm.integration.plugin.configured") >> true
            1 * requireExists(_, _, _, "scm.not.a.valid.action.actionid") >> true
            0 * _(*_)
        }
        controller.scmService = Mock(ScmService) {

            1 * projectHasConfiguredPlugin(integration, projectName) >> true
            1 * getInputView(_, integration, projectName, actionName) >> Mock(BasicInputView)
            1 * exportStatusForJobs(_) >> [
                job1: new JobStateImpl(synchState: SynchState.EXPORT_NEEDED),
                job2: new JobStateImpl(synchState: SynchState.EXPORT_NEEDED),
                job3: new JobStateImpl(synchState: SynchState.EXPORT_NEEDED),
            ]
            1 * exportFilePathsMapForJobs(_) >> [
                job1: 'item1',
                job2: 'item2',
                job3: 'item3',
            ]
            1 * getRenamedJobPathsForProject(projectName) >> renamed
            1 * performExportAction(
                actionName, _, projectName, config, {
                it*.uuid == selectedJobIds
            }, deleteditems
            ) >> [valid: true, nextAction: [id: 'someAction']]
            0 * _(*_)
        }

        response.format = 'json'
        request.method = 'POST'
        request.contentType = ctype
        request.content = requestData.bytes

        when:
        controller.apiProjectActionPerform()

        then:
        response.json == [
            success         : true,
            validationErrors: null,
            nextAction      : 'someAction',
            message         : 'api.scm.action.integration.success.message'
        ]
        response.status == 200


        where:
        ctype              | requestData         | selectedJobIds   | renamed         | deleteditems
        'application/json' | exportJsonReqString | ['job1', 'job2'] | [:]             | ['del1', 'del2']
        'application/json' | exportJsonReqString | ['job1', 'job2'] | [job2: 'path3'] | ['del1', 'del2', 'path3']
        'application/xml'  | exportXmlReqString  | ['job1', 'job2'] | [:]             | ['del1', 'del2']
        'application/xml'  | exportXmlReqString  | ['job1', 'job2'] | [job2: 'path3'] | ['del1', 'del2', 'path3']
        integration = 'export'
        config = [message: 'blah']
    }

    def 'api import action project perform'() {
        given:
        def projectName = 'testproj'
        def actionName = 'testAction'
        params.actionId = actionName
        params.project = projectName
        params.integration = integration

        controller.frameworkService = Mock(FrameworkService) {
            1 * existsFrameworkProject(projectName) >> true
            1 * authResourceForProject(projectName)
            1 * authorizeApplicationResourceAny(_, _, _)
            getAuthContextForSubjectAndProject(_, projectName) >> Mock(UserAndRolesAuthContext)
            0 * _(*_)
        }
        controller.apiService = Mock(ApiService) {
            1 * requireAuthorized(_, _, _) >> true
            1 * parseJsonXmlWith(_, _, _) >> { args ->
                args[2].json(args[0].JSON)
                true
            }
            1 * requireExists(_, _, _, "no.scm.integration.plugin.configured") >> true
            1 * requireExists(_, _, _, "scm.not.a.valid.action.actionid") >> true
            0 * _(*_)
        }
        controller.scmService = Mock(ScmService) {

            1 * projectHasConfiguredPlugin(integration, projectName) >> true
            1 * getInputView(_, integration, projectName, actionName) >> Mock(BasicInputView)
            1 * performImportAction(actionName, _, projectName, _, _, _) >>
            [valid: true, nextAction: [id: 'someAction']]
            0 * _(*_)
        }

        response.format = 'json'
        request.method = 'POST'
        request.contentType = 'application/json'
        request.content = '{"input":null}'.bytes

        when:
        controller.apiProjectActionPerform()

        then:
        response.json == [
            success         : true,
            validationErrors: null,
            nextAction      : 'someAction',
            message         : 'api.scm.action.integration.success.message'
        ]
        response.status == 200

        where:
        integration | _
        'import'    | _
    }

    static final String importJsonReqString = '''{"input":
{"message":"blah"},
"jobs":[
"job1",
"job2"
],
"items":[
"item1",
"item2"
],
"deletedJobs":[
"del1",
"del2"
]}'''
    static final String importXmlReqString = '''<scmAction>
    <input>
        <entry key="message">blah</entry>
    </input>
    <jobs>
        <job jobId="job1"/>
        <job jobId="job2"/>
    </jobs>
    <items>
        <item itemId="item1"/>
        <item itemId="item2"/>
    </items>
    <deletedJobs>
        <job jobId="del1"/>
        <job jobId="del2"/>
    </deletedJobs>
</scmAction>'''

    @Unroll
    def 'api import action project perform with all inputs'() {
        given:
        def projectName = 'testproj'
        def actionName = 'testAction'
        params.actionId = actionName
        params.project = projectName
        params.integration = integration

        controller.frameworkService = Mock(FrameworkService) {
            1 * existsFrameworkProject(projectName) >> true
            1 * authResourceForProject(projectName)
            1 * authorizeApplicationResourceAny(_, _, _)
            getAuthContextForSubjectAndProject(_, projectName) >> Mock(UserAndRolesAuthContext)
            0 * _(*_)
        }
        controller.apiService = Mock(ApiService) {
            1 * requireAuthorized(_, _, _) >> true
            1 * parseJsonXmlWith(_, _, _) >> { args ->
                if (ctype == 'application/json') {
                    args[2].json(args[0].JSON)
                } else {
                    args[2].xml(args[0].XML)
                }
                true
            }
            1 * requireExists(_, _, _, "no.scm.integration.plugin.configured") >> true
            1 * requireExists(_, _, _, "scm.not.a.valid.action.actionid") >> true
            0 * _(*_)
        }
        controller.scmService = Mock(ScmService) {

            1 * projectHasConfiguredPlugin(integration, projectName) >> true
            1 * getInputView(_, integration, projectName, actionName) >> Mock(BasicInputView)
            1 * getTrackingItemsForAction(projectName, actionName) >> []
            1 * performImportAction(actionName, _, projectName, config, tracked, deletejobs) >>
            [valid: true, nextAction: [id: 'someAction']]
            0 * _(*_)
        }

        response.format = 'json'
        request.method = 'POST'
        request.contentType = ctype
        request.content = requestData.bytes

        when:
        controller.apiProjectActionPerform()

        then:
        response.json == [
            success         : true,
            validationErrors: null,
            nextAction      : 'someAction',
            message         : 'api.scm.action.integration.success.message'
        ]
        response.status == 200

        where:
        ctype              | requestData         | config            | tracked            | deletejobs
        'application/json' | importJsonReqString | [message: 'blah'] | ['item1', 'item2'] | ['del1', 'del2']
        'application/xml'  | importXmlReqString  | [message: 'blah'] | ['item1', 'item2'] | ['del1', 'del2']
        integration = 'import'
    }

    static final String importJsonReqString2 = '''{"input":
{"message":"blah"},
"jobs":[
"job1",
"job2"
]}'''
    static final String importXmlReqString2 = '''<scmAction>
    <input>
        <entry key="message">blah</entry>
    </input>
    <jobs>
        <job jobId="job1"/>
        <job jobId="job2"/>
    </jobs>
</scmAction>'''

    @Unroll
    def 'api import action project perform with job inputs'() {
        given:
        def projectName = 'testproj'
        def actionName = 'testAction'
        params.actionId = actionName
        params.project = projectName
        params.integration = integration

        controller.frameworkService = Mock(FrameworkService) {
            1 * existsFrameworkProject(projectName) >> true
            1 * authResourceForProject(projectName)
            1 * authorizeApplicationResourceAny(_, _, _)
            getAuthContextForSubjectAndProject(_, projectName) >> Mock(UserAndRolesAuthContext)
            0 * _(*_)
        }
        controller.apiService = Mock(ApiService) {
            1 * requireAuthorized(_, _, _) >> true
            1 * parseJsonXmlWith(_, _, _) >> { args ->
                if (ctype == 'application/json') {
                    args[2].json(args[0].JSON)
                } else {
                    args[2].xml(args[0].XML)
                }
                true
            }
            1 * requireExists(_, _, _, "no.scm.integration.plugin.configured") >> true
            1 * requireExists(_, _, _, "scm.not.a.valid.action.actionid") >> true
            0 * _(*_)
        }
        controller.scmService = Mock(ScmService) {

            1 * projectHasConfiguredPlugin(integration, projectName) >> true
            1 * getInputView(_, integration, projectName, actionName) >> Mock(BasicInputView)
            1 * getTrackingItemsForAction(projectName, actionName) >> [
                ScmImportTrackedItemBuilder.builder().id("/a/path").jobId("job1").deleted(true).build(),
                ScmImportTrackedItemBuilder.builder().id("/a/path2").jobId("job2").deleted(false).build(),
                ScmImportTrackedItemBuilder.builder().id("/a/path3").jobId("job3").deleted(false).build(),
            ]
            1 * performImportAction(actionName, _, projectName, config, tracked, deletejobs) >>
            [valid: true, nextAction: [id: 'someAction']]
            0 * _(*_)
        }

        response.format = 'json'
        request.method = 'POST'
        request.contentType = ctype
        request.content = requestData.bytes

        when:
        controller.apiProjectActionPerform()

        then:
        response.json == [
            success         : true,
            validationErrors: null,
            nextAction      : 'someAction',
            message         : 'api.scm.action.integration.success.message'
        ]
        response.status == 200

        where:
        ctype              | requestData          | config            | tracked      | deletejobs
        'application/json' | importJsonReqString2 | [message: 'blah'] | ['/a/path2'] | ['job1']
        'application/xml'  | importXmlReqString2  | [message: 'blah'] | ['/a/path2'] | ['job1']
        integration = 'import'
    }


    def 'perform import fetch without items'() {
        given:
        def projectName = 'testproj'
        def actionName = 'testAction'
        params.actionId = actionName
        params.project = projectName
        params.integration = integration

        controller.frameworkService = Mock(FrameworkService) {
            1 * authResourceForProject(projectName)
            1 * authorizeApplicationResourceAny(_, _, _) >> true
            getAuthContextForSubjectAndProject(_, projectName) >> Mock(UserAndRolesAuthContext)
            0 * _(*_)
        }

        controller.scmService = Mock(ScmService) {

            1 * projectHasConfiguredPlugin(integration, projectName) >> true
            1 * getInputView(_, integration, projectName, actionName) >> Mock(BasicInputView)
            1 * getRenamedJobPathsForProject(projectName) >> [:]
            1 * loadProjectPluginDescriptor(projectName, integration)
            1 * getTrackingItemsForAction(projectName, actionName) >> null
            1 * importStatusForJobs([])
            1 * getPluginStatus(_,integration, projectName)
            0 * _(*_)
        }

        response.format = 'json'
        request.method = 'POST'
        request.contentType = 'application/json'
        request.content = '{"input":null}'.bytes

        when:
        controller.performAction(integration, projectName, actionName)

        then:
        /*response.json == [
            success         : true,
            validationErrors: null,
            nextAction      : 'someAction',
            message         : 'api.scm.action.integration.success.message'
        ]*/
        response.status == 200

        where:
        integration | _
        'import'    | _
    }
}
