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
* DispatchedScriptImpl.java
* 
* User: Greg Schueler <a href="mailto:greg@dtosolutions.com">greg@dtosolutions.com</a>
* Created: Mar 10, 2010 2:12:41 PM
* $Id$
*/
package com.dtolabs.rundeck.core.dispatcher;

import com.dtolabs.rundeck.core.utils.NodeSet;

import java.io.InputStream;
import java.util.Map;

/**
 * DispatchedScriptImpl is ...
 *
 * @author Greg Schueler <a href="mailto:greg@dtosolutions.com">greg@dtosolutions.com</a>
 * @version $Revision$
 */
public class DispatchedScriptImpl implements IDispatchedScript{
    private NodeSet nodeSet;
    private String frameworkProject;
    private String script;
    private InputStream scriptAsStream;
    private String serverScriptFilePath;
    private String[] args;
    private int loglevel;
    private Map<String, Map<String, String>> dataContext;

    public DispatchedScriptImpl(final NodeSet nodeSet, final String frameworkProject,
                                final String script,
                                final InputStream scriptAsStream,
                                final String serverScriptFilePath, final String[] args, final int loglevel) {
        this.nodeSet = nodeSet;
        this.frameworkProject = frameworkProject;
        this.script = script;
        this.scriptAsStream = scriptAsStream;
        this.serverScriptFilePath = serverScriptFilePath;
        this.args = args;
        this.loglevel = loglevel;
    }

    public NodeSet getNodeSet() {
        return nodeSet;
    }

    public void setNodeSet(NodeSet nodeSet) {
        this.nodeSet = nodeSet;
    }

    public String getFrameworkProject() {
        return frameworkProject;
    }

    public void setFrameworkProject(String frameworkProject) {
        this.frameworkProject = frameworkProject;
    }

    public String getScript() {
        return script;
    }

    public void setScript(String script) {
        this.script = script;
    }

    public InputStream getScriptAsStream() {
        return scriptAsStream;
    }

    public void setScriptAsStream(InputStream scriptAsStream) {
        this.scriptAsStream = scriptAsStream;
    }

    public String getServerScriptFilePath() {
        return serverScriptFilePath;
    }

    public void setServerScriptFilePath(String serverScriptFilePath) {
        this.serverScriptFilePath = serverScriptFilePath;
    }

    public String[] getArgs() {
        return args;
    }

    public void setArgs(String[] args) {
        this.args = args;
    }

    public int getLoglevel() {
        return loglevel;
    }

    public void setLoglevel(int loglevel) {
        this.loglevel = loglevel;
    }


    public Map<String, Map<String, String>> getDataContext() {
        return dataContext;
    }

    public void setDataContext(Map<String, Map<String, String>> dataContext) {
        this.dataContext = dataContext;
    }
}
