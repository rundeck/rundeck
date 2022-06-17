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

import com.dtolabs.rundeck.app.api.ApiMarshallerRegistrar
import com.dtolabs.rundeck.app.api.ApiVersions
import com.dtolabs.rundeck.core.authentication.tokens.AuthTokenMode
import com.dtolabs.rundeck.core.authentication.tokens.AuthTokenType
import com.dtolabs.rundeck.core.authorization.UserAndRolesAuthContext
import grails.converters.JSON
import grails.converters.XML
import grails.testing.gorm.DataTest
import grails.testing.web.controllers.ControllerUnitTest
import grails.web.JSONBuilder
import grails.web.mapping.LinkGenerator
import org.quartz.Scheduler
import org.quartz.SchedulerMetaData
import org.rundeck.app.authorization.AppAuthContextProcessor
import org.rundeck.core.auth.AuthConstants
import org.rundeck.app.authorization.domain.AppAuthorizer
import org.rundeck.core.auth.app.RundeckAccess
import org.rundeck.core.auth.app.type.AuthorizingSystem
import org.rundeck.core.auth.web.RdAuthorizeSystem
import rundeck.AuthToken
import rundeck.User
import rundeck.UtilityTagLib
import rundeck.services.ApiService
import rundeck.services.ConfigurationService
import rundeck.services.FrameworkService
import spock.lang.Specification
import spock.lang.Unroll

import javax.security.auth.Subject
import java.lang.annotation.Annotation

class ApiControllerSpec extends Specification implements ControllerUnitTest<ApiController>, DataTest {


    def setup() {
        mockDomain User
        mockDomain AuthToken
        def apiMarshallerRegistrar = new ApiMarshallerRegistrar()
        apiMarshallerRegistrar.registerMarshallers()
        apiMarshallerRegistrar.registerApiMarshallers()
        session.subject = new Subject()
        controller.apiService = Mock(ApiService)
        controller.frameworkService = Mock(FrameworkService)
        controller.rundeckAuthContextProcessor=Mock(AppAuthContextProcessor)
    }

    def "api token list does not include webhook tokens"() {
        given:
        User bob = new User(login: 'bob')
        bob.save()
        User bob2 = new User(login: 'bob2')
        bob2.save()
        AuthToken createdToken = new AuthToken(
                user: bob,
                type: AuthTokenType.USER,
                token: 'abc',
                authRoles: 'a,b',
                uuid: '123uuid',
                creator: 'elf',
                )
        createdToken.save()
        AuthToken webhookToken = new AuthToken(
            user: bob,
            type: AuthTokenType.WEBHOOK,
            tokenMode: AuthTokenMode.LEGACY,
            token: 'whk',
            authRoles: 'a,b',
            uuid: '123uuidwhk',
            creator: 'elf',
            )
        webhookToken.save()
        AuthToken createdToken2 = new AuthToken(
                user: bob2,
                type: AuthTokenType.USER,
                token: 'abc',
                authRoles: 'a,b',
                uuid: '456uuid',
                creator: 'elf',
                )
        createdToken2.save()
        AuthToken webhookToken2 = new AuthToken(
            user: bob2,
            type: AuthTokenType.WEBHOOK,
            tokenMode: AuthTokenMode.LEGACY,
            token: 'whk',
            authRoles: 'a,b',
            uuid: '456uuidwhk',
            creator: 'elf',
            )
        webhookToken2.save()

        controller.apiService = Mock(ApiService) {
            hasTokenAdminAuth(_) >> { true }
        }
        controller.frameworkService = Mock(FrameworkService)
        controller.rundeckAuthContextProcessor=Mock(AppAuthContextProcessor)
        request.api_version = 33
        request.addHeader('accept', 'application/json')
        JSON.use('v' + request.api_version)
        when:
        controller.apiTokenList()

        then:
        1 * controller.apiService.requireApi(_, _) >> true
        1 * controller.apiService.requireVersion(_, _, _) >> true
        response.json.size() == 2
        response.json[0].id == "123uuid"
        response.json[1].id == "456uuid"

    }
    def "api token list unauthorized self param #inputUser"() {
        given:
        User bob = new User(login: 'bob')
        bob.save()
        AuthToken createdToken = new AuthToken(
                user: bob,
                type: AuthTokenType.USER,
                token: 'abc',
                authRoles: 'a,b',
                uuid: '123uuid',
                creator: 'elf',
                )
        createdToken.save()
        AuthToken webhookToken = new AuthToken(
            user: bob,
            type: AuthTokenType.WEBHOOK,
            tokenMode: AuthTokenMode.LEGACY,
            token: 'whk',
            authRoles: 'a,b',
            uuid: '123uuidwhk',
            creator: 'elf',
            )
        webhookToken.save()

        controller.apiService = Mock(ApiService) {
            hasTokenAdminAuth(_) >> { false }
        }
        controller.frameworkService = Mock(FrameworkService)
        controller.rundeckAuthContextProcessor=Mock(AppAuthContextProcessor){
            1 * getAuthContextForSubject(_)>>Mock(UserAndRolesAuthContext){
                1 * getUsername()>>'bob'
            }
        }
        request.api_version = 33
        request.addHeader('accept', 'application/json')
        JSON.use('v' + request.api_version)
        when:
        controller.apiTokenList(inputUser)

        then:
        1 * controller.apiService.findUserTokensCreator('bob') >> [createdToken]
        1 * controller.apiService.requireApi(_, _) >> true
        1 * controller.apiService.requireVersion(_, _, _) >> true
        response.json.size() == 1
        response.json[0].id == "123uuid"

        where:
            inputUser <<['bob',null]

    }
    def "api token list authorized non-self"() {
        given:
        User bob = new User(login: 'bob')
        bob.save()
        AuthToken createdToken = new AuthToken(
                user: bob,
                type: AuthTokenType.USER,
                token: 'abc',
                authRoles: 'a,b',
                uuid: '123uuid',
                creator: 'elf',
                )
        createdToken.save()
        AuthToken webhookToken = new AuthToken(
            user: bob,
            type: AuthTokenType.WEBHOOK,
            tokenMode: AuthTokenMode.LEGACY,
            token: 'whk',
            authRoles: 'a,b',
            uuid: '123uuidwhk',
            creator: 'elf',
            )
        webhookToken.save()

        controller.apiService = Mock(ApiService) {
            hasTokenAdminAuth(_) >> { true }
        }
        controller.frameworkService = Mock(FrameworkService)
        controller.rundeckAuthContextProcessor=Mock(AppAuthContextProcessor)
        request.api_version = 33
        request.addHeader('accept', 'application/json')
        JSON.use('v' + request.api_version)
        when:
        controller.apiTokenList('bob')

        then:
        1 * controller.apiService.findUserTokensCreator('bob') >> [createdToken]
        1 * controller.apiService.requireApi(_, _) >> true
        1 * controller.apiService.requireVersion(_, _, _) >> true
        response.json.size() == 1
        response.json[0].id == "123uuid"

    }

    def "api token list unauthorized non-self"() {
        given:
        User bob = new User(login: 'bob')
        bob.save()
        AuthToken createdToken = new AuthToken(
                user: bob,
                type: AuthTokenType.USER,
                token: 'abc',
                authRoles: 'a,b',
                uuid: '123uuid',
                creator: 'elf',
                )
        createdToken.save()
        AuthToken webhookToken = new AuthToken(
            user: bob,
            type: AuthTokenType.WEBHOOK,
            tokenMode: AuthTokenMode.LEGACY,
            token: 'whk',
            authRoles: 'a,b',
            uuid: '123uuidwhk',
            creator: 'elf',
            )
        webhookToken.save()

        controller.apiService = Mock(ApiService) {
            hasTokenAdminAuth(_) >> { false }
        }
        controller.frameworkService = Mock(FrameworkService)
        controller.rundeckAuthContextProcessor=Mock(AppAuthContextProcessor){
            1 * getAuthContextForSubject(_)>>Mock(UserAndRolesAuthContext){
                1 * getUsername()>>'notbob'
            }
        }
        request.api_version = 33
        request.addHeader('accept', 'application/json')
        JSON.use('v' + request.api_version)
        when:
        controller.apiTokenList('bob')

        then:
        1 * controller.apiService.requireApi(_, _) >> true
        1 * controller.apiService.requireVersion(_, _, _) >> true
        1 * controller.apiService.renderUnauthorized(_, [AuthConstants.ACTION_ADMIN, 'User', 'bob'])
    }

    def "api token create v18"() {
        given:
        request.method = 'POST'
        params.user = 'bob'
        request.api_version = apivers
        request.addHeader('accept', 'application/json')

        controller.apiService = Mock(ApiService)
        controller.frameworkService = Mock(FrameworkService)
            controller.rundeckAuthContextProcessor=Mock(AppAuthContextProcessor)
        AuthToken createdToken = new AuthToken(
                user: new User(login: 'bob'),
                token: 'abc',
                authRoles: 'a,b',
                uuid: '123uuid',
                creator: 'elf',
                )
        createdToken.save(flush: true)
        def roles = AuthToken.parseAuthRoles('api_token_group')
        XML.use('v' + request.api_version)
        JSON.use('v' + request.api_version)
        when:
        controller.apiTokenCreate()

        then:
        1 * controller.apiService.requireApi(_, _) >> true
        1 * controller.apiService.generateUserToken(null, null, 'bob', roles, _, _, _) >> createdToken
        0 * controller.apiService._(*_)

        response.status == 201
        response.json == [
                user: "bob",
                id  : "abc"
        ]
        where:
        apivers | _
        18      | _

    }

    def "api token create v19"() {
        given:
        request.method = 'POST'
        request.api_version = apivers
        request.addHeader('accept', 'application/json')
        def requestJson = [
                user : 'bob',
                roles: ['a', 'b']
        ]

        controller.apiService = Mock(ApiService)
        controller.frameworkService = Mock(FrameworkService)
        controller.rundeckAuthContextProcessor=Mock(AppAuthContextProcessor)
        AuthToken createdToken = new AuthToken(
                user: new User(login: 'bob'),
                token: 'abc',
                authRoles: 'a,b',
                uuid: '123uuid',
                creator: 'elf',
                expiration: new Date(123)
                )
        createdToken.save(flush: true)
        def roles = AuthToken.parseAuthRoles('a,b')
        XML.use('v' + request.api_version)
        JSON.use('v' + request.api_version)
        when:
        controller.apiTokenCreate()

        then:
        1 * controller.apiService.requireApi(_, _) >> true
            1 * controller.rundeckAuthContextProcessor.getAuthContextForSubject(_) >> Mock(UserAndRolesAuthContext) {
            getUsername() >> 'bob'
            getRoles() >> (['a', 'z'] as Set)
        }
        1 * controller.apiService.parseJsonXmlWith(_, _, _) >> {
            it[2].json(requestJson)
            true
        }
        1 * controller.apiService.generateUserToken(_, null, 'bob', roles, _, _, _) >> createdToken
        0 * controller.apiService._(*_)

        response.status == 201
        response.json == [
                creator   : 'elf',
                expired   : true,
                roles     : ['a', 'b'],
                expiration: '1970-01-01T00:00:00Z',
                id        : '123uuid',
                user      : 'bob',
                token     : 'abc'
        ]

        where:
        apivers | _
        19      | _

    }
    def "api token create * roles"() {
        given:
        request.method = 'POST'
        request.api_version = apivers
        request.addHeader('accept', 'application/json')
        def requestJson = [
                user : 'bob',
                roles: ['*']
        ]

        controller.apiService = Mock(ApiService)
        controller.rundeckAuthContextProcessor=Mock(AppAuthContextProcessor)
        AuthToken createdToken = new AuthToken(
                user: new User(login: 'bob'),
                token: 'abc',
                authRoles: 'a,b',
                uuid: '123uuid',
                creator: 'elf',
                expiration: new Date(123)
                )
        createdToken.save(flush: true)
        def roles = AuthToken.parseAuthRoles('a,b')
        XML.use('v' + request.api_version)
        JSON.use('v' + request.api_version)
        when:
        controller.apiTokenCreate()

        then:
        1 * controller.apiService.requireApi(_, _) >> true
            1 * controller.rundeckAuthContextProcessor.getAuthContextForSubject(_) >> Mock(UserAndRolesAuthContext) {
            getUsername() >> 'bob'
            getRoles() >> (['a', 'z'] as Set)
        }
        1 * controller.apiService.parseJsonXmlWith(_, _, _) >> {
            it[2].json(requestJson)
            true
        }
        1 * controller.apiService.generateUserToken(_, null, 'bob', null, _, _, _) >> createdToken
        0 * controller.apiService._(*_)

        response.status == 201
        response.json == [
                creator   : 'elf',
                expired   : true,
                roles     : ['a', 'b'],
                expiration: '1970-01-01T00:00:00Z',
                id        : '123uuid',
                user      : 'bob',
                token     : 'abc'
        ]

        where:
        apivers | _
        19      | _

    }

    @Unroll
    def "api metrics links response"() {
        given:
        def endpoints = ['metrics', 'healthcheck', 'ping', 'threads']
        controller.apiService = Mock(ApiService)
        controller.configurationService = Mock(ConfigurationService)
        controller.grailsLinkGenerator = Mock(LinkGenerator) {
            _ * link(*_) >> {
                it[0].uri
            }
            0 * _(*_)
        }

        when:
        def result = controller.apiMetrics(input)

        then:

        response.forwardedUrl == (forwarded ? "/metrics/$input?" : null)
        response.json == (
            forwarded ? null : [
                '_links': links
            ]
        )

        1 * controller.apiService.requireVersion(_, _, 25) >> true

        1 * controller.configurationService.getBoolean("metrics.enabled", true) >> enabledAll
        (enabledAll?1:0) * controller.configurationService.getBoolean("metrics.api.enabled", true) >> enabledAll
        for (def i = 0; i < endpoints.size(); i++) {
            def endp = endpoints[i]
            (enabledAll ? 1 : 0) * controller.configurationService.getBoolean(
                "metrics.api.${endp}.enabled",
                true
            ) >> (enabledApi[endp] ? true : false)
        }
        (forwarded ? 1 : 0) * controller.configurationService.getString('metrics.servletUrlPattern', '/metrics/*') >>
        '/metrics/*'

        where:
        input | enabledApi                                                    | enabledAll   | forwarded | links
        ''    | [:]                                                           | false              | false     | [:]
        ''    | [:]                                                           | false  | false     | [:]
        ''    | [metrics: true]                                               | false | false     | [:]
        ''    | [metrics: true]                                               | true                                                                          | false                                                                                                | [metrics: [href: '/api/'+ApiVersions.API_CURRENT_VERSION+'/metrics/metrics']]
        ''    | [healthcheck: true]                                           | true                                                                      | false                                                                                                | [healthcheck: [href: '/api/'+ApiVersions.API_CURRENT_VERSION+'/metrics/healthcheck']]
        ''    | [ping: true]                                                  | true                                                                             | false                                                                                                | [ping: [href: '/api/'+ApiVersions.API_CURRENT_VERSION+'/metrics/ping']]
        ''    | [threads: true]                                               | true                                                                          | false                                                                                                | [threads: [href: '/api/'+ApiVersions.API_CURRENT_VERSION+'/metrics/threads']]
        ''    | [threads: true, ping: true, metrics: true, healthcheck: true] | true                            | false                                                                                                | [
            metrics    : [href: '/api/'+ApiVersions.API_CURRENT_VERSION+'/metrics/metrics'],
            threads    : [href: '/api/'+ApiVersions.API_CURRENT_VERSION+'/metrics/threads'],
            healthcheck: [href: '/api/'+ApiVersions.API_CURRENT_VERSION+'/metrics/healthcheck'],
            ping       : [href: '/api/'+ApiVersions.API_CURRENT_VERSION+'/metrics/ping'],
        ]
    }

    @Unroll
    def "api system info should return only basic data if user doesnt have read authorizarion"() {
        given:
        request.api_version = ApiVersions.V14
        request.addHeader('accept', 'application/json')
        session.subject = new Subject()
        controller.apiService = Mock(ApiService)
        controller.frameworkService = Mock(FrameworkService)
        controller.grailsLinkGenerator = Mock(LinkGenerator)
        controller.configurationService = Mock(ConfigurationService)
        controller.quartzScheduler = Mock(Scheduler) {
            (isAuth ? 1 : 0) * getCurrentlyExecutingJobs() >> []
            (isAuth ? 1 : 0) * getMetaData() >> Mock(SchedulerMetaData)
        }
        controller.rundeckAppAuthorizer = Mock(AppAuthorizer) {
            1 * system(_) >> Mock(AuthorizingSystem) {
                isAuthorized(RundeckAccess.System.READ_OR_OPS_ADMIN) >> isAuth
            }
        }
        def assetTaglib = mockTagLib(UtilityTagLib)

        when:
        controller.apiSystemInfo()

        then:
        1 * controller.apiService.requireApi(_, _) >> true
        1 * controller.apiService.renderSuccessJson(_,_) >> {
            JSONBuilder builder = new JSONBuilder();
            JSON json = builder.build(it[1]);
            json.toString() == jsonResult
        }

        where:
        isAuth| jsonResult
        false | '{"system":{"executions":null,"extended":null,"healthcheck":null,"jvm":null,"metrics":null,"os":null,"ping":null,"rundeck":{"apiversion":"41","base":null,"build":"@build.ident@","buildGit":null,"node":null,"serverUUID":null,"version":"4.4.0-SNAPSHOT"},"stats":null,"threadDump":null,"timestamp":null}}'
        true  | '{"system":{"executions":{"active":"false","executionMode":"passive"},"extended":null,"healthcheck":{"contentType":"application/json","href":null},"jvm":{"implementationVersion":"25.302-b08","name":"OpenJDK 64-Bit Server VM","vendor":"Oracle Corporation","version":"1.8.0_302"},"metrics":{"contentType":"application/json","href":null},"os":{"arch":"amd64","name":"Linux","version":"5.13.0-48-generic"},"ping":{"contentType":"text/plain","href":null},"rundeck":{"apiversion":"41","base":null,"build":"@build.ident@","buildGit":null,"node":null,"serverUUID":null,"version":"4.4.0-SNAPSHOT"},"stats":{"cpu":{"loadAverage":{"average":0,"unit":"percent"},"processors":12},"memory":{"free":601737712,"max":773324800,"total":773324800,"unit":"byte"},"scheduler":{"running":0,"threadPoolSize":0},"threads":{"active":12},"uptime":{"duration":351050,"since":{"datetime":"2022-06-14T19:27:17Z","epoch":1655234837036,"unit":"ms"},"unit":"ms"}},"threadDump":{"contentType":"text/plain","href":null},"timestamp":{"datetime":"2022-06-14T19:33:08Z","epoch":1655235188086,"unit":"ms"}}}'
    }

    @Unroll
    def "api metrics forwarding"() {
        given:
        def endpoints = ['metrics', 'healthcheck', 'ping', 'threads']
        controller.apiService = Mock(ApiService)
        controller.configurationService = Mock(ConfigurationService)
        controller.grailsLinkGenerator = Mock(LinkGenerator) {
            _ * link(*_) >> {
                it[0].uri
            }
            0 * _(*_)
        }
        when:
        def result = controller.apiMetrics(input)

        then:

        response.forwardedUrl == "/metrics/$input?"


        1 * controller.apiService.requireVersion(_, _, 25) >> true


        1 * controller.configurationService.getBoolean("metrics.enabled", true) >> true
        1 * controller.configurationService.getBoolean("metrics.api.enabled", true) >> true
        for (def i = 0; i < endpoints.size(); i++) {
            def endp = endpoints[i]
            1 * controller.configurationService.getBoolean(
                "metrics.api.${endp}.enabled",
                true
            ) >> true
        }
        1 * controller.configurationService.getString('metrics.servletUrlPattern', '/metrics/*') >>
        '/metrics/*'

        where:
        input << ['metrics', 'ping', 'threads', 'healthcheck']
    }


    @Unroll
    def "system auth access for endpoint #endpoint"() {
        when:
            def result = getControllerMethodAnnotation(endpoint, RdAuthorizeSystem)
        then:
            result.value() == access
        where:
            endpoint        | access
            'apiMetrics'    | RundeckAccess.System.AUTH_READ_OR_ANY_ADMIN
    }

    private <T extends Annotation> T getControllerMethodAnnotation(String name, Class<T> clazz) {
        artefactInstance.getClass().getDeclaredMethods().find { it.name == name }.getAnnotation(clazz)
    }

    @Unroll
    def "api metrics bad endpoint"() {
        given:
        def endpoints = ['metrics', 'healthcheck', 'ping', 'threads']
        controller.apiService = Mock(ApiService)
        controller.configurationService = Mock(ConfigurationService)
        controller.grailsLinkGenerator = Mock(LinkGenerator) {
            _ * link(*_) >> {
                it[0].uri
            }
            0 * _(*_)
        }
        when:
        def result = controller.apiMetrics(input)

        then:

        response.forwardedUrl == null
        response.status == 404


        1 * controller.apiService.requireVersion(_, _, 25) >> true

        1 * controller.configurationService.getBoolean("metrics.enabled", true) >> true
        1 * controller.configurationService.getBoolean("metrics.api.enabled", true) >> true
        for (def i = 0; i < endpoints.size(); i++) {
            def endp = endpoints[i]
            1 * controller.configurationService.getBoolean(
                "metrics.api.${endp}.enabled",
                true
            ) >> true
        }
        1 * controller.apiService.renderErrorFormat(
            _, {
            it.code == 'api.error.invalid.request' && it.status == 404
        }
        ) >> { it[0].status = it[1].status }
        where:
        input << ['blah', 'asdfasdf']
    }

    @Unroll
    def "api metrics disabled"() {
        given:
        def endpoints = ['metrics', 'healthcheck', 'ping', 'threads']
        controller.apiService = Mock(ApiService)
        controller.configurationService = Mock(ConfigurationService)
        controller.grailsLinkGenerator = Mock(LinkGenerator) {
            _ * link(*_) >> {
                it[0].uri
            }
            0 * _(*_)
        }
        when:
        def result = controller.apiMetrics(input)

        then:

        response.forwardedUrl == null
        response.status == 404

        1 * controller.apiService.requireVersion(_, _, 25) >> true

        1 * controller.configurationService.getBoolean("metrics.enabled", true) >> enabledAll
        (enabledAll?1:0) * controller.configurationService.getBoolean("metrics.api.enabled", true) >> enabledAll
        for (def i = 0; i < endpoints.size(); i++) {
            def endp = endpoints[i]
            (enabledAll ? 1 : 0) * controller.configurationService.getBoolean(
                "metrics.api.${endp}.enabled",
                true
            ) >> (enabledApi[endp] ? true : false)
        }
        1 * controller.apiService.renderErrorFormat(
            _, {
            it.code == 'api.error.invalid.request' && it.status == 404
        }
        ) >> { it[0].status = it[1].status }

        where:
        input         | enabledApi       | enabledAll
        'metrics'     | [metrics: false] | true
        'metrics'     | [metrics: true]  | false
        'healthcheck' | [metrics: true]  | true
        'ping'        | [metrics: true]  | true
        'threads'     | [metrics: true]  | true
    }


}
