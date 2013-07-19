<%@ page import="rundeck.Execution; com.dtolabs.rundeck.server.authorization.AuthConstants" %>
<html>
  <head>
    <meta http-equiv="Content-Type" content="text/html; charset=UTF-8"/>
    <meta name="tabpage" content="events"/>
    <meta name="layout" content="base" />
    <title><g:message code="main.app.name"/> - <g:if test="${null==execution?.dateCompleted}">Now Running - </g:if><g:if test="${scheduledExecution}">${scheduledExecution?.jobName.encodeAsHTML()} :  </g:if><g:else>Adhoc</g:else> Execution at <g:relativeDate atDate="${execution.dateStarted}" /> by ${execution.user}</title>
    <g:set var="followmode" value="${params.mode in ['browse','tail','node']?params.mode:'tail'}"/>
      <g:set var="authKeys" value="${[AuthConstants.ACTION_KILL, AuthConstants.ACTION_READ,AuthConstants.ACTION_CREATE,AuthConstants.ACTION_RUN]}"/>
      <g:set var="authChecks" value="${[:]}"/>
      <g:each in="${authKeys}" var="actionName">
      <g:if test="${execution.scheduledExecution}">
          <%-- set auth values --%>
          %{
              authChecks[actionName]=auth.jobAllowedTest(job:execution.scheduledExecution,action: actionName)
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
      <g:javascript src="executionControl.js?v=${grailsApplication.metadata['app.version']}"/>
      <g:javascript library="prototype/effects"/>
      <g:javascript>
        <g:if test="${scheduledExecution}">
        /** START history
         *
         */
        function loadHistory(){
            new Ajax.Updater('histcontent',"${createLink(controller:'reports',action:'eventsFragment')}",{
                parameters:{compact:true,nofilters:true,jobIdFilter:'${scheduledExecution.id}'},
                evalScripts:true,
                onComplete: function(transport) {
                    if (transport.request.success()) {
                        Element.show('histcontent');
                    }
                }
            });
        }
        </g:if>
        var followControl = new FollowControl('${execution?.id}','outputappendform',{
            appLinks:appLinks,
            iconUrl: "${resource(dir: 'images', file: 'icon-small')}",
            smallIconUrl: "${resource(dir: 'images', file: 'icon-small')}",
            extraParams:"<%="true" == params.disableMarkdown ? '&disableMarkdown=true' : ''%>&markdown=${params.markdown}",
            lastlines: ${params.lastlines ? params.lastlines : defaultLastLines},
            maxLastLines: ${maxLastLines},
            collapseCtx: {value:${null == execution?.dateCompleted },changed:false},
            showFinalLine: {value:false,changed:false},
            tailmode: ${followmode == 'tail'},
            browsemode: ${followmode == 'browse'},
            nodemode: ${followmode == 'node'},
            execData: {node:"${session.Framework.getFrameworkNodeHostname()}"},
            groupOutput:{value:${followmode == 'browse'}},
            updatepagetitle:${null == execution?.dateCompleted},
            <g:if test="${authChecks[AuthConstants.ACTION_KILL]}">
            killjobhtml: '<span class="action button textbtn" onclick="followControl.docancel();">Kill <g:message code="domain.ScheduledExecution.title"/> <img src="${resource(dir: 'images', file: 'icon-tiny-removex.png')}" alt="Kill" width="12px" height="12px"/></span>',
            </g:if>
            <g:if test="${!authChecks[AuthConstants.ACTION_KILL]}">
            killjobhtml: "",
            </g:if>
            totalDuration : 0 + ${scheduledExecution?.totalTime ? scheduledExecution.totalTime : -1},
            totalCount: 0 + ${scheduledExecution?.execCount ? scheduledExecution.execCount : -1}
            <g:if test="${scheduledExecution}">
            , onComplete:loadHistory
            </g:if>
        });


        function init() {
            followControl.beginFollowingOutput('${execution?.id}');
            <g:if test="${!(grailsApplication.config.rundeck?.gui?.enableJobHoverInfo in ['false', false])}">
            $$('.obs_bubblepopup').each(function(e) {
                new BubbleController(e,null,{offx:-14,offy:null}).startObserving();
            });
            </g:if>
        }

        Event.observe(window, 'load', init);

      </g:javascript>
      <style type="text/css">

        #log{
            margin-bottom:20px;
        }

        .inline_only {
            display: none;
        }

      </style>
  </head>

  <body id="executionShowPage">
    <div class="pageTop extra">
        <div class="jobHead">
            <table cellspacing="0" cellpadding="0" width="100%">
                <tr>
                    <td  style="vertical-align: top;">
                        <div class="jobInfo">
                        <g:render template="/scheduledExecution/showExecutionHead"
                                  model="[scheduledExecution: scheduledExecution, execution: execution, followparams: [mode: followmode, lastlines: params.lastlines]]"/>
                        <span class="executioncontrol">

                        <g:if test="${null == execution.dateCompleted}">
                            <g:if test="${authChecks[AuthConstants.ACTION_KILL]}">
                                <span id="cancelresult" style="margin-left:10px">
                                    <span class="action button textbtn"
                                          onclick="followControl.docancel();">Kill <g:message
                                            code="domain.ScheduledExecution.title"/> <img
                                            src="${resource(dir: 'images', file: 'icon-tiny-removex.png')}" alt="Kill"
                                            width="12px" height="12px"/></span>
                                </span>
                            </g:if>
                        </g:if>

                        <span id="execRetry"
                              style="${wdgt.styleVisible(if: null != execution.dateCompleted && null != execution.failedNodeList)}; margin-right:10px;">
                            <g:if test="${scheduledExecution}">
                                <g:if test="${authChecks[AuthConstants.ACTION_RUN]}">
                                    <g:link controller="scheduledExecution" action="execute"
                                            id="${scheduledExecution.extid}"
                                            params="${[retryFailedExecId: execution.id]}"
                                            title="Run Job on the failed nodes"
                                            class="action button" style="margin-left:10px">
                                        <img src="${resource(dir: 'images', file: 'icon-small-run.png')}" alt="run"
                                             width="16px" height="16px"/>
                                        Retry Failed Nodes  &hellip;
                                    </g:link>
                                </g:if>
                            </g:if>
                            <g:else>
                                <g:if test="${auth.resourceAllowedTest(kind: 'job', action: [AuthConstants.ACTION_CREATE]) || adhocRunAllowed}">
                                    <g:link controller="scheduledExecution" action="createFromExecution"
                                            params="${[executionId: execution.id, failedNodes: true]}"
                                            class="action button"
                                            title="Retry on the failed nodes&hellip;" style="margin-left:10px">
                                        <img src="${resource(dir: 'images', file: 'icon-small-run.png')}" alt="run"
                                             width="16px" height="16px"/>
                                        Retry Failed Nodes &hellip;
                                    </g:link>
                                </g:if>
                            </g:else>
                        </span>
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
                                <g:if test="${auth.resourceAllowedTest(kind: 'job', action: [AuthConstants.ACTION_CREATE]) || adhocRunAllowed}">
                                    <g:if test="${!scheduledExecution || scheduledExecution && authChecks[AuthConstants.ACTION_READ]}">
                                        <g:link
                                                controller="scheduledExecution"
                                                action="createFromExecution"
                                                params="${[executionId: execution.id]}"
                                                class="action button"
                                                title="Save these Execution parameters as a Job, or run again...">
                                            <g:img file="icon-small-run.png" alt="run" width="16px" height="16px"/>
                                            Run Again or Save &hellip;
                                        </g:link>
                                    </g:if>
                                </g:if>
                            </g:else>
                        </span>

                            <g:if test="${eprev || enext}">
                                <span style="margin-left:10px; float:right; white-space: nowrap">
                                    <g:if test="${eprev}">
                                        <g:link action="show" controller="execution" id="${eprev.id}"
                                            title="Previous Execution #${eprev.id}">
                                            <g:message code="${scheduledExecution ? 'job' : 'adhoc'}.previous.execution"
                                                args="${[eprev.id]}"/>
                                        </g:link>
                                    </g:if>
                                    <g:else>
                                        <span class="info note">
                                            <g:message code="no.previous.executions" />
                                        </span>
                                    </g:else>
                                    <g:if test="${enext}">
                                        <g:link action="show" controller="execution" class="sepL"
                                                title="Next Execution #${enext.id}"
                                            id="${enext.id}">
                                            <g:message code="${scheduledExecution ? 'job' : 'adhoc'}.next.execution"
                                                       args="${[enext.id]}"/>
                                        </g:link>
                                    </g:if>
                                    <g:else>
                                        <span class="sepL info note">
                                            <g:message code="no.more.executions" />
                                        </span>
                                    </g:else>
                                </span>
                            </g:if>
                        </span>
                        <div style="display: inline-block; margin-left: 10px;">
                            <div id="progressContainer" class="progressContainer" >
                                <div class="progressBar" id="progressBar"
                                     title="Progress is an estimate based on average execution time for this ${g.message(code: 'domain.ScheduledExecution.title')}.">0%</div>
                            </div>
                            </div>
                        </div>

                        <g:set var="isAdhoc" value="${!scheduledExecution && execution.workflow.commands.size() == 1}"/>
                        <table>
                        <g:if test="${scheduledExecution}">
                            <tr>
                                <td>
                                    <span class="label">Job:</span>
                                </td>
                                <td>
                                    <g:render template="showJobHead"
                                              model="${[scheduledExecution: scheduledExecution]}"/>
                                </td>
                            </tr>
                        </g:if>
                        <g:if test="${execution.argString}">
                            <tr>
                                <td>
                                    <span class="label">Options:</span>
                                </td>
                                <td>
                                    <span class="argString">${execution?.argString.encodeAsHTML()}</span>
                                </td>
                            </tr>
                        </g:if>
                        <g:if test="${isAdhoc}">
                        %{--<span class="label">Adhoc:</span>--}%
                            <tr>
                                <td colspan="2">
                                <g:render template="/execution/execDetailsWorkflow"
                                          model="${[workflow: execution.workflow, context: execution, project: execution.project, isAdhoc: isAdhoc]}"/>
                                </td>
                            </tr>
                        </g:if>
                        </table>

                        <div style="">
                            <g:expander key="schedExDetails${scheduledExecution?.id ? scheduledExecution?.id : ''}"
                                        imgfirst="true">Definition</g:expander>
                            <div class="presentation" style="display:none" id="schedExDetails${scheduledExecution?.id}">
                                <g:render template="execDetails"
                                          model="[execdata: execution, showArgString: false, hideAdhoc: isAdhoc]"/>
                            </div>
                        </div>
                        <g:javascript>
                        var workflow=${execution.workflow.commands*.toMap().encodeAsJSON()};
                        </g:javascript>

                    </td>

                </tr>
            </table>
        </div>
        <div class="clear"></div>
    </div>

  <g:render template="/execution/showFragment" model="[execution:execution,scheduledExecution: scheduledExecution,inlineView:false,followmode:followmode]"/>

    <g:if test="${scheduledExecution}">
        <div class="runbox">History</div>
        <div class="pageBody">
            <div id="histcontent"></div>
            <g:if test="${execution.dateCompleted!=null}">
            <g:javascript>
                fireWhenReady('histcontent',loadHistory);
            </g:javascript>
            </g:if>
        </div>
    </g:if>

    <g:javascript library="ace/ace"/>
    <g:javascript>
      fireWhenReady('executionShowPage', function (z) {
          $$('.apply_ace').each(function (t) {
              _applyAce(t);
          })
      });
        fireWhenReady('outputappendform',function(z){
            followControl.bindActions('outputappendform');
        });
    </g:javascript>

  </body>
</html>


