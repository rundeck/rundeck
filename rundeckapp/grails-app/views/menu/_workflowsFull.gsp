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

<%@ page import="rundeck.User; com.dtolabs.rundeck.server.authorization.AuthConstants" %>
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

<g:if test="${session.user && User.findByLogin(session.user)?.jobfilters}">
    <g:set var="filterset" value="${User.findByLogin(session.user)?.jobfilters}"/>
</g:if>

<div id="wffilterform">
    <g:render template="/common/messages"/>
    <g:set var="wasfiltered" value="${paginateParams?.keySet().grep(~/(?!proj).*Filter|groupPath|idlist$/)}"/>
    <g:if test="${params.createFilters}">
        <span class="note help">
            <g:message code="job.filter.create.help" />
        </span>
    </g:if>
    <g:set var="filtersOpen" value="${params.createFilters||params.editFilters||params.saveFilter?true:false}"/>
    <table cellspacing="0" cellpadding="0" width="100%">
        <tr>

            <td style="text-align:left;vertical-align:top;width:200px; ${wdgt.styleVisible(if:filtersOpen)}" id="${enc(attr:rkey)}filter" class="wffilter" >

            <g:form action="jobs" params="[project:params.project]" method="POST" class="form" useToken="true">
                <g:if test="${params.compact}">
                    <g:hiddenField name="compact" value="${params.compact}"/>
                </g:if>
                <g:hiddenField name="project" value="${params.project}"/>
                <span class="textbtn textbtn-default obs_filtertoggle">
                    <g:message code="filter.title" />
                    <b class="glyphicon glyphicon-chevron-down"></b>
                </span>
                <g:if test="${!filterName}">
                    <a class="btn btn-xs pull-right btn-success"
                          data-toggle="modal"
                          href="#saveFilterModal" title="${message(code:"job.filter.save.button.title")}">
                        <i class="glyphicon glyphicon-plus"></i> <g:message code="job.filter.save.button" />
                    </a>
                </g:if>
                <g:else >
                    <div class="filterdef saved clear">
                                    <span class="prompt"><g:enc>${filterName}</g:enc></span>
                    <a class="btn btn-xs btn-link btn-danger pull-right" data-toggle="modal"
                          href="#deleteFilterModal" title="${message(code:"job.filter.delete.button.title")}">
                        <b class="glyphicon glyphicon-remove"></b>
                        <g:message code="job.filter.delete.button" />
                    </a>
                    </div>
                </g:else>
                <g:render template="/common/queryFilterManagerModal" model="${[rkey:rkey,filterName:filterName,
                        filterset:filterset,update:'wffilterform',
                        deleteActionSubmit:'deleteJobfilter',
                        storeActionSubmit:'storeJobfilter']}"/>

                <div class="filter">

                            <g:hiddenField name="max" value="${max}"/>
                            <g:hiddenField name="offset" value="${offset}"/>
                            <g:if test="${params.idlist}">
                                <div class="form-group">
                                    <label for="${enc(attr:rkey)}idlist"><g:message code="jobquery.title.idlist"/></label>:
                                    <g:textField name="idlist" id="${rkey}idlist" value="${params.idlist}"
                                                 class="form-control" />
                                </div>
                            </g:if>
                            <div class="form-group">
                                <label for="${enc(attr:rkey)}jobFilter"><g:message code="jobquery.title.jobFilter"/></label>:
                                <g:textField name="jobFilter" id="${rkey}jobFilter" value="${params.jobFilter}"
                                             class="form-control" />
                            </div>

                            <div class="form-group">
                                <label for="${enc(attr:rkey)}groupPath"><g:message code="jobquery.title.groupPath"/></label>:
                                <g:textField name="groupPath" id="${rkey}groupPath" value="${params.groupPath}"
                                             class="form-control"/>
                            </div>

                            <div class="form-group">
                                <label for="${enc(attr:rkey)}descFilter"><g:message code="jobquery.title.descFilter"/></label>:
                                <g:textField name="descFilter" id="${rkey}descFilter" value="${params.descFilter}"
                                             class="form-control"/>
                            </div>


                            <div class="form-group">
                                    <g:actionSubmit value="${message(code:'job.filter.apply.button.title')}" name="filterAll" controller='menu' action='jobs' class="btn btn-primary btn-sm"/>
                                    <g:actionSubmit value="${message(code:'job.filter.clear.button.title')}" name="clearFilter" controller='menu' action='clearJobsFilter' class="btn btn-default btn-sm"/>
                            </div>
                </div>
            </g:form>

            </td>
            <td style="text-align:left;vertical-align:top;" id="${enc(attr:rkey)}wfcontent" class="wfcontent">

                <div class="jobscontent head">
    <g:if test="${!params.compact}">
        <div class=" pull-right" id="jobpageactionbuttons">

            <g:if test="${scmExportEnabled && scmExportStatus || scmImportEnabled  && scmImportStatus}">
            %{--SCM synch status--}%
                <g:set var="projectExportStatus" value="${scmExportEnabled ?scmExportStatus :null}"/>
                <g:set var="projectImportStatus" value="${scmImportEnabled ?scmImportStatus :null}"/>
                <g:render template="/scm/scmExportStatus" model="[
                        exportStatus:projectExportStatus?.state,
                        importStatus:projectImportStatus?.state,
                        text:'',
                        exportMessage:projectExportStatus?.message?:'',
                        importMessage:projectImportStatus?.message?:'',
                        meta:[:]
                ]"/>
            </g:if>


            <div class="btn-group">
            <button type="button" class="btn btn-default btn-sm dropdown-toggle" data-toggle="dropdown">
                <g:message code="job.actions" />
                <span class="caret"></span>
            </button>
            <ul class="dropdown-menu pull-right" role="menu" id="job_action_menu">

        <auth:resourceAllowed kind="job" action="${AuthConstants.ACTION_CREATE}" project="${params.project ?: request.project}">
                <li><g:link controller="scheduledExecution" action="create"
                    params="[project: params.project ?: request.project]"
                            class="">
                    <i class="glyphicon glyphicon-plus"></i>
                    <g:message code="new.job.button.label" />
                </g:link></li>
                <li class="divider">
                </li>
                <li>
                    <g:link controller="scheduledExecution" action="upload"
                            params="[project: params.project ?: request.project]"
                            class="">
                        <i class="glyphicon glyphicon-upload"></i>
                        <g:message code="upload.definition.button.label" />
                    </g:link>
                </li>
                <li class="divider"></li>
        </auth:resourceAllowed>
                <li>
                    <a href="#"
                        data-bind="click: beginEdit"
                    >
                        <g:message code="job.bulk.activate.menu.label" />
                    </a>
                </li>
            <g:if test="${(scmExportEnabled && scmExportActions) || (scmImportEnabled && scmImportActions)}">
                <g:if test="${scmExportEnabled && scmExportActions}">
                    <li class="divider">
                    </li>

                    <li role="presentation" class="dropdown-header">
                        <g:icon name="circle-arrow-right"/>
                        <g:message code="scm.export.actions.title" />
                    </li>
                    <g:each in="${scmExportActions}" var="action">
                        <g:if test="${action.id == '-'}">
                            <li class="divider"></li>
                        </g:if>
                        <g:else>
                            <li>
                                <g:render template="/scm/actionLink"
                                    model="[action:action,integration:'export',project:params.project]"
                                />

                            </li>
                        </g:else>
                    </g:each>

                </g:if>
                <g:if test="${scmImportEnabled && scmImportActions}">

                    <li class="divider"></li>
                    <li role="presentation" class="dropdown-header">
                        <g:icon name="circle-arrow-left"/>
                        <g:message code="scm.import.actions.title" />
                    </li>
                    <g:each in="${scmImportActions}" var="action">
                        <g:if test="${action.id == '-'}">
                            <li class="divider"></li>
                        </g:if>
                        <g:else>
                            <li>

                                <g:render template="/scm/actionLink"
                                          model="[action:action,integration:'import',project:params.project]"
                                />

                            </li>
                        </g:else>
                    </g:each>

                </g:if>
                </g:if>
            </ul>
            </div>
        </div>
    </g:if>

                <g:if test="${wasfiltered}">
                    <div>
                    <g:if test="${!params.compact}">
                        <span class="h4"><g:enc>${totalauthorized}</g:enc>
                        <g:message code="jobs.matching.filter" />
                    </g:if>

                    <g:if test="${filterset}">
                        <g:render template="/common/selectFilter" model="[noSelection:'-All-',filterset:filterset,filterName:filterName,prefName:'workflows']"/>
                        <!--<span class="info note">Filter:</span>-->
                    </g:if>
                    </div>

                            <span title="Click to modify filter" class="info textbtn textbtn-default query obs_filtertoggle"  id='${rkey}filter-toggle'>
                                <g:each in="${wasfiltered.sort()}" var="qparam">
                                    <span class="querykey"><g:message code="jobquery.title.${qparam}"/></span>:

                                    <g:if test="${paginateParams[qparam] instanceof java.util.Date}">
                                        <span class="queryvalue date" title="${enc(attr:paginateParams[qparam].toString())}">
                                            <g:relativeDate atDate="${paginateParams[qparam]}"/>
                                        </span>
                                    </g:if>
                                    <g:else>
                                        <span class="queryvalue text">
                                            ${g.message(code:'jobquery.title.'+qparam+'.label.'+paginateParams[qparam].toString(),default:enc(html:paginateParams[qparam].toString()).toString())}
                                        </span>
                                    </g:else>

                                </g:each>

                                <b class="glyphicon glyphicon-chevron-right"></b>
                            </span>
                </g:if>
                <g:else>
                    <g:if test="${!params.compact}">
                    <span class="h4">
                        <g:message code="Job.plural" /> (<g:enc>${totalauthorized}</g:enc>)
                    </span>
                    </g:if>

                    <span class="textbtn textbtn-default obs_filtertoggle"  id="${enc(attr:rkey)}filter-toggle">
                        <g:message code="filter.title" />
                        <b class="glyphicon glyphicon-chevron-${wasfiltered?'down':'right'}"></b>
                    </span>
                    <g:if test="${filterset}">
                        <span style="margin-left:10px;">
                            <span class="info note"><g:message code="choose.a.filter" /></span>
                            <g:render template="/common/selectFilter" model="[noSelection:'-All-',filterset:filterset,filterName:filterName,prefName:'workflows']"/>
                        </span>
                    </g:if>
                </g:else>
                    <span id="group_controls">
                    <span class="textbtn textbtn-default" data-bind="click: expandAllComponents">
                        <g:message code="expand.all" />
                    </span>
                    <span class="textbtn textbtn-default" data-bind="click: collapseAllComponents">
                        <g:message code="collapse.all" />
                    </span>
                    </span>
                    <div class="clear"></div>
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
                    <g:form controller="scheduledExecution"  useToken="true" params="[project: params.project ?: request.project]">
                        <div class="modal fade" id="bulk_del_confirm" tabindex="-1" role="dialog" aria-hidden="true">
                            <div class="modal-dialog">
                                <div class="modal-content">
                                    <div class="modal-header">
                                        <button type="button" class="close" data-dismiss="modal"
                                                aria-hidden="true">&times;</button>
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
                                                data-dismiss="modal" ><g:message code="no"/></button>

                                        <span data-bind="if: isDisableSchedule">
                                            <g:actionSubmit action="flipScheduleDisabledBulk"
                                                            value="${message(code:'job.bulk.disable.schedule.button')}"
                                                            class="btn btn-danger"/>
                                        </span>

                                        <span data-bind="if: isEnableSchedule">
                                            <g:actionSubmit action="flipScheduleEnabledBulk"
                                                            value="${message(code:'job.bulk.enable.schedule.button')}"
                                                            class="btn btn-danger"/>
                                        </span>
                                        <span data-bind="if: isDisableExecution">
                                            <g:actionSubmit action="flipExecutionDisabledBulk"
                                                            value="${message(code:'scheduledExecution.action.disable.execution.button.label')}"
                                                            class="btn btn-danger"/>
                                        </span>
                                        <span data-bind="if: isEnableExecution">
                                            <g:actionSubmit action="flipExecutionEnabledBulk"
                                                            value="${message(code:'scheduledExecution.action.enable.execution.button.label')}"
                                                            class="btn btn-danger"/>
                                        </span>


                                        <auth:resourceAllowed kind="job" action="${AuthConstants.ACTION_DELETE  }"
                                                              project="${params.project ?: request.project}">
                                        <span data-bind="if: isDelete">
                                            <g:actionSubmit action="deleteBulk"
                                                            value="${message(code:'job.bulk.delete.button')}" class="btn btn-danger"/>
                                        </span>
                                        </auth:resourceAllowed>
                                    </div>
                                </div><!-- /.modal-content -->
                            </div><!-- /.modal-dialog -->
                        </div><!-- /.modal -->

                    <div class="floatr" style="margin-top: 10px; display: none;" id="bulk_edit_panel" data-bind="visible: enabled" >
                        <div class="bulk_edit_controls panel panel-warning"  >
                            <div class="panel-heading">
                                <button type="button" class="close "
                                        data-bind="click: cancelEdit"
                                        aria-hidden="true">&times;</button>
                                <h3 class="panel-title">
                                    <g:message code="job.bulk.panel.select.title" />
                                </h3>
                            </div>
                            <div class="panel-body">
                                <span class="btn btn-default btn-xs " data-bind="click: selectAll">
                                    <g:icon name="check"/>
                                    <g:message code="select.all" />
                                </span>
                                <span class="btn btn-default btn-xs " data-bind="click: selectNone" >
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
                                        <auth:resourceAllowed kind="job" action="${AuthConstants.ACTION_DELETE  }"
                                                              project="${params.project ?: request.project}">


                                        <li>
                                            <a id="bulk_del_prompt"
                                               data-toggle="modal"
                                               href="#bulk_del_confirm"
                                               data-bind="click: actionDelete"
                                               class="" ><g:message code="delete.selected.jobs" /></a>
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
                                               data-bind="click: enableExecution"
                                               class="" >
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
                        <div id="job_group_tree">
                        <g:if test="${jobgroups}">

                            <g:timerStart key="groupTree"/>

                    <g:render template="groupTree" model="${[small:params.compact?true:false,currentJobs:jobgroups['']?jobgroups['']:[],wasfiltered:wasfiltered?true:false, clusterMap: clusterMap,nextExecutions:nextExecutions,jobauthorizations:jobauthorizations,authMap:authMap,max:max,offset:offset,paginateParams:paginateParams,sortEnabled:true]}"/>


                            <g:timerEnd key="groupTree"/>
                        </g:if>
                        </div>
                    </g:form>

                <g:if test="${!jobgroups}">
                    <div class="presentation">

                        <auth:resourceAllowed kind="job" action="${AuthConstants.ACTION_CREATE}" project="${params.project ?: request.project}">
                            <ul>
                            <li style="padding:5px"><g:link controller="scheduledExecution" action="create"
                                                            params="[project: params.project ?: request.project]"
                                                            class="btn btn-default btn-sm">
                                <g:message code="job.create.button" />
                            </g:link></li>
                            <li style="padding:5px"><g:link controller="scheduledExecution" action="upload"
                                                            params="[project: params.project ?: request.project]"
                                                            class="btn btn-default btn-sm">
                                <g:message code="job.upload.button.title" />
                            </g:link></li>
                            </ul>
                        </auth:resourceAllowed>

                    </div>
                </g:if>
    <g:timerStart key="tail"/>
            </td>
        </tr>
    </table>
</div>


<g:timerEnd key="tail"/>
<g:timerEnd key="_workflowsFull.gsp"/>
