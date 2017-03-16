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

package rundeck.services

import com.dtolabs.rundeck.core.authorization.UserAndRolesAuthContext
import com.dtolabs.rundeck.core.authorization.Validation
import grails.converters.JSON
import grails.test.mixin.Mock
import grails.test.mixin.TestFor
import grails.test.mixin.TestMixin
import grails.test.mixin.web.ControllerUnitTestMixin
import grails.web.JSONBuilder
import groovy.xml.MarkupBuilder
import rundeck.AuthToken
import rundeck.User
import spock.lang.Specification

import java.time.Clock
import java.time.Instant
import java.time.ZoneId

/**
 * Created by greg on 7/28/15.
 */
@TestFor(ApiService)
@TestMixin(ControllerUnitTestMixin)
@Mock([User, AuthToken])
class ApiServiceSpec extends Specification {
    def "renderWrappedFileContents json"(){
        given:
        def builder = new JSONBuilder()
        when:
        def result=builder.build {
            service.renderWrappedFileContents('x','json',delegate)
        }

        then:
        result.toString()=='{"contents":"x"}'

    }
    def "renderWrappedFileContents xml"(){
        given:
        def sw = new StringWriter()
        def builder = new MarkupBuilder(sw)
        when:
        service.renderWrappedFileContents('x','xml',builder)

        then:
        sw.toString()=='<contents><![CDATA[x]]></contents>'

    }
    def "jsonRenderDirlist"(){
        given:
        def builder = new JSONBuilder()
        when:
        def result=builder.build {
            service.jsonRenderDirlist(
                    '',
                    {it},
                    {"http://localhost:8080/api/14/project/test/acl/${it}"},
                    ['blah.aclpolicy','adir/'],
                    delegate
            )
        }
        def parsed=JSON.parse(result.toString())

        then:
        parsed==[
                resources:[
                        [
                                name:'blah.aclpolicy',
                                path: 'blah.aclpolicy',
                                type: 'file',
                                href: 'http://localhost:8080/api/14/project/test/acl/blah.aclpolicy'

                        ],
                        [
                                path:'adir/',
                                type: 'directory',
                                href: 'http://localhost:8080/api/14/project/test/acl/adir/'
                        ]
                ],
                path:'',
                type:'directory',
                href:'http://localhost:8080/api/14/project/test/acl/']
    }
    def "xmlRenderDirList"(){
        given:
        def sw = new StringWriter()
        def builder = new MarkupBuilder(sw)
        when:
        service.xmlRenderDirList(
                '',
                {it},
                {"http://localhost:8080/api/14/project/test/acl/${it}"},
                ['blah.aclpolicy','adir/'],
                builder
        )
        def parsed=new XmlSlurper().parse(new StringReader(sw.toString()))

        then:
        parsed.'@path'.text()==''
        parsed.'@type'.text()=='directory'
        parsed.'@href'.text()=='http://localhost:8080/api/14/project/test/acl/'
        parsed.contents.size()==1
        parsed.contents.resource.size()==2
        parsed.contents.resource[0].'@path'.text()=='blah.aclpolicy'
        parsed.contents.resource[0].'@name'.text()=='blah.aclpolicy'
        parsed.contents.resource[0].'@type'.text()=='file'
        parsed.contents.resource[0].'@href'.text()=='http://localhost:8080/api/14/project/test/acl/blah.aclpolicy'
        parsed.contents.resource[1].'@path'.text()=='adir/'
        parsed.contents.resource[1].'@type'.text()=='directory'
        parsed.contents.resource[1].'@href'.text()=='http://localhost:8080/api/14/project/test/acl/adir/'
    }

    def "renderJsonAclpolicyValidation"(){
        given:

        def builder = new JSONBuilder()
        when:
        def validation=Stub(Validation){
            isValid()>>false
            getErrors()>>['file1[1]':['error1','error2'],'file2[1]':['error3','error4']]
        }
        def result=builder.build {
            service.renderJsonAclpolicyValidation(
                    validation,
                    delegate
            )
        }
        def parsed=JSON.parse(result.toString())
        then:
        parsed==[valid:false,
        policies:[
                [policy:'file1[1]',errors:['error1','error2']],
                [policy:'file2[1]',errors:['error3','error4']]
        ]]
    }
    def "renderXmlAclpolicyValidation"(){
        given:
        def sw = new StringWriter()
        def builder = new MarkupBuilder(sw)
        when:
        def validation=Stub(Validation){
            isValid()>>false
            getErrors()>>['file1[1]':['error1','error2'],'file2[1]':['error3','error4']]
        }
        service.renderXmlAclpolicyValidation(
                validation,
                builder
        )
        def parsed=new XmlSlurper().parse(new StringReader(sw.toString()))
        then:
        parsed.name()=='validation'
        parsed.'@valid'.text()=='false'
        parsed.'policy'.size()==2
        parsed.'policy'[0].'@id'.text()=='file1[1]'
        parsed.'policy'[0].'error'.size()==2
        parsed.'policy'[0].'error'[0].text()=='error1'
        parsed.'policy'[0].'error'[1].text()=='error2'

        parsed.'policy'[1].'@id'.text()=='file2[1]'
        parsed.'policy'[1].'error'.size()==2
        parsed.'policy'[1].'error'[0].text()=='error3'
        parsed.'policy'[1].'error'[1].text()=='error4'

    }

    def "generateTokenExpirationDate"() {
        given:
        service.systemClock = Clock.fixed(Instant.ofEpochMilli(1000), ZoneId.of("Z"))
        when:
        def result = service.generateTokenExpirationDate(duration, max)
        then:
        result == expected
        where:
        duration | max | expected
        123      | 200 | [date: new Date(124000), max: false]
        123      | 100 | [date: new Date(101000), max: true]
        0        | 100 | [date: new Date(101000), max: false]
        200      | 0   | [date: new Date(201000), max: false]
        0        | 0   | [date: null, max: false]

    }

    def "generate user token unauthorized"() {
        given:
        def auth = Mock(UserAndRolesAuthContext) {
            getUsername() >> 'auser'
            getRoles() >> (['role1', 'role2'] as Set)
        }
        def tokenUser = 'auser'
        def tokenRoles = ['role1'] as Set

        def tokenTime = 3600

        service.frameworkService = Mock(FrameworkService)
        service.configurationService = Mock(ConfigurationService)
        service.userService = Mock(UserService)

        when:
        def result = service.generateUserToken(auth, tokenTime, tokenUser, tokenRoles)
        then:
        service.frameworkService.authorizeApplicationResourceType(auth, 'user', _) >> false
        Exception e = thrown()
        e.message =~ /Unauthorized/
    }

    def "generate self token wrong username unauthorized"() {
        given:
        def auth = Mock(UserAndRolesAuthContext) {
            getUsername() >> 'auser'
            getRoles() >> (['role1', 'role2'] as Set)
        }
        def tokenRoles = ['role1'] as Set

        def tokenTime = 3600

        service.frameworkService = Mock(FrameworkService)
        service.configurationService = Mock(ConfigurationService)
        service.userService = Mock(UserService)
        def user = new User(login: 'auser')

        when:
        def result = service.generateUserToken(auth, tokenTime, tokenUser, tokenRoles)
        then:
        service.frameworkService.authorizeApplicationResourceType(auth, 'user', 'generate_self_token') >> true
        service.userService.findOrCreateUser('auser') >> user
        Exception e = thrown()
        e.message =~ /Unauthorized/

        where:
        tokenUser  | _
        'notauser' | _
    }

    def "generate user token exceed max duration"() {
        given:
        def auth = Mock(UserAndRolesAuthContext) {
            getUsername() >> 'auser'
            getRoles() >> (['role1', 'role2'] as Set)
        }
        def tokenUser = 'auser'
        def tokenRoles = ['role1'] as Set

        def tokenTime = 3600

        service.frameworkService = Mock(FrameworkService)
        service.configurationService = Mock(ConfigurationService)
        service.userService = Mock(UserService)
        def user = new User(login: 'auser')

        when:
        def result = service.generateUserToken(auth, tokenTime, tokenUser, tokenRoles)
        then:
        service.frameworkService.authorizeApplicationResourceType(auth, 'user', 'admin') >> true
        service.configurationService.getInteger("api.tokens.duration.max", 0) >> 123
        service.userService.findOrCreateUser('auser') >> user
        Exception e = thrown()
        e.message =~ /Duration exceeds maximum allowed: /
    }

    def "generate user token wrong group user auth"() {
        given:
        def auth = Mock(UserAndRolesAuthContext) {
            getUsername() >> 'auser'
            getRoles() >> (['role1', 'role2'] as Set)
        }
        def tokenUser = 'auser'
        def tokenRoles = ['role3'] as Set

        def tokenTime = 3600

        service.frameworkService = Mock(FrameworkService)
        service.configurationService = Mock(ConfigurationService)
        service.userService = Mock(UserService)
        def user = new User(login: 'auser')

        when:
        def result = service.generateUserToken(auth, tokenTime, tokenUser, tokenRoles)
        then:
        service.frameworkService.authorizeApplicationResourceType(auth, 'user', 'generate_self_token') >> true
        service.userService.findOrCreateUser('auser') >> user
        Exception e = thrown()
        e.message =~ /Invalid Group/
    }

    def "generate user token own groups any auth"() {
        given:
        def auth = Mock(UserAndRolesAuthContext) {
            getUsername() >> 'auser'
            getRoles() >> (['role1', 'role2'] as Set)
        }
        def tokenUser = 'auser'
        def tokenRoles = ['role1'] as Set

        def tokenTime = 3600

        service.frameworkService = Mock(FrameworkService)
        service.configurationService = Mock(ConfigurationService)
        service.userService = Mock(UserService)
        def user = new User(login: 'auser')

        when:
        def result = service.generateUserToken(auth, tokenTime, tokenUser, tokenRoles)
        then:
        result != null
        result.user == user
        result.authRoles == 'role1'
        result.authRolesSet() == tokenRoles
        result.expiration != null
        service.frameworkService.authorizeApplicationResourceType(auth, 'user', authrole) >> true
        service.userService.findOrCreateUser('auser') >> user

        where:
        authrole                 | _
        'admin'                  | _
        'generate_self_token'    | _
        'generate_service_token' | _
    }

    def "generate user token service groups service/admin auth"() {
        given:
        def auth = Mock(UserAndRolesAuthContext) {
            getUsername() >> 'auser'
            getRoles() >> (['role1', 'role2'] as Set)
        }
        def tokenUser = 'auser'
        def tokenRoles = ['role1', 'svc_roleA'] as Set

        def tokenTime = 3600

        service.frameworkService = Mock(FrameworkService)
        service.configurationService = Mock(ConfigurationService)
        service.userService = Mock(UserService)
        def user = new User(login: 'auser')

        when:
        def result = service.generateUserToken(auth, tokenTime, tokenUser, tokenRoles)
        then:
        result != null
        result.user == user
        result.authRoles == 'role1,svc_roleA'
        result.authRolesSet() == tokenRoles
        result.expiration != null
        service.frameworkService.authorizeApplicationResourceType(auth, 'user', authrole) >> true
        service.userService.findOrCreateUser('auser') >> user
        service.configurationService.getString('api.tokens.allowed.service.roles', null) >> 'svc_roleA,svc_roleB'

        where:
        authrole                 | _
        'admin'                  | _
        'generate_service_token' | _
    }

    def "generate service token service username service/admin auth"() {
        given:
        def auth = Mock(UserAndRolesAuthContext) {
            getUsername() >> 'auser'
            getRoles() >> (['role1', 'role2'] as Set)
        }
        def tokenRoles = ['role1', 'svc_roleA'] as Set

        def tokenTime = 3600

        service.frameworkService = Mock(FrameworkService)
        service.configurationService = Mock(ConfigurationService)
        service.userService = Mock(UserService) {
            findOrCreateUser(tokenUser) >> new User(login: tokenUser)
        }

        when:
        def result = service.generateUserToken(auth, tokenTime, tokenUser, tokenRoles)
        then:
        result != null
        result.user.login == tokenUser
        result.authRoles == 'role1,svc_roleA'
        result.authRolesSet() == tokenRoles
        result.expiration != null
        service.frameworkService.authorizeApplicationResourceType(auth, 'user', authrole) >> true
        service.configurationService.getString('api.tokens.allowed.service.roles', null) >> 'svc_roleA,svc_roleB'
        service.configurationService.getString('api.tokens.allowed.service.names', null) >> allowedUsers

        where:
        authrole                 | tokenUser  | allowedUsers | _
        'admin'                  | 'someuser' | 'someuser'   | _
        'generate_service_token' | 'someuser' | 'someuser'   | _
    }

    def "generate service token service username not allowed username"() {
        given:
        def auth = Mock(UserAndRolesAuthContext) {
            getUsername() >> 'auser'
            getRoles() >> (['role1', 'role2'] as Set)
        }
        def tokenRoles = ['role1', 'svc_roleA'] as Set

        def tokenTime = 3600

        service.frameworkService = Mock(FrameworkService)
        service.configurationService = Mock(ConfigurationService)
        service.userService = Mock(UserService) {
            findOrCreateUser(tokenUser) >> new User(login: tokenUser)
        }

        when:
        def result = service.generateUserToken(auth, tokenTime, tokenUser, tokenRoles)
        then:
        service.frameworkService.authorizeApplicationResourceType(auth, 'user', authrole) >> true
        service.configurationService.getString('api.tokens.allowed.service.roles', null) >> 'svc_roleA,svc_roleB'
        service.configurationService.getString('api.tokens.allowed.service.names', null) >> allowedUsers
        Exception e = thrown()
        e.message =~ /Invalid Token Username/

        where:
        authrole                 | tokenUser   | allowedUsers | _
        'generate_service_token' | 'someBuser' | 'someuser'   | _
    }

    def "generate user token service groups no service auth"() {
        given:
        def auth = Mock(UserAndRolesAuthContext) {
            getUsername() >> 'auser'
            getRoles() >> (['role1', 'role2'] as Set)
        }
        def tokenUser = 'auser'
        def tokenRoles = ['role1', 'svc_roleA'] as Set

        def tokenTime = 3600

        service.frameworkService = Mock(FrameworkService)
        service.configurationService = Mock(ConfigurationService)
        service.userService = Mock(UserService)
        def user = new User(login: 'auser')

        when:
        def result = service.generateUserToken(auth, tokenTime, tokenUser, tokenRoles)
        then:
        service.frameworkService.authorizeApplicationResourceType(auth, 'user', authrole) >> true
        service.userService.findOrCreateUser('auser') >> user
        service.configurationService.getString('api.tokens.allowed.service.roles', null) >> 'svc_roleA,svc_roleB'
        Exception e = thrown()
        e.message =~ /Invalid Group/

        where:
        authrole              | _
        'generate_self_token' | _
    }

    def "generate user token any groups admin auth"() {
        given:
        def auth = Mock(UserAndRolesAuthContext) {
            getUsername() >> 'auser'
            getRoles() >> (['role1', 'role2'] as Set)
        }
        def tokenRoles = ['role1', 'svc_roleA', 'any_role'] as Set

        def tokenTime = 3600

        service.frameworkService = Mock(FrameworkService)
        service.configurationService = Mock(ConfigurationService)
        service.userService = Mock(UserService)
        def user = new User(login: tokenUser)

        when:
        def result = service.generateUserToken(auth, tokenTime, tokenUser, tokenRoles)
        then:
        result != null
        result.user == user
        result.authRolesSet() == tokenRoles
        result.expiration != null
        service.frameworkService.authorizeApplicationResourceType(auth, 'user', authrole) >> true
        service.userService.findOrCreateUser(tokenUser) >> user
        service.configurationService.getString('api.tokens.allowed.service.roles', null) >> 'svc_roleA,svc_roleB'

        where:
        authrole | tokenUser | _
        'admin'  | 'auser'   | _
        'admin'  | 'anyuser' | _
    }
    def "generate user token any groups no admin auth"() {
        given:
        def auth = Mock(UserAndRolesAuthContext) {
            getUsername() >> 'auser'
            getRoles() >> (['role1', 'role2'] as Set)
        }
        def tokenUser = 'auser'
        def tokenRoles = ['role1', 'svc_roleA', 'any_role'] as Set

        def tokenTime = 3600

        service.frameworkService = Mock(FrameworkService)
        service.configurationService = Mock(ConfigurationService)
        service.userService = Mock(UserService)
        def user = new User(login: 'auser')

        when:
        def result = service.generateUserToken(auth, tokenTime, tokenUser, tokenRoles)
        then:
        service.frameworkService.authorizeApplicationResourceType(auth, 'user', authrole) >> true
        service.userService.findOrCreateUser('auser') >> user
        service.configurationService.getString('api.tokens.allowed.service.roles', null) >> 'svc_roleA,svc_roleB'
        Exception e = thrown()
        e.message =~ /Invalid Group/

        where:
        authrole                 | _
        'generate_service_token' | _
        'generate_self_token'    | _
    }
}
