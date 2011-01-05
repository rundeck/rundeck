    <g:set var="followmode" value="${params.mode in ['browse','tail','node']?params.mode:null==execution?.dateCompleted?'tail':'browse'}"/>
    <g:set var="executionResource" value="${ ['jobName': execution.scheduledExecution ? execution.scheduledExecution.jobName : 'adhoc', 'groupPath': execution.scheduledExecution ? execution.scheduledExecution.groupPath : 'adhoc'] }"/>


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
                    <auth:allowed job="${executionResource}" name="${UserAuth.WF_KILL}">
                        <span id="cancelresult" style="margin-left:10px">
                            <span class="action button textbtn act_cancel" onclick="docancel();">Kill <g:message code="domain.ScheduledExecution.title"/> <img src="${resource(dir:'images',file:'icon-tiny-removex.png')}" alt="Kill" width="12px" height="12px"/></span>
                        </span>
                    </auth:allowed>

            </g:else>

                    <span id="execRetry" style="${wdgt.styleVisible(if:null!=execution.dateCompleted && null!=execution.failedNodeList)}; margin-right:10px;">
                        <g:if test="${scheduledExecution}">
                            <g:set var="jobRunAuth" value="${ auth.allowedTest(job:executionResource,action:UserAuth.WF_RUN)}"/>
                            <g:set var="canRun" value="${ ( !authMap || authMap[scheduledExecution.id.toString()] ||jobAuthorized ) && jobRunAuth}"/>
                            <g:if test="${ canRun}">
                                <g:link controller="scheduledExecution" action="execute" id="${scheduledExecution.id}" params="${[retryFailedExecId:execution.id]}" title="Run Job on the failed nodes" class="action button" style="margin-left:10px" >
                                    <img src="${resource(dir:'images',file:'icon-small-run.png')}" alt="run" width="16px" height="16px"/>
                                    Retry Failed Nodes  &hellip;
                                </g:link>
                            </g:if>
                        </g:if>
                        <g:else>
                            <g:set var="jobRunAuth" value="${ auth.allowedTest(job:executionResource,action:[UserAuth.WF_CREATE,UserAuth.WF_READ])}"/>
                            <g:set var="canRun" value="${ jobRunAuth}"/>
                            <g:if test="${canRun}">
                                <g:link controller="scheduledExecution" action="createFromExecution" params="${[executionId:execution.id,failedNodes:true]}" class="action button" title="Retry on the failed nodes&hellip;" style="margin-left:10px">
                                    <img src="${resource(dir:'images',file:'icon-small-run.png')}"  alt="run" width="16px" height="16px"/>
                                    Retry Failed Nodes &hellip;
                                </g:link>
                            </g:if>
                        </g:else>
                    </span>
                    <span id="execRerun" style="${wdgt.styleVisible(if:null!=execution.dateCompleted)}" >
                        <g:set var="jobRunAuth" value="${ auth.allowedTest(job:executionResource, action:[UserAuth.WF_CREATE,UserAuth.WF_READ])}"/>
                        <g:if test="${jobRunAuth }">
                            <g:link controller="scheduledExecution" action="createFromExecution" params="${[executionId:execution.id]}" class="action button" title="Create a Job&hellip;" ><img src="${resource(dir:'images',file:'icon-small-add.png')}"  alt="run" width="14px" height="14px"/> Create a Job&hellip;</g:link>
                        </g:if>
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
                        title="View the last lines of the output file"
                        controller="execution"  action="show" id="${execution.id}" params="${[lastlines:params.lastlines,mode:'tail'].findAll{it.value}}">Tail Output</g:link>
                    <g:link class="tab ${followmode=='browse'?' selected':''} out_setmode_browse  out_setmode" style="padding:5px;"
                        title="Load the entire file in grouped contexts "
                        controller="execution"  action="show" id="${execution.id}" params="[mode:'browse']">Annotated</g:link>
                    <g:link class="tab ${followmode=='node'?' selected':''} out_setmode_node  out_setmode" style="padding:5px;"
                        title="Load the entire file in grouped contexts "
                        controller="execution"  action="show" id="${execution.id}" params="[mode:'node']">Node Output</g:link>
                    
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
                <label
                    class="action textbtn button"
                    title="Click to change"
                    id="autoscrollTrueLabel"
                >
                <input
                    type="checkbox"
                    name="outputautoscroll"
                    class=" opt_auto_scroll_true"
                    id="outputautoscrolltrue"
                    value="true"
                    ${followmode=='tail'?'':'checked="CHECKED"'}
                    onclick="setOutputAutoscroll(this.checked);"
                    style=""/>

                Scroll</label>

                <%--
                <input
                    type="radio"
                    name="outputautoscroll"
                    id="outputautoscrollfalse"
                    value="false"
                    style="display:none;"/>
                <label for="outputautoscrollfalse"><span
                    class="action textbtn button"
                    title="Click to change"
                    id="autoscrollFalseLabel"
                    onclick="setOutputAutoscroll(false);"
                >no</span></label>
                --%>
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
                    value="${params.lastlines?params.lastlines:20}"
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


