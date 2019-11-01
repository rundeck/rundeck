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
package rundeck.services

import com.dtolabs.rundeck.core.authorization.UserAndRolesAuthContext
import com.dtolabs.rundeck.core.common.IFramework
import com.dtolabs.rundeck.core.plugins.ConfiguredPlugin
import com.dtolabs.rundeck.core.plugins.Plugin
import com.dtolabs.rundeck.core.utils.IPropertyLookup
import com.dtolabs.rundeck.plugins.ServiceNameConstants
import com.dtolabs.rundeck.plugins.user.groups.UserGroupSourcePlugin
import com.dtolabs.rundeck.server.plugins.RundeckPluginRegistry
import grails.testing.gorm.DataTest
import grails.testing.services.ServiceUnitTest
import org.apache.commons.logging.Log
import org.slf4j.Logger
import rundeck.CommandExec
import rundeck.Execution
import rundeck.ScheduledExecution
import rundeck.User
import rundeck.Workflow
import spock.lang.Specification
import spock.lang.Unroll

import java.lang.reflect.Field
import java.lang.reflect.Modifier


class UserServiceSpec extends Specification implements ServiceUnitTest<UserService>, DataTest {
    void setupSpec() {
        mockDomain User
        mockDomain CommandExec
        mockDomain ScheduledExecution
        mockDomain Workflow
    }
    def "UpdateUserProfile"() {
        setup:
        String login = "theusername"
        service.findOrCreateUser(login)
        when:
        User user = User.findByLogin(login)
        !user.firstName
        !user.lastName
        !user.email
        service.updateUserProfile(login,"User","The","the@user.com")


        then:
        user.firstName == "The"
        user.lastName == "User"
        user.email == "the@user.com"
    }

    @Unroll
    def "registerLogin"(){
        setup:
        String login = "theusername"
        String sessionId = "exampleSessionId01"
        service.configurationService = Mock(ConfigurationService) {
            1 * getBoolean(UserService.SESSION_ID_ENABLED, false) >> true
            1 * getString(UserService.SESSION_ID_METHOD, 'hash') >> method
        }
        service.frameworkService = Mock(FrameworkService) {
            getServerHostname() >> { "server" }
        }

        when:
        User user = service.registerLogin(login, sessionId)

        then:
        user.login == "theusername"
        user.lastLogin
        user.lastSessionId == expect
        !user.lastLogout
        where:
            method  | expect
            'plain' | 'exampleSessionId01'
            'hash'  | 'b74ab8c45bd5bce20e3e481e98c63324507dd9f4586f365473298fdacbc68c22'
    }

    def "registerLogin session id disabled"(){
        setup:
        String login = "theusername"
        String sessionId = "exampleSessionId01"
        service.configurationService = Mock(ConfigurationService) {
            1 * getBoolean(UserService.SESSION_ID_ENABLED, false) >> false
        }
        service.frameworkService = Mock(FrameworkService) {
            getServerHostname() >> { "server" }
        }


        when:
        User user = service.registerLogin(login, sessionId)

        then:
        user.login == "theusername"
        user.lastLogin
        user.lastSessionId == null
        !user.lastLogout
    }

    def "registerLogout"(){
        setup:
        String login = "theusername"

        when:
        User user = service.registerLogout(login)

        then:
        user.login == "theusername"
        !user.lastLogin
        user.lastLogout
    }

    def "findWithFilters basic"() {
        given:
            def userToSearch = 'admin'
            def email = 'test@test.com'
            def lastSessionId = 'exampleSessionId01'
            User u = new User(login: userToSearch, lastSessionId: lastSessionId)
            u.save()
            User u2 = new User(login: userToSearch + '_other', lastSessionId: null)
            u2.save()

            ScheduledExecution job = new ScheduledExecution(
                    jobName: 'blue',
                    project: 'AProject',
                    groupPath: 'some/where',
                    description: 'a job',
                    argString: '-a b -c d',
                    workflow: new Workflow(
                            keepgoing: true,
                            commands: [new CommandExec(
                                    [adhocRemoteString: 'test buddy', argString: '-delay 12 -monkey cheese -particle']
                            )]
                    ),
                    retry: '1'
            )
            job.save()
            def exec = new Execution(
                    scheduledExecution: job,
                    dateStarted: new Date(),
                    dateCompleted: null,
                    user: userToSearch,
                    project: 'AProject'
            ).save()

            service.configurationService = Mock(ConfigurationService) {
                getInteger(_, _) >> { it[1] }
            }
        when:
            def result = service.findWithFilters(false, [login: userToSearch], 0, 100)

        then:
            result.users
            result.users.size() == 1
            result.users.find { it.login == userToSearch }
            result.users.find { it.login == userToSearch }.id == u.id

    }
    def "Get User Group Source Plugin Roles"() {
        when:
        TestUserGroupSourcePlugin testPlugin = new TestUserGroupSourcePlugin(groups)
        RundeckPluginRegistry rundeckPluginRegistry = Mock(RundeckPluginRegistry)
        PluginService pluginService = Mock(PluginService) {
            listPlugins(UserGroupSourcePlugin) >> { [testPlugin:testPlugin] }
            configurePlugin(_,_,_,_) >> { new ConfiguredPlugin<UserGroupSourcePlugin>(testPlugin,[:]) }
        }
        FrameworkService fwkService = Mock(FrameworkService) {
            getRundeckPluginRegistry() >> rundeckPluginRegistry
            getPluginService() >> pluginService
        }
        service.frameworkService = fwkService
        def roles = service.getUserGroupSourcePluginRoles(user)

        then:
        roles == groups

        where:
        user  | groups
        "any" | ["one","two"]
        "any" | []
    }

    def "User Group Source Plugin doesn't process misconfigured plugin"() {
        when:
        boolean errorCalled = false
        service.log.metaClass.static.error =  { String msg, Throwable ex ->
            errorCalled = true
        }

        TestUserGroupSourcePlugin testPlugin = new TestUserGroupSourcePlugin([])
        RundeckPluginRegistry rundeckPluginRegistry = Mock(RundeckPluginRegistry)
        PluginService pluginService = Mock(PluginService) {
            listPlugins(UserGroupSourcePlugin) >> { [testPlugin:testPlugin] }
            configurePlugin(_,_,_,_) >> { null }
        }
        FrameworkService fwkService = Mock(FrameworkService) {
            getRundeckPluginRegistry() >> rundeckPluginRegistry
            getPluginService() >> pluginService
        }
        service.frameworkService = fwkService
        def roles = service.getUserGroupSourcePluginRoles("any")

        then:
        roles == []
        !errorCalled


    }

    @Unroll
    def "getLoginStatus exec time"() {
        setup:
        String login = "theusername"
        service.findOrCreateUser(login)

            service.configurationService = Mock(ConfigurationService) {
                getInteger(UserService.SESSION_ABANDONDED_MINUTES, _) >> timeout
            }
        when:
        User user = User.findByLogin(login)
        !user.firstName
        !user.lastName
        !user.email
            user.lastLogin = lastLogin
            user.lastLogout = logout
            user.save(flush: true)
        service.updateUserProfile(login,"User","The","the@user.com")
            def logginStatus = service.getLoginStatus(user)
        then:
            logginStatus
            logginStatus == expect.value
        where:
            execTime   | lastLogin                                        |logout| timeout | expect
            null       | null                                             |null| 30      | UserService.LogginStatus.NOTLOGGED
            null       | (new Date(System.currentTimeMillis() - 1000000)) |null| 15      | UserService.LogginStatus.ABANDONED
            null       | (new Date() - 1)                                 |null| 30      | UserService.LogginStatus.ABANDONED
            null       | (new Date() - 1)                                 |null| 7200    | UserService.LogginStatus.LOGGEDIN
            null       | (new Date() - 2)                                 |(new Date() - 1)| 7200    | UserService.LogginStatus.LOGGEDOUT
            new Date() | (new Date() - 3)                                 |null| 30      | UserService.LogginStatus.ABANDONED
            new Date() | null                                             |null| 30      | UserService.LogginStatus.NOTLOGGED
    }

    @Plugin(name = "test-user-group-source",service= ServiceNameConstants.UserGroupSource)
    class TestUserGroupSourcePlugin implements UserGroupSourcePlugin {

        List<String> groups

        TestUserGroupSourcePlugin(List<String> groups = []) { this.groups = groups }

        @Override
        List<String> getGroups(final String username, final Map<String, Object> config) {
            return groups
        }
    }

    def "validateUserExists"() {
        setup:
        User uone = new User(login: "one")
        uone.save()

        when:
        boolean result = service.validateUserExists(checkUname)

        then:
        result == expected

        where:
        checkUname | expected
        "one"      | true
        "two"      | false

    }
}
