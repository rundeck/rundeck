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
 * Notification.java
 * 
 * User: Greg Schueler <a href="mailto:greg@dtosolutions.com">greg@dtosolutions.com</a>
 * Created: May 17, 2010 11:20:53 AM
 * $Id$
 */

/**
 * Represents a registration of a orchestrator and configuration
 */
public class Orchestrator {
    /**
     * type is the type of orchestrator to initiate, e.g. "limitRun" 
     */
    String type
    /**
     * content contains data to use for the orchestrator, e.g. a percentage
     */
    String content

   // static belongsTo=[scheduledExecution:ScheduledExecution]

    static constraints={
        type(nullable:false,blank:false)
        content(nullable:true,blank:true)
    }
    static mapping = {
        content type: 'text'
    }
    //ignore fake property 'configuration' and do not store it
    static transients = ['configuration'] 
    
    public Map getConfiguration() {
        //de-serialize the json
        if (null != content) {
            final ObjectMapper mapper = new ObjectMapper()
            try{
                return mapper.readValue(content, Map.class)
            }catch (JsonParseException e){
                return null
            }
        } else {
            return null
        }

    }

    public void setConfiguration(Map obj) {
        //serialize json and store into field
        if (null != obj) {
            final ObjectMapper mapper = new ObjectMapper()
            content = mapper.writeValueAsString(obj)
        } else {
            content = null
        }
    }

    public static Orchestrator fromMap(Map data){
        Orchestrator n = new Orchestrator(type:data.type)
        n.configuration=data.configuration instanceof Map?data.configuration:[:]
        return n;
    }
    public Map toMap(){
        if(type){
            def data=[type:type]
            if(this.configuration){
                data.configuration=this.configuration
            }
            return data
        }else{
            return null
        }
    }


    public String toString ( ) {
        return "Orchestrator{" +
        "type='" + type + '\'' +
        ", content='" + content + '\'' +
        '}' ;
    }

}
