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
import grails.gorm.transactions.Transactional
import rundeck.User


@Transactional
class UserService {

    FrameworkService frameworkService
    private static final int DEFAULT_TIMEOUT = 30

    enum LogginStatus{
        LIN('LOGGED IN'),LOUT('LOGGED OUT'),ABND('ABANDONNED'),NL('NOT LOGGED')
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
                System.err.println("unable to save user: ${u}, ${u.errors.allErrors.join(',')}");
            }
            user=u
        }
        return user
    }

    def registerLogin(String login){
        User user = User.findByLogin(login)
        if(!user){
            user = new User(login:login)
        }
        user.lastLogin = new Date()
        if(!user.save(flush:true)){
            System.err.println("unable to save user: ${u}, ${u.errors.allErrors.join(',')}");
        }
        return user
    }

    def registerLogout(String login){
        User user = User.findByLogin(login)
        if(!user){
            user = new User(login:login)
        }
        user.lastLogout = new Date()
        if(!user.save(flush:true)){
            System.err.println("unable to save user: ${u}, ${u.errors.allErrors.join(',')}");
        }
        return user
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
                roles.addAll(configuredPlugin.instance.getGroups(username,configuredPlugin.configuration))
            } catch(Exception ex) {
                log.error("Unable to get groups from plugin: " + prov.key, ex)
            }
        }

        return roles
    }

    def getLoginStatus(User user, Date lastExecution){
        def status = LogginStatus.NL.value
        if(user){
            Date lastDate = getLastDate(user.getLastLogin(), lastExecution)
            if(lastDate != null){
                int minutes = DEFAULT_TIMEOUT
                if(frameworkService.getRundeckFramework().getPropertyLookup().hasProperty("framework.session.abandonned.minutes")){
                    minutes = Integer.valueOf(frameworkService.getRundeckFramework().getPropertyLookup().getProperty("framework.session.abandonned.minutes"))
                }
                Calendar calendar = Calendar.getInstance()
                calendar.setTime(lastDate)
                calendar.add(Calendar.MINUTE, minutes)
                if(user.lastLogout != null){
                    if(lastDate.after(user.lastLogout)){
                        if(calendar.getTime().before(new Date())){
                            status = LogginStatus.ABND.value
                        }else{
                            status = LogginStatus.LIN.value
                        }
                    }else{
                        status = LogginStatus.LOUT.value
                    }
                }else if(calendar.getTime().after(new Date())){
                    status = LogginStatus.LIN.value
                }else{
                    status = LogginStatus.ABND.value
                }
            }else {
                status = LogginStatus.NL.value
            }
        }
        status
    }

    def getLastDate(Date firstDt, Date secondDt){
        if(firstDt == null){
            return secondDt
        }else if(secondDt == null){
            return firstDt
        }
        if(firstDt.after(secondDt)){
            return firstDt
        }else{
            return secondDt
        }
    }

}
