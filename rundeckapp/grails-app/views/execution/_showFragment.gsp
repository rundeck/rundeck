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
                        <span class="action button textbtn act_cancel" onclick="docancel();">Kill <g:message
                                code="domain.ScheduledExecution.title"/> <img
                                src="${resource(dir: 'images', file: 'icon-tiny-removex.png')}" alt="Kill"
                                width="12px" height="12px"/></span>
                    </span>
                </g:if>

            </g:else>

            <span id="execRerun" style="${wdgt.styleVisible(if: null != execution.dateCompleted)}">
                <g:if test="${scheduledExecution}">
                    <g:if test="${authChecks[AuthConstants.ACTION_RUN]}">
                        &nbsp;
                        <g:link controller="scheduledExecution"
                                action="execute"
                                id="${scheduledExecution.extid}"
                                params="${[retryExecId: execution.id]}"
                                class="action button"
                                title="Run this Job Again with the same options">
                            <g:img file="icon-small-run.png" alt="run" width="16px" height="16px"/>
                            Run Again &hellip;
                        </g:link>
                    </g:if>
                </g:if>
                <g:else>
                    <g:if
                            test="${jobCreateAllowed || adhocRunAllowed}">
                        <g:if
                                test="${!scheduledExecution || scheduledExecution && authChecks[AuthConstants.ACTION_READ]}">
                            <g:link
                                    controller="scheduledExecution"
                                    action="createFromExecution"
                                    params="${[executionId: execution.id]}"
                                    class="action button"
                                    title="${g.message(code: 'execution.action.saveAsJob', default: 'Save as Job')}&hellip;">
                                <g:img file="icon-small-run.png" alt="run" width="16px" height="16px"/>
                                <g:message code="execution.action.saveAsJob" default="Save as Job"/>&hellip;
                            </g:link>
                        </g:if>
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
<table width="100%" cellpadding="0" cellspacing="0" style="border-collapse: collapse; border-spacing: 0;">

<tr>

    <td class="outputButtons" style="padding:0">
        <span class="tabset">
            <g:link class="tab ${followmode == 'tail' ? ' selected' : ''} out_setmode_tail"
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

            %{--<g:link class="tab ${followmode == 'browse' ? ' selected' : ''} out_setmode_browse"--}%
                    %{--title="${g.message(code: 'execution.show.mode.Annotated.desc')}"--}%
                    %{--controller="execution" action="show" id="${execution.id}" params="[mode: 'browse']"--}%
                    %{--onclick="selectTab(this);">--}%
                %{--<g:message code="execution.show.mode.Annotated.title" default="Grouped"/>--}%
            %{--</g:link>--}%

            <g:link class="tab ${followmode == 'node' ? ' selected' : ''} out_setmode_node"
                    title="${g.message(code: 'execution.show.mode.Compact.desc')}"
                    controller="execution" action="show" id="${execution.id}" params="[mode: 'node']"
                    onclick="selectTab(this);">
                <g:message code="execution.show.mode.Compact.title" default="Compact"/>
            </g:link>
        </span>

        <span id="viewoptions" style="${wdgt.styleVisible(unless: followmode == 'node')}" class="obs_node_false">

            %{--<span--}%
            %{--class="action  join"--}%
            %{--title="Click to change"--}%
            %{--id="showGroupedLabel"--}%
            %{--style="${wdgt.styleVisible(if: followmode=='tail')}"--}%
            %{--onclick="followControl.setGroupOutput($('showGrouped').checked);">--}%
            %{--<input--}%
            %{--type="checkbox"--}%
            %{--name="showGrouped"--}%
            %{--id="showGrouped"--}%
            %{--value="true"--}%
            %{--${followmode == 'tail' ? '' :  'checked="CHECKED"' }--}%
            %{--style=""/>--}%
            %{--<label for="showGrouped">Grouped</label>--}%
            %{--</span>--}%
            <span id="fullviewopts" style="${followmode != 'browse' ? 'display:none' : ''}"
                  class="obs_grouped_true">
                <span class="info note">View options:</span>
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
                <span class="info note">Show columns:</span>

                <label
                        class="action  join"
                        title="Click to change"
                        id="colTimeShowLabel">
                    <g:checkBox
                            name="coltime"
                            id="colTimeShow"
                            value="true"
                            checked="true"
                            class="opt_display_col_time"
                            />
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
                            class="opt_display_col_node"
                            />
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
                            class="opt_display_col_step"
                            />
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
        </span>
    </td>
    <td style="width:50%; text-align: right;">
        <span style="${execution.dateCompleted ? '' : 'display:none'}" id="viewoptionscomplete">
            <span>
                <g:link class="action txtbtn" style="padding:5px;"
                        title="View raw text output"
                        controller="execution" action="downloadOutput" id="${execution.id}"
                        params="[view: 'inline', formatted: false]">
                    Raw</g:link>
            </span>
            <span class="sepL">
                <g:link class="action txtbtn" style="padding:5px;"
                        title="View raw text output"
                        controller="execution" action="follow" id="${execution.id}"
                        params="[markdown: params.markdown=='group'?'none':'group',mode:params.mode]">
                    ${params.markdown == 'group'?'No Markdown':'Markdown'}</g:link>
            </span>
            <span class="sepL">
                <g:link class="action txtbtn" style="padding:5px;"
                        title="Download ${filesize > 0 ? filesize + ' bytes' : ''}"
                        controller="execution" action="downloadOutput" id="${execution.id}">
                    <img src="${resource(dir: 'images', file: 'icon-small-file.png')}" alt="Download"
                         width="13px" height="16px"/>
                    Download</g:link>
            </span>
        </span>
    </td>
</tr>
</table>
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
