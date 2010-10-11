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
 * ApiQuery a CommandObject class for Reports API Query
 * 
 * User: greg
 * Created: Feb 13, 2008 4:02:12 PM
 * $Id$
 */
class ApiQuery extends BaseQuery{
    String actionStr
    String actionType
    String author
    String commandName
    String controllerStr
    Date date
    String resourceName
    String resourceType
    String itemType
    String maprefUri
    String message
    String nodename
    String patternName
    String project
    String reportId
    String status

    //output params
    String format
    int dateStart
    int dateEnd


    static constraints = {
        project(nullable:false,blank:false)
        itemType(inList:["commandExec","object","type","pattern","project"])
        actionType(inList:["create","update","delete","succeed","fail"])
        status(inList:["succeed","fail"])
        format(inList:["xml","json" /*, "yaml"*/])
        sortBy(inList:[
            "action",
            "actionType",
            "author",
            "commandName",
            "controller",
            "date",
            "resourceName",
            "resourceType",
            "itemType",
            "maprefUri",
            "message",
            "nodename",
            "patternName",
            "project",
            "reportId"
        ])

        dateStart(min:0)
        dateEnd(min:0)
    }
}