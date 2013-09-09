<%@ page import="com.dtolabs.rundeck.server.authorization.AuthConstants" %>


<g:if test="${inlineView}">
    <div class="inlinestatus">
            <div>
                <span class="inline_only ">
                    <g:link class="action txtbtn" style="padding:5px;"
                            title="Show execution #${execution.id}"
                            controller="execution" action="show" id="${execution.id}"
                            params="">
                        Execution #${execution.id} &raquo;</g:link>
                </span>
            <g:if test="${null != execution.dateCompleted}">

                Status:
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
                Status:

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

            <span id="execRerun" class="retrybuttons" style="${wdgt.styleVisible(if: null != execution.dateCompleted)}">
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
                                title="${g.message(code: 'execution.action.saveAsJob', default: 'Save as Job')}&hellip;">
                            <g:message code="execution.action.saveAsJob" default="Save as Job"/>&hellip;
                        </g:link>
                    </g:if>
                    <g:if test="${adhocRunAllowed && !inlineView}">
                            <g:link
                                    controller="framework"
                                    action="nodes"
                                    params="${[fromExecId: execution.id]}"
                                    class="btn btn-default btn-xs"
                                    title="${g.message(code: 'execution.action.runAgain')}">
                                <i class="glyphicon glyphicon-play"></i>
                                <g:message code="execution.action.runAgain"/>&hellip;
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

    <div class="col-sm-12">
        <ul class="nav nav-tabs">
            <li class="${followmode == 'tail' ? ' active' : ''}">
            <g:link class="tab out_setmode_tail out_setmode"
                    title="${g.message(code: 'execution.show.mode.Tail.desc')}"
                    controller="execution" action="show" id="${execution.id}"
                    params="${[lastlines: params.lastlines, mode: 'tail'].findAll { it.value }}"
                    onclick="selectTab(this);">
                <g:if test="${inlineView}">
                    <g:message code="execution.show.mode.Tail.title" default="Tail Output"/>
                </g:if>
                <g:else>
                    <g:message code="execution.show.mode.Log.title" default="Log Output"/>
                </g:else>
            </g:link>
            </li>
            %{--<g:link class="tab ${followmode == 'browse' ? ' selected' : ''} out_setmode_browse"--}%
                    %{--title="${g.message(code: 'execution.show.mode.Annotated.desc')}"--}%
                    %{--controller="execution" action="show" id="${execution.id}" params="[mode: 'browse']"--}%
                    %{--onclick="selectTab(this);">--}%
                %{--<g:message code="execution.show.mode.Annotated.title" default="Grouped"/>--}%
            %{--</g:link>--}%
            <li class="${followmode == 'node' ? ' active' : ''}">
            <g:link class="tab out_setmode_node out_setmode"
                    title="${g.message(code: 'execution.show.mode.Compact.desc')}"
                    controller="execution" action="show" id="${execution.id}" params="[mode: 'node']"
                    onclick="selectTab(this);">
                <g:message code="execution.show.mode.Compact.title" default="Compact"/>
            </g:link>
            </li>
            <li>
                <div id="viewoptions" style="${wdgt.styleVisible(unless: followmode == 'node')}"
                      class="obs_node_false tabs-sibling">

                    <span id="fullviewopts" style="${followmode != 'browse' ? 'display:none' : ''}"
                          class="obs_grouped_true form-inline">
                        <span class="text-muted">View options:</span>
                        <label
                                class="action "
                                title="Click to change"
                                id="ctxcollapseLabel"
                                onclick="followControl.setCollapseCtx($('ctxcollapse').checked);">
                            <input
                                    type="checkbox"
                                    name="ctxcollapse"
                                    id="ctxcollapse"
                                    value="true"
                                    class="opt_collapse_ctx"
                                ${followmode == 'tail' ? '' : null == execution?.dateCompleted ? 'checked="CHECKED"' : ''}
                                    style=""/>
                            Collapse
                        </label>

                    </span>


                    <span class="obs_grouped_false" style="${wdgt.styleVisible(if: followmode == 'tail')}">
                        <span class="text-muted">Show columns:</span>

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

                    %{--<span id="taildelaycontrol" style="${execution.dateCompleted?'display:none':''}">,--}%
                    %{--and update every--}%


                    %{--<span class="action textbtn button"--}%
                    %{--title="Click to reduce"--}%
                    %{--onmousedown="followControl.modifyTaildelay(-1);return false;">-</span>--}%
                    %{--<input--}%
                    %{--type="text"--}%
                    %{--name="taildelay"--}%
                    %{--id="taildelayvalue"--}%
                    %{--value="1"--}%
                    %{--size="2"--}%
                    %{--onchange="updateTaildelay(this.value)"--}%
                    %{--onkeypress="var x= noenter();if(!x){this.blur();};return x;"--}%
                    %{--style=""/>--}%
                    %{--<span class="action textbtn button"--}%
                    %{--title="Click to increase"--}%
                    %{--onmousedown="followControl.modifyTaildelay(1);return false;">+</span>--}%

                    %{--seconds--}%
                </div>
            </li>
            <li class="pull-right">
                <div class="tabs-sibling" style="${execution.dateCompleted ? '' : 'display:none'}"
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
                                title="View raw text output"
                                controller="execution" action="follow" id="${execution.id}"
                                params="[markdown: params.markdown == 'group' ? 'none' : 'group', mode: params.mode]">
                            ${params.markdown == 'group' ? 'No Markdown' : 'Markdown'}</g:link>
                    </span>
                    <span class="sepL">
                        <g:link class="textbtn" style="padding:5px;"
                                title="Download ${filesize > 0 ? filesize + ' bytes' : ''}"
                                controller="execution" action="downloadOutput" id="${execution.id}">
                            <b class="glyphicon glyphicon-file"></b>
                            Download</g:link>
                    </span>
                </div>
            </li>
        </ul>
        </div>

</div>
</form>
</div>

<div id="fileload2" style="display:none;" class="outputdisplayopts"><img
        src="${resource(dir: 'images', file: 'icon-tiny-disclosure-waiting.gif')}" alt="Spinner"/> <span
        id="fileloadpercent"></span></div>

<div
        id="commandPerform"
        class="commandcontent"
        style="display:none;  "></div>

<div id="fileload" style="display:none;" class="outputdisplayopts"><img
        src="${resource(dir: 'images', file: 'icon-tiny-disclosure-waiting.gif')}" alt="Spinner"/> <span
        id="fileload2percent"></span></div>

<div id="log"></div>
