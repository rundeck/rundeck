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
import com.dtolabs.rundeck.core.config.Features
import grails.test.mixin.Mock
import grails.test.mixin.TestFor
import grails.testing.web.controllers.ControllerUnitTest
import grails.testing.web.interceptor.InterceptorUnitTest
import org.grails.gsp.GroovyPagesTemplateEngine
import org.grails.spring.beans.factory.InstanceFactoryBean
import org.grails.web.gsp.io.CachingGrailsConventionGroovyPageLocator
import org.grails.web.servlet.view.GroovyPageViewResolver
import org.grails.web.util.GrailsApplicationAttributes
import org.hibernate.exception.JDBCConnectionException
import org.rundeck.app.access.InterceptorHelper
import org.rundeck.app.authorization.AppAuthContextEvaluator
import org.springframework.beans.factory.config.MethodInvokingFactoryBean
import rundeck.controllers.ErrorController
import rundeck.controllers.MenuController
import rundeck.controllers.ProjectController
import rundeck.services.FrameworkService
import rundeck.services.feature.FeatureService
import spock.lang.Specification

import javax.security.auth.Subject
import java.security.Principal
import java.sql.SQLException

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
        interceptor.interceptorHelper = Mock(InterceptorHelper) {
            matchesAllowedAsset(_,_) >> false
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

        interceptor.interceptorHelper = Mock(InterceptorHelper) {
            matchesAllowedAsset(_,_) >> false
        }
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
            rundeckAuthContextEvaluator(InstanceFactoryBean,Mock(AppAuthContextEvaluator){
                authorizeApplicationResourceAny(*_) >> false
            })
            frameworkService(MethodInvokingFactoryBean) {
                targetObject = this
                targetMethod = "buildMockFrameworkService"
                arguments = [true, false]

            }
        }
        interceptor.interceptorHelper = Mock(InterceptorHelper) {
            matchesAllowedAsset(_,_) >> false
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
        interceptor.interceptorHelper = Mock(InterceptorHelper) {
            matchesAllowedAsset(_,_) >> false
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
        def featureMock = Mock(FeatureService){
            featurePresent(Features.SIDEBAR_PROJECT_LISTING)>>true
        }
        def frameworkMock=Mock(FrameworkService) {
            1 * existsFrameworkProject(_) >> true
            1 * refreshSessionProjects(_,_)
        }
        defineBeans {
            rundeckAuthContextEvaluator(InstanceFactoryBean,Mock(AppAuthContextEvaluator){
                1 * authorizeApplicationResourceAny(*_) >> true
            })
            frameworkService(InstanceFactoryBean,frameworkMock)
            featureService(InstanceFactoryBean, featureMock)
        }
        interceptor.interceptorHelper = Mock(InterceptorHelper) {
            matchesAllowedAsset(_,_) >> false
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
    def "projectSelection project param is string[]"() {
        given:
        def controller = (ProjectController)mockController(ProjectController)
        def featureMock = Mock(FeatureService){
            featurePresent(Features.SIDEBAR_PROJECT_LISTING)>>true
        }
        def frameworkMock=Mock(FrameworkService) {
            1 * existsFrameworkProject(_) >> true
            1 * refreshSessionProjects(_,_)
        }
        defineBeans {
            rundeckAuthContextEvaluator(InstanceFactoryBean,Mock(AppAuthContextEvaluator){
                1 * authorizeApplicationResourceAny(*_) >> true
            })
            frameworkService(InstanceFactoryBean,frameworkMock)
            featureService(InstanceFactoryBean, featureMock)
        }
        interceptor.interceptorHelper = Mock(InterceptorHelper) {
            matchesAllowedAsset(_,_) >> false
        }
        session.user = 'bob'
        session.subject = new Subject()
        request.remoteUser = 'bob'
        request.userPrincipal = Mock(Principal) {
            getName() >> 'bob'
        }
        params.project = ['testProject','testProject'].toArray(new String[2])

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

    }

    def "projectSelection disable sidebarProjectListing feature"() {
        given:
        def controller = (ProjectController)mockController(ProjectController)
        def featureMock = Mock(FeatureService){
            featurePresent(Features.SIDEBAR_PROJECT_LISTING)>>false
        }
        def frameworkMock=Mock(FrameworkService) {
            1 * existsFrameworkProject(_) >> true
            0 * refreshSessionProjects(_,_)
            1 * loadSessionProjectLabel(_,'testProject')
        }
        defineBeans {
            rundeckAuthContextEvaluator(InstanceFactoryBean,Mock(AppAuthContextEvaluator){
                1 * authorizeApplicationResourceAny(*_) >> true
            })
            frameworkService(InstanceFactoryBean,frameworkMock)
            featureService(InstanceFactoryBean, featureMock)
        }
        interceptor.interceptorHelper = Mock(InterceptorHelper) {
            matchesAllowedAsset(_,_) >> false
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

    def "Skip Interceptor if requesting fiveHundred action on error page"() {
        given:
            withRequest(controller: 'error', action: 'fiveHundred')
        when: "The interceptor check if request matches the interceptor"

            def result = interceptor.doesMatch()

        then: "Should not matches"
            !result

    }

    private FrameworkService buildMockFrameworkService(boolean exists, boolean authorized) {
        Mock(FrameworkService) {
            existsFrameworkProject(_) >> exists
        }
    }
}
