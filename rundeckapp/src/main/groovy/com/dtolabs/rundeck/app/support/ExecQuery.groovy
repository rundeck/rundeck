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

import grails.validation.Validateable
import org.rundeck.app.data.model.v1.query.RdExecQuery

/*
 * ExecQuery.java
 * 
 * User: greg
 * Created: Feb 29, 2008 3:17:13 PM
 * $Id$
 */
class ExecQuery extends ReportQuery implements Validateable, RdExecQuery{
    String controllerFilter
    String cmdFilter
    String groupPathFilter
    String groupPathExactFilter
    List execIdFilter
    List execProjects

    static constraints = {
        abortedByFilter(nullable: true)
        controllerFilter(nullable: true)
        typeFilter(nullable: true)
        recentFilter nullable: true, matches: /^((\d+)([hdwmy])|-)$/
        tagsFilter(nullable: true)
        jobListFilter(nullable: true)
        startafterFilter(nullable: true)
        jobFilter(nullable: true)
        reportIdFilter(nullable: true)
        objFilter(nullable: true)
        projFilter(nullable: true)
        messageFilter(nullable: true)
        userFilter(nullable: true)
        maprefUriFilter(nullable: true)
        titleFilter(nullable: true)
        groupPathFilter(nullable: true)
        jobIdFilter(nullable: true)
        cmdFilter(nullable: true)
        groupPathExactFilter(nullable: true)
        endbeforeFilter(nullable: true)
        endafterFilter(nullable: true)
        nodeFilter(nullable: true)
        startbeforeFilter(nullable: true)
        excludeJobListFilter(nullable: true)
        statFilter(nullable:true,inList:["succeed","fail","cancel","missed"])
        execnodeFilter(nullable: true)
        execIdFilter(nullable:true)
        execProjects(nullable:true)
        sortBy(nullable:true,inList:[
            "jobFilter",
            "jobIdFilter",
            "projFilter",
            "objFilter",
            "controllerFilter",
            "typeFilter",
            "cmdFilter",
            "userFilter",
            "maprefUriFilter",
            "messageFilter",
            "reportIdFilter",
            "abortedByFilter",
        ])

    }
    public static final ArrayList<String> exportProps = ReportQuery.exportProps +[
             'controllerFilter',
             'cmdFilter',
             'groupPathFilter',
             'groupPathExactFilter',
             'execIdFilter',
             'execProjects'
    ]
    def Map toMap() {

        def map = this.properties.subMap(exportProps)
        map
    }

    static Map fromMap(Map map) {
        ExecQuery execQuery = new ExecQuery()
        execQuery.controllerFilter = map.controllerFilter
        execQuery.cmdFilter = map.cmdFilter
        execQuery.groupPathFilter = map.groupPathFilter
        execQuery.groupPathExactFilter = map.groupPathExactFilter
        execQuery.execIdFilter = map.execIdFilter
        execQuery.execProjects = map.execProjects
        execQuery
    }
}
