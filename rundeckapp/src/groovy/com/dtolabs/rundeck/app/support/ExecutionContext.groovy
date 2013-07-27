package com.dtolabs.rundeck.app.support

import com.dtolabs.rundeck.app.support.BaseNodeFilters
/**
 * ExecutionContext
 */
abstract class ExecutionContext extends BaseNodeFilters{
    String project
    String argString
    String user
    String loglevel="WARN"
    String serverNodeUUID

    static mapping = {
        user column: "rduser"
        argString type:'text'
        serverNodeUUID type:'text'
    }
    Boolean nodeKeepgoing=false
    Boolean doNodedispatch=false
    Integer nodeThreadcount=1
    String nodeRankAttribute
    Boolean nodeRankOrderAscending=true
}

