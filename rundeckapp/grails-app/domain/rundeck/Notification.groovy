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
 * Represents a registration of notification to happen on some event trigger, and some type of notification.
 */
public class Notification {
    /**
     * eventTrigger is the name of the event to cause the notification, e.g. "onfailure" to happen when a
     * failure of some type occurs
     */
    String eventTrigger
    /**
     * type is the type of notification to initiate, e.g. "email" to send an email, "url" to POST to a url
     */
    String type
    /**
     * content contains data to use for the notification, e.g. a list of email addresses, or a list of URLs
     */
    String content

    static belongsTo=[scheduledExecution:ScheduledExecution]

    static constraints={
        eventTrigger(nullable:false,blank:false)
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
    /**
     * Return the configuration map for a mail notification
     *
     * @return
     */
    public Map mailConfiguration(){
        if (content.startsWith('{') && content.endsWith('}')) {
            //parse as json
            return getConfiguration()
        }
        return [recipients: content]
    }

    public static Notification fromMap(String key, Map data){
        Notification n = new Notification(eventTrigger:key)
        if(data.email || data.recipients){
            n.type='email'
            def map=[recipients: data.recipients?:data.email.recipients]
            if(data.email && data.email.subject){
                map['subject']= data.email.subject
            }
            if(data.email && data.email.attachLog){
                map['attachLog']= data.email.attachLog in ['true',true]
            }
            n.configuration=map
        }else if(data.urls){
            n.type='url'
            n.content=data.urls
        }else if(data.type){
            n.type=data.type
            if(data.configuration && data.configuration instanceof Map){
                n.configuration=data.configuration
            }else if(data.configuration && data.configuration instanceof String){
                n.configuration = ['_content':data.configuration]
            }else{
                n.content=null
            }
        }
        return n;
    }
    public Map toMap(){
        if(type=='email'){
            return ['email':mailConfiguration()]
        }else if(type=='url'){
            return [urls:content]
        }else if(type){
            def config=[:]
            if(content){
                config=this.configuration
            }
            return [type:type,configuration:config]
        }else{
            return null
        }
    }


    public String toString ( ) {
        return "Notification{" +
        "eventTrigger='" + eventTrigger + '\'' +
        ", type='" + type + '\'' +
        ", content='" + content + '\'' +
        '}' ;
    }

}
