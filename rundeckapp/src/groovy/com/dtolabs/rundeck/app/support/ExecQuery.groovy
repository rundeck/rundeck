package com.dtolabs.rundeck.app.support
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
 * ExecQuery.java
 * 
 * User: greg
 * Created: Feb 29, 2008 3:17:13 PM
 * $Id$
 */
class ExecQuery extends ReportQuery{
    String controllerFilter
    String cmdFilter
    String groupPathFilter
    String groupPathExactFilter

    static constraints = {
        statFilter(nullable:true,inList:["succeed","fail"])
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
}
