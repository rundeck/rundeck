<%@ page import="com.dtolabs.rundeck.server.authorization.AuthConstants" %>

<g:if test="${!execution.dateCompleted}">
    <g:jsonToken id="exec_cancel_token" url="${request.forwardURI}"/>
</g:if>

<g:if test="${inlineView}">
    <div class="panel-heading" data-affix="wrap">
    <div class="executionshow panel-heading-affix" data-affix="top" data-affix-padding-top="8" id="jobInfo_${execution.id}" >
        <div class="row">
            <div class="col-sm-6">
                <span class="inline_only ">
                    <g:link class="primary"
                            title="Show execution #${execution.id}"
                            controller="execution" action="show" id="${execution.id}"
                            params="[project:execution.project]">
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
                <span class="retrybuttons execRerun " style="${wdgt.styleVisible(if: null != execution.dateCompleted)}">
                    <g:if test="${scheduledExecution}">
                        <g:if test="${authChecks[AuthConstants.ACTION_RUN]}">
                            <g:link controller="scheduledExecution"
                                    action="execute"
                                    id="${scheduledExecution.extid}"
                                    params="${[retryExecId: execution.id,project:execution.project]}"
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
                                    params="${[executionId: execution.id, project: execution.project]}"
                                    class="btn btn-default btn-xs"
                                    title="${g.message(code: 'execution.action.saveAsJob', default: 'Save as Job')}">
                                <g:message code="execution.action.saveAsJob" default="Save as Job"/>&hellip;
                            </g:link>
                        </g:if>
                    </g:else>
                </span>
            </div>
            <div class="col-sm-6">
                <g:render template="wfItemView" model="[
                        item: execution.workflow.commands[0],
                        icon: 'icon-med',
                        iwidth: '24px',
                        iheight: '24px',
                ]"/>
                <button class="close closeoutput">&times;</button>
                <div class="affixed-shown pull-right">
                    <a class="textbtn textbtn-default textbtn-on-hover btn-xs" href="#top">
                        <g:message code="scroll.to.top" />
                        <i class="glyphicon glyphicon-arrow-up"></i>
                    </a>
                </div>
                <div class="affixed-hidden pull-right">
                    <a class="textbtn textbtn-default textbtn-on-hover btn-xs" href="#bottom">
                        Scroll to Bottom
                        <i class="glyphicon glyphicon-arrow-down"></i>
                    </a>
                </div>
            </div>
        </div>
    </div>
    </div>
</g:if>
<div class="${inlineView?'panel-body inlineexecution':''}">
<div id="commandFlow" class="outputcontrols">

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

                <label class="ansi-color-toggle">
                    <input type="checkbox" checked/>
                    <g:message code="execution.show.mode.ansicolor.title" default="Ansi Color"/>
                </label>

                </div>
        </div>
        <div class="col-sm-4">
            <div class="pull-right">
                <span class="tabs-sibling" style="${execution.dateCompleted ? '' : 'display:none'}"
                     id="viewoptionscomplete">
                    <span>
                        <g:link class="textbtn" style="padding:5px;"
                                title="View text output"
                                controller="execution" action="downloadOutput" id="${execution.id}"
                                params="[view: 'inline', formatted: false, project: execution.project,
                                        stripansi:true]">
                            Text</g:link>
                    </span>
                    <span>
                        <g:link class="textbtn" style="padding:5px;"
                                title="View colorized output"
                                controller="execution" action="renderOutput" id="${execution.id}"
                                params="[project: execution.project, ansicolor:'on',loglevels:'on']">
                            HTML</g:link>
                    </span>

                    <span class="sepL">
                        <g:link class="textbtn" style="padding:5px;"
                                title="Download ${enc(attr: filesize > 0 ? filesize + ' bytes' : '')}"
                                controller="execution" action="downloadOutput" id="${execution.id}"
                                params="[project: execution.project]">
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

<div id="commandPerform" class="commandcontent ansicolor ansicolor-on" style="display:none;  "></div>

<div id="log"></div>
</div>
