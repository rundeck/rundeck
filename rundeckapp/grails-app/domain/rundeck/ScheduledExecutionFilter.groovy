package rundeck

import com.dtolabs.rundeck.app.api.ApiBulkJobDeleteRequest
import com.dtolabs.rundeck.app.support.ScheduledExecutionQuery
/*
 * Copyright 2010 DTO Labs, Inc. (http://dtolabs.com)
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *        http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

/*
 * ScheduledExecutionFilter.java
 * 
 * User: Greg Schueler <a href="mailto:greg@dtosolutions.com">greg@dtosolutions.com</a>
 * Created: Feb 12, 2010 1:03:43 PM
 * $Id$
 */

public class ScheduledExecutionFilter {
    String name
    String jobFilter
    String projFilter
    String groupPath

    String descFilter
    String loglevelFilter

    String idlist

    static belongsTo = [user:User]
    static constraints={
        name(blank: false, matches: /^[^<>&'"\/]+$/)
        idlist(nullable: true, matches: ApiBulkJobDeleteRequest.IDLIST_REGEX)
        jobFilter(nullable:true)
        projFilter(nullable:true)
        groupPath(nullable:true)
        descFilter(nullable:true)
        loglevelFilter(nullable:true)
    }

    public ScheduledExecutionQuery createQuery(){
        ScheduledExecutionQuery query = new ScheduledExecutionQuery(this.properties.findAll{it.key=~/(.*Filter|groupPath|idlist)$/})
        return query
    }

    public static ScheduledExecutionFilter fromQuery(ScheduledExecutionQuery query){
        final ScheduledExecutionFilter filter = new ScheduledExecutionFilter(query.properties.findAll{it.key=~/(.*Filter|groupPath|idlist)$/})
        filter.fix()
        return filter
    }
    public void fix(){
        ['idlist','jobFilter','projFilter','groupPath', 'descFilter', 'loglevelFilter'].each{
            if(!this[it]){
                this[it]=''
            }
        }
    }

}
