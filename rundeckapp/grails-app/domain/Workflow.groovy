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
 * Workflow.java
 * 
 * User: Greg Schueler <a href="mailto:greg@dtosolutions.com">greg@dtosolutions.com</a>
 * Created: Feb 25, 2010 3:01:44 PM
 * $Id$
 */

public class Workflow {

//    Boolean nodeKeepgoing=false
    Integer threadcount=1
    Boolean keepgoing=false
    List commands
    String strategy="node-first"
    static hasMany=[commands:CommandExec]
    static constraints = {
        strategy(nullable:false, inList:['node-first','step-first'])
    }
    public String toString() {
        return "Workflow:(threadcount:${threadcount}){ ${commands} }"
    }

    public Workflow(){

    }

    public Workflow(Workflow source){
        this.threadcount=source.threadcount
        this.keepgoing=source.keepgoing
        this.strategy=source.strategy
        commands = new ArrayList()
        source.commands.each {
            commands.add(createItem(it))
        }
    }
    public static CommandExec createItem(CommandExec item){
        return item.createClone()
    }

    /** create canonical map representation */
    public Map toMap(){
        return [/*threadcount:threadcount,*/keepgoing:keepgoing,strategy:strategy,commands:commands.collect{it.toMap()}]
    }

    static Workflow fromMap(Map data){
        Workflow wf = new Workflow()
        if(data.keepgoing){
            wf.keepgoing=true
        }
        wf.strategy=data.strategy?data.strategy:'node-first'
        if(data.commands){
            ArrayList commands = new ArrayList()
            data.commands.each{map->
                if(map.jobref){
                    JobExec exec = JobExec.jobExecFromMap(map)
                    commands <<exec
                }else{
                    CommandExec exec=CommandExec.fromMap(map)
                    commands<<exec
                }
            }
            wf.commands=commands
        }
        return wf
    }
}