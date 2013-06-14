package com.dtolabs.rundeck.app.internal.logging

import com.dtolabs.rundeck.core.logging.LogEvent
import com.dtolabs.rundeck.core.logging.LogLevel

/*
 * Copyright 2013 DTO Labs, Inc. (http://dtolabs.com)
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
 *
 */

/*
 * DefaultLogEvent.java
 * 
 * User: Greg Schueler <a href="mailto:greg@dtosolutions.com">greg@dtosolutions.com</a>
 * Created: 1/22/13 6:57 PM
 * 
 */
class DefaultLogEvent implements LogEvent{
    LogLevel loglevel
    Date datetime
    String message
    String eventType
    Map<String,String> metadata

    public DefaultLogEvent(){

    }
    public DefaultLogEvent(LogEvent event){
        this(event,[:])
    }

    public DefaultLogEvent(LogEvent event, Map defaultMetadata){
        this.loglevel = event.loglevel
        this.datetime = event.datetime
        this.message = event.message
        this.eventType = event.eventType
        this.metadata=defaultMetadata+(event.metadata?:[:])
    }
    @Override
    public String toString() {
        return "DefaultLogEvent{" +
               ", eventType=" + eventType +
               ", loglevel=" + loglevel +
               ", datetime=" + datetime +
               ", message='" + message + '\'' +
               ", metadata=" + metadata +
               '}';
    }
}
