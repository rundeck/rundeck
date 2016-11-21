package rundeck

import com.fasterxml.jackson.core.JsonParseException
import com.fasterxml.jackson.databind.ObjectMapper

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
    String pluginConfig;
    static hasMany=[commands:WorkflowStep]
    static constraints = {
        strategy(nullable:false, maxSize: 256)
        pluginConfig(nullable: true, blank:true)
    }

    static mapping = {
        pluginConfig type: 'text'
        commands lazy: false
    }
    //ignore fake property 'configuration' and do not store it
    static transients = ['pluginConfigMap']

    public Map getPluginConfigMap() {
        //de-serialize the json
        if (null != pluginConfig) {
            final ObjectMapper mapper = new ObjectMapper()
            try{
                return mapper.readValue(pluginConfig, Map.class)
            }catch (JsonParseException e){
                return null
            }
        } else {
            return null
        }
    }
    public def getPluginConfigData(String type,String name) {
        def map = getPluginConfigMap()
        if(!map){
            map=[(type):[:]]
        }else if(!map[type]){
            map[type]=[:]
        }
        map?.get(type)?.get(name)?:[:]
    }
    public void setPluginConfigData(String type,String name, data) {
        def map = getPluginConfigMap()
        if(!map){
            map=[:]
        }
        if(!map[type]){
            map[type]=[:]
        }
        map[type][name] = data
        setPluginConfigMap(map)
    }

    public void setPluginConfigMap(Map obj) {
        //serialize json and store into field
        if (null != obj) {
            final ObjectMapper mapper = new ObjectMapper()
            pluginConfig = mapper.writeValueAsString(obj)
        } else {
            pluginConfig = null
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
        this.pluginConfigMap=source.pluginConfigMap
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
        newwf.pluginConfigMap=this.pluginConfigMap
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
        def plugins=pluginConfigMap?[pluginConfig:pluginConfigMap]:[:]
        return [/*threadcount:threadcount,*/ keepgoing:keepgoing, strategy:strategy, commands:commands.collect{it.toMap()}] + plugins
    }

    static Workflow fromMap(Map data){
        Workflow wf = new Workflow()
        if(data.keepgoing){
            wf.keepgoing=true
        }
        wf.strategy=data.strategy?data.strategy:'node-first'
        if(data.pluginConfig instanceof Map){
            wf.pluginConfigMap=data.pluginConfig
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
