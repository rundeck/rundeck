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
import grails.web.mapping.LinkGenerator
import org.rundeck.app.authorization.AppAuthContextProcessor
import rundeck.AuthToken
import rundeck.User
import rundeck.services.ApiService
import rundeck.services.ConfigurationService
import rundeck.services.FrameworkService
import spock.lang.Specification
import spock.lang.Unroll

class ApiControllerSpec extends Specification implements ControllerUnitTest<ApiController>, DataTest {


    def setup() {
        mockDomain User
        mockDomain AuthToken
        def apiMarshallerRegistrar = new ApiMarshallerRegistrar()
        apiMarshallerRegistrar.registerMarshallers()
        apiMarshallerRegistrar.registerApiMarshallers()
    }

    def "api token list does not include webhook tokens"() {
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
        controller.apiTokenList()

        then:
        1 * controller.apiService.requireApi(_, _) >> true
        1 * controller.apiService.requireVersion(_, _, _) >> true
        response.json.size() == 1
        response.json[0].id == "123uuid"

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

            setAuthProcessor()
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
            setAuthProcessor()
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
    def "api metrics unauthorized"() {
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

            setAuthProcessor(false)
        when:
        def result = controller.apiMetrics(input)

        then:

        response.status == 403


        1 * controller.apiService.requireVersion(_, _, 25) >> true

        1 * controller.apiService.renderErrorFormat(
            _, {
            it.code == 'api.error.item.unauthorized' && it.status == 403
        }
        ) >> { it[0].status = it[1].status }

        where:
        input << ['metrics', 'ping', 'threads', 'healthcheck']
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
            setAuthProcessor()
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
            setAuthProcessor()
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

    public void setAuthProcessor(boolean allow=true) {
        controller.rundeckAuthContextProcessor = Mock(AppAuthContextProcessor) {
            1 * getAuthContextForSubject(_)

            1 * authorizeApplicationResource(_, [type: 'resource', kind: 'system'], 'read') >> allow
        }
    }
}
