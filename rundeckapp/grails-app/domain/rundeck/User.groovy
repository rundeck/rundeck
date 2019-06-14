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

package rundeck

import rundeck.services.AnyDomainEmailValidator


class User {
    String login
    String password
    String firstName
    String lastName
    String email
    Date dateCreated
    Date lastUpdated
    
    static mapping = {
        table "rduser"
    }
    String dashboardPref
    String filterPref
    Date lastLogin
    Date lastLogout
    String lastSessionId
    String lastLoggedHostName

    static hasMany = [reportfilters:ReportFilter,jobfilters:ScheduledExecutionFilter,nodefilters:NodeFilter]
    static constraints={
        login(matches: '^[a-zA-Z0-9\\.,@\\(\\)\\s_\\\\/-]+$')
        firstName(nullable:true, matches: '^[a-zA-Z0-9À-ÖØ-öø-ÿ\\s\\.,\\(\\)-]+$')
        lastName(nullable:true, matches: '^[a-zA-Z0-9À-ÖØ-öø-ÿ\\s\\.,\\(\\)-]+$')
        email(nullable:true,validator: { val ->
            (!val || new AnyDomainEmailValidator().isValid(val)) ? null : 'email.invalid'
        })
        password(nullable:true)
        dashboardPref(nullable:true)
        filterPref(nullable:true)
        lastLogin(nullable:true)
        lastLogout(nullable:true)
        lastSessionId(nullable:true)
        lastLoggedHostName(nullable:true)
    }
}
