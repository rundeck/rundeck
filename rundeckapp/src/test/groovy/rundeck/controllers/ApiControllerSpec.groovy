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
import com.dtolabs.rundeck.core.authorization.UserAndRolesAuthContext
import grails.converters.JSON
import grails.converters.XML
import grails.test.mixin.Mock
import grails.test.mixin.TestFor
import rundeck.AuthToken
import rundeck.User
import rundeck.services.ApiService
import rundeck.services.FrameworkService
import spock.lang.Specification

@TestFor(ApiController)
@Mock([User, AuthToken])
class ApiControllerSpec extends Specification {


    def setup() {

        def apiMarshallerRegistrar = new ApiMarshallerRegistrar()
        apiMarshallerRegistrar.registerMarshallers()
        apiMarshallerRegistrar.registerApiMarshallers()
    }

    def "api token create v18"() {
        given:
        request.method = 'POST'
        params.user = 'bob'
        request.api_version = apivers
        request.addHeader('accept', 'application/json')

        controller.apiService = Mock(ApiService)
        controller.frameworkService = Mock(FrameworkService)
        AuthToken createdToken = new AuthToken(
                user: new User(login: 'bob'),
                token: 'abc',
                authRoles: 'a,b',
                uuid: '123uuid',
                creator: 'elf',
                )
        def roles = AuthToken.parseAuthRoles('api_token_group')
        XML.use('v' + request.api_version)
        JSON.use('v' + request.api_version)
        when:
        controller.apiTokenCreate()

        then:
        1 * controller.apiService.requireApi(_, _) >> true
        1 * controller.apiService.generateUserToken(null, null, 'bob', roles) >> createdToken
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
        AuthToken createdToken = new AuthToken(
                user: new User(login: 'bob'),
                token: 'abc',
                authRoles: 'a,b',
                uuid: '123uuid',
                creator: 'elf',
                expiration: new Date(123)
                )
        def roles = AuthToken.parseAuthRoles('a,b')
        XML.use('v' + request.api_version)
        JSON.use('v' + request.api_version)
        when:
        controller.apiTokenCreate()

        then:
        1 * controller.apiService.requireApi(_, _) >> true
        1 * controller.frameworkService.getAuthContextForSubject(_) >> Mock(UserAndRolesAuthContext) {
            getUsername() >> 'bob'
            getRoles() >> (['a', 'z'] as Set)
        }
        1 * controller.apiService.parseJsonXmlWith(_, _, _) >> {
            it[2].json(requestJson)
            true
        }
        1 * controller.apiService.generateUserToken(_, null, 'bob', roles) >> createdToken
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
        controller.frameworkService = Mock(FrameworkService)
        AuthToken createdToken = new AuthToken(
                user: new User(login: 'bob'),
                token: 'abc',
                authRoles: 'a,b',
                uuid: '123uuid',
                creator: 'elf',
                expiration: new Date(123)
                )
        def roles = AuthToken.parseAuthRoles('a,b')
        XML.use('v' + request.api_version)
        JSON.use('v' + request.api_version)
        when:
        controller.apiTokenCreate()

        then:
        1 * controller.apiService.requireApi(_, _) >> true
        1 * controller.frameworkService.getAuthContextForSubject(_) >> Mock(UserAndRolesAuthContext) {
            getUsername() >> 'bob'
            getRoles() >> (['a', 'z'] as Set)
        }
        1 * controller.apiService.parseJsonXmlWith(_, _, _) >> {
            it[2].json(requestJson)
            true
        }
        1 * controller.apiService.generateUserToken(_, null, 'bob', null) >> createdToken
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
}
