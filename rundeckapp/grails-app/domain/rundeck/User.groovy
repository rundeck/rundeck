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

import org.rundeck.app.data.model.v1.user.RdUser
import rundeck.data.validation.validators.AnyDomainEmailValidator

class User implements RdUser{
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

    static constraints={
        login(matches: '^[a-zA-Z0-9\\p{L}\\p{M}\\.,@\\(\\)\\s_\\\\/-]+$')
        firstName(nullable:true)
        lastName(nullable:true)
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
