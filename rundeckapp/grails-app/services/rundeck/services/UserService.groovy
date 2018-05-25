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

import grails.gorm.transactions.Transactional
import rundeck.User

@Transactional
class UserService {

    FrameworkService frameworkService

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
}
