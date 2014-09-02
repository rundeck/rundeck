package com.dtolabs.rundeck.app.support

import com.dtolabs.rundeck.core.utils.NodeSet
import com.dtolabs.rundeck.core.utils.OptsUtil
import grails.validation.Validateable

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
@Validateable
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
    String filter

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

    public boolean nodeFilterIsEmpty(){
        return !(nodeInclude||nodeExclude||nodeIncludeName||nodeExcludeName||
               nodeIncludeTags || nodeExcludeTags|| nodeIncludeOsName || nodeExcludeOsName || nodeIncludeOsFamily ||
            nodeExcludeOsFamily || nodeIncludeOsArch||nodeExcludeOsArch || nodeIncludeOsVersion||nodeExcludeOsVersion||filter)
    }

    /**
     * Generate a filter string given the node filters
     * @return
     */
    public String asFilter(){
        if(filter){
            return filter
        }
        return asFilter([include:asIncludeMap(),exclude:asExcludeMap()])
    }
    public static String asFilter(Map<String,Map<String, String>> filtermap){
        Map<String,String> include= filtermap.include
        Map<String, String> exclude= filtermap.exclude
        return OptsUtil.join(
                (include?.keySet()?.findAll{include[it]}.collect{
                    [it+':',include[it]]
                }?.flatten()?:[])
                + (exclude?.keySet()?.findAll { exclude[it] }.collect{
                    ['!'+it+':',exclude[it]]
                }?.flatten()?:[])
        )
    }

    public Map<String, String> asExcludeMap(){
        return asExcludeMap(this)
    }

    public static Map<String, String> asExcludeMap(filters) {
        def nodeMap = [:]
        nodeMap[NodeSet.HOSTNAME] = filters.nodeExclude
        nodeMap[NodeSet.NAME] = filters.nodeExcludeName
        nodeMap[NodeSet.TAGS] = filters.nodeExcludeTags
        nodeMap[NodeSet.OS_NAME] = filters.nodeExcludeOsName
        nodeMap[NodeSet.OS_FAMILY] = filters.nodeExcludeOsFamily
        nodeMap[NodeSet.OS_ARCH] = filters.nodeExcludeOsArch
        nodeMap[NodeSet.OS_VERSION] = filters.nodeExcludeOsVersion
        if (filters.filter) {
            nodeMap.putAll(NodeSet.parseFilter(filters.filter).exclude)
        }
        return nodeMap
    }
    public Map<String, String> asIncludeMap(){
        return asIncludeMap(this)
    }
    public static Map<String, String> asIncludeMap(filters){
        def nodeMap=[:]
        nodeMap[NodeSet.HOSTNAME] = filters.nodeInclude
        nodeMap[NodeSet.NAME] = filters.nodeIncludeName
        nodeMap[NodeSet.TAGS] = filters.nodeIncludeTags
        nodeMap[NodeSet.OS_NAME] = filters.nodeIncludeOsName
        nodeMap[NodeSet.OS_FAMILY] = filters.nodeIncludeOsFamily
        nodeMap[NodeSet.OS_ARCH] = filters.nodeIncludeOsArch
        nodeMap[NodeSet.OS_VERSION] = filters.nodeIncludeOsVersion
        if (filters.filter) {
            nodeMap.putAll(NodeSet.parseFilter(filters.filter).include)
        }
        return nodeMap
    }

    static filterKeys = [hostname: '',  tags: 'Tags', 'os-name': 'OsName', 'os-family': 'OsFamily',
    'os-arch': 'OsArch', 'os-version': 'OsVersion','name':'Name']

    public String toString ( ) {
    return "BaseNodeFilters{" +
           (nodeInclude?"nodeInclude='" + nodeInclude + '\'':'') +
    (nodeExclude?", nodeExclude='" + nodeExclude + '\'':'') +
    (nodeIncludeName?", nodeIncludeName='" + nodeIncludeName + '\'' : '') +
    (nodeExcludeName?", nodeExcludeName='" + nodeExcludeName + '\'' : '') +
    (nodeIncludeTags?", nodeIncludeTags='" + nodeIncludeTags + '\'' : '') +
    (nodeExcludeTags?", nodeExcludeTags='" + nodeExcludeTags + '\'' : '') +
    (nodeIncludeOsName?", nodeIncludeOsName='" + nodeIncludeOsName + '\'' : '') +
    (nodeExcludeOsName?", nodeExcludeOsName='" + nodeExcludeOsName + '\'' : '') +
    (nodeIncludeOsFamily?", nodeIncludeOsFamily='" + nodeIncludeOsFamily + '\'' : '') +
    (nodeExcludeOsFamily?", nodeExcludeOsFamily='" + nodeExcludeOsFamily + '\'' : '') +
    (nodeIncludeOsArch?", nodeIncludeOsArch='" + nodeIncludeOsArch + '\'' : '') +
    (nodeExcludeOsArch?", nodeExcludeOsArch='" + nodeExcludeOsArch + '\'' : '') +
    (nodeIncludeOsVersion?", nodeIncludeOsVersion='" + nodeIncludeOsVersion + '\'' : '') +
    (nodeExcludeOsVersion?", nodeExcludeOsVersion='" + nodeExcludeOsVersion + '\'' : '') +
    (nodeExcludePrecedence?", nodeExcludePrecedence=" + nodeExcludePrecedence : '') +
    (filter?", filter=" + filter : '') +
    '}' ;
    }}
