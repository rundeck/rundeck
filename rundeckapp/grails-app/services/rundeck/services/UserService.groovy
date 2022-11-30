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

import com.dtolabs.rundeck.core.plugins.PluggableProviderService
import com.dtolabs.rundeck.core.plugins.configuration.PropertyScope
import com.dtolabs.rundeck.plugins.user.groups.UserGroupSourcePlugin
import grails.events.annotation.Subscriber
import grails.gorm.transactions.Transactional
import groovy.transform.CompileStatic
import org.rundeck.app.data.model.v1.user.RdUser
import org.rundeck.app.data.model.v1.user.dto.UserProperties
import org.rundeck.app.data.providers.v1.UserDataProvider
import org.rundeck.spi.data.DataAccessException

@Transactional
class UserService {
    public static final String G_EVENT_LOGIN_PROFILE_CHANGE = 'user.login.profile.change'
    ConfigurationService configurationService
    FrameworkService frameworkService
    UserDataProvider userDataProvider

    public static final int DEFAULT_TIMEOUT = 30
    public static final String SESSION_ID_ENABLED = 'userService.login.track.sessionId.enabled'
    public static final String SESSION_ID_METHOD = 'userService.login.track.sessionId.method'
    public static final String SESSION_ABANDONDED_MINUTES = 'userService.login.track.sessionAbandoned'
    public static final String SHOW_LOGIN_STATUS = 'gui.userSummaryShowLoginStatus'
    public static final String SHOW_LOGGED_USERS_DEFAULT = 'gui.userSummaryShowLoggedUsersDefault'

    RdUser findOrCreateUser(String login) {
        return userDataProvider.findOrCreateUser(login)
    }

    String getOwnerName(Long userId) {
        userDataProvider.get(userId).login
    }

    def registerLogin(String login, String sessionId){
        try {
            return userDataProvider.registerLogin(login, sessionId)
        } catch(DataAccessException dae) {
            writeErr(dae.getMessage())
            return userDataProvider.buildUser()
        }
    }

    def registerLogout(String login){
        try {
            return userDataProvider.registerLogout(login)
        } catch(DataAccessException dae) {
            writeErr(dae.getMessage())
            return userDataProvider.buildUser()
        }
    }

    static void writeErr(String errMsg) {
        System.err.println(errMsg)
    }


    /**
     * Parse a "key=value,key=value" string and return a Map of string->String
     */
    public static Map parseKeyValuePref(String pref){


        def inpref =[:]
        if(pref){
            def list=pref.split(",")
            list.each{String item->
                def p=item.split("=",2)
                if(p.size()>1 && p[1]){
                    inpref[p[0]]=p[1]
                }
            }
        }
        return inpref
    }



    /**
    * Take a map of String->String and generate a string like "key=value,key2=value2"
     */
    public static String genKeyValuePref(Map map){
        if(map){
            return map.collect{item-> item.key+"="+item.value }.join(",")
        }
        return ""
    }

    /**
     * @param username
     * @deprecated
     */
    public Map getFilterPref(String username){
        def RdUser u = findOrCreateUser(username)
        if(!u){
            return null
        }
        return parseKeyValuePref(u.filterPref)
    }
    /**
     * Store filter pref input string for a user
     * @deprecated
     */
    public storeFilterPref(String username, pref){
        def RdUser u = findOrCreateUser(username)
        if(!u){
            return [error:"Couldn't find user: ${username}"]
        }
        def inpref = pref instanceof String?parseKeyValuePref(pref):pref
        def storedpref=parseKeyValuePref(u.filterPref)
        storedpref.putAll(inpref)
        storedpref=storedpref.findAll{it.value!='!'}

        userDataProvider.updateFilterPref(username, genKeyValuePref(storedpref))
        return [user:u,storedpref:storedpref]
    }

    @CompileStatic
    @Subscriber(G_EVENT_LOGIN_PROFILE_CHANGE)
    def userLoginProfileChange(UserProfileData data){
        updateUserProfile(
            data.username,
            data.lastName,
            data.firstName,
            data.email
        )
    }
    @CompileStatic
    static class UserProfileData{
        String username
        String lastName
        String firstName
        String email
    }
    void updateUserProfile(String username, String lastName, String firstName, String email) {
        userDataProvider.updateUserProfile(username, lastName, firstName, email)
    }

    List<String> getUserGroupSourcePluginRoles(String username) {
        PluggableProviderService groupSourcePluginService = frameworkService.getRundeckPluginRegistry().createPluggableService(UserGroupSourcePlugin)

        List<String> roles = []

        frameworkService.getPluginService().listPlugins(UserGroupSourcePlugin).each { prov ->
            try {
                def configuredPlugin = frameworkService.getPluginService().
                        configurePlugin(
                                prov.key,
                                groupSourcePluginService,
                                frameworkService.getFrameworkPropertyResolverFactory(),
                                PropertyScope.Unspecified
                        )
                if(configuredPlugin && configuredPlugin.instance) roles.addAll(configuredPlugin.instance.getGroups(username,configuredPlugin.configuration))
            } catch(Exception ex) {
                log.error("Unable to get groups from plugin: " + prov.key, ex)
            }
        }

        return roles
    }

    def getLoginStatus(RdUser user){
        return userDataProvider.getLoginStatus(user)
    }

    def findWithFilters(boolean loggedInOnly, def filters, offset, max){
        return userDataProvider.findWithFilters(loggedInOnly, filters, offset, max)
    }

    HashMap<String, UserProperties> getInfoFromUsers(List usernames) {
        return userDataProvider.getInfoFromUsers(usernames)
    }

    /**
     * It looks for property to enable session id related data to be stored at DB.
     * @return boolean
     */
    def isSessionIdRegisterEnabled(){
        configurationService.getBoolean(SESSION_ID_ENABLED, false)
    }

    /**
     * It looks for property to enable session id related data to be stored at DB.
     * @return boolean
     */
    def getSessionIdRegisterMethod() {
        configurationService.getString(SESSION_ID_METHOD, 'hash')
    }

    boolean validateUserExists(String username) {
        userDataProvider.validateUserExists(username)
    }

    /**
     * It looks for property to choose whether to show users logged status or not
     * @return Map
     */
    def getSummaryPageConfig(){
        [
                loggedOnly      :configurationService.getBoolean(SHOW_LOGGED_USERS_DEFAULT, false),
                showLoginStatus :configurationService.getBoolean(SHOW_LOGIN_STATUS, false)
        ]
    }
}
