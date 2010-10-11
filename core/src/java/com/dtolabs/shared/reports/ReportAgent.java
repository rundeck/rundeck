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
 * IndexAgent.java
 * 
 * User: greg
 * Created: Jun 24, 2005 4:11:58 PM
 * $Id: IndexAgent.java 8623 2008-08-14 17:03:54Z gschueler $
 */
package com.dtolabs.shared.reports;


import com.dtolabs.shared.SharedConstants;
import org.apache.log4j.Logger;
import org.apache.log4j.MDC;

import java.util.Collection;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;
import java.util.Date;


/**
 * IndexAgent provides convenience methods for logging events for the IndexService. Events have these common fields:
 * <ul> <li>message - description of the action or comment from the user</li> <li>project - name of the project</li>
 * <li>action - brief title for the action being performed (whether it failed or not), e.g. "Create Object"</li>
 * <li>actionType - the type of the action, limited to those constants defined in {@link Constants.ActionType}</li>
 * <li>author - name of the user who performed the event</li> </ul> In addition to these fields, there are other fields
 * depending on the type of entity that is affected by the event.
 *
 * @author Greg Schueler <a href="mailto:greg@dtosolutions.com">greg@dtosolutions.com</a>
 * @version $Revision: 8623 $
 */
public class ReportAgent {
    public static final Logger commonLog = Logger.getLogger(Constants.INDEX_LOGGER_NAME);
    /**
     * Log events concerning a Type.
     * @param project name of the project
     * @param author
     * @param entType
     * @param action
     * @param actionType
     * @param message
     */
    public static void logTypeInfo(String project,
                                   String author,
                                   String entType,
                                   String action,
                                   Constants.ActionType actionType,
                                   String message) {
        MDC.put(Constants.MDC_PROJECT_KEY, project);
        MDC.put(Constants.MDC_AUTHOR_KEY, author);
        MDC.put(Constants.MDC_ENT_TYPE_KEY, entType);
        MDC.put(Constants.MDC_ACTION_KEY, action);
        MDC.put(Constants.MDC_ACTION_TYPE_KEY, actionType.toString());
        MDC.put(Constants.MDC_ITEM_TYPE_KEY, "type");
        if(null!= SharedConstants.FRAMEWORK_SERVER_HOSTNAME){
            MDC.put(Constants.MDC_NODENAME_KEY, SharedConstants.FRAMEWORK_SERVER_HOSTNAME);
        }

        commonLog.info(message);
        MDC.remove(Constants.MDC_PROJECT_KEY);
        MDC.remove(Constants.MDC_AUTHOR_KEY);
        MDC.remove(Constants.MDC_ENT_TYPE_KEY);
        MDC.remove(Constants.MDC_ACTION_KEY);
        MDC.remove(Constants.MDC_ACTION_TYPE_KEY);
        MDC.remove(Constants.MDC_ITEM_TYPE_KEY);
        MDC.remove(Constants.MDC_NODENAME_KEY);

    }

    /**
     * Log events concerning an entity object.
     *
     * @param maprefUri  URI of the object
     * @param project    project name
     * @param author     author of the event
     * @param entName    name of the object
     * @param entType    type name of the object
     * @param action
     * @param actionType ActionType for the type of the action.
     * @param message
     */
    public static void logObjectInfo(String maprefUri,
                                     String project,
                                     String author,
                                     String entName,
                                     String entType,
                                     String action,
                                     Constants.ActionType actionType,
                                     String message) {
        MDC.put(Constants.MDC_MAPREF_KEY, maprefUri);
        MDC.put(Constants.MDC_PROJECT_KEY, project);
        MDC.put(Constants.MDC_AUTHOR_KEY, author);
        MDC.put(Constants.MDC_ENT_NAME_KEY, entName);
        MDC.put(Constants.MDC_ENT_TYPE_KEY, entType);
        MDC.put(Constants.MDC_ACTION_KEY, action);
        MDC.put(Constants.MDC_ACTION_TYPE_KEY, actionType.toString());
        if (null != SharedConstants.FRAMEWORK_SERVER_HOSTNAME) {
            MDC.put(Constants.MDC_NODENAME_KEY, SharedConstants.FRAMEWORK_SERVER_HOSTNAME);
        }
        MDC.put(Constants.MDC_ITEM_TYPE_KEY, "object");

        commonLog.info(message);
        MDC.remove(Constants.MDC_MAPREF_KEY);
        MDC.remove(Constants.MDC_PROJECT_KEY);
        MDC.remove(Constants.MDC_AUTHOR_KEY);
        MDC.remove(Constants.MDC_ENT_NAME_KEY);
        MDC.remove(Constants.MDC_ENT_TYPE_KEY);
        MDC.remove(Constants.MDC_ACTION_KEY);
        MDC.remove(Constants.MDC_ACTION_TYPE_KEY);
        MDC.remove(Constants.MDC_ITEM_TYPE_KEY);
        MDC.remove(Constants.MDC_NODENAME_KEY);

    }
    public static void logObjectInfoReports(Collection reports){
        for (Iterator i = reports.iterator(); i.hasNext();) {
            Object o = i.next();
            logObjectInfo(o);
        }
    }
    public static void logObjectInfo(Object memento){
        Map map = (Map) memento;
        MDC.put(Constants.MDC_MAPREF_KEY, map.get(Constants.MDC_MAPREF_KEY));
        MDC.put(Constants.MDC_PROJECT_KEY, map.get(Constants.MDC_PROJECT_KEY));
        MDC.put(Constants.MDC_AUTHOR_KEY, map.get(Constants.MDC_AUTHOR_KEY));
        MDC.put(Constants.MDC_ENT_NAME_KEY, map.get(Constants.MDC_ENT_NAME_KEY));
        MDC.put(Constants.MDC_ENT_TYPE_KEY, map.get(Constants.MDC_ENT_TYPE_KEY));
        MDC.put(Constants.MDC_ACTION_KEY, map.get(Constants.MDC_ACTION_KEY));
        MDC.put(Constants.MDC_ACTION_TYPE_KEY, map.get(Constants.MDC_ACTION_TYPE_KEY));
        MDC.put(Constants.MDC_ITEM_TYPE_KEY, map.get(Constants.MDC_ITEM_TYPE_KEY));
        if (null != map.get(Constants.MDC_NODENAME_KEY)) {
            MDC.put(Constants.MDC_NODENAME_KEY, map.get(Constants.MDC_NODENAME_KEY));
        }

        commonLog.info(map.get("logMessage"));
        MDC.remove(Constants.MDC_MAPREF_KEY);
        MDC.remove(Constants.MDC_PROJECT_KEY);
        MDC.remove(Constants.MDC_AUTHOR_KEY);
        MDC.remove(Constants.MDC_ENT_NAME_KEY);
        MDC.remove(Constants.MDC_ENT_TYPE_KEY);
        MDC.remove(Constants.MDC_ACTION_KEY);
        MDC.remove(Constants.MDC_ACTION_TYPE_KEY);
        MDC.remove(Constants.MDC_ITEM_TYPE_KEY);
        MDC.remove(Constants.MDC_NODENAME_KEY);
    }
    /**
     * Log events concerning an entity object.
     *
     * @param maprefUri  URI of the object
     * @param project    project name
     * @param author     author of the event
     * @param entName    name of the object
     * @param entType    type name of the object
     * @param action
     * @param actionType ActionType for the type of the action.
     * @param message
     */
    public static Object objectInfoMemento(String maprefUri,
                                     String project,
                                     String author,
                                     String entName,
                                     String entType,
                                     String action,
                                     Constants.ActionType actionType,
                                     String message) {
        HashMap map = new HashMap();
        map.put(Constants.MDC_MAPREF_KEY, maprefUri);
        map.put(Constants.MDC_PROJECT_KEY, project);
        map.put(Constants.MDC_AUTHOR_KEY, author);
        map.put(Constants.MDC_ENT_NAME_KEY, entName);
        map.put(Constants.MDC_ENT_TYPE_KEY, entType);
        map.put(Constants.MDC_ACTION_KEY, action);
        map.put(Constants.MDC_ACTION_TYPE_KEY, actionType.toString());
        map.put(Constants.MDC_ITEM_TYPE_KEY, "object");
        if (null != SharedConstants.FRAMEWORK_SERVER_HOSTNAME) {
            map.put(Constants.MDC_NODENAME_KEY, SharedConstants.FRAMEWORK_SERVER_HOSTNAME);
        }
        map.put("logMessage", message);

        return map;

    }
    /**
     * Log events concerning an entity object.
     *
     * @param maprefUri  URI of the object
     * @param project    project name
     * @param author     author of the event
     * @param entName    name of the object
     * @param entType    type name of the object
     * @param action
     * @param actionType ActionType for the type of the action.
     * @param message
     * @deprecated use {@link #logCommandInfo(String, String, String, String, String, String, String, String, org.opendepo.services.correlation.Constants.ActionType, String, String, String)} instead.
     */
    public static void logCommandInfo(String maprefUri,
                                      String project,
                                      String author,
                                      String entName,
                                      String entType,
                                      String commandName,
                                      String controllerName,
                                      String action,
                                      Constants.ActionType actionType,
                                      String message) {
        logCommandInfo(maprefUri,
                       project,
                       author,
                       entName,
                       entType,
                       commandName,
                       controllerName,
                       action,
                       actionType,
                       null,
                       null,
                       message);
    }

    /**
     * Log events concerning an entity object.
     *
     * @param maprefUri  URI of the object
     * @param project    project name
     * @param author     author of the event
     * @param entName    name of the object
     * @param entType    type name of the object
     * @param action
     * @param actionType ActionType for the type of the action.
     * @param nodename   The name of the node where the command was run
     * @param reportId   The reportId
     * @param message
     */
    public static void logCommandInfo(String maprefUri,
                                      String project,
                                      String author,
                                      String entName,
                                      String entType,
                                      String commandName,
                                      String controllerName,
                                      String action,
                                      Constants.ActionType actionType,
                                      String nodename,
                                      String reportId,
                                      String message) {
        logCommandInfo(maprefUri,
                       project,
                       author,
                       entName,
                       entType,
                       commandName,
                       controllerName,
                       action,
                       actionType,
                       nodename,
                       reportId,
                       message,
                       null,
                       null,
                       null);
    }

    /**
     * Log events concerning an entity object.
     *
     * @param maprefUri  URI of the object
     * @param project    project name
     * @param author     author of the event
     * @param entName    name of the object
     * @param entType    type name of the object
     * @param action     Description of the action
     * @param actionType ActionType for the type of the action.
     * @param nodename   The name of the node where the command was run
     * @param reportId   The reportId
     * @param message    message text
     * @param tags       comma separated tags
     * @param startTime  date started
     * @param endTime    date ended
     */
    public static void logCommandInfo(String maprefUri,
                                      String project,
                                      String author,
                                      String entName,
                                      String entType,
                                      String commandName,
                                      String controllerName,
                                      String action,
                                      Constants.ActionType actionType,
                                      String nodename,
                                      String reportId,
                                      String message,
                                      String tags,
                                      Date startTime,
                                      Date endTime) {
        if (null != maprefUri) {
            MDC.put(Constants.MDC_MAPREF_KEY, maprefUri);
        }
        MDC.put(Constants.MDC_PROJECT_KEY, project);
        MDC.put(Constants.MDC_AUTHOR_KEY, author);
        if (null != entName) {
            MDC.put(Constants.MDC_ENT_NAME_KEY, entName);
        }
        if (null != entType) {
            MDC.put(Constants.MDC_ENT_TYPE_KEY, entType);
        }
        if(null!=nodename){
            MDC.put(Constants.MDC_NODENAME_KEY, nodename);
        }
        if(null!=reportId) {
            MDC.put(Constants.MDC_REPORTID_KEY, reportId);
        }
        MDC.put(Constants.MDC_ACTION_KEY, action);
        MDC.put(Constants.MDC_ACTION_TYPE_KEY, actionType.toString());
        MDC.put(Constants.MDC_CMD_NAME_KEY, commandName);
        MDC.put(Constants.MDC_CONTROLLER_KEY, controllerName);
        MDC.put(Constants.MDC_ITEM_TYPE_KEY, "commandExec");
        if(null!=startTime){
            MDC.put("epochDateStarted", String.valueOf(startTime.getTime()));
        }
        if(null!=endTime){
            MDC.put("epochDateEnded", String.valueOf(endTime.getTime()));
        }
        if(null!=tags) {
            MDC.put("rundeckTags", tags);
        }

        commonLog.info(message);
        MDC.remove("rundeckTags");
        MDC.remove("epochDateEnded");
        MDC.remove("epochDateStarted");
        MDC.remove(Constants.MDC_MAPREF_KEY);
        MDC.remove(Constants.MDC_PROJECT_KEY);
        MDC.remove(Constants.MDC_AUTHOR_KEY);
        MDC.remove(Constants.MDC_ENT_NAME_KEY);
        MDC.remove(Constants.MDC_ENT_TYPE_KEY);
        MDC.remove(Constants.MDC_NODENAME_KEY);
        MDC.remove(Constants.MDC_REPORTID_KEY);
        MDC.remove(Constants.MDC_ACTION_KEY);
        MDC.remove(Constants.MDC_ACTION_TYPE_KEY);
        MDC.remove(Constants.MDC_CMD_NAME_KEY);
        MDC.remove(Constants.MDC_CONTROLLER_KEY);
        MDC.remove(Constants.MDC_ITEM_TYPE_KEY);

    }

    /**
     * Log events concerning an adhoc or run-exec script execution.
     *
     * @param project    project name
     * @param author     author of the event
     * @param action     Description of the action
     * @param actionType ActionType for the type of the action.
     * @param nodename   The name of the node where the command was run
     * @param reportId   The reportId
     * @param message    message text
     * @param tags       comma separated tags
     * @param startTime  date started
     * @param endTime    date ended
     */
    public static void logExecInfo(final String project,
                                   final String author,
                                   final String action,
                                   final Constants.ActionType actionType,
                                   final String nodename,
                                   final String reportId,
                                   final String message,
                                   final String tags,
                                   final String adhocScript,
                                   final Date startTime,
                                   final Date endTime) {

        MDC.put(Constants.MDC_ADHOCEXEC_KEY, Boolean.toString(true));
        MDC.put(Constants.MDC_PROJECT_KEY, project);
        MDC.put(Constants.MDC_AUTHOR_KEY, author);
        if (null != nodename) {
            MDC.put(Constants.MDC_NODENAME_KEY, nodename);
        }
        if (null != reportId) {
            MDC.put(Constants.MDC_REPORTID_KEY, reportId);
        }
        MDC.put(Constants.MDC_ACTION_KEY, action);
        MDC.put(Constants.MDC_ACTION_TYPE_KEY, actionType.toString());
        MDC.put(Constants.MDC_ITEM_TYPE_KEY, "commandExec");
        if (null != startTime) {
            MDC.put(Constants.MDC_EPOCHSTART_KEY, String.valueOf(startTime.getTime()));
        }
        if (null != endTime) {
            MDC.put(Constants.MDC_EPOCHEND_KEY, String.valueOf(endTime.getTime()));
        }
        if (null != tags) {
            MDC.put(Constants.MDC_TAGS_KEY, tags);
        }
        if (null != adhocScript) {
            MDC.put(Constants.MDC_ADHOCSCRIPT_KEY, adhocScript);
        }

        commonLog.info(message);
        MDC.remove(Constants.MDC_ADHOCSCRIPT_KEY);
        MDC.remove(Constants.MDC_TAGS_KEY);
        MDC.remove(Constants.MDC_EPOCHEND_KEY);
        MDC.remove(Constants.MDC_EPOCHSTART_KEY);
        MDC.remove(Constants.MDC_ITEM_TYPE_KEY);
        MDC.remove(Constants.MDC_ACTION_TYPE_KEY);
        MDC.remove(Constants.MDC_ACTION_KEY);
        MDC.remove(Constants.MDC_REPORTID_KEY);
        MDC.remove(Constants.MDC_NODENAME_KEY);
        MDC.remove(Constants.MDC_AUTHOR_KEY);
        MDC.remove(Constants.MDC_PROJECT_KEY);
        MDC.remove(Constants.MDC_ADHOCEXEC_KEY);

    }

    /**
     * Log events concerning Patterns.
     *
     * @param project
     * @param author
     * @param patName    Name of the pattern
     * @param action
     * @param actionType
     * @param message
     */
    public static void logPatternInfo(String project,
                                      String author,
                                      String patName,
                                      String action,
                                      Constants.ActionType actionType,
                                      String message) {
        MDC.put(Constants.MDC_PROJECT_KEY, project);
        MDC.put(Constants.MDC_AUTHOR_KEY, author);
        MDC.put(Constants.MDC_PAT_NAME_KEY, patName);
        MDC.put(Constants.MDC_ACTION_KEY, action);
        MDC.put(Constants.MDC_ACTION_TYPE_KEY, actionType.toString());
        if (null != SharedConstants.FRAMEWORK_SERVER_HOSTNAME) {
            MDC.put(Constants.MDC_NODENAME_KEY, SharedConstants.FRAMEWORK_SERVER_HOSTNAME);
        }
        MDC.put(Constants.MDC_ITEM_TYPE_KEY, "pattern");

        commonLog.info(message);
        MDC.remove(Constants.MDC_PROJECT_KEY);
        MDC.remove(Constants.MDC_AUTHOR_KEY);
        MDC.remove(Constants.MDC_PAT_NAME_KEY);
        MDC.remove(Constants.MDC_ACTION_KEY);
        MDC.remove(Constants.MDC_ACTION_TYPE_KEY);
        MDC.remove(Constants.MDC_ITEM_TYPE_KEY);
        MDC.remove(Constants.MDC_NODENAME_KEY);

    }

    /**
     * Log events concerning Projects.
     *
     * @param project    name of the project.
     * @param author
     * @param action
     * @param actionType
     * @param message
     */
    public static void logProjectInfo(String project,
                                      String author,
                                      String action,
                                      Constants.ActionType actionType,
                                      String message) {
        MDC.put(Constants.MDC_PROJECT_KEY, project);
        MDC.put(Constants.MDC_AUTHOR_KEY, author);
        MDC.put(Constants.MDC_ACTION_KEY, action);
        MDC.put(Constants.MDC_ACTION_TYPE_KEY, actionType.toString());
        if (null != SharedConstants.FRAMEWORK_SERVER_HOSTNAME) {
            MDC.put(Constants.MDC_NODENAME_KEY, SharedConstants.FRAMEWORK_SERVER_HOSTNAME);
        }
        MDC.put(Constants.MDC_ITEM_TYPE_KEY, "project");

        commonLog.info(message);
        MDC.remove(Constants.MDC_PROJECT_KEY);
        MDC.remove(Constants.MDC_AUTHOR_KEY);
        MDC.remove(Constants.MDC_ACTION_KEY);
        MDC.remove(Constants.MDC_ACTION_TYPE_KEY);
        MDC.remove(Constants.MDC_ITEM_TYPE_KEY);
        MDC.remove(Constants.MDC_NODENAME_KEY);

    }
}
