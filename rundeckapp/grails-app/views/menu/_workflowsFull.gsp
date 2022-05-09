%{--
  - Copyright 2016 SimplifyOps, Inc. (http://simplifyops.com)
  -
  - Licensed under the Apache License, Version 2.0 (the "License");
  - you may not use this file except in compliance with the License.
  - You may obtain a copy of the License at
  -
  -     http://www.apache.org/licenses/LICENSE-2.0
  -
  - Unless required by applicable law or agreed to in writing, software
  - distributed under the License is distributed on an "AS IS" BASIS,
  - WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
  - See the License for the specific language governing permissions and
  - limitations under the License.
  --}%

<%@ page import="rundeck.User; org.rundeck.core.auth.AuthConstants" %>
<%--
 Copyright 2010 DTO Labs, Inc. (http://dtolabs.com)

 Licensed under the Apache License, Version 2.0 (the "License");
 you may not use this file except in compliance with the License.
 You may obtain a copy of the License at

      http://www.apache.org/licenses/LICENSE-2.0

 Unless required by applicable law or agreed to in writing, software
 distributed under the License is distributed on an "AS IS" BASIS,
 WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 See the License for the specific language governing permissions and
 limitations under the License.

 --%>
<%--
    workflowsFull.gsp

    Author: Greg Schueler <a href="mailto:greg@dtosolutions.com">greg@dtosolutions.com</a>
    Created: Feb 9, 2010 11:14:07 AM
    $Id$
 --%>

<g:timerStart key="_workflowsFull.gsp"/>
<g:timerStart key="head"/>
<%-- define form display conditions --%>

<g:set var="rkey" value="${rkey?:g.rkey()}"/>

<g:set var="authProjectSCMAdmin" value="${auth.resourceAllowedTest(
        context: AuthConstants.CTX_APPLICATION,
        type: AuthConstants.TYPE_PROJECT,
        action: [AuthConstants.ACTION_CONFIGURE, AuthConstants.ACTION_ADMIN, AuthConstants.ACTION_APP_ADMIN],
        any: true,
        name: params.project
)}"/>
<g:set var="status" value="${(hasConfiguredScmPluginsEnabled)?'off':'on'}"/>


<div id="wffilterform">
  <g:render template="/common/messages"/>
  <g:set var="wasfiltered" value="${paginateParams?.keySet().grep(~/(?!proj).*Filter|groupPath|idlist$/)}"/>
  <g:set var="filtersOpen" value="${params.createFilters||params.editFilters||params.saveFilter?true:false}"/>
  <div>
    <div>
    <!-- filter -->
      <div class="modal" id="jobs_filters" tabindex="-1" role="dialog" aria-labelledby="jobs_filters_title" aria-hidden="true">
        <div class="modal-dialog modal-lg">
          <g:form action="jobs" params="[project:params.project, jobListType:params.jobListType]" method="POST" class="form form-horizontal" useToken="true">
            <div class="modal-content">
              <div class="modal-header">
                <button type="button" class="close" data-dismiss="modal" aria-hidden="true">&times;</button>
                <h4 class="modal-title" id="jobs_filters_title"><g:message code="filter.jobs" /></h4>
              </div>
              <div class="modal-body" id="jobs_filters_content">
                <g:if test="${params.compact}">
                  <g:hiddenField name="compact" value="${params.compact}"/>
                </g:if>
                <g:hiddenField name="project" value="${params.project}"/>
                <g:hiddenField name="max" value="-1"/>
                <g:hiddenField name="offset" value="0"/>
                <g:if test="${params.idlist}">
                  <div class="form-group">
                    <label class="col-sm-2 control-label" for="${enc(attr:rkey)}idlist"><g:message code="jobquery.title.idlist"/></label>
                      <div class="col-sm-10">
                        <g:textField name="idlist" id="${rkey}idlist" value="${params.idlist}" class="form-control" />
                      </div>
                    </div>
                  </g:if>
                  <div class="form-group">
                    <label class="col-sm-2 control-label" for="${enc(attr:rkey)}jobFilter"><g:message code="jobquery.title.jobFilter"/></label>
                    <div class="col-sm-10">
                      <g:textField name="jobFilter" id="${rkey}jobFilter" value="${params.jobFilter}" class="form-control" />
                    </div>
                  </div>
                  <div class="form-group">
                    <label class="col-sm-2 control-label" for="${enc(attr:rkey)}groupPath"><g:message code="jobquery.title.groupPath"/></label>
                    <div class="col-sm-10">
                      <div class="input-group">
                        <span class="input-group-addon"><i class="glyphicon glyphicon-folder-open"></i></span>
                        <g:textField name="groupPath" id="${rkey}groupPath" value="${params.groupPath}" class="form-control"/>
                      </div>
                    </div>
                  </div>
                  <div class="form-group">
                    <label class="col-sm-2 control-label" for="${enc(attr:rkey)}descFilter"><g:message code="jobquery.title.descFilter"/></label>
                    <div class="col-sm-10">
                      <g:textField name="descFilter" id="${rkey}descFilter" value="${params.descFilter}" class="form-control"/>
                    </div>
                  </div>
                  <div class="form-group">
                    <label class="col-sm-2 control-label" for="${enc(attr:rkey)}scheduledFilter"><g:message code="jobquery.title.scheduledFilter"/></label>
                      <div class="col-sm-10">
                      <label class="radio-inline">
                        <g:radio name="scheduledFilter" id="${rkey}scheduledFilter" value="true" checked="${params.scheduledFilter==true}"/>
                        <g:message code="yes" />
                      </label>
                      <label class="radio-inline">
                        <g:radio name="scheduledFilter" id="${rkey}scheduledFilter" value="false" checked="${params.scheduledFilter == false}"/>
                        <g:message code="no" />
                      </label>
                      <label class="radio-inline">
                        <g:radio name="scheduledFilter" id="${rkey}scheduledFilter" value="" checked="${params.scheduledFilter == null}"/>
                        <g:message code="all"/>
                      </label>
                    </div>
                  </div>
                  <g:if test="${clusterModeEnabled}">
                    <div class="form-group">
                      <label class="col-sm-2 control-label" for="${enc(attr:rkey)}serverNodeUUIDFilter"><g:message code="jobquery.title.serverNodeUUIDFilter"/></label>
                      <div class="col-sm-10">
                        <g:textField name="serverNodeUUIDFilter" id="${rkey}serverUuid" value="${params.serverNodeUUIDFilter}" class="form-control"/>
                      </div>
                    </div>
                  </g:if>
                  <g:if test="${jobQueryComponents}">
                    <g:each in="${jobQueryComponents}" var="component">
                      <g:if test="${component.value.queryProperties}">
                        <g:each in="${component.value.queryProperties}" var="properties">
                          <g:render template="/framework/pluginConfigPropertiesInputs"
                            model="${[
                              properties         : properties,
                              report             : null,
                              prefix             : '',
                              values             : params,
                              fieldnamePrefix    : '',
                              origfieldnamePrefix: 'orig.' ,
                              messagePrefix       :'',
                              messagesType       : 'job.query'
                            ]}"/>
                        </g:each>
                      </g:if>
                    </g:each>
                  </g:if>
              </div>
              <div class="modal-footer" id="jobs_filters_footer">
                <button type="button" class="btn btn-default" data-dismiss="modal">
                  <g:message code="cancel"/>
                </button>
                <g:actionSubmit value="${message(code:'job.filter.apply.button.title')}" controller='menu' action='jobs' class="btn btn-primary "/>
                <a
                  class="btn btn-success pull-right"
                  data-dismiss="modal"
                  data-toggle="modal"
                  href="#saveJobFilterKOModal"
                  title="${message(code:"job.filter.save.button.title")}" >
                    <i class="glyphicon glyphicon-plus"></i> <g:message code="job.filter.save.button" />
                </a>
              </div>
            </div>
          </g:form>
        </div>
      </div>
      <div style="text-align:left;vertical-align:top;" id="${enc(attr:rkey)}wfcontent" class="wfcontent">
        <div class="jobscontent head" style="margin-bottom:1em;">
          <g:if test="${!params.compact}">
            <div class=" pull-right" id="jobpageactionbuttons">
              <span style="display: none;" data-bind="visible: displaySCMMEssage()" id="scm_message" data-ko-bind="bulkeditor" class="" data-placement="left" data-toggle="popover" data-popover-content-ref="#scmStatusPopoverOK" data-trigger="hover" title="" data-original-title="Project Import/Export Status">
                <span class="text-info">
                  <i class="glyphicon glyphicon-exclamation-sign "></i>
                  <!--ko text: defaultDisplayText()--><!--/ko-->
                </span>
              </span>
              <div id="scmStatusPopoverOK" style="display: none;" data-ko-bind="bulkeditor">
                <!-- ko if: displayExport() -->
                <dl>
                  <dt><g:message code="scm.export.title"/></dt>
                  <dd>
                    <!--ko text: exportMessage() --><!--/ko-->
                  </dd>
                </dl>
                <!-- /ko -->
                <!-- ko if: displayImport() -->
                <dl>
                  <dt><g:message code="scm.import.title"/></dt>
                  <dd>
                    <!--ko text: importMessage() --><!--/ko-->
                  </dd>
                </dl>
                  <!-- /ko -->
              </div>
              <div class="flex flex-justify--end">
                <div class="btn-group">
                  <button type="button" class="btn btn-default dropdown-toggle mr-2" data-toggle="dropdown">
                    <g:message code="job.actions" />
                    <span class="caret"></span>
                  </button>
                  <ul class="dropdown-menu pull-right" role="menu" id="job_action_menu" data-ko-bind="bulkeditor">
                    <auth:resourceAllowed kind="${AuthConstants.TYPE_JOB}" action="${AuthConstants.ACTION_CREATE}" project="${params.project ?: request.project}">
                      
                      <li>
                        <g:link controller="scheduledExecution" action="upload" params="[project: params.project ?: request.project]" class="">
                          <i class="glyphicon glyphicon-upload"></i>
                          <g:message code="upload.definition.button.label" />
                        </g:link>
                      </li>
                      <li class="divider"></li>
                    </auth:resourceAllowed>
                    <li>
                      <a href="#" data-bind="click: beginEdit">
                        <g:message code="job.bulk.activate.menu.label" />
                      </a>
                    </li>
                    <li data-bind="visible: isLoadingSCMActions()" id="scm_export_actions_loading" role="presentation" class="dropdown-header">
                      <g:message code="loading" />
                    </li>
                    <!-- ko if: scmExportEnabled() -->
                    <li data-bind="visible: scmExportActions()" class="divider"></li>
                    <li data-bind="visible: scmExportActions()" role="presentation" class="dropdown-header">
                      <g:icon name="circle-arrow-right"/>
                      <g:message code="scm.export.actions.title" />
                    </li>
                    <!-- ko foreach: scmExportActions() -->
                    <li>
                      <a href="${g.createLink(action:'performAction',controller:'scm',params:[project:params.project, integration:'export', actionId:'<$>'])}"
                          data-bind="urlPathParam: $data.id, text: $data.title, attr: { title: $data.description}">
                      </a>
                    </li>
                    <!-- /ko -->
                    <!-- /ko -->
                    <!-- ko if: scmImportEnabled() -->
                    <li data-bind="visible: scmImportActions()" class="divider"></li>
                    <li data-bind="visible: scmImportActions()" role="presentation" class="dropdown-header">
                      <g:icon name="circle-arrow-left"/>
                      <g:message code="scm.import.actions.title" />
                    </li>
                    <!-- ko foreach: scmImportActions() -->
                    <li>
                      <a href="${g.createLink(action:'performAction',controller:'scm',params:[project:params.project, integration:'import', actionId:'<$>'])}"
                        data-bind="urlPathParam: $data.id, text: $data.title, attr: { title: $data.description}">
                      </a>
                    </li>
                    <!-- /ko -->
                    <!-- /ko -->
                    <g:if test="${authProjectSCMAdmin && hasConfiguredScmPlugins}">
                    <li class="divider"></li>
                    <li>
                      <a id="toggle_btn"
                          data-toggle="modal"
                          href="#toggle_confirm"
                          class="">${g.message(code:'job.toggle.scm.menu.'+status)}</a>
                    </li>
                    </g:if>
                  </ul>
          
                </div>
           
                 <g:link controller="scheduledExecution" action="create" params="[project: params.project ?: request.project]" class="btn btn-primary">
                    <i class="glyphicon glyphicon-plus"></i>
                    <g:message code="new.job.button.label" />
                  </g:link>
              </div>
            </div>
          </g:if>
          <span id="group_controls" data-ko-bind="bulkeditor">
            <span class="btn btn-secondary btn-simple btn-hover btn-xs" data-bind="click: expandAllComponents">
              <g:message code="expand.all" />
            </span>
            <span class="btn btn-secondary btn-simple btn-hover btn-xs" data-bind="click: collapseAllComponents">
              <g:message code="collapse.all" />
            </span>
          </span>
        </div>

        <g:if test="${flash.savedJob}">
          <div class="newjob">
            <span class="popout message note" style="background:white">
                <g:enc>${flash.savedJobMessage?:message(code:"job.save.completed.message")}</g:enc>:
                <g:link controller="scheduledExecution" action="show" id="${flash.savedJob.id}"
                        params="[project: params.project ?: request.project]"><g:enc>${flash.savedJob.generateFullName()}</g:enc></g:link>
            </span>
          </div>
          <g:javascript>
            fireWhenReady('jobrow_${enc(js:flash.savedJob.id)}',doyft.curry('jobrow_${enc(js:flash.savedJob.id)}'));
          </g:javascript>
        </g:if>

        <span id="busy" style="display:none"></span>
          <g:timerEnd key="head"/>
            <g:if test="${authProjectSCMAdmin && hasConfiguredScmPlugins}">
              <g:form name="toggleScmForm" controller="menu" params="[project: params.project ?: request.project]">
                <div class="modal fade" id="toggle_confirm" tabindex="-1" role="dialog" aria-hidden="true">
                  <div class="modal-dialog">
                    <div class="modal-content">
                      <div class="modal-header">
                        <button type="button" class="close" data-dismiss="modal"
                          aria-hidden="true">&times;
                        </button>
                        <h4 class="modal-title"><g:message code="job.toggle.scm.confirm.panel.title" /></h4>
                      </div>
                      <div class="modal-body">
                        <p><g:message code="job.toggle.scm.confirm.${status}"/></p>
                      </div>
                      <div class="modal-footer">
                        <button type="button"
                          class="btn btn-default"
                          data-bind="click: cancel"
                          data-dismiss="modal" ><g:message code="no"/>
                        </button>
                        <g:actionSubmit controller="menu"
                          action="projectToggleSCM"
                          form="toggleScmForm"
                          value="${message(code:'job.toggle.scm.button.label.'+status)}"
                          class="btn btn-danger"
                        />
                      </div>
                    </div><!-- /.modal-content -->
                  </div><!-- /.modal-dialog -->
                </div><!-- /.modal -->
              </g:form>
            </g:if>
            <g:form name="jobsListForm" controller="scheduledExecution"  useToken="true" params="[project: params.project ?: request.project]">
              <div class="modal fade" id="bulk_del_confirm" tabindex="-1" role="dialog" aria-hidden="true" data-ko-bind="bulkeditor">
                <div class="modal-dialog">
                  <div class="modal-content">
                    <div class="modal-header">
                      <button type="button" class="close" data-dismiss="modal"
                        aria-hidden="true">&times;
                      </button>
                      <h4 class="modal-title"><g:message code="job.bulk.modify.confirm.panel.title" /></h4>
                    </div>
                    <div class="modal-body">
                      <p data-bind="if: isDelete"><g:message code="really.delete.these.jobs"/></p>
                      <p data-bind="if: isDisableSchedule"><g:message code="job.bulk.disable.schedule.confirm.message" /></p>
                      <p data-bind="if: isEnableSchedule"><g:message code="job.bulk.enable.schedule.confirm" /></p>
                      <p data-bind="if: isDisableExecution"><g:message code="job.bulk.disable.execution.confirm" /></p>
                      <p data-bind="if: isEnableExecution"><g:message code="job.bulk.enable.execution.confirm" /></p>
                    </div>
                    <div class="modal-footer">
                      <button type="button"
                        class="btn btn-default"
                        data-bind="click: cancel"
                        data-dismiss="modal" ><g:message code="no"/>
                      </button>
                      <span data-bind="if: isDisableSchedule">
                        <g:actionSubmit action="flipScheduleDisabledBulk"
                          form="jobsListForm"
                          value="${message(code:'job.bulk.disable.schedule.button')}"
                          class="btn btn-danger"/>
                      </span>
                      <span data-bind="if: isEnableSchedule">
                        <g:actionSubmit action="flipScheduleEnabledBulk"
                          form="jobsListForm"
                          value="${message(code:'job.bulk.enable.schedule.button')}"
                          class="btn btn-danger"/>
                        </span>
                        <span data-bind="if: isDisableExecution">
                          <g:actionSubmit action="flipExecutionDisabledBulk"
                            form="jobsListForm"
                            value="${message(code:'scheduledExecution.action.disable.execution.button.label')}"
                            class="btn btn-danger"/>
                        </span>
                        <span data-bind="if: isEnableExecution">
                          <g:actionSubmit action="flipExecutionEnabledBulk"
                            form="jobsListForm"
                            value="${message(code:'scheduledExecution.action.enable.execution.button.label')}"
                            class="btn btn-danger"/>
                        </span>
                        <auth:resourceAllowed kind="${AuthConstants.TYPE_JOB}" action="${AuthConstants.ACTION_DELETE  }"
                          project="${params.project ?: request.project}">
                          <span data-bind="if: isDelete">
                            <g:actionSubmit action="deleteBulk"
                              form="jobsListForm"
                              value="${message(code:'job.bulk.delete.button')}" class="btn btn-danger"/>
                          </span>
                        </auth:resourceAllowed>
                      </div>
                    </div><!-- /.modal-content -->
                  </div><!-- /.modal-dialog -->
                </div><!-- /.modal -->

                <div class="floatr" style="margin-top: 10px; display: none;" id="bulk_edit_panel" data-bind="visible: enabled" data-ko-bind="bulkeditor">
                  <div class="bulk_edit_controls panel panel-warning"  >
                    <div class="panel-heading">
                      <button type="button" class="close "
                        data-bind="click: cancelEdit"
                        aria-hidden="true">&times;
                      </button>
                      <h3 class="panel-title">
                        <g:message code="job.bulk.panel.select.title" />
                      </h3>
                    </div>
                    <div class="panel-body">
                    <span class="btn btn-simple btn-xs btn-hover" data-bind="click: selectAll">
                      <g:icon name="check"/>
                      <g:message code="select.all" />
                    </span>
                    <span class="btn btn-simple btn-xs btn-hover " data-bind="click: selectNone" >
                      <g:icon name="unchecked"/>
                      <g:message code="select.none" />
                    </span>
                  </div>

                  <div class="panel-footer">
                    <div class="btn-group">
                      <button type="button" class="btn btn-default btn-sm dropdown-toggle" data-toggle="dropdown">
                        <g:message code="job.bulk.perform.action.menu.label" />
                        <span class="caret"></span>
                      </button>
                      <ul class="dropdown-menu " role="menu">
                        <auth:resourceAllowed kind="${AuthConstants.TYPE_JOB}" action="${AuthConstants.ACTION_DELETE  }"
                          project="${params.project ?: request.project}">
                          <li>
                            <a id="bulk_del_prompt"
                              data-toggle="modal"
                              href="#bulk_del_confirm"
                              data-bind="click: actionDelete">
                              <g:message code="delete.selected.jobs" />
                            </a>
                          </li>
                          <li class="divider"></li>
                        </auth:resourceAllowed>
                        <li>
                          <a
                            data-toggle="modal"
                            href="#bulk_del_confirm"
                            data-bind="click: enableSchedule"
                            class="" >
                            <g:message code="scheduledExecution.action.enable.schedule.button.label"/>
                          </a>
                        </li>
                        <li>
                          <a
                            data-toggle="modal"
                            href="#bulk_del_confirm"
                            data-bind="click: disableSchedule"
                            class="" >
                              <g:message code="scheduledExecution.action.disable.schedule.button.label"/>
                          </a>
                        </li>
                        <li class="divider"></li>
                        <li>
                          <a
                            data-toggle="modal"
                            href="#bulk_del_confirm"
                            data-bind="click: enableExecution" class="" >
                            <g:message code="scheduledExecution.action.enable.execution.button.label"/>
                          </a>
                        </li>
                        <li>
                          <a
                            data-toggle="modal"
                            href="#bulk_del_confirm"
                            data-bind="click: disableExecution" 
                            class="" >
                            <g:message code="scheduledExecution.action.disable.execution.button.label"/>
                          </a>
                        </li>
                        %{--<li class="divider"></li>--}%
                      </ul>
                    </div>
                  </div>
                </div>
              </div>
              <div id="job_group_tree" data-ko-bind="bulkeditor">
                <g:if test="${jobgroups}">
                  <g:timerStart key="groupTree"/>
                  <g:set var="projectExecutionModeActive" value="${g.executionMode(active:true,project:params.project ?: request.project)}"/>
                  <g:set var="projectScheduleModeActive" value="${g.scheduleMode(active:true,project:params.project ?: request.project)}"/>
                  <g:render template="groupTree" model="${[projectScheduleModeActive:projectScheduleModeActive,projectExecutionModeActive:projectExecutionModeActive,jobExpandLevel:jobExpandLevel,small:params.compact?true:false,currentJobs:jobgroups['']?jobgroups['']:[],wasfiltered:wasfiltered?true:false, clusterMap: clusterMap,nextExecutions:nextExecutions,jobauthorizations:jobauthorizations,authMap:authMap,max:max,offset:offset,paginateParams:paginateParams,sortEnabled:true]}"/>
                  <g:timerEnd key="groupTree"/>
                </g:if>
              </div>
            </g:form>
            <g:if test="${!jobgroups}">
              <div class="presentation">
                <auth:resourceAllowed kind="${AuthConstants.TYPE_JOB}" action="${AuthConstants.ACTION_CREATE}" project="${params.project ?: request.project}">
                  <g:link controller="scheduledExecution" action="create"
                    params="[project: params.project ?: request.project]"
                    class="btn btn-cta">
                      <g:message code="job.create.button" />
                  </g:link>
                  <g:link controller="scheduledExecution" action="upload"
                    params="[project: params.project ?: request.project]"
                    class="btn btn-default">
                    <g:message code="job.upload.button.title" />
                  </g:link>
                </auth:resourceAllowed>
              </div>
            </g:if>
            <g:timerStart key="tail"/>
          </div>
        </div>
      </div>
  </div>
<g:timerEnd key="tail"/>
<g:timerEnd key="_workflowsFull.gsp"/>
