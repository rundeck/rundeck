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

import com.dtolabs.rundeck.core.plugins.ConfiguredPlugin
import com.dtolabs.rundeck.core.plugins.Plugin
import com.dtolabs.rundeck.plugins.ServiceNameConstants
import com.dtolabs.rundeck.plugins.user.groups.UserGroupSourcePlugin
import com.dtolabs.rundeck.server.plugins.RundeckPluginRegistry
import grails.testing.gorm.DataTest
import grails.testing.services.ServiceUnitTest
import org.apache.commons.logging.Log
import org.slf4j.Logger
import rundeck.User
import spock.lang.Specification

import java.lang.reflect.Field
import java.lang.reflect.Modifier


class UserServiceSpec extends Specification implements ServiceUnitTest<UserService>, DataTest {
    void setupSpec() {
        mockDomain User
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

    def "registerLogin"(){
        setup:
        String login = "theusername"
        String sessionId = "exampleSessionId01"

        when:
        User user = service.registerLogin(login, sessionId)

        then:
        user.login == "theusername"
        user.lastLogin
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

    def "getLoginStatus" (){
        setup:
        String login = "theusername"
        service.findOrCreateUser(login)

        when:
        User user = User.findByLogin(login)
        !user.firstName
        !user.lastName
        !user.email
        service.updateUserProfile(login,"User","The","the@user.com")
        def logginStatus = service.getLoginStatus(user, new Date())
        then:
        logginStatus
        logginStatus == UserService.LogginStatus.LOGGEDIN.value
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

}
