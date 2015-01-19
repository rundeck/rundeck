/*
 * Copyright 2014 SimplifyOps Inc, <http://simplifyops.com>
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

package com.dtolabs.rundeck.app.support

import grails.validation.Validateable

/**
 * RunJobParams is ...
 * @author Greg Schueler <a href="mailto:greg@simplifyops.com">greg@simplifyops.com</a>
 * @since 2014-08-15
 */
@Validateable
class RunJobCommand {
    String id
//    ExtraCommand extra = new ExtraCommand()
    static constraints = {
//        extra(nullable: true, validator: { cmd, obj ->
//            // manually trigger the inner command validation
//            if (!cmd.validate()) {
////                obj.errors.addAllErrors(cmd.errors)
//                return 'invalid.extra.message'
//            }
//        })
    }

    @Override
    public String toString() {
        return "RunJobCommand{" +
                "id=" + id +
                '}';
    }
}

@Validateable
class ExtraCommand {
    Boolean debug=false
    String loglevel
    static constraints={
        loglevel(nullable: true,inList: ['DEBUG','NORMAL','INFO'])
    }

    @Override
    public String toString() {
        return "ExtraCommand{" +
                "debug=" + debug +
                ", loglevel='" + loglevel + '\'' +
                '}';
    }
}
