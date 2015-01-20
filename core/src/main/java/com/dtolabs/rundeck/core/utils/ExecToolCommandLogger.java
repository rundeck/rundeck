/*
 * Copyright 2010 DTO Labs, Inc. (http://dtolabs.com)
 *
 *  Licensed under the Apache License, Version 2.0 (the "License");
 *  you may not use this file except in compliance with the License.
 *  You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 *  Unless required by applicable law or agreed to in writing, software
 *  distributed under the License is distributed on an "AS IS" BASIS,
 *  WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *  See the License for the specific language governing permissions and
 *  limitations under the License.
 */

/*
* ExecToolCommandLogger.java
* 
* User: Greg Schueler <a href="mailto:greg@dtosolutions.com">greg@dtosolutions.com</a>
* Created: May 27, 2010 2:46:21 PM
* $Id$
*/
package com.dtolabs.rundeck.core.utils;

import org.apache.tools.ant.BuildEvent;
import org.apache.tools.ant.DefaultLogger;
import org.apache.tools.ant.Project;

import java.util.HashMap;

/**
 * ExecToolCommandLogger reformats ant log messages using a Reformatter
 *
 * @author Greg Schueler <a href="mailto:greg@dtosolutions.com">greg@dtosolutions.com</a>
 * @version $Revision$
 */
public class ExecToolCommandLogger extends DefaultLogger{
    private Reformatter formatter;

    public ExecToolCommandLogger(final Reformatter formatter) {
        this.formatter = formatter;
    }

    /**
     * Logs a message, if the priority is suitable. 
     * <br>
     * Overridden to add a prefix string.
     *
     * @param event A BuildEvent containing message information. Must not be <code>null</code>.
     */
    public void messageLogged(final BuildEvent event) {
        int priority = event.getPriority();
        // Filter out messages based on priority
        if (priority <= msgOutputLevel) {

            final String msg = expandMessage(event, event.getMessage());

            if (priority != Project.MSG_ERR) {
                printMessage(msg, out, priority);
            } else {
                printMessage(msg, err, priority);
            }
            log(msg);
        }
    }

    /**
     * Process string specified by the framework.log.dispatch.console.format property replacing any well known tokens with values
     * from the event.
     *
     * @param event   The BuildEvent
     * @param message The concatenated message
     * @return message string with tokens replaced by values.
     */
    private String expandMessage(final BuildEvent event, final String message) {
        final HashMap<String,String> data=new HashMap<String, String>();

        final String user = retrieveUserName(event);
        if(null!=user){
            data.put("user", user);
        }
        final String node = retrieveNodeName(event);
        if(null!=node){
            data.put("node", node);
        }
        data.put("level", logLevelToString(event.getPriority()));
        if(null!=formatter){

            return formatter.reformat(data, message);
        }else {
            return message;
        }
    }


    /**
     * Look up the module name value from the project of the build event
     *
     * @param event the BuildEvent
     *
     * @return the module name
     */
    public static String retrieveModuleName(BuildEvent event) {
        return event.getProject().getProperty("module.name");
    }

    /**
     * Look up the server name value from the project of the build event
     *
     * @param event the BuildEvent
     *
     * @return the value of framework.server.name
     */
    public static String retrieveNodeName(BuildEvent event) {
        return event.getProject().getProperty("framework.server.name");
    }


    /**
     * Look up the user name value from the project of the build event
     *
     * @param event the BuildEvent
     *
     * @return the user name
     */
    public static String retrieveUserName(BuildEvent event) {
        return event.getProject().getProperty("user.name");
    }

    /**
     * Returns a string representing the specified log level
     *
     * @param level Log level
     *
     * @return log level name in string form
     */
    public static String logLevelToString(final int level) {
        String logLevel;
        switch (level) {
            case Project.MSG_DEBUG:
                logLevel = "DEBUG";
                break;
            case Project.MSG_VERBOSE:
                logLevel = "VERBOSE";
                break;
            case Project.MSG_INFO:
                logLevel = "INFO";
                break;
            case Project.MSG_WARN:
                logLevel = "WARN";
                break;
            case Project.MSG_ERR:
                logLevel = "ERROR";
                break;
            default:
                logLevel = "UNKNOWN";
                break;
        }
        return logLevel;

    }

    public int getLevel() {
        return msgOutputLevel;
    }
}
