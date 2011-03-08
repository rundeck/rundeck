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

    public static Notification fromMap(String key,Map data){
        Notification n = new Notification(eventTrigger:key)
        if(data.recipients){
            n.type='email'
            n.content=data.recipients
        }else if(data.urls){
            n.type='url'
            n.content=data.urls
        }
        return n;
    }
    public Map toMap(){
        if(type=='email'){
            return [recipients:content]
        }else if(type=='url'){
            return [urls:content]
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