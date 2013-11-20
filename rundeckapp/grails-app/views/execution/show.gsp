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
      <g:javascript src="moment.min.js"/>
      <g:javascript src="executionControl.js"/>
      <g:javascript src="workflow.js"/>
      <g:javascript src="executionState.js"/>
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
        var workflowData=${execution.workflow.commands*.toMap().encodeAsJSON()};
        var workflow = new RDWorkflow(workflowData,{
            nodeSteppluginDescriptions:${stepPluginDescriptions.node.collectEntries { [(it.key): [title: it.value.title]] }.encodeAsJSON()},
            wfSteppluginDescriptions:${stepPluginDescriptions.workflow.collectEntries { [(it.key): [title: it.value.title]] }.encodeAsJSON()}
        });

        var followControl = new FollowControl('${execution?.id}','outputappendform',{
            parentElement:'commandPerform',
            fileloadId:'fileload',
            fileloadPctId:'fileloadpercent',
            fileloadProgressId:'fileloadprogress',
            viewoptionsCompleteId:'viewoptionscomplete',
            cmdOutputErrorId:'cmdoutputerror',
            outfileSizeId:'outfilesize',
            progressContainerId:'progressContainer',
            progressBarId:'progressBar',
            execDurationPctId:'execDurationPct',
            workflow:workflow,
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
            killjobhtml: '<span class="btn btn-danger btn-sm textbtn" onclick="followControl.docancel();">Kill <g:message code="domain.ScheduledExecution.title"/> <i class="glyphicon glyphicon-remove"></i></span>',
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
        var flowState = new FlowState('${execution?.id}','flowstate',{
            workflow:workflow,
            loadUrl: "${g.createLink(controller: 'execution', action: 'ajaxExecState', id: execution.id)}",
            outputUrl:"${g.createLink(controller: 'execution', action: 'tailExecutionOutput', id: execution.id)}.json",
            selectedOutputStatusId:'selectedoutputview',
            reloadInterval:1500
         });
         var stepState= new StepFlow(flowState,'flowstate');
         var nodeState= new NodeFlow(flowState,'nodeflowstate',{
            actions:{
                nodeexpand:function (flow,elem,data){
                    var node=elem.getAttribute('data-node');
                    var sel = '.wfnodestate[data-node='+node+'] .wfnodecollapse';
                    $(flow.targetElement).select(sel).each(Element.toggle);
                    if($(flow.targetElement).down(sel).visible()){
                        $(elem).up('.wfnodestate').addClassName('open');
                        $(elem).addClassName('auto-caret-container');
                        $(elem).addClassName('active');
                    }else{
                        $(elem).up('.wfnodestate').removeClassName('open');
                        $(elem).removeClassName('auto-caret-container');
                        $(elem).removeClassName('active');
                    }
                },
                nodeoutput:function(flow,elem,data){
                    var node=elem.getAttribute('data-node');
                    flow.showOutputForNodeStep(elem,null,node,data);
                },
                stepoutput:function(flow,elem,data){
                    var node=elem.getAttribute('data-node');
                    var stepctx=elem.getAttribute('data-stepctx');
                    var result=flow.showOutputForNodeStep(elem,stepctx,node,data);
                    if(result){
                        //showing output
                        $(elem).up('.wfnodestep').addClassName("open");
                    }else{
                        //hidden output
                        $(elem).up('.wfnodestep').removeClassName("open");
                    }
                }
            }
         });

        function init() {
            followControl.beginFollowingOutput('${execution?.id}');
//            flowState.addUpdater(stepState);
            flowState.addUpdater(nodeState);
            flowState.beginFollowing();
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

        .execstate.isnode[data-execstate=RUNNING],.execstate[data-execstate=RUNNING_HANDLER] {
            background-image: url(${g.resource(dir: 'images',file: 'icon-tiny-disclosure-waiting.gif')});
            padding-right: 16px;
            background-repeat: no-repeat;
            background-position: right 2px;
        }



        .errmsg {
            color: gray;
        }
      </style>
  </head>

<g:set var="isAdhoc" value="${!scheduledExecution && execution.workflow.commands.size() == 1}"/>
  <body id="executionShowPage">
    <div class="">
        <div class="executionshow_wrap" data-affix="wrap">
        <div class="executionshow" data-affix="top" data-affix-padding-top="21">
            <div class="row">
                    <div class="col-sm-2">
                        <span class="jobInfo" id="jobInfo_${execution.id}">
                            <span class="h3">
                                <g:render template="/scheduledExecution/showExecutionLink"
                                     model="[scheduledExecution: scheduledExecution, noimgs:true, execution: execution, followparams: [mode: followmode, lastlines: params.lastlines]]"/>
                            </span>
                        </span>
                        <g:if test="${eprev || enext}">
                            <div class="affixed-hidden">

                                <ul class="pager pager-embed pager-left pager-sm">
                                    <g:if test="${eprev}">
                                        <li>
                                            <g:link action="show" controller="execution" id="${eprev.id}"
                                                    title="Previous Execution #${eprev.id}">
                                                <i class="glyphicon glyphicon-arrow-left"></i>
                                                <g:message
                                                        code="${scheduledExecution ? 'job' : 'adhoc'}.previous.execution"
                                                        args="${[eprev.id]}"/>
                                            </g:link>
                                        </li>
                                    </g:if>
                                    <g:else>
                                        <li class="disabled">
                                            <span><g:message code="no.previous.executions"/></span>
                                        </li>
                                    </g:else>
                                    <g:if test="${enext}">
                                        <li>
                                            <g:link action="show" controller="execution"
                                                    title="Next Execution #${enext.id}"
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
                    <div class="col-sm-6">

                                <g:if test="${scheduledExecution}">
                                    <div class="row">
                                        %{--<div class="col-sm-1 control-label">--}%
                                            %{--Job:--}%
                                        %{--</div>--}%

                                        %{--<div class="col-sm-8">--}%
                                            %{--<span class="h3">--}%
                                                <g:render template="/scheduledExecution/showHead"
                                                      model="${[scheduledExecution: scheduledExecution, ]}"/>
                                            %{--</span>--}%
                                            %{--<span class="text-muted">--}%
                                                %{--${scheduledExecution?.description?.encodeAsHTML()}--}%
                                            %{--</span>--}%
                                        %{--</div>--}%
                                        %{--<div class="col-sm-4">--}%

                                            %{--<span>--}%
                                                %{--<g:render template="showJobHead"--}%
                                                          %{--model="${[scheduledExecution: scheduledExecution, groupOnly: true]}"/>--}%
                                            %{--</span>--}%
                                        %{--</div>--}%
                                    </div>
                                </g:if>
                                <g:if test="${execution.argString}">
                                    <div class="row">
                                        %{--<div class="col-sm-1 control-label">--}%
                                            %{--Options:--}%
                                        %{--</div>--}%

                                        <div class="col-sm-12">
                                            <g:render template="/execution/execArgString" model="[argString:execution.argString]"/>
                                        </div>

                                    </div>
                                </g:if>
                                <g:if test="${isAdhoc}">
                                    <div class="row">
                                        %{--<div class="col-sm-2 control-label">--}%
                                            %{--Command:--}%
                                        %{--</div>--}%

                                        <div class="col-sm-12">
                                            <g:render template="wfItemView" model="[
                                                    item:execution.workflow.commands[0],
                                                    icon:'icon-med',
                                                    iwidth:'24px',
                                                    iheight:'24px',
                                            ]"/>
                                            %{--<g:render template="/execution/execDetailsWorkflow"--}%
                                                      %{--model="${[workflow: execution.workflow, context: execution, project: execution.project, isAdhoc: isAdhoc]}"/>--}%
                                        </div>
                                    </div>
                                </g:if>
                    </div>

                            <div class="col-sm-4">
                                %{--adhoc--}%
                                <g:if test="${!scheduledExecution}">
                                <div class="btn-group pull-right">
                                    %{--save as job link--}%
                                    <g:if test="${auth.resourceAllowedTest(kind: 'job', action: [AuthConstants.ACTION_CREATE])}">
                                        <g:link
                                                controller="scheduledExecution"
                                                action="createFromExecution"
                                                params="${[executionId: execution.id]}"
                                                class=" btn btn-primary btn-sm header execRerun execRetry"
                                                title="${g.message(code: 'execution.action.saveAsJob')}"
                                                style="${wdgt.styleVisible(if: null != execution.dateCompleted)}">
                                            <g:message code="execution.action.saveAsJob"
                                                       default="Save as Job"/>&hellip;
                                        </g:link>
                                    </g:if>
                                    %{--run again links--}%
                                    <g:if test="${adhocRunAllowed}">
                                        %{--run again only--}%
                                    <g:link
                                            controller="framework"
                                            action="nodes"
                                            params="${[fromExecId: execution.id]}"
                                            title="${g.message(code: 'execution.action.runAgain')}"
                                            class="btn btn-default btn-sm force-last-child execRerun"
                                            style="${wdgt.styleVisible(if: null != execution.dateCompleted && null == execution.failedNodeList)}">

                                        <b class="glyphicon glyphicon-play"></b>
                                        <g:message code="execution.action.runAgain"/>&hellip;
                                    </g:link>
                                        %{--run again and retry failed --}%
                                    <div class="btn-group execRetry"
                                         style="${wdgt.styleVisible(if: null != execution.dateCompleted && null!=execution.failedNodeList )}">
                                        <button class="btn btn-default btn-sm dropdown-toggle force-last-child" data-target="#"
                                                data-toggle="dropdown">
                                            Run Again
                                            <i class="caret"></i>
                                        </button>
                                        <ul class="dropdown-menu pull-right" role="menu">
                                            <li >
                                                    <g:link
                                                            controller="framework"
                                                            action="nodes"
                                                            params="${[fromExecId: execution.id]}"
                                                            title="${g.message(code: 'execution.action.runAgain')}">

                                                        <b class="glyphicon glyphicon-play"></b>
                                                        <g:message code="execution.action.runAgain"/>&hellip;
                                                    </g:link>
                                            </li>
                                            <li class="divider  ">

                                            </li>
                                            <li>
                                                    <g:link
                                                            controller="framework"
                                                            action="nodes"
                                                            params="${[retryFailedExecId: execution.id]}"
                                                            title="${g.message(code: 'retry.failed.nodes.description')}">

                                                        <b class="glyphicon glyphicon-play"></b>
                                                        <g:message code="retry.failed.nodes"/>&hellip;
                                                    </g:link>
                                            </li>
                                        </ul>
                                    </div>
                                    </g:if>

                                </div>
                                </g:if>
                                <g:else>
                                %{--job--}%
                                <div class="pull-right">
                                    <g:if test="${!groupOnly && auth.jobAllowedTest(job: scheduledExecution, action: AuthConstants.ACTION_UPDATE)}">
                                        <g:link controller="scheduledExecution" title="Edit Job" action="edit"
                                                       id="${scheduledExecution.extid}"
                                                       class="btn btn-info btn-sm">
                                            <i class="glyphicon glyphicon-edit"></i>
                                            edit Job
                                        </g:link>
                                    </g:if>
                                    <g:if test="${authChecks[AuthConstants.ACTION_RUN]}">
                                        %{--Run again link--}%
                                        <g:link controller="scheduledExecution"
                                                action="execute"
                                                id="${scheduledExecution.extid}"
                                                class="btn btn-default btn-sm execRerun"
                                                params="${[retryExecId: execution.id]}"
                                                title="${g.message(code: 'execution.job.action.runAgain')}"
                                                style="${wdgt.styleVisible(if: null != execution.dateCompleted && null == execution.failedNodeList)};">
                                            <b class="glyphicon glyphicon-play"></b>
                                            <g:message code="execution.action.runAgain"/>&hellip;
                                        </g:link>
                                        %{--Run again and retry failed links in a dropdown --}%
                                        <div class="btn-group execRetry"
                                             style="${wdgt.styleVisible(if: null != execution.dateCompleted && null != execution.failedNodeList)};">
                                            <button class="btn btn-default btn-sm dropdown-toggle"
                                                    data-target="#"
                                                    data-toggle="dropdown">
                                                Run Again
                                                <i class="caret"></i>
                                            </button>
                                            <ul class="dropdown-menu pull-right" role="menu">
                                                <li class="retrybuttons execRerun">
                                                    <g:link controller="scheduledExecution"
                                                            action="execute"
                                                            id="${scheduledExecution.extid}"
                                                            params="${[retryExecId: execution.id]}"
                                                            title="${g.message(code: 'execution.job.action.runAgain')}">
                                                        <b class="glyphicon glyphicon-play"></b>
                                                        <g:message code="execution.action.runAgain"/>&hellip;
                                                    </g:link>
                                                </li>
                                                <li class="divider  execRetry">

                                                </li>
                                                <li class="retrybuttons execRetry">
                                                    <g:link controller="scheduledExecution" action="execute"
                                                            id="${scheduledExecution.extid}"
                                                            params="${[retryFailedExecId: execution.id]}"
                                                            title="${g.message(code: 'retry.job.failed.nodes')}">
                                                        <b class="glyphicon glyphicon-play"></b>
                                                        <g:message code="retry.failed.nodes"/>&hellip;
                                                    </g:link>
                                                </li>
                                            </ul>
                                        </div>

                                    </g:if>

                                </div>
                                </g:else>
                                <div class="affixed-shown pull-right">
                                    <a class="textbtn textbtn-default textbtn-on-hover btn-xs" href="#top">
                                        <g:message code="scroll.to.top" />
                                        <i class="glyphicon glyphicon-arrow-up"></i>
                                    </a>
                                </div>
                            </div>

            </div>
            <div class="row row-space" >
                    <div class="col-sm-4">
                        <span class="jobInfo" >
                                <g:render template="/scheduledExecution/showExecutionDate"
                                          model="[scheduledExecution: scheduledExecution, noimgs: true, execution: execution, followparams: [mode: followmode, lastlines: params.lastlines]]"/>
                        </span>
                        <g:if test="${null == execution.dateCompleted}">
                        <g:if test="${authChecks[AuthConstants.ACTION_KILL]}">
                            <span id="cancelresult" style="margin-left:10px">
                                <span class="btn btn-danger btn-xs"
                                      onclick="followControl.docancel();">Kill <g:message
                                        code="domain.ScheduledExecution.title"/>
                                    <i class="glyphicon glyphicon-remove"></i>
                                </span>
                            </span>
                        </g:if>
                    </g:if>
                    </div>
                    <div class="col-sm-4 " >
                        <div id="selectedoutputview" style="display: none;" class="runoutput">
                            <span class="text-info">Output:</span>
                            <span class="nodectx isnode"
                                  data-bind="nodename"
                                  ></span>

                            <span class="stepctx"><span class="subctx" data-bind="substepctx"></span><span
                                    data-bind="mainstepctx"></span>.
                            </span>
                            <span class="stepident">
                                <i class="rdicon icon-small" data-bind-class="type"></i>
                                <span data-bind="stepident"></span>
                            </span>
                        </div>
                    </div>
                    <div class="col-sm-4 runstatus" id="progressContainer"
                         style="${wdgt.styleVisible(unless: execution.dateCompleted)}">
                        <g:set var="progressClass" value=""/>
                        <g:set var="innerContent" value=""/>
                        <g:set var="showpercent" value="${true}"/>
                        <g:set var="progressBarClass" value="progress-bar-info"/>
                        <g:if test="${!execution.scheduledExecution || !execution.scheduledExecution.totalTime && !execution.scheduledExecution.execCount}">
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
             %{--<div class="row row-space">--}%
                    %{--<div class="col-sm-12"></div>--}%
                %{--</div>--}%



        </div>
        </div>
            <div class="row row-space">
                <div class="col-sm-12">

                    <ul class="nav nav-tabs">
                        <g:if test="${scheduledExecution}">
                            <li class="active"><a href="#state" data-toggle="tab">Flow</a></li>
                        </g:if>
                        <li class="${scheduledExecution ? '' : 'active'}"><a href="#output" data-toggle="tab">Log Output</a></li>
                        <li><a href="#schedExDetails${scheduledExecution?.id}" data-toggle="tab">Definition</a></li>
                    </ul>
                </div>
            </div>


    <div class="row">
        <div class="col-sm-12">
            <div class="tab-content">
                <g:if test="${scheduledExecution}">
                    <div class="tab-pane active" id="state">
                        <g:if test="${workflowState}">
                            <a href="${g.createLink(controller: 'execution', action: 'ajaxExecState', id: execution.id)}">json</a>

                            <div class="flowstate" id="nodeflowstate">
                               <g:render template="wfstateNodeModelDisplay" bean="${workflowState}" var="workflowState"/>
                            </div>
                            %{--<div class="flowstate" id="flowstate">--}%
                                %{--<g:render template="wfstateStepModelDisplay" bean="${workflowState}" var="workflowState"/>--}%
                            %{--</div>--}%

                            %{--<div class="row">--}%
                                %{--<div class="col-sm-12">--}%
                                    %{--<pre id="flowstate_output"></pre>--}%
                                %{--</div>--}%
                            %{--</div>--}%
                            %{--<div class="row row-space">--}%
                                %{--<div class="col-sm-12" id="flowstate_log">--}%

                                %{--</div>--}%
                            %{--</div>--}%

                        </g:if>
                    </div>
                </g:if>
                <div class="tab-pane ${scheduledExecution?'':'active'}" id="output">
                    <g:render template="/execution/showFragment"
                              model="[execution: execution, scheduledExecution: scheduledExecution, inlineView: false, followmode: followmode]"/>
                </div>
                <div class="tab-pane" id="schedExDetails${scheduledExecution?.id}">
                    <div class="presentation" >
                        <g:render template="execDetails"
                                  model="[execdata: execution, showArgString: false, hideAdhoc: isAdhoc]"/>
                    </div>
                </div>
            </div>
        </div>
    </div>



    <g:if test="${scheduledExecution}">
        <h4 class="text-muted" id="history_header" style="${wdgt.styleVisible(if: execution.dateCompleted != null)}">
            <g:message code="page.section.Activity"/>
        </h4>
        <div class="pageBody">
            <g:render template="/reports/historyTableContainer" model="[nowrunning: false]"/>
            <g:if test="${execution.dateCompleted!=null}">
            <g:javascript>
                fireWhenReady('histcontent', loadHistory);
                fireWhenReady('histcontent', function () {
                    $('history_header').show();
                });
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


