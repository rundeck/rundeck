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
import grails.test.mixin.Mock
import grails.test.mixin.TestFor
import rundeck.ScheduledExecution
import rundeck.services.ApiService
import rundeck.services.FrameworkService
import rundeck.services.ScmService
import spock.lang.Specification

/**
 * See the API for {@link grails.test.mixin.web.ControllerUnitTestMixin} for usage instructions
 */
@TestFor(ScmController)
@Mock(ScheduledExecution)
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
        response.redirectedUrl == '/scheduledExecution/index?project=test1'
    }

    def 'api action project perform'() {
        given:
        def projectName = 'testproj'
        def actionName = 'testAction'
        params.actionId = actionName
        params.project = projectName
        params.integration = 'export'

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

            1 * projectHasConfiguredPlugin('export', projectName) >> true
            1 * getInputView(_, 'export', projectName, actionName) >> Mock(BasicInputView)
            1 * exportStatusForJobs([]) >> [:]
            1 * exportFilePathsMapForJobs([]) >> [:]
            1 * getRenamedJobPathsForProject(projectName) >> [:]
            1 * performExportAction(_, _, _, _, _, _) >> [valid: true, nextAction: [id: 'someAction']]
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

    }
}
