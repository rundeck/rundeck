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

package com.dtolabs.rundeck.core.logging.internal;

import com.dtolabs.rundeck.core.logging.LogEvent;
import com.dtolabs.rundeck.core.logging.LogLevel;
import java.util.Date;
import java.util.HashMap;
import java.util.Map;

/*
 * DefaultLogEvent.java
 * 
 * User: Greg Schueler <a href="mailto:greg@dtosolutions.com">greg@dtosolutions.com</a>
 * Created: 1/22/13 6:57 PM
 * 
 */
public class DefaultLogEvent implements LogEvent{
    private LogLevel loglevel;
    private Date datetime;
    private String message;
    private String eventType;
    private Map<String,String> metadata;

    public DefaultLogEvent(){

    }

    public DefaultLogEvent(
            final LogLevel loglevel,
            final Date datetime,
            final String message,
            final String eventType,
            final Map<String, String> metadata
    ) {
        this.loglevel = loglevel;
        this.datetime = datetime;
        this.message = message;
        this.eventType = eventType;
        this.metadata = metadata;
    }


    public DefaultLogEvent(LogEvent event){
        this(event,new HashMap<>());
    }

    public DefaultLogEvent(LogEvent event, Map<String,String> defaultMetadata){
        this.loglevel = event.getLoglevel();
        this.datetime = event.getDatetime();
        this.message = event.getMessage();
        this.eventType = event.getEventType();
        this.metadata = new HashMap<>(defaultMetadata);
        if (event.getMetadata() != null) {
            this.metadata.putAll(event.getMetadata());
        }
    }

    static DefaultLogEvent with(LogEvent event, Map<String,String> metadata) {
        return new DefaultLogEvent(event, metadata);
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

    @Override
    public LogLevel getLoglevel() {
        return loglevel;
    }

    public void setLoglevel(LogLevel loglevel) {
        this.loglevel = loglevel;
    }

    @Override
    public Date getDatetime() {
        return datetime;
    }

    public void setDatetime(Date datetime) {
        this.datetime = datetime;
    }

    @Override
    public String getMessage() {
        return message;
    }

    public void setMessage(String message) {
        this.message = message;
    }

    @Override
    public String getEventType() {
        return eventType;
    }

    public void setEventType(String eventType) {
        this.eventType = eventType;
    }

    @Override
    public Map<String, String> getMetadata() {
        return metadata;
    }

    public void setMetadata(Map<String, String> metadata) {
        this.metadata = metadata;
    }
}
