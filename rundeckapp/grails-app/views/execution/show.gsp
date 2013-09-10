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
      <g:javascript src="executionControl.js"/>
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
            killjobhtml: '<span class="btn btn-danger btn-sm textbtn" onclick="followControl.docancel();">Kill <g:message code="domain.ScheduledExecution.title"/> <i class="glyphicons glyphicons-remove"></i></span>',
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
    <div class="">
        <div class=" ">
            <div class="row">
                    <div class="col-sm-5">
                    <g:render template="/scheduledExecution/showExecutionHead"
                                  model="[scheduledExecution: scheduledExecution, noimgs:true, execution: execution, followparams: [mode: followmode, lastlines: params.lastlines]]"/>
                    </div>
                    <div class="col-sm-4">

                        <g:if test="${null == execution.dateCompleted}">
                            <g:if test="${authChecks[AuthConstants.ACTION_KILL]}">
                                <span id="cancelresult" style="margin-left:10px">
                                    <span class="btn btn-danger btn-sm"
                                          onclick="followControl.docancel();">Kill <g:message
                                            code="domain.ScheduledExecution.title"/>
                                        <i class="glyphicons glyphicons-remove"></i>
                                    </span>
                                </span>
                            </g:if>
                        </g:if>

                        <span id="execRetry" class="retrybuttons"
                              style="${wdgt.styleVisible(if: null != execution.dateCompleted && null != execution.failedNodeList)};">
                            <g:if test="${scheduledExecution}">
                                <g:if test="${authChecks[AuthConstants.ACTION_RUN]}">
                                    <g:link controller="scheduledExecution" action="execute"
                                            id="${scheduledExecution.extid}"
                                            params="${[retryFailedExecId: execution.id]}"
                                            title="${g.message(code: 'retry.job.failed.nodes')}"
                                            class=" btn btn-default btn-sm header"
                                    >
                                        <b class="glyphicon glyphicon-play"></b>
                                        <g:message code="retry.failed.nodes" />&hellip;
                                    </g:link>
                                </g:if>
                            </g:if>
                            <g:else>
                                <g:if test="${adhocRunAllowed}">
                                    <g:link
                                            controller="framework"
                                            action="nodes"
                                            params="${[retryFailedExecId: execution.id]}"
                                            class=" btn btn-default btn-sm header"
                                            title="${g.message(code:'retry.failed.nodes.description')}">

                                        <b class="glyphicon glyphicon-play"></b>
                                        <g:message code="retry.failed.nodes"/>&hellip;
                                    </g:link>
                                </g:if>
                            </g:else>
                        </span>
                        <span id="execRerun" class="retrybuttons" style="${wdgt.styleVisible(if: null != execution.dateCompleted)}">
                            <g:if test="${scheduledExecution}">
                                <g:if test="${authChecks[AuthConstants.ACTION_RUN]}">
                                    <g:link controller="scheduledExecution"
                                            action="execute"
                                            id="${scheduledExecution.extid}"
                                            params="${[retryExecId: execution.id]}"
                                            class=" btn btn-success btn-sm header"
                                            title="${g.message(code: 'execution.job.action.runAgain')}">
                                        <b class="glyphicon glyphicon-play"></b>
                                        <g:message code="execution.action.runAgain"/>&hellip;
                                    </g:link>
                                </g:if>
                            </g:if>
                            <g:else>
                                <g:if test="${auth.resourceAllowedTest(kind: 'job', action: [AuthConstants.ACTION_CREATE])}">
                                    <g:link
                                            controller="scheduledExecution"
                                            action="createFromExecution"
                                            params="${[executionId: execution.id]}"
                                            class=" btn btn-primary btn-sm header"
                                            title="${g.message(code:'execution.action.saveAsJob')}">
                                        <g:message code="execution.action.saveAsJob" default="Save as Job"/>&hellip;
                                    </g:link>
                                </g:if>

                                <g:if test="${adhocRunAllowed}">
                                    <g:link
                                            controller="framework"
                                            action="nodes"
                                            params="${[fromExecId: execution.id]}"
                                            class=" btn btn-success btn-sm header"
                                            title="${g.message(code: 'execution.action.runAgain')}">

                                        <b class="glyphicon glyphicon-play"></b>
                                        <g:message code="execution.action.runAgain" />&hellip;
                                    </g:link>
                                </g:if>
                            </g:else>
                        </span>

                        </div>
                        <g:if test="${eprev || enext}">
                            <div class="col-sm-3">
                                <ul class="pager pager-embed pager-right">
                                    <g:if test="${eprev}">
                                       <li>
                                           <g:link action="show" controller="execution" id="${eprev.id}"
                                                title="Previous Execution #${eprev.id}">
                                                <i class="glyphicon glyphicon-arrow-left"></i>
                                                <g:message code="${scheduledExecution ? 'job' : 'adhoc'}.previous.execution"
                                                    args="${[eprev.id]}"/>
                                            </g:link>
                                       </li>
                                    </g:if>
                                    <g:else>
                                        <li class="disabled">
                                            <span><g:message code="no.previous.executions" /></span>
                                        </li>
                                    </g:else>
                                    <g:if test="${enext}">
                                        <li>
                                            <g:link action="show" controller="execution" title="Next Execution #${enext.id}"
                                                    id="${enext.id}">
                                                <g:message code="${scheduledExecution ? 'job' : 'adhoc'}.next.execution"
                                                           args="${[enext.id]}"/>
                                                <i class="glyphicon glyphicon-arrow-right"></i>
                                            </g:link>
                                        </li>
                                    </g:if>
                                    <g:else>
                                        <li class="disabled">
                                            <span><g:message code="no.more.executions"/></span>
                                        </li>
                                    </g:else>
                                </ul>
                            </div>
                        </g:if>


                </div>
                <div class="row row-space" id="progressContainer" style="${wdgt.styleVisible(unless:execution.dateCompleted)}">
                    <div class="col-sm-12 runstatus">
                        <g:set var="progressClass" value=""/>
                        <g:set var="innerContent" value=""/>
                        <g:set var="showpercent" value="${true}"/>
                        <g:set var="progressBarClass" value="progress-bar-info"/>
                        <g:if test="${!execution.scheduledExecution}">
                            <g:set var="progressClass" value="indefinite progress-striped active indefinite"/>
                            <g:set var="innerContent" value="Running"/>
                            <g:set var="showpercent" value="${true}"/>
                        </g:if>
                        <g:render template="/common/progressBar"
                                  model="[completePercent: execution.dateCompleted ? 100 : 0,
                                          progressClass: 'rd-progress-exec '+progressClass,
                                          progressBarClass: progressBarClass,
                                          containerId: 'progressContainer2',
                                          innerContent: innerContent,
                                          showpercent: showpercent,
                                          progressId: 'progressBar']"/>
                    </div>
                </div>
                <g:set var="isAdhoc" value="${!scheduledExecution && execution.workflow.commands.size() == 1}"/>
                 <div class="row row-space">
                    <div class="col-sm-12">
                        <div class="panel panel-default">
                        <div class="panel-body">
                            <g:if test="${scheduledExecution}">
                                <div class="row">
                                    <div class="col-sm-1 control-label">
                                            Job:
                                    </div>
                                    <div class="col-sm-11">
                                        <g:render template="showJobHead"
                                                  model="${[scheduledExecution: scheduledExecution, nameOnly: true]}"/>

                                        <g:render template="showJobHead"
                                                  model="${[scheduledExecution: scheduledExecution, groupOnly: true]}"/>

                                        <span class="text-muted">
                                            ${scheduledExecution?.description?.encodeAsHTML()}
                                        </span>
                                    </div>
                                </div>
                            </g:if>
                            <g:if test="${execution.argString}">
                                <div class="row">
                                    <div class="col-sm-1 control-label">
                                        Options:
                                    </div>
                                    <div class="col-sm-11">
                                        <span class="argString">${execution?.argString.encodeAsHTML()}</span>
                                    </div>
                                </div>
                            </g:if>

                            <g:if test="${isAdhoc}">
                                <div class="row">
                                    <div class="col-sm-2 control-label">
                                        Command:
                                    </div>
                                    <div class="col-sm-10">
                                <g:render template="/execution/execDetailsWorkflow"
                                                  model="${[workflow: execution.workflow, context: execution, project: execution.project, isAdhoc: isAdhoc]}"/>

                                    </div>
                                </div>
                            </g:if>
                        </div>
                        </div>
                    </div>
                </div>

                <div class="row">
                <div class="col-sm-12">
                            <g:expander key="schedExDetails${scheduledExecution?.id ? scheduledExecution?.id : ''}"
                                        imgfirst="true">Definition</g:expander>
                            <div class="presentation" style="display:none" id="schedExDetails${scheduledExecution?.id}">
                                <g:render template="execDetails"
                                          model="[execdata: execution, showArgString: false, hideAdhoc: isAdhoc]"/>
                            </div>
                </div>
                </div>
                        <g:javascript>
                        var workflow=${execution.workflow.commands*.toMap().encodeAsJSON()};
                        </g:javascript>

    </div>

  <g:render template="/execution/showFragment" model="[execution:execution,scheduledExecution: scheduledExecution,inlineView:false,followmode:followmode]"/>

    <g:if test="${scheduledExecution}">
        <h4 class="text-muted"><g:message code="page.section.Activity"/></h4>
        <div class="pageBody">
            <table cellpadding="0" cellspacing="0" class="jobsList list history table table-hover table-condensed"
                   style="width:100%">
                <tbody id="histcontent"></tbody>
            </table>
            <g:if test="${execution.dateCompleted!=null}">
            <g:javascript>
                fireWhenReady('histcontent',loadHistory);
            </g:javascript>
            </g:if>
        </div>
    </g:if>

  <!--[if (gt IE 8)|!(IE)]><!--> <g:javascript library="ace/ace"/><!--<![endif]-->
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


