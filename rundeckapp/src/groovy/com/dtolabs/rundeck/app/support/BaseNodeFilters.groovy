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

package com.dtolabs.rundeck.app.support

import com.dtolabs.rundeck.core.utils.NodeSet
import com.dtolabs.rundeck.core.utils.OptsUtil
import grails.validation.Validateable

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

    String filter

    static constraints = {

        filter(nullable: true)
    }
    static mapping = {
        filter(type: 'text')
    }

    public boolean nodeFilterIsEmpty(){
        return !filter
    }

    /**
     * Generate a filter string given the node filters
     * @return
     */
    public String asFilter(){
        return filter
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

    public static Map<String, String> asExcludeMap(filters) {
        def nodeMap = [:]
        if (filters.filter) {
            nodeMap.putAll(NodeSet.parseFilter(filters.filter).exclude)
        }
        return nodeMap
    }
    public static Map<String, String> asIncludeMap(filters){
        def nodeMap=[:]
        if (filters.filter) {
            nodeMap.putAll(NodeSet.parseFilter(filters.filter).include)
        }
        return nodeMap
    }

    static filterKeys = [hostname: '',  tags: 'Tags', 'os-name': 'OsName', 'os-family': 'OsFamily',
    'os-arch': 'OsArch', 'os-version': 'OsVersion','name':'Name']

    public String toString ( ) {
    return "BaseNodeFilters{" +
    (filter?", filter=" + filter : '') +
    '}' ;
    }}
