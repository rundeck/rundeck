<%@ page import="com.dtolabs.rundeck.server.authorization.AuthConstants" %>
    <g:set var="followmode" value="${params.mode in ['browse','tail','node']?params.mode:null==execution?.dateCompleted?'tail':'browse'}"/>
<g:set var="authKeys"
       value="${[AuthConstants.ACTION_KILL, AuthConstants.ACTION_READ, AuthConstants.ACTION_CREATE, AuthConstants.ACTION_RUN]}"/>
<g:set var="authChecks" value="${[:]}"/>
<g:each in="${authKeys}" var="actionName">
    <g:if test="${execution.scheduledExecution}">
    <%-- set auth values --%>
        %{
            authChecks[actionName] = auth.jobAllowedTest(job: execution.scheduledExecution, action: actionName)
        }%
    </g:if>
    <g:else>
        %{
            authChecks[actionName] = auth.adhocAllowedTest(action: actionName)
        }%
    </g:else>
</g:each>
<g:set var="adhocRunAllowed" value="${auth.adhocAllowedTest(action: AuthConstants.ACTION_RUN)}"/>

<g:set var="defaultLastLines" value="${grailsApplication.config.rundeck.gui.execution.tail.lines.default}"/>
<g:set var="maxLastLines" value="${grailsApplication.config.rundeck.gui.execution.tail.lines.max}"/>

    <div id="commandFlow" class="commandFlow">
        <table width="100%">
            <tr>
                <td width="50%">

        <g:if test="${null!=execution.dateCompleted}">

                    Status:
                    <span class="${execution.status=='true'?'succeed':'fail'}" >
                        <g:if test="${execution.status=='true'}">
                            Successful
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
                        <img src="${resource(dir:'images',file:'icon-tiny-disclosure-waiting.gif')}" alt="Spinner"/>
                        Now Running&hellip;
                        </span>
                        </span>
                <g:if test="${authChecks[AuthConstants.ACTION_KILL]}">
                        <span id="cancelresult" style="margin-left:10px">
                            <span class="action button textbtn act_cancel" onclick="docancel();">Kill <g:message code="domain.ScheduledExecution.title"/> <img src="${resource(dir:'images',file:'icon-tiny-removex.png')}" alt="Kill" width="12px" height="12px"/></span>
                        </span>
                </g:if>

            </g:else>

                    <span id="execRerun" style="${wdgt.styleVisible(if:null!=execution.dateCompleted)}" >
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
                                test="${auth.resourceAllowedTest(kind: 'job', action: [AuthConstants.ACTION_CREATE]) || adhocRunAllowed}">
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
                </td>
                <td width="50%" >
                    <div id="progressContainer" class="progressContainer" >
                        <div class="progressBar" id="progressBar" title="Progress is an estimate based on average execution time for this ${g.message(code:'domain.ScheduledExecution.title')}.">0%</div>
                    </div>
                </td>
            </tr>
        </table>
    </div>

    <div id="commandPerformOpts" class="outputdisplayopts" style="margin: 0 20px;">
        <form action="#" id="outputappendform">

        <table width="100%">
            <tr>
                <td class="buttonholder" style="padding:10px;">
                    <g:link class="tab ${followmode=='tail'?' selected':''} out_setmode_tail out_setmode" style="padding:5px;"
                        title="${g.message(code:'execution.show.mode.Tail.desc')}"
                        controller="execution"  action="show" id="${execution.id}" params="${[lastlines:params.lastlines,mode:'tail'].findAll{it.value}}">Tail Output</g:link>
                    <g:link class="tab ${followmode=='browse'?' selected':''} out_setmode_browse  out_setmode" style="padding:5px;"
                        title="${g.message(code:'execution.show.mode.Annotated.desc')}"
                        controller="execution"  action="show" id="${execution.id}" params="[mode:'browse']">Annotated</g:link>
                    <g:link class="tab ${followmode=='node'?' selected':''} out_setmode_node  out_setmode" style="padding:5px;"
                        title="${g.message(code:'execution.show.mode.Compact.desc')}"
                        controller="execution"  action="show" id="${execution.id}" params="[mode:'node']"><g:message code="execution.show.mode.Compact.title" default="Compact"/></g:link>
                    
            <span id="fullviewopts" style="${followmode!='browse'?'display:none':''}" class="opt_mode_browse opt_mode">
                    <label
                        class="action textbtn button opt_append_top_true"
                        title="Click to change"
                            id="appendTopLabel"
                        onclick="setOutputAppendTop(true);">
                    <input
                        type="radio"
                        name="outputappend"
                        id="outputappendtop"
                        value="top"
                        style="display: none;"/>
                    Top</label>
                    <label
                            class="action textbtn button  opt_append_top_false"
                            title="Click to change"
                            id="appendBottomLabel"
                            onclick="setOutputAppendTop(false);"
                        >
                    <input
                        type="radio"
                        name="outputappend"
                        id="outputappendbottom"
                        value="bottom"
                        checked="CHECKED"
                        style="display: none;"/>
                    Bottom</label>

<%--
            </td>
            <td>--%>
                <label class="action textbtn button "
                      title="Click to change"
                      id="ctxshowgroupoption"
                      >
                <input
                    class="opt_group_output"
                    onclick="setGroupOutput($('ctxshowgroup').checked);"
                    type="checkbox"
                    name="ctxshowgroup"
                    id="ctxshowgroup"
                    value="true"
                    ${followmode=='tail'?'':'checked="CHECKED"'}
                    />
                    Group commands</label>
<%--
                </td>

            <td >--%>
                &nbsp;
                <label
                    class="action textbtn button "
                    title="Click to change"
                    id="ctxcollapseLabel"
                    >
                <input
                    class="opt_collapse_ctx"
                    onclick="setCollapseCtx($('ctxcollapse').checked);"
                    type="checkbox"
                    name="ctxcollapse"
                    id="ctxcollapse"
                    value="true"
                    ${followmode=='tail'?'':null==execution?.dateCompleted?'checked="CHECKED"':''}
                    />
                    Collapse</label>
<%--
            </td>
            <td>--%>
                &nbsp;
                <label class="action textbtn button"
                      title="Click to change"
                      id="ctxshowlastlineoption"
                      >
                <input
                    class="opt_show_final"
                    onclick="setShowFinalLine($('ctxshowlastline').checked);"
                    type="checkbox"
                    name="ctxshowlastline"
                    id="ctxshowlastline"
                    value="true"
                    checked="CHECKED"
                    />
                    Show final line</label>
            </span>
                <span class="opt_mode_tail opt_mode" style="${wdgt.styleVisible(if:followmode=='tail')}">
<%---
                </td>
                <td>--%>
                    Show the last
                    <span class="action textbtn button opt_last_lines_dec"
                      title="Click to reduce"
                      onmousedown="modifyLastlines(-5);return false;">-</span>
                <input
                    class=" opt_last_lines_val"
                    type="number"
                    min="5"
                    max="100"
                    step="5"
                    name="lastlines"
                    id="lastlinesvalue"
                    value="${params.lastlines?params.lastlines: defaultLastLines}"
                    size="3"
                    onchange="updateLastlines(this.value)"
                    onkeypress="var x= noenter();if(!x){this.blur();};return x;"
                    style=""/>
                    <span class="action textbtn button  opt_last_lines_inc"
                      title="Click to increase"
                      onmousedown="modifyLastlines(5);return false;">+</span>

                    lines<span id="taildelaycontrol" style="${execution.dateCompleted?'display:none':''}">,
                    and update every


                    <span class="action textbtn button opt_update_every_dec"
                      title="Click to reduce"
                      onmousedown="modifyTaildelay(-1);return false;">-</span>
                <input
                    class=" opt_update_every_val"
                    type="number"
                    min="1"
                    max="30"
                    step="1"
                    name="taildelay"
                    id="taildelayvalue"
                    value="1"
                    size="2"
                    onchange="updateTaildelay(this.value)"
                    onkeypress="var x= noenter();if(!x){this.blur();};return x;"
                    style=""/>
                    <span class="action textbtn button  opt_update_every_inc"
                      title="Click to increase"
                      onmousedown="modifyTaildelay(1);return false;">+</span>

                    seconds
                </span>
                </span>
                </td>
                <td align="right">
                    <span style="${execution.dateCompleted ? '' : 'display:none'}" class="sepL" id="viewoptionscomplete">
                        <g:link class="action txtbtn" style="padding:5px;"
                            title="Download entire output file" 
                            controller="execution" action="downloadOutput" id="${execution.id}"><img src="${resource(dir:'images',file:'icon-small-file.png')}" alt="Download" title="Download output" width="13px" height="16px"/> Download <span id="outfilesize">${filesize?filesize+' bytes':''}</span></g:link>
                    </span>
                </td>
            </tr>
            </table>
        </form>
    </div>
    <div id="fileload2" style="display:none;" class="outputdisplayopts"><img src="${resource(dir:'images',file:'icon-tiny-disclosure-waiting.gif')}" alt="Spinner"/> Loading Output... <span id="fileloadpercent"></span></div>
    <div
        id="commandPerform"
        style="display:none; margin: 0 20px; "></div>
    <div id="fileload" style="display:none;" class="outputdisplayopts"><img src="${resource(dir:'images',file:'icon-tiny-disclosure-waiting.gif')}" alt="Spinner"/> Loading Output... <span id="fileload2percent"></span></div>
    <div id="log"></div> 


