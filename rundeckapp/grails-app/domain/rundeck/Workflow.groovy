package rundeck

import com.fasterxml.jackson.core.JsonParseException
import com.fasterxml.jackson.databind.ObjectMapper

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

    Integer threadcount=1
    Boolean keepgoing=false
    List<WorkflowStep> commands
    String strategy="node-first"
    String strategyConfig;
    static hasMany=[commands:WorkflowStep]
    static constraints = {
        strategy(nullable:false, maxSize: 256)
        strategyConfig(nullable: true, blank:true)
    }

    static mapping = {
        strategyConfig type: 'text'
        commands lazy: false
    }
    //ignore fake property 'configuration' and do not store it
    static transients = ['strategyConfigMap']

    public Map getStrategyConfigMap() {
        //de-serialize the json
        if (null != strategyConfig) {
            final ObjectMapper mapper = new ObjectMapper()
            try{
                return mapper.readValue(strategyConfig, Map.class)
            }catch (JsonParseException e){
                return null
            }
        } else {
            return null
        }
    }

    public void setStrategyConfigMap(Map obj) {
        //serialize json and store into field
        if (null != obj) {
            final ObjectMapper mapper = new ObjectMapper()
            strategyConfig = mapper.writeValueAsString(obj)
        } else {
            strategyConfig = null
        }
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
        this.strategyConfigMap=source.strategyConfigMap
        commands = new ArrayList()
        source.commands.each { WorkflowStep cmd->
            final item = createItem(cmd)
            if(cmd.errorHandler){
                final handler=createItem(cmd.errorHandler)
                item.errorHandler=handler
            }
            commands.add(item)
        }
    }
    public Workflow createClone(){
        Workflow newwf=new Workflow()
        newwf.threadcount = this.threadcount
        newwf.keepgoing = this.keepgoing
        newwf.strategy = this.strategy
        newwf.strategyConfigMap=this.strategyConfigMap
        newwf.commands = new ArrayList()

        this.commands?.each { WorkflowStep cmd ->
            final item = createItem(cmd)
            if (cmd.errorHandler) {
                final handler = createItem(cmd.errorHandler)
                item.errorHandler = handler
            }
            newwf.commands.add(item)
        }
        return newwf
    }
    public static WorkflowStep createItem(WorkflowStep item){
        return item.createClone()
    }

    /** create canonical map representation */
    public Map toMap(){
        return [/*threadcount:threadcount,*/keepgoing:keepgoing,strategy:strategy,strategyConfig:strategyConfigMap,commands:commands.collect{it.toMap()}]
    }

    static Workflow fromMap(Map data){
        Workflow wf = new Workflow()
        if(data.keepgoing){
            wf.keepgoing=true
        }
        wf.strategy=data.strategy?data.strategy:'node-first'
        if(data.strategyConfig instanceof Map){
            wf.strategyConfigMap=data.strategyConfig
        }
        if(data.commands){
            ArrayList commands = new ArrayList()
            Set handlers = new HashSet()
            def createStep={Map map->
                WorkflowStep exec
                if (map.jobref!=null) {
                    exec = JobExec.jobExecFromMap(map)
                } else if (map.exec != null || map.script != null || map.scriptfile != null || map.scripturl != null) {
                    exec = CommandExec.fromMap(map)
                } else {
                    exec = PluginStep.fromMap(map)
                }
                exec
            }
            data.commands.each{map->
                WorkflowStep exec=createStep(map)
                if(map.errorhandler){
                    WorkflowStep handler=createStep(map.errorhandler)
                    exec.errorHandler=handler
                }
                commands<<exec
            }
            wf.commands=commands
        }
        return wf
    }
}
