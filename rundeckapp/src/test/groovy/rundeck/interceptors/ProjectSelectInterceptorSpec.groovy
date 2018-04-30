/*
 * Copyright 2018 Rundeck, Inc. (http://rundeck.com)
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
package rundeck.interceptors

import com.codahale.metrics.MetricRegistry
import grails.test.mixin.Mock
import grails.test.mixin.TestFor
import grails.testing.web.controllers.ControllerUnitTest
import grails.testing.web.interceptor.InterceptorUnitTest
import org.grails.gsp.GroovyPagesTemplateEngine
import org.grails.web.gsp.io.CachingGrailsConventionGroovyPageLocator
import org.grails.web.servlet.view.GroovyPageViewResolver
import org.springframework.beans.factory.config.MethodInvokingFactoryBean
import rundeck.controllers.MenuController
import rundeck.controllers.ProjectController
import rundeck.services.FrameworkService
import spock.lang.Specification

import javax.security.auth.Subject
import java.security.Principal

/**
 * @author greg
 * @since 12/7/17
 */
class ProjectSelectInterceptorSpec extends Specification implements InterceptorUnitTest<ProjectSelectInterceptor> {

    def "placeholder"() {
        //work around for grails bug where the first test doesn't
        //correctly register the interceptor in the application context
        //so the test doesn't call the before() interceptor method at all
        //subsequent tests work fine :-(
        given:
        def controller = (ProjectController)mockController(ProjectController)
        defineBeans {
            frameworkService(MethodInvokingFactoryBean) {
                targetObject = this
                targetMethod = "buildMockFrameworkService"
                arguments = [false, false]

            }
        }
        session.user = 'bob'
        session.subject = new Subject()
        request.remoteUser = 'bob'
        request.userPrincipal = Mock(Principal) {
            getName() >> 'bob'
        }
        params.project = 'testProject'
        when:
        withInterceptors(controller: 'project',action: 'index') {
            controller.index()
        }
        then:
        true == true
    }

    def "projectSelection project not found"() {
        given:
        def controller = (ProjectController)mockController(ProjectController)

        defineBeans {
            frameworkService(MethodInvokingFactoryBean) {
                targetObject = this
                targetMethod = "buildMockFrameworkService"
                arguments = [false, false]

            }
        }
        session.user = 'bob'
        session.subject = new Subject()
        request.remoteUser = 'bob'
        request.userPrincipal = Mock(Principal) {
            getName() >> 'bob'
        }
        params.project = 'testProject'

        when:
        withInterceptors(controller: 'project',action: 'index') {
            controller.index()
        }
        then:
        response.status == 404

        flash.error == null
        request.title == 'Not Found'
        request.errorCode == 'scheduledExecution.project.invalid.message'
        request.errorArgs == ['testProject']

    }

    def "projectSelection project not authorized"() {
        given:
        def controller = (ProjectController)mockController(ProjectController)

        defineBeans {
            frameworkService(MethodInvokingFactoryBean) {
                targetObject = this
                targetMethod = "buildMockFrameworkService"
                arguments = [true, false]

            }
        }
        session.user = 'bob'
        session.subject = new Subject()
        request.remoteUser = 'bob'
        request.userPrincipal = Mock(Principal) {
            getName() >> 'bob'
        }
        params.project = 'testProject'

        when:
        withInterceptors(controller: 'project',action: 'index') {
            controller.index()
        }
        then:
        response.status == 403

        flash.error == null
        request.title == null
        request.titleCode == 'request.error.unauthorized.title'
        request.errorCode == 'request.error.unauthorized.message'
        request.errorArgs == ['view', 'Project', 'testProject']

    }

    def "projectSelection project name invalid"() {
        given:
        def controller = (ProjectController)mockController(ProjectController)

        defineBeans {
            frameworkService(MethodInvokingFactoryBean) {
                targetObject = this
                targetMethod = "buildMockFrameworkService"
                arguments = [true, true]

            }
        }
        session.user = 'bob'
        session.subject = new Subject()
        request.remoteUser = 'bob'
        request.userPrincipal = Mock(Principal) {
            getName() >> 'bob'
        }
        params.project = projectName

        when:
        withInterceptors(controller: 'project',action: 'index'){
            controller.index()
        }
        then:
        response.status == 400

        flash.error == null
        request.title == null
        request.titleCode == null
        request.errorCode == 'project.name.invalid'
        request.errorArgs == null

        where:
        projectName   | _
        'Name/asdf'   | _
        'Name asdf'   | _
        'Name \'asdf' | _
    }

    def "projectSelection project authorized"() {
        given:
        def controller = (ProjectController)mockController(ProjectController)

        defineBeans {
            frameworkService(MethodInvokingFactoryBean) {
                targetObject = this
                targetMethod = "buildMockFrameworkService"
                arguments = [true, true]

            }
        }
        session.user = 'bob'
        session.subject = new Subject()
        request.remoteUser = 'bob'
        request.userPrincipal = Mock(Principal) {
            getName() >> 'bob'
        }
        params.project = 'testProject'

        when:
        withInterceptors(controller: 'project') {
            controller.index()
        }
        then:
        response.status == 302

        flash.error == null
        request.title == null
        request.titleCode == null
        request.errorCode == null
        request.errorArgs == null
//        response.redirectedUrl == '/menu/jobs' //TODO: The interceptor test dont get redirectedUrl, even the status is 302.

    }

    private FrameworkService buildMockFrameworkService(boolean exists, boolean authorized) {
        Mock(FrameworkService) {
            existsFrameworkProject(_) >> exists
            authorizeApplicationResourceAny(*_) >> authorized
        }
    }
}
