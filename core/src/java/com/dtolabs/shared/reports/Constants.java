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
 * Constants.java
 * 
 * User: greg
 * Created: Jun 22, 2005 11:44:00 AM
 * $Id: Constants.java 9667 2010-01-10 17:14:29Z ahonor $
 */
package com.dtolabs.shared.reports;





/**
 * Constants contains shared constant values.
 *
 * @author Greg Schueler <a href="mailto:greg@dtosolutions.com">greg@dtosolutions.com</a>
 * @version $Revision: 9667 $
 */
public class Constants {
    public static final String INDEX_LOGGER_NAME = "com.dtolabs.rundeck.log.common";


    public static final String MDC_REMOTE_HOST_KEY = "rundeckRemoteHost";
    public static final String MDC_PROJECT_KEY = "rundeckProject";
    public static final String MDC_MAPREF_KEY = "rundeckMaprefUri";
    public static final String MDC_AUTHOR_KEY = "rundeckAuthor";
    public static final String MDC_ENT_TYPE_KEY = "rundeckResourceType";
    public static final String MDC_ENT_NAME_KEY = "rundeckResourceName";
    public static final String MDC_PAT_NAME_KEY = "rundeckPatternName";
    public static final String MDC_ACTION_KEY = "rundeckAction";
    public static final String MDC_ACTION_TYPE_KEY = "rundeckActionType";
    public static final String MDC_ITEM_TYPE_KEY = "rundeckItemType";
    public static final String MDC_CMD_NAME_KEY = "rundeckCommandName";
    public static final String MDC_CONTROLLER_KEY = "rundeckController";
    public static final String MDC_NODENAME_KEY = "rundeckNodeName";
    public static final String MDC_REPORTID_KEY = "rundeckReportId";
    public static final String MDC_ADHOCEXEC_KEY = "rundeckAdhocExec";
    public static final String MDC_ADHOCSCRIPT_KEY = "rundeckAdhocScript";
    public static final String MDC_EPOCHSTART_KEY = "epochDateStarted";
    public static final String MDC_EPOCHEND_KEY = "epochDateEnded";
    public static final String MDC_TAGS_KEY = "rundeckTags";

    public static final String LUC_REMOTE_HOST_FIELD= "remoteHost";
    public static final String LUC_PROJECT_FIELD = "project";
    public static final String LUC_MAPREF_FIELD = "maprefUri";
    public static final String LUC_AUTHOR_FIELD = "author";
    public static final String LUC_ENT_TYPE_FIELD = "resourceType";
    public static final String LUC_ENT_NAME_FIELD = "resourceName";
    public static final String LUC_PAT_NAME_FIELD = "patternName";
    public static final String LUC_ACTION_FIELD = "action";
    public static final String LUC_ACTION_TYPE_FIELD = "actionType";
    public static final String LUC_ITEM_TYPE_FIELD = "itemType";
    public static final String LUC_CMD_NAME_FIELD = "commandName";
    public static final String LUC_CONTROLLER_FIELD = "controller";
    public static final String LUC_NODENAME_FIELD = "nodename";

    public static final String LUC_REPORTID_FIELD = "reportId";

    public static final String LUC_LEVEL_FIELD = "level";
    public static final String LUC_DATE_FIELD = "date";
    public static final String LUC_MESSAGE_FIELD = "message";

    /**
     * enum class for action types.
     */
    public static final class ActionType{
        private String type;
        ActionType(String type){
            this.type=type;
        }
        public String toString(){return type;}
        public String getType(){return type;}

        public static final ActionType CREATE = new ActionType("create");
        public static final ActionType DELETE = new ActionType("delete");
        public static final ActionType FAIL = new ActionType("fail");
        public static final ActionType SUCCEED = new ActionType("succeed");
        public static final ActionType CANCEL = new ActionType("cancel");
        public static final ActionType UPDATE = new ActionType("update");
    }
}
