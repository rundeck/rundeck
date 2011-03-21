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
* NodesQuery.java
*
* User: greg
* Created: Jan 20, 2010 11:48:26 AM
* $Id$
*/

/**
 * Represents a query corresponding to the filters available for a NodeSet
 */
public class BaseNodeFilters {

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

    static constraints = {
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
    }

    public boolean nodeFilterIsEmpty(){
        return !(nodeInclude||nodeExclude||nodeIncludeName||nodeExcludeName||
               nodeIncludeTags || nodeExcludeTags|| nodeIncludeOsName || nodeExcludeOsName || nodeIncludeOsFamily ||
            nodeExcludeOsFamily || nodeIncludeOsArch||nodeExcludeOsArch || nodeIncludeOsVersion||nodeExcludeOsVersion)
    }


    static filterKeys = [hostname: '',  tags: 'Tags', 'os-name': 'OsName', 'os-family': 'OsFamily',
    'os-arch': 'OsArch', 'os-version': 'OsVersion','name':'Name']
}