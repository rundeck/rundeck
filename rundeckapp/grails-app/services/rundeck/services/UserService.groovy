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
import org.hibernate.StaleStateException
import org.springframework.dao.OptimisticLockingFailureException
import rundeck.User

@Transactional
class UserService {
    public static final String G_EVENT_LOGIN_PROFILE_CHANGE = 'user.login.profile.change'
    ConfigurationService configurationService
    FrameworkService frameworkService
    public static final int DEFAULT_TIMEOUT = 30
    public static final String SESSION_ID_ENABLED = 'userService.login.track.sessionId.enabled'
    public static final String SESSION_ID_METHOD = 'userService.login.track.sessionId.method'
    public static final String SESSION_ABANDONDED_MINUTES = 'userService.login.track.sessionAbandoned'
    public static final String SHOW_LOGIN_STATUS = 'gui.userSummaryShowLoginStatus'
    public static final String SHOW_LOGGED_USERS_DEFAULT = 'gui.userSummaryShowLoggedUsersDefault'

    static enum LogginStatus{
        LOGGEDIN('LOGGED IN'),LOGGEDOUT('LOGGED OUT'),ABANDONED('ABANDONED'),NOTLOGGED('NOT LOGGED')
        private final String value
        LogginStatus(String value){
            this.value = value
        }
        String getValue(){
            this.value
        }
    }

    def findOrCreateUser(String login) {
        def User user = User.findByLogin(login)
        if(!user){
            def User u = new User(login:login)
            if(!u.save(flush:true)){
                writeErr("unable to save user: ${u}, ${u.errors.allErrors.join(',')}");
            }
            user=u
        }
        return user
    }

    def registerLogin(String login, String sessionId){
        User user = User.findByLogin(login)
        if(!user){
            user = new User(login:login)
        }
        user.lastLogin = new Date()
        user.lastLoggedHostName = frameworkService.getServerHostname()
        if(isSessionIdRegisterEnabled()){
            if(sessionIdRegisterMethod == 'plain') {
                user.lastSessionId = sessionId
            }else{
                user.lastSessionId = sessionId.encodeAsSHA256()
            }
        }else{
            user.lastSessionId = null
        }
        try {
            if (!user.save(flush: true)) {
                writeErr("unable to save user: ${user}, ${user.errors.allErrors.join(',')}");
            }
            return user
        }catch(StaleStateException exception){
            log.warn("registerLogin: for ${login}, caught StaleStateException: $exception")
        }catch(OptimisticLockingFailureException exception){
            log.warn("registerLogin: for ${login}, caught OptimisticLockingFailureException: $exception")
        }
    }

    def registerLogout(String login){
        User user = User.findByLogin(login)
        if(!user){
            user = new User(login:login)
        }
        user.lastLogout = new Date()
        if(!user.save(flush:true)){
            writeErr("unable to save user: ${user}, ${user.errors.allErrors.join(',')}")
        }
        return user
    }

    static void writeErr(String errMsg) {
        System.err.println(errMsg);
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
        def User u = findOrCreateUser(username)
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
        def User u = findOrCreateUser(username)
        if(!u){
            return [error:"Couldn't find user: ${username}"]
        }
        def inpref = pref instanceof String?parseKeyValuePref(pref):pref
        def storedpref=parseKeyValuePref(u.filterPref)
        storedpref.putAll(inpref)
        storedpref=storedpref.findAll{it.value!='!'}

        u.filterPref=genKeyValuePref(storedpref)
        u.save()
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
        User u = findOrCreateUser(username)
        u.firstName = firstName
        u.lastName = lastName
        u.email = email
        u.save()
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
                                frameworkService.getFrameworkPropertyResolver(),
                                PropertyScope.Unspecified
                        )
                if(configuredPlugin && configuredPlugin.instance) roles.addAll(configuredPlugin.instance.getGroups(username,configuredPlugin.configuration))
            } catch(Exception ex) {
                log.error("Unable to get groups from plugin: " + prov.key, ex)
            }
        }

        return roles
    }

    def getLoginStatus(User user){
        def status = LogginStatus.NOTLOGGED.value
        if(user){
            Date lastDate = user.getLastLogin()
            if(lastDate != null){
                int minutes = configurationService.getInteger(SESSION_ABANDONDED_MINUTES, DEFAULT_TIMEOUT)
                Calendar calendar = Calendar.getInstance()
                calendar.setTime(lastDate)
                calendar.add(Calendar.MINUTE, minutes)
                if(user.lastLogout != null){
                    if(lastDate.after(user.lastLogout)){
                        if(calendar.getTime().before(new Date())){
                            status = LogginStatus.ABANDONED.value
                        }else{
                            status = LogginStatus.LOGGEDIN.value
                        }
                    }else{
                        status = LogginStatus.LOGGEDOUT.value
                    }
                }else if(calendar.getTime().after(new Date())){
                    status = LogginStatus.LOGGEDIN.value
                }else{
                    status = LogginStatus.ABANDONED.value
                }
            }else {
                status = LogginStatus.NOTLOGGED.value
            }
        }
        status
    }

    def findWithFilters(boolean loggedInOnly, def filters, offset, max){

        int timeOutMinutes = configurationService.getInteger(SESSION_ABANDONDED_MINUTES, DEFAULT_TIMEOUT)
        boolean showLoginStatus = configurationService.getBoolean(SHOW_LOGIN_STATUS, false)
        Calendar calendar = Calendar.getInstance()
        calendar.add(Calendar.MINUTE, -timeOutMinutes)

        def totalRecords = User.createCriteria().count(){
            if(showLoginStatus && loggedInOnly){
                or{
                    and{
                        isNotNull("lastLogin")
                        isNotNull("lastLogout")
                        gtProperty("lastLogin", "lastLogout")
                        gt("lastLogin", calendar.getTime())
                    }
                    and{
                        isNotNull("lastLogin")
                        isNull("lastLogout")
                        gt("lastLogin", calendar.getTime())
                    }
                }
            }
            if(filters){
                filters.each {k,v ->
                    eq(k, v)
                }
            }
        }

        def users = []
        if(totalRecords > 0){
            users = User.createCriteria().list(max:max, offset:offset){
                if(showLoginStatus && loggedInOnly){
                    or{
                        and{
                            isNotNull("lastLogin")
                            isNotNull("lastLogout")
                            gtProperty("lastLogin", "lastLogout")
                            gt("lastLogin", calendar.getTime())
                        }
                        and{
                            isNotNull("lastLogin")
                            isNull("lastLogout")
                            gt("lastLogin", calendar.getTime())
                        }
                    }
                }

                if(filters){
                    filters.each {k,v ->
                        eq(k, v)
                    }
                }
                order("login", "asc")
            }
        }
        [
            totalRecords    : totalRecords,
            users           : users,
            showLoginStatus : showLoginStatus
        ]
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
        User.findByLogin(username) != null
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
