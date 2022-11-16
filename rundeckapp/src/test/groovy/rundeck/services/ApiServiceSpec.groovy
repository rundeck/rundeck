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

import com.dtolabs.rundeck.app.api.ApiVersions
import com.dtolabs.rundeck.core.authorization.UserAndRolesAuthContext
import com.dtolabs.rundeck.core.authorization.Validation
import grails.converters.JSON
import grails.testing.gorm.DataTest
import grails.testing.web.controllers.ControllerUnitTest
import grails.web.JSONBuilder
import groovy.xml.MarkupBuilder
import org.grails.plugins.codecs.JSONCodec
import org.rundeck.app.authorization.AppAuthContextEvaluator
import org.rundeck.app.data.providers.GormTokenDataProvider
import org.rundeck.app.web.WebUtilService
import org.rundeck.core.auth.AuthConstants
import org.rundeck.spi.data.DataManager
import rundeck.AuthToken
import rundeck.User
import rundeck.controllers.ApiController
import rundeck.services.data.AuthTokenDataService
import spock.lang.Specification
import spock.lang.Unroll

import java.time.Clock
import java.time.Instant
import java.time.ZoneId

/**
 * Created by greg on 7/28/15.
 */
class ApiServiceSpec extends Specification implements ControllerUnitTest<ApiController>, DataTest {

    ApiService service
    GormTokenDataProvider provider = new GormTokenDataProvider()

    void setup() {
        mockCodec(JSONCodec)
        mockDomains(AuthToken, User)
        mockDataService(AuthTokenDataService)

        provider.authTokenDataService = applicationContext.getBean(AuthTokenDataService)

        provider.userService = Mock(UserService){
            findOrCreateUser(_) >>  new User(login: 'auser')
        }
        service = new ApiService()
        service.rundeckDataManager =  Mock(DataManager){
            getProviderForType(_) >>  {
                provider
            }
        }
    }

    def "renderWrappedFileContents xml"(){
        given:
        def sw = new StringWriter()
        def builder = new MarkupBuilder(sw)
        when:
        service.renderWrappedFileContentsXml('x','xml',builder)

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
                    ['blah.aclpolicy','adir/']
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
                    validation
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


        service.rundeckAuthContextEvaluator=Mock(AppAuthContextEvaluator)
        service.configurationService = Mock(ConfigurationService)
        service.userService = Mock(UserService)

        when:
        def result = service.generateUserToken(auth, tokenTime, tokenUser, tokenRoles)
        then:
        service.rundeckAuthContextEvaluator.authorizeApplicationResourceType(auth, 'apitoken', _) >> false
        Exception e = thrown()
        e.message =~ /Unauthorized/
    }

    def "check token authorization unauthorized"() {
        given:
        def auth = Mock(UserAndRolesAuthContext) {
            getUsername() >> 'auser'
            getRoles() >> (['role1', 'role2'] as Set)
        }
        def tokenUser = 'auser'
        def tokenRoles = ['role1'] as Set


        service.rundeckAuthContextEvaluator=Mock(AppAuthContextEvaluator)
        service.configurationService = Mock(ConfigurationService)
        service.userService = Mock(UserService)

        when:
        def result = service.checkTokenAuthorization(auth, tokenUser, tokenRoles)
        then:
        service.rundeckAuthContextEvaluator.authorizeApplicationResourceType(auth, 'apitoken', _) >> false
        !result.authorized
        result.message =~ /Unauthorized/
    }

    def "generate user token wrong username unauthorized"() {
        given:
        def auth = Mock(UserAndRolesAuthContext) {
            getUsername() >> 'auser'
            getRoles() >> (['role1', 'role2'] as Set)
        }
        def tokenRoles = ['role1'] as Set

        def tokenTime = 3600

        service.rundeckAuthContextEvaluator=Mock(AppAuthContextEvaluator)
        service.configurationService = Mock(ConfigurationService)
        service.userService = Mock(UserService)
        def user = new User(login: 'auser')

        when:
        def result = service.generateUserToken(auth, tokenTime, tokenUser, tokenRoles)
        then:
        service.rundeckAuthContextEvaluator.authorizeApplicationResourceType(auth, 'apitoken', 'generate_user_token') >> true
        service.userService.findOrCreateUser('auser') >> user
        Exception e = thrown()
        e.message =~ /Unauthorized/

        where:
        tokenUser  | _
        'notauser' | _
    }

    def "check token auth wrong username unauthorized"() {
        given:
        def auth = Mock(UserAndRolesAuthContext) {
            getUsername() >> 'auser'
            getRoles() >> (['role1', 'role2'] as Set)
        }
        def tokenRoles = ['role1'] as Set

        def tokenTime = 3600

        service.rundeckAuthContextEvaluator=Mock(AppAuthContextEvaluator)
        service.configurationService = Mock(ConfigurationService)
        service.userService = Mock(UserService)
        def user = new User(login: 'auser')

        when:
        def result = service.checkTokenAuthorization(auth, tokenUser, tokenRoles)
        then:
        service.rundeckAuthContextEvaluator.authorizeApplicationResourceType(auth, 'apitoken', 'generate_user_token') >> true
        service.userService.findOrCreateUser('auser') >> user
        !result.authorized
        result.message =~ /Unauthorized/

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

        def tokenTime = 124

        service.rundeckAuthContextEvaluator=Mock(AppAuthContextEvaluator)
        service.configurationService = Mock(ConfigurationService)
        service.userService = Mock(UserService)
        def user = new User(login: 'auser')

        when:
        def result = service.generateUserToken(auth, tokenTime, tokenUser, tokenRoles)
        then:
            service.rundeckAuthContextEvaluator.authorizeApplicationResourceAny(auth,
                                                                                AuthConstants.RESOURCE_TYPE_APITOKEN, [AuthConstants.ACTION_ADMIN, AuthConstants.ACTION_APP_ADMIN]) >> true
        service.configurationService.getString("api.tokens.duration.max", null) >> '123'
        service.userService.findOrCreateUser('auser') >> user
        Exception e = thrown()
        e.message =~ /Duration exceeds maximum allowed: /
    }

    @Unroll
    def "generate user token own groups any auth"() {
        given:
        def auth = Mock(UserAndRolesAuthContext) {
            getUsername() >> 'auser'
            getRoles() >> (['role1', 'role2'] as Set)
        }
        def tokenUser = 'auser'
        def tokenRoles = ['role1'] as Set

        def tokenTime = 3600

        service.rundeckAuthContextEvaluator=Mock(AppAuthContextEvaluator)
        service.configurationService = Mock(ConfigurationService)
        service.userService = Mock(UserService)
        def user = new User(login: 'auser').save()

        when:
        def result = service.generateUserToken(auth, tokenTime, tokenUser, tokenRoles)
        then:
        result != null
        result.user.login == user.login
        result.generateAuthRoles(result.getAuthRolesSet()) == 'role1'
        result.getAuthRolesSet() == tokenRoles
        result.expiration != null
        _ * service.rundeckAuthContextEvaluator.authorizeApplicationResourceAny(auth, AuthConstants.RESOURCE_TYPE_APITOKEN, [AuthConstants.ACTION_ADMIN, AuthConstants.ACTION_APP_ADMIN]) >>
                (tokenaction == 'admin')
        _ * service.rundeckAuthContextEvaluator.authorizeApplicationResourceAny(auth, AuthConstants.RESOURCE_TYPE_USER, [AuthConstants.ACTION_ADMIN, AuthConstants.ACTION_APP_ADMIN]) >> false
        _ * service.rundeckAuthContextEvaluator.authorizeApplicationResource(auth, AuthConstants.RESOURCE_TYPE_APITOKEN, AuthConstants.ACTION_GENERATE_SERVICE_TOKEN) >>
                ((tokenaction == 'generate_service_token'))
        _ * service.rundeckAuthContextEvaluator.authorizeApplicationResource(auth, AuthConstants.RESOURCE_TYPE_APITOKEN, tokenaction) >> true

        service.userService.findOrCreateUser('auser') >> user

        where:
        tokenaction              | _
        'admin'                  | _
        'generate_user_token'    | _
        'generate_service_token' | _
    }

    @Unroll
    def "generate service token service groups #tokenaction auth"() {
        given:
        def auth = Mock(UserAndRolesAuthContext) {
            getUsername() >> 'auser'
            getRoles() >> (['role1', 'role2'] as Set)
        }
        def tokenUser = 'auser'
        def tokenRoles = ['role1', 'svc_roleA'] as Set

        def tokenTime = 3600

        service.rundeckAuthContextEvaluator=Mock(AppAuthContextEvaluator)
        service.configurationService = Mock(ConfigurationService)
        service.userService = Mock(UserService)
        def user = new User(login: 'auser').save()

        when:
        def result = service.generateUserToken(auth, tokenTime, tokenUser, tokenRoles)
        then:
        result != null
        result.user.login == user.login
        result.authRoles == 'role1,svc_roleA'
        result.getAuthRolesSet() == tokenRoles
        result.expiration != null
        if (tokenaction == 'admin') {
            1 * service.rundeckAuthContextEvaluator.authorizeApplicationResourceAny(auth, AuthConstants.RESOURCE_TYPE_APITOKEN, [AuthConstants.ACTION_ADMIN, AuthConstants.ACTION_APP_ADMIN]) >> true
            0 * service.rundeckAuthContextEvaluator.authorizeApplicationResource(*_)
        } else {
            1 * service.rundeckAuthContextEvaluator.authorizeApplicationResource(auth, AuthConstants.RESOURCE_TYPE_APITOKEN, tokenaction) >> true
            1 * service.rundeckAuthContextEvaluator.authorizeApplicationResource(
                    auth,
                    [type: 'apitoken', username: tokenUser, roles: 'svc_roleA'],
                    'create'
            ) >> true
        }
        service.userService.findOrCreateUser('auser') >> user

        where:
        tokenaction              | _
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

        service.rundeckAuthContextEvaluator=Mock(AppAuthContextEvaluator)
        service.configurationService = Mock(ConfigurationService)
        def mockedUserService =  Mock(UserService) {
            findOrCreateUser(tokenUser) >> new User(login: tokenUser)
        }
        service.userService = mockedUserService
        provider.userService = mockedUserService
        service.rundeckDataManager =  Mock(DataManager){
            getProviderForType(_) >>  {
                provider
            }
        }

        when:
        def result = service.generateUserToken(auth, tokenTime, tokenUser, tokenRoles)
        then:
        result != null
        result.user.login == tokenUser
        result.authRoles == 'role1,svc_roleA'
        result.getAuthRolesSet() == tokenRoles
        result.expiration != null
        _ * service.rundeckAuthContextEvaluator.authorizeApplicationResourceAny(auth, AuthConstants.RESOURCE_TYPE_APITOKEN, [AuthConstants.ACTION_ADMIN, AuthConstants.ACTION_APP_ADMIN]) >>
                (tokenaction == 'admin')
        _ * service.rundeckAuthContextEvaluator.authorizeApplicationResourceAny(auth, AuthConstants.RESOURCE_TYPE_USER, [AuthConstants.ACTION_ADMIN, AuthConstants.ACTION_APP_ADMIN]) >> false
        _ * service.rundeckAuthContextEvaluator.authorizeApplicationResource(auth, AuthConstants.RESOURCE_TYPE_APITOKEN, tokenaction) >> true
        if (tokenaction == 'admin') {
            0 * service.rundeckAuthContextEvaluator.authorizeApplicationResource(*_)
        } else {
            1 * service.rundeckAuthContextEvaluator.authorizeApplicationResource(
                    auth,
                    [type: 'apitoken', username: tokenUser, roles: 'svc_roleA'],
                    'create'
            ) >> true
        }

        where:
        tokenaction              | tokenUser
        'admin'                  | 'someuser'
        'generate_service_token' | 'someuser'
    }

    def "generate service token service username not allowed username"() {
        given:
        def auth = Mock(UserAndRolesAuthContext) {
            getUsername() >> 'auser'
            getRoles() >> (['role1', 'role2'] as Set)
        }
        def tokenRoles = ['role1', 'svc_roleA'] as Set

        def tokenTime = 3600

        service.rundeckAuthContextEvaluator=Mock(AppAuthContextEvaluator)
        service.configurationService = Mock(ConfigurationService)
        service.userService = Mock(UserService) {
            findOrCreateUser(tokenUser) >> new User(login: tokenUser)
        }

        when:
        def result = service.generateUserToken(auth, tokenTime, tokenUser, tokenRoles)
        then:
        service.rundeckAuthContextEvaluator.authorizeApplicationResourceAny(auth, AuthConstants.RESOURCE_TYPE_APITOKEN, [AuthConstants.ACTION_ADMIN, AuthConstants.ACTION_APP_ADMIN]) >> false
        _ * service.rundeckAuthContextEvaluator.authorizeApplicationResourceAny(auth, AuthConstants.RESOURCE_TYPE_USER, [AuthConstants.ACTION_ADMIN, AuthConstants.ACTION_APP_ADMIN]) >> false
        service.rundeckAuthContextEvaluator.authorizeApplicationResource(auth, AuthConstants.RESOURCE_TYPE_APITOKEN, tokenaction) >> true
        1 * service.rundeckAuthContextEvaluator.authorizeApplicationResource(
                auth,
                ['type': 'apitoken', 'username': 'someBuser', 'roles': 'svc_roleA'],
                AuthConstants.ACTION_CREATE
        ) >> false

        Exception e = thrown()
        e.message =~ /Unauthorized: create API token for $tokenUser with roles/

        where:
        tokenaction              | tokenUser
        'generate_service_token' | 'someBuser'
    }


    def "generate user token any groups admin auth"() {
        given:
        def auth = Mock(UserAndRolesAuthContext) {
            getUsername() >> 'auser'
            getRoles() >> (['role1', 'role2'] as Set)
        }
        def tokenRoles = ['role1', 'svc_roleA', 'any_role'] as Set

        def tokenTime = 3600

        service.rundeckAuthContextEvaluator=Mock(AppAuthContextEvaluator)
        service.configurationService = Mock(ConfigurationService)
        service.userService = Mock(UserService)
        def user = new User(login: tokenUser).save()

        when:
        def result = service.generateUserToken(auth, tokenTime, tokenUser, tokenRoles)
        then:
        result != null
        result.user.login == user.login
        result.getAuthRolesSet() == tokenRoles
        result.expiration != null
        service.rundeckAuthContextEvaluator.authorizeApplicationResourceAny(auth, AuthConstants.RESOURCE_TYPE_APITOKEN, [AuthConstants.ACTION_ADMIN, AuthConstants.ACTION_APP_ADMIN]) >> true
        service.userService.findOrCreateUser(tokenUser) >> user
        provider.userService.findOrCreateUser(tokenUser) >> user


        where:
         tokenUser | _
         'auser'   | _
         'anyuser' | _
    }

    @Unroll
    def "generate user token any groups only #tokenaction auth"() {
        given:
        def auth = Mock(UserAndRolesAuthContext) {
            getUsername() >> 'auser'
            getRoles() >> (['role1', 'role2'] as Set)
        }
        def tokenUser = 'auser'
        def tokenRoles = ['role1', 'svc_roleA', 'any_role'] as Set

        def tokenTime = 3600

        service.rundeckAuthContextEvaluator=Mock(AppAuthContextEvaluator)
        service.configurationService = Mock(ConfigurationService)
        service.userService = Mock(UserService)
        def user = new User(login: 'auser')

        when:
        def result = service.generateUserToken(auth, tokenTime, tokenUser, tokenRoles)
        then:
        _ * service.rundeckAuthContextEvaluator.authorizeApplicationResourceAny(auth, AuthConstants.RESOURCE_TYPE_APITOKEN, [AuthConstants.ACTION_ADMIN, AuthConstants.ACTION_APP_ADMIN]) >> false
        _ * service.rundeckAuthContextEvaluator.authorizeApplicationResourceAny(auth, AuthConstants.RESOURCE_TYPE_USER, [AuthConstants.ACTION_ADMIN, AuthConstants.ACTION_APP_ADMIN]) >> false
        _ * service.rundeckAuthContextEvaluator.authorizeApplicationResource(auth, AuthConstants.RESOURCE_TYPE_APITOKEN, AuthConstants.ACTION_GENERATE_SERVICE_TOKEN) >>
                (tokenaction == 'generate_service_token')
        _ * service.rundeckAuthContextEvaluator.authorizeApplicationResource(auth, AuthConstants.RESOURCE_TYPE_APITOKEN, tokenaction) >> true
        _ * service.rundeckAuthContextEvaluator.authorizeApplicationResource(auth, _, AuthConstants.ACTION_CREATE) >> false
        service.userService.findOrCreateUser('auser') >> user
        Exception e = thrown()
        e.message =~ expectMessage

        where:
        tokenaction              | expectMessage
        'generate_service_token' | /Unauthorized: create API token for auser with roles/
        'generate_user_token'    | /Unauthorized: create API token for auser with roles/
    }

    @Unroll
    def "generate user token no roles as owner uses auth roles"() {
        given:
        def auth = Mock(UserAndRolesAuthContext) {
            getUsername() >> 'auser'
            getRoles() >> (['role1', 'role2'] as Set)
        }
        def tokenUser = 'auser'
        def tokenRoles = null

        def tokenTime = 3600

        service.rundeckAuthContextEvaluator=Mock(AppAuthContextEvaluator)
        service.configurationService = Mock(ConfigurationService)
        service.userService = Mock(UserService)
        def user = new User(login: 'auser')

        when:
        def result = service.generateUserToken(auth, tokenTime, tokenUser, tokenRoles)
        then:
        _ * service.rundeckAuthContextEvaluator.authorizeApplicationResourceAny(auth, AuthConstants.RESOURCE_TYPE_APITOKEN, [AuthConstants.ACTION_ADMIN, AuthConstants.ACTION_APP_ADMIN]) >> false
        _ * service.rundeckAuthContextEvaluator.authorizeApplicationResourceAny(auth, AuthConstants.RESOURCE_TYPE_USER, [AuthConstants.ACTION_ADMIN, AuthConstants.ACTION_APP_ADMIN]) >> false
        _ * service.rundeckAuthContextEvaluator.authorizeApplicationResource(auth, AuthConstants.RESOURCE_TYPE_APITOKEN, AuthConstants.ACTION_GENERATE_SERVICE_TOKEN) >>
                (tokeaction == 'generate_service_token')
        _ * service.rundeckAuthContextEvaluator.authorizeApplicationResource(auth, AuthConstants.RESOURCE_TYPE_APITOKEN, AuthConstants.ACTION_GENERATE_USER_TOKEN) >>
                (tokeaction == 'generate_user_token')

        service.userService.findOrCreateUser('auser') >> user
        result.getAuthRolesSet() == (['role1', 'role2'] as Set)
        where:
        tokeaction               | _
        'generate_service_token' | _
        'generate_user_token'    | _

    }

    @Unroll
    def "generate service token no roles as other user is error"() {
        given:
        def auth = Mock(UserAndRolesAuthContext) {
            getUsername() >> 'auser'
            getRoles() >> (['role1', 'role2'] as Set)
        }
        def tokenUser = 'otheruser'
        def tokenRoles = null

        def tokenTime = 3600

        service.rundeckAuthContextEvaluator=Mock(AppAuthContextEvaluator)
        service.configurationService = Mock(ConfigurationService)
        service.userService = Mock(UserService)
        def user = new User(login: 'auser')

        when:
        def result = service.generateUserToken(auth, tokenTime, tokenUser, tokenRoles)
        then:
        _ * service.rundeckAuthContextEvaluator.authorizeApplicationResourceAny(auth, AuthConstants.RESOURCE_TYPE_APITOKEN, [AuthConstants.ACTION_ADMIN, AuthConstants.ACTION_APP_ADMIN]) >> false
        _ * service.rundeckAuthContextEvaluator.authorizeApplicationResourceAny(auth, AuthConstants.RESOURCE_TYPE_USER, [AuthConstants.ACTION_ADMIN, AuthConstants.ACTION_APP_ADMIN]) >> false
        _ * service.rundeckAuthContextEvaluator.authorizeApplicationResource(auth, AuthConstants.RESOURCE_TYPE_APITOKEN, AuthConstants.ACTION_GENERATE_SERVICE_TOKEN) >>
                true

        Exception e = thrown()
        e.message =~ /Roles are required/

    }

    def "render success xml response unwrapped"() {
        given:
        service.rundeckWebUtil= Mock(WebUtilService)
        when:
        def closureCalled = false
        service.renderSuccessXml(response) {
            closureCalled = true
            responseData(test:true){
                value('something')
            }
        }

        then:
            1 * service.rundeckWebUtil.respondOutput(_, 'application/xml', _) >> {
                def slurper = new XmlSlurper()
                def gpath = slurper.parseText(it[2])
                assert gpath.name() == 'responseData'
                assert gpath.'@test'.text() == 'true'
                assert gpath.value.text() == 'something'
            }
        closureCalled
    }


}
