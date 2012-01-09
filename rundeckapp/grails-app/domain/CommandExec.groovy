
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

import com.dtolabs.rundeck.execution.IWorkflowCmdItem

/*
* CommandExec.java
*
* User: Greg Schueler <a href="mailto:greg@dtosolutions.com">greg@dtosolutions.com</a>
* Created: Feb 25, 2010 3:02:01 PM
* $Id$
*/

public class CommandExec extends ExecutionContext implements IWorkflowCmdItem {
    String returnProperty
    String ifString
    String unlessString
    String equalsString
    static belongsTo = [workflow: Workflow]

    public String toString() {
        StringBuffer sb = new StringBuffer()
        sb << "command( "
        sb << (returnProperty ? "return=\"${returnProperty}\"" : '')
        sb << (ifString ? " if=\"${ifString}\"" : '')
        sb << (unlessString ? "unless=\"${unlessString}\"" : '')
        sb << (equalsString ? "equals=\"${equalsString}\"" : '')
        sb << (adhocRemoteString ? "exec: ${adhocRemoteString}" : '')
        sb << (adhocLocalString ? "script: ${adhocLocalString}" : '')
        sb << (adhocFilepath ? "scriptfile: ${adhocFilepath}" : '')
        sb << (argString ? "scriptargs: ${argString}" : '')
        sb<<")"

        return sb.toString()
    }

    public String summarize(){
        StringBuffer sb = new StringBuffer()
        sb << (adhocRemoteString ? "${adhocRemoteString}" : '')
        sb << (adhocLocalString ? "${adhocLocalString}" : '')
        sb << (adhocFilepath ? "${adhocFilepath}" : '')
        sb << (argString ? " -- ${argString}" : '')
        return sb.toString()
    }

    static constraints = {
        returnProperty(nullable: true)
        ifString(nullable: true)
        unlessString(nullable: true)
        equalsString(nullable: true)
        project(nullable: true)
        argString(nullable: true)
        user(nullable: true)
        adhocRemoteString(nullable:true)
        adhocLocalString(nullable:true)
        adhocFilepath(nullable:true)
        nodeRankAttribute(nullable:true)
        nodeRankOrderAscending(nullable:true)

    }

    public CommandExec createClone(){
        CommandExec ce = new CommandExec(this.properties)
        return ce
    }



    /**
    * Return canonical map representation
     */
    public Map toMap(){
        def map=[:]
        if(adhocRemoteString){
            map.exec=adhocRemoteString
        }else if(adhocLocalString){
            map.script=adhocLocalString
        }else {
            map.scriptfile=adhocFilepath
        }
        if(argString && !adhocRemoteString){
            map.args=argString
        }
        return map
    }

    static CommandExec fromMap(Map data){
        CommandExec ce = new CommandExec()
        if(data.exec){
            ce.adhocExecution = true
            ce.adhocRemoteString=data.exec
        }else if(data.script){
            ce.adhocExecution = true
            ce.adhocLocalString=data.script
        }else if(data.scriptfile){
            ce.adhocExecution = true
            ce.adhocFilepath=data.scriptfile
        }
        if(data.args && !ce.adhocRemoteString){
            ce.argString=data.args
        }
        return ce
    }
}