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
 * CorrelationAppender.java
 * 
 * User: greg
 * Created: Jun 21, 2005 12:54:11 PM
 * $Id: LuceneAppender.java 5688 2006-01-14 20:00:28Z connary_scott $
 */
package com.dtolabs.rundeck.services;


import com.dtolabs.shared.reports.Constants;
import org.apache.log4j.AppenderSkeleton;
import org.apache.log4j.spi.LoggingEvent;

import java.util.Date;
import java.util.HashMap;


/**
 *
 * @author Greg Schueler <a href="mailto:greg@dtosolutions.com">greg@dtosolutions.com</a>
 * @version $Revision$
 */
public class ReportAppender extends AppenderSkeleton {

    private ReportAcceptor acceptor;

    public ReportAppender(ReportAcceptor acceptor) {
        this.acceptor = acceptor;


    }

    protected void append(LoggingEvent event) {
        HashMap map = new HashMap();
        map.put("author", event.getMDC(Constants.MDC_AUTHOR_KEY));
        map.put("node", event.getMDC(Constants.MDC_NODENAME_KEY));
        map.put("reportId", event.getMDC(Constants.MDC_REPORTID_KEY));
        map.put("ctxProject", event.getMDC(Constants.MDC_PROJECT_KEY));
        map.put("ctxType", event.getMDC(Constants.MDC_ENT_TYPE_KEY));
        map.put("ctxName", event.getMDC(Constants.MDC_ENT_NAME_KEY));
        map.put("ctxCommand", event.getMDC(Constants.MDC_CMD_NAME_KEY));
        map.put("maprefUri", event.getMDC(Constants.MDC_MAPREF_KEY));
        map.put("evtAction", event.getMDC(Constants.MDC_ACTION_KEY));
        map.put("evtActionType", event.getMDC(Constants.MDC_ACTION_TYPE_KEY));
        map.put("ctxController", event.getMDC(Constants.MDC_CONTROLLER_KEY));
        map.put("evtItemType", event.getMDC(Constants.MDC_ITEM_TYPE_KEY));
        map.put("evtRemoteHost", event.getMDC(Constants.MDC_REMOTE_HOST_KEY));
        map.put("evtPatternName", event.getMDC(Constants.MDC_PAT_NAME_KEY));
        map.put("adhocExecution", event.getMDC(Constants.MDC_ADHOCEXEC_KEY));
        map.put("rundeckExecId", event.getMDC("rundeckExecId"));
        map.put("rundeckJobId", event.getMDC("rundeckJobId"));
        map.put("rundeckJobName", event.getMDC("rundeckJobName"));
        map.put("rundeckAbortedBy", event.getMDC("rundeckAbortedBy"));
        map.put("epochDateStarted", event.getMDC(Constants.MDC_EPOCHSTART_KEY));
        map.put("epochDateEnded", event.getMDC(Constants.MDC_EPOCHEND_KEY));
        if (null!=event.getMDC("adhocScript")) {
            map.put("adhocScript", event.getMDC("adhocScript"));
        } else if(null!=event.getMDC(Constants.MDC_ADHOCSCRIPT_KEY)) {
            map.put("adhocScript", event.getMDC(Constants.MDC_ADHOCSCRIPT_KEY));
        }
        map.put("tags", event.getMDC(Constants.MDC_TAGS_KEY));
        map.put("dateCompleted", new Date());
        map.put("message", event.getMessage());
        acceptor.makeReport(map);
    }

    public void activateOptions() {
        super.activateOptions();

    }

    public boolean requiresLayout() {
        return false;
    }

    public void close() {
        closed = true;
    }


}
