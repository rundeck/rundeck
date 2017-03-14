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

import com.dtolabs.rundeck.app.support.ExtNodeFilters
/*
 * NodeFilter.java
 * 
 * User: Greg Schueler <a href="mailto:greg@dtosolutions.com">greg@dtosolutions.com</a>
 * Created: Apr 14, 2010 3:36:40 PM
 * $Id$
 */

public class NodeFilter {

    String name
    String project
    String filter

    static belongsTo = [user:User]
    static constraints={
        name(blank:false, matches: /^[^<>&'"\/]+$/)
        project(nullable: true)
        filter(nullable: true)
    }
    static mapping = {
        filter(type: 'text')
    }

    public ExtNodeFilters createExtNodeFilters(){
        ExtNodeFilters query = new ExtNodeFilters(this.properties.subMap(['filter','project']))
        return query
    }
    public String asFilter(){
        return filter
    }
}
