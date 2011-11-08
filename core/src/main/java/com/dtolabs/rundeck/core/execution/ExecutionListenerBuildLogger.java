/*
 * Copyright 2011 DTO Solutions, Inc. (http://dtosolutions.com)
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
* ExecutionListenerBuildLogger.java
* 
* User: Greg Schueler <a href="mailto:greg@dtosolutions.com">greg@dtosolutions.com</a>
* Created: 3/25/11 4:19 PM
* 
*/
package com.dtolabs.rundeck.core.execution;

import com.dtolabs.rundeck.core.utils.LogReformatter;
import org.apache.tools.ant.BuildEvent;
import org.apache.tools.ant.BuildException;
import org.apache.tools.ant.BuildLogger;
import org.apache.tools.ant.Project;

import java.io.PrintStream;
import java.util.Map;

/**
 * ExecutionListenerBuildLogger is ...
 *
 * @author Greg Schueler <a href="mailto:greg@dtosolutions.com">greg@dtosolutions.com</a>
 */
public class ExecutionListenerBuildLogger implements BuildLogger {
    ExecutionListener listener;
    int msgOutputLevel;
    private LogReformatter reformatter;
    private Map<String,String> logContext;

    public ExecutionListenerBuildLogger(final ExecutionListener listener) {
        this.listener = listener;
    }

    public void setMessageOutputLevel(final int i) {
        msgOutputLevel = i;

    }

    public int getMessageOutputLevel() {
        return msgOutputLevel;
    }

    public void setOutputPrintStream(final PrintStream output) {
    }

    public void setEmacsMode(final boolean emacsMode) {
    }

    public void setErrorPrintStream(final PrintStream err) {
    }

    public void buildStarted(final BuildEvent e) {
        listener.log(Project.MSG_VERBOSE, reformat(e));
    }

    private static String lSep = System.getProperty("line.separator");

    public void buildFinished(final BuildEvent event) {
        final Throwable error = event.getException();
        final StringBuffer message = new StringBuffer();
        if (error != null) {

            message.append("Command failed.");
            message.append(lSep);

            if (Project.MSG_VERBOSE <= msgOutputLevel || !(error instanceof BuildException)) {
                message.append(org.apache.tools.ant.util.StringUtils.getStackTrace(error));
            } else {
                message.append(error.toString()).append(lSep);
            }
            listener.log(Project.MSG_ERR, message.toString());
        }
    }

    public void targetStarted(final BuildEvent e) {
        listener.log(Project.MSG_VERBOSE, reformat(e));
    }

    public void targetFinished(final BuildEvent e) {
        listener.log(Project.MSG_VERBOSE, reformat(e));
    }

    private String reformat(BuildEvent e) {
        if(null!=reformatter){
            return reformatter.reformat(logContext,e.getMessage());
        }else{
            return e.getMessage();
        }
    }


    public void taskStarted(final BuildEvent event) {
    }

    public void taskFinished(final BuildEvent event) {
    }

    public void messageLogged(final BuildEvent event) {
        listener.log(event.getPriority(), reformat(event));

    }

    public LogReformatter getReformatter() {
        return reformatter;
    }

    public void setReformatter(LogReformatter reformatter) {
        this.reformatter = reformatter;
    }

    public Map<String, String> getLogContext() {
        return logContext;
    }

    public void setLogContext(Map<String, String> logContext) {
        this.logContext = logContext;
    }
}
