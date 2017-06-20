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

import grails.test.mixin.TestFor
import rundeck.services.FrameworkService
import rundeck.services.ScmService
import spock.lang.Specification

/**
 * See the API for {@link grails.test.mixin.web.ControllerUnitTestMixin} for usage instructions
 */
@TestFor(ScmController)
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
}
