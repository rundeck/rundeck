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

package com.dtolabs.rundeck.core.utils;

import com.dtolabs.rundeck.core.Constants;
import org.apache.log4j.Logger;
import org.apache.log4j.MDC;
import org.apache.log4j.NDC;
import org.apache.log4j.helpers.NullEnumeration;
import org.apache.tools.ant.BuildEvent;
import org.apache.tools.ant.BuildListener;
import org.apache.tools.ant.Project;
import org.apache.tools.ant.Target;

import java.net.InetAddress;
import java.net.UnknownHostException;
import java.util.Date;


/**
 * Listener which sends events to Log4j logging system
 *
 * @author <a href="mailto:alexh@hoho.local">Alex Honor</a>
 * @version 1.0
 */
public class CommandListener implements BuildListener {

    /**
     * Indicates if the listener was initialized.
     */
    private boolean initialized = false;

    private long startTime;

    /**
     * Construct the listener and make sure there is a valid appender.
     */
    public CommandListener() {
        initialized = false;
        this.startTime = new Date().getTime();
        Logger cat = Logger.getLogger(CommandListener.class.getName());
        Logger rootCat = cat.getRootLogger();
        if (!(rootCat.getAllAppenders() instanceof NullEnumeration)) {
            initialized = true;
        } else {
            cat.error("No log4j.properties in $RDECK_HOME/etc");
        }
    }

    /**
     * Retrieves execution id from project context.
     *
     * @param project a <code>Project</code> value
     * @return a <code>String</code> value
     */
    private String getCid(Project project) {
        return project.getProperty(Constants.FWK_CMD_INV_ID);
    }


    /**
     * Populates the MDC with the command info
     *
     * @param p a <code>Project</code> value
     */
    private void setCommandContext(Project p) {
        String moduleVers = p.getProperty("module.version");
        if (null != moduleVers) {
            MDC.put("command.module.version", moduleVers);
        }
        String handler = p.getProperty("command.handler.file");
        if (null != handler) {
            MDC.put("command.handler", handler);
        }
        String module = p.getProperty("module.name");
        if (null != module) {
            MDC.put("module.name", module);
        }
        String name = p.getProperty("command.name");
        if (null != name) {
            MDC.put("command.name", name);
        }
        if (null != module && null != name) {
            String maprefUri = p.getProperty("command." + module + "." + name
                    + ".mapref-uri");
            if (null != maprefUri) {
                MDC.put("command.mapref-uri", maprefUri);
            }
            String revision = p.getProperty("command." + module + "." + name
                    + ".revision");
            if (null != revision) {
                MDC.put("command.rev-num", revision);
            }
            String moduleName = p.getProperty("command." + module + "." + name
                    + ".controller");
            if (null != moduleName) {
                MDC.put("command.controller", moduleName);
            }
        }
    }

    /**
     * Populates the MDC with managed entity info
     *
     * @param p a <code>Project</code> value
     */
    private void setEntityContext(Project p) {
        String maprefUri = p.getProperty("resource.mapref-uri");
        if (null != maprefUri) {
            MDC.put("resource.mapref-uri", maprefUri);
        }
        String revNum = p.getProperty("resource.revision");
        if (null != revNum) {
            MDC.put("entity.rev-num", revNum);
        }
        String type = p.getProperty("resource.classname");
        if (null != type) {
            MDC.put("entity.type", type);
        }
        String object = p.getProperty("resource.name");
        if (null != object) {
            MDC.put("resource.name", object);
        }
        String project = p.getProperty("context.project");
        if (null != project) {
            MDC.put("resource.project", project);
        }
        String user = p.getProperty("context.user");
        if (null != user) {
            MDC.put("context.user", user);
        }
        String node = p.getProperty("framework.node.hostname");
        if (null == node) {
            try {
                node = InetAddress.getLocalHost().getHostName();
            } catch (UnknownHostException e) {
                node = "UNKNOWN";
            }
        }
        MDC.put("node", node);
    }


    /**
     * Overridden but does nothing.
     *
     * @see BuildListener#buildStarted
     */
    public void buildStarted(BuildEvent event) {
    }

    /**
     * Adds execution.status and execution.elapsed-time to MDC
     *
     * @see BuildListener#buildFinished
     */
    public void buildFinished(BuildEvent event) {
        if (initialized) {
            long finishTime = new Date().getTime();
            long runTime = finishTime - this.startTime;
            String secs = runTime / 1000 + "." + runTime % 1000;
            Logger cat = Logger.getLogger(CommandListener.class.getName());
            MDC.put("execution.elapsed-time", secs);
            if (event.getException() == null) {
                MDC.put("execution.status", "success");
                cat.info("Command successful. " + secs + "s");
            } else {
                MDC.put("execution.status", "fail");
                cat.error("Command failed: "
                        + event.getException().toString());
            }

            NDC.clear();
        }
    }

    /**
     * Checks to see if a command handler is being fired (i.e., not command-controller)
     * and then set execution id, command context and entity context.
     *
     * @see BuildListener#targetStarted
     */
    public void targetStarted(BuildEvent event) {
        if (initialized) {
            Target t = event.getTarget();
            Project p = t.getProject();
            String cid = "ciid:" + getCid(p);
            String last = NDC.peek();//don't put duplicates on the stack
            if (!cid.equals(last)) {
                NDC.push("ciid:" + getCid(p));
            }
            setCommandContext(p);
            setEntityContext(p);
        }
    }

    /**
     * Overridden but does nothing
     *
     * @see BuildListener#targetFinished
     */
    public void targetFinished(BuildEvent event) {
    }

    /**
     * Overridden but does nothing
     *
     * @see BuildListener#taskStarted
     */
    public void taskStarted(BuildEvent event) {
    }

    /**
     * Overridden but does nothing.
     *
     * @see BuildListener#taskFinished
     */
    public void taskFinished(BuildEvent event) {
    }

    /**
     * @see BuildListener#messageLogged
     */
    public void messageLogged(BuildEvent event) {
        if (initialized) {
            Object categoryObject = event.getTask();
            if (categoryObject == null) {
                categoryObject = event.getTarget();
                if (categoryObject == null) {
                    categoryObject = event.getProject();
                }
            }

            Logger cat = Logger.getLogger(categoryObject.getClass().getName());
            final String message = event.getMessage();
            switch (event.getPriority()) {
                case Project.MSG_ERR:
                    cat.error(message);
                    break;
                case Project.MSG_WARN:
                    cat.warn(message);
                    break;
                case Project.MSG_INFO:
                    cat.info(message);
                    break;
                case Project.MSG_VERBOSE:
                    cat.debug(message);
                    break;
                case Project.MSG_DEBUG:
                    cat.debug(message);
                    break;
                default:
                    cat.error(message);
                    break;
            }
        }
    }
}
