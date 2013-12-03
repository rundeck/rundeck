<%@ page import="com.dtolabs.rundeck.server.authorization.AuthConstants" %>


<g:if test="${inlineView}">
    <div class="inlinestatus row" id="jobInfo_${execution.id}">
            <div class="col-sm-6">
                <span class="inline_only ">
                    <g:link class="primary"
                            title="Show execution #${execution.id}"
                            controller="execution" action="show" id="${execution.id}"
                            params="">
                        <i class="exec-status icon ${!execution.dateCompleted ? 'running' : execution.status == 'true' ? 'succeed' : execution.cancelled ? 'warn' : 'fail'}">
                        </i>
                        <g:message code="execution.identity" args="[execution.id]"/>
                    </g:link>
                </span>
            <g:if test="${null != execution.dateCompleted}">

                <span class="${execution.status == 'true' ? 'succeed' : 'fail'}">
                    <g:if test="${execution.status == 'true'}">
                        Succeeded
                    </g:if>
                    <g:elseif test="${execution.cancelled}">
                        Killed
                    </g:elseif>
                    <g:else>
                        Failed
                    </g:else>
                </span>
            </g:if>
            <g:else>
                <span id="runstatus">
                    <span class="nowrunning">
                        <img src="${resource(dir: 'images', file: 'icon-tiny-disclosure-waiting.gif')}"
                             alt="Spinner"/>
                        Now Running&hellip;
                    </span>
                </span>
                <g:if test="${authChecks[AuthConstants.ACTION_KILL]}">
                    <span id="cancelresult" style="margin-left:10px">
                        <span class="btn btn-danger btn-xs act_cancel" onclick="docancel();">Kill <g:message
                                code="domain.ScheduledExecution.title"/>
                            <i class="glyphicon glyphicon-remove"></i>
                        </span>
                    </span>
                </g:if>

            </g:else>
        </div>
        <div class="col-sm-6">
            <span  class="retrybuttons execRerun pull-right" style="${wdgt.styleVisible(if: null != execution.dateCompleted)}">
                <g:if test="${scheduledExecution}">
                    <g:if test="${authChecks[AuthConstants.ACTION_RUN]}">
                        <g:link controller="scheduledExecution"
                                action="execute"
                                id="${scheduledExecution.extid}"
                                params="${[retryExecId: execution.id]}"
                                class="btn btn-default btn-xs"
                                title="${g.message(code: 'execution.job.action.runAgain')}">
                            <i class="glyphicon glyphicon-play"></i>
                            <g:message code="execution.action.runAgain"/>&hellip;
                        </g:link>
                    </g:if>
                </g:if>
                <g:else>
                    <g:if test="${jobCreateAllowed}">
                        <g:link
                                controller="scheduledExecution"
                                action="createFromExecution"
                                params="${[executionId: execution.id]}"
                                class="btn btn-default btn-xs"
                                title="${g.message(code: 'execution.action.saveAsJob', default: 'Save as Job')}">
                            <g:message code="execution.action.saveAsJob" default="Save as Job"/>&hellip;
                        </g:link>
                    </g:if>
                </g:else>
            </span>
        </div>
        %{--<td width="50%">--}%
            %{--<div id="progressContainer" class="progressContainer">--}%
                %{--<div class="progressBar" id="progressBar"--}%
                     %{--title="Progress is an estimate based on average execution time for this ${g.message(code: 'domain.ScheduledExecution.title')}.">0%</div>--}%
            %{--</div>--}%
        %{--</td>--}%
    </div>
</g:if>

<div id="commandFlow" class="commandFlow">

<form action="#" id="outputappendform">
<div class="row row-space">

    <div class="col-sm-8" style="margin-bottom: 10px">

    <a href="#" class="textbtn textbtn-default btn-xs pull-left collapser"
       data-toggle="collapse" data-target="#viewoptions"
       title="Log Output View Options">
        View Options
        <i class="glyphicon glyphicon-chevron-right"></i>
    </a>

    <div class="collapse" id="viewoptions">
                <span  style="${wdgt.styleVisible(unless: followmode == 'node')}"
                      class="obs_node_false ">
                    <span class="obs_grouped_false" style="${wdgt.styleVisible(if: followmode == 'tail')}">
                        <span class="text-muted">Log view:</span>

                        <label
                                class="action  join"
                                title="Click to change"
                                id="colTimeShowLabel">
                            <g:checkBox
                                    name="coltime"
                                    id="colTimeShow"
                                    value="true"
                                    checked="true"
                                    class="opt_display_col_time"/>
                            Time
                        </label>
                        <label
                                class="action  join"
                                title="Click to change"
                                id="colNodeShowLabel">
                            <g:checkBox
                                    name="coltime"
                                    id="colNodeShow"
                                    value="true"
                                    checked="true"
                                    class="opt_display_col_node"/>
                            Node
                        </label>
                        <label
                                class="action  "
                                title="Click to change"
                                id="colStepShowLabel">
                            <g:checkBox
                                    name="coltime"
                                    id="colStepShow"
                                    value="true"
                                    checked="${!inlineView}"
                                    class="opt_display_col_step"/>
                            Step
                        </label>
                    </span>

                </span>
        <span class="text-muted">Node view:</span>
                <label class="out_setmode_toggle out_setmode">
                    <input type="checkbox" ${followmode == 'node' ? 'checked' : ''}/>
                    <g:message code="execution.show.mode.Compact.title" default="Compact"/>
                </label>
                </div>
        </div>
        <div class="col-sm-4">
            <div class="pull-right">
                <span class="tabs-sibling" style="${execution.dateCompleted ? '' : 'display:none'}"
                     id="viewoptionscomplete">
                    <span>
                        <g:link class="textbtn" style="padding:5px;"
                                title="View raw text output"
                                controller="execution" action="downloadOutput" id="${execution.id}"
                                params="[view: 'inline', formatted: false]">
                            Raw</g:link>
                    </span>
                    
                    <span class="sepL">
                        <g:link class="textbtn" style="padding:5px;"
                                title="Download ${filesize > 0 ? filesize + ' bytes' : ''}"
                                controller="execution" action="downloadOutput" id="${execution.id}">
                            <b class="glyphicon glyphicon-file"></b>
                            Download</g:link>
                    </span>
                </span>
            </div>
        </div>
    </div>

</form>
</div>

<div id="fileload" style="display:none;" class=" row">
    <div class="col-sm-12">
        <div class=" progress " id="fileloadprogress" style="width:100%">
            <div class="progress-bar progress-bar-info" role="progressbar" aria-valuenow="10" aria-valuemin="0" aria-valuemax="100" style="width: 10%;">
                <img src="${resource(dir: 'images', file: 'icon-tiny-disclosure-waiting.gif')}" alt="Spinner"/>
                <span id="fileloadpercent"></span>
            </div>
        </div>
    </div>
</div>

<div id="commandPerform" class="commandcontent" style="display:none;  "></div>

<div id="log"></div>
