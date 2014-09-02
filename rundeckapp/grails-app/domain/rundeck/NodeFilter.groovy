package rundeck

import com.dtolabs.rundeck.app.support.ExtNodeFilters
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
 * NodeFilter.java
 * 
 * User: Greg Schueler <a href="mailto:greg@dtosolutions.com">greg@dtosolutions.com</a>
 * Created: Apr 14, 2010 3:36:40 PM
 * $Id$
 */

public class NodeFilter {

    String name
    String nodeInclude
    String nodeExclude
    String nodeIncludeName
    String nodeExcludeName
    String nodeIncludeTags
    String nodeExcludeTags
    String nodeIncludeOsName
    String nodeExcludeOsName
    String nodeIncludeOsFamily
    String nodeExcludeOsFamily
    String nodeIncludeOsArch
    String nodeExcludeOsArch
    String nodeIncludeOsVersion
    String nodeExcludeOsVersion
    Boolean nodeExcludePrecedence=true
    String project
    String filter

    static belongsTo = [user:User]
    static constraints={
        name(blank:false, matches: /^[^<>&'"\/]+$/)
        nodeInclude(nullable: true)
        nodeExclude(nullable: true)
        nodeIncludeName(nullable: true)
        nodeExcludeName(nullable: true)
        nodeIncludeTags(nullable: true)
        nodeExcludeTags(nullable: true)
        nodeIncludeOsName(nullable: true)
        nodeExcludeOsName(nullable: true)
        nodeIncludeOsFamily(nullable: true)
        nodeExcludeOsFamily(nullable: true)
        nodeIncludeOsArch(nullable: true)
        nodeExcludeOsArch(nullable: true)
        nodeIncludeOsVersion(nullable: true)
        nodeExcludeOsVersion(nullable: true)
        nodeExcludePrecedence(nullable: true)
        project(nullable: true)
        filter(nullable: true)
    }
    static mapping = {

        nodeInclude(type: 'text')
        nodeExclude(type: 'text')
        nodeIncludeName(type: 'text')
        nodeExcludeName(type: 'text')
        nodeIncludeTags(type: 'text')
        nodeExcludeTags(type: 'text')
        nodeIncludeOsName(type: 'text')
        nodeExcludeOsName(type: 'text')
        nodeIncludeOsFamily(type: 'text')
        nodeExcludeOsFamily(type: 'text')
        nodeIncludeOsArch(type: 'text')
        nodeExcludeOsArch(type: 'text')
        nodeIncludeOsVersion(type: 'text')
        nodeExcludeOsVersion(type: 'text')
        filter(type: 'text')
    }

    public ExtNodeFilters createExtNodeFilters(){
        ExtNodeFilters query = new ExtNodeFilters(this.properties.findAll{it.key=~/^(filter|project|node(Include|Exclude).*)$/})
        return query
    }
    public String asFilter(){
        return createExtNodeFilters().asFilter()
    }
}
