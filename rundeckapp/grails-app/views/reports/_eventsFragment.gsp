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

<%@ page import="org.rundeck.core.auth.AuthConstants" %>
<g:set var="rkey" value="${g.rkey()}" />

<user:getReportFilters user="${session.user}">
    <g:set var="filterset" value="${filters}"/>
</user:getReportFilters>
<g:set var="isCompact" value="${params.compact?true:false}"/>

<g:render template="/common/queryFilterManagerModal" model="${[rkey:rkey,filterName:filterName,filterset:filterset,update:rkey+'evtsForm',deleteActionSubmit:'deleteFilter', storeActionSubmit:'storeFilter']}"/>


<div id="${enc(attr:rkey)}evtsForm">
    <g:if test="${params.createFilters}">
        <span class="note help">
            Enter filter parameters below and click "save this filter" to set a name and save it.
        </span>
    </g:if>
    <g:set var="wasfiltered" value="${paginateParams}"/>
    <div class="queryTable card">
      <g:form action="index" class="form" role="form" params="${[project: params.project ?: request.project]}" useToken="true">
      <div class="card-content">
        <div class="form-group ">
            <div class="filterdef saved  collapse ${filterName ? 'in' : ''} obs_filter_is_selected">
                Selected filter: <span class="prompt obs_selected_filter_name"><g:enc>${filterName}</g:enc></span>
                <a class="btn btn-xs btn-danger pull-right " data-toggle="modal"
                        href="#deleteFilterModal" title="Click to delete this saved filter">
                    <b class="glyphicon glyphicon-remove"></b>
                    Delete Filter
                </a>
            </div>
        </div>
        <g:if test="${!params.nofilters}">
        <div id="${enc(attr:rkey)}filter" >
                <g:if test="${params.compact}">
                    <g:hiddenField name="compact" value="${params.compact}"/>
                </g:if>
                <g:hiddenField name="formInput" value="true"/>
                <g:hiddenField name="projFilter" value="${params.project ?: request.project}"/>
                <g:hiddenField name="project" value="${params.project ?: request.project}"/>

                <g:ifServletContextAttribute attribute="RSS_ENABLED" value="true">
                    <a title="RSS 2.0" class="floatr"
                       href="${createLink(controller: "feed", action: "index", params: paginateParams)}"
                        style="margin: 0 10px;"
                       id="rsslink"><img
                            src="${resource(dir: 'images', file: 'feed.png')}" width="14px" height="14px"
                            alt=""/> RSS</a>
                </g:ifServletContextAttribute>
                <g:hiddenField name="max" value="${max}"/>
                <g:render template="baseFiltersPlain" model="${[ query: query]}"/>
                <g:render template="advDateFiltersPlain" model="${[params:params,query:query]}"/>

                <div class="row">
                  <div class="col-xs-12 col-sm-9">
                    <g:submitButton value="${g.message(code:'button.action.Filter',default:'Filter')}" name="filterAll" class="btn btn-default btn-block"/>
                  </div>
                  <div class="col-xs-12 col-sm-3">
                    <a class="btn btn-success btn-block ${filterName?'':'in'} obs_filter_is_deselected"
                            style="${wdgt.styleVisible(unless: params.saveFilter)}"
                            data-toggle="modal"
                            href="#saveFilterModal" title="Click to save this filter with a name">
                        <g:message code="save.filter.ellipsis"/>
                    </a>
                  </div>
                </div>



        </div>
        </g:if>
      </div>
      </g:form>
    </div>
            <div id="${enc(attr:rkey)}evtscontent" class="card">
                <g:if test="${!params.nofilters}">
                <div class="queryresultsinfo card-header">
                  <div class="row">
                    <div class="col-xs-6" style="padding-top:10px">
                      <g:if test="${!params.compact}">
                          <span class="prompt"><span class="_obs_histtotal"><g:enc>${total}</g:enc></span> Results</span>
                          matching ${filterName?'filter':'your query'}
                      </g:if>

                      <g:if test="${ !filterName && filterset}">
                          <span class="info note">or choose a saved filter:</span>
                      </g:if>
                      <g:render template="/common/selectFilter" model="[noSelection:'-Within 1 Day-',filterset:filterset,filterName:filterName,prefName:'events']"/>
                      <g:if test="${includeBadge}">

                          <span class="badgeholder" id="eventsCountBadge" style="display:none">
                              <g:link action="index"
                                      title="click to load new events"
                                      params="${filterName ? [filterName: filterName] : params}"><span
                                      class="badge newcontent active" id="eventsCountContent"
                                      title="click to load new events"></span></g:link>
                          </span>
                      </g:if>
                    </div>
                    <div class="col-xs-6">
                      <g:if test="${includeAutoRefresh}">
                        <div class="form-group pull-right" style="display:inline-block;">
                          <div class="checkbox">
                            <g:checkBox name="refresh" value="true" checked="${params.refresh=='true'}" class="autorefresh" id="autorefresh"/>
                            <label for="autorefresh">
                                Auto refresh
                            </label>
                          </div>
                        </div>
                      </g:if>
                    </div>
                  </div>
                </div>
                </g:if>

                <div class="jobsReport clear card-content">
                    <g:if test="${reports}">
                        <g:form action="bulkDelete" controller="execution" method="POST" name="bulkDeleteForm" useToken="true">
                            <g:hiddenField name="project" value="${params.project}"/>
                            <g:hiddenField name="checkedIds" value="${params.checkedIds}" id="checkedIdsField"/>
                        <table class=" table table-hover table-condensed events-table" style="width:100%">
                        <g:if test="${includeNowRunning}">
                            <tbody id="nowrunning"></tbody>
                        </g:if>
                        <tbody id="histcontent">
                            <g:render template="baseReport" model="['reports':reports,options:params.compact?[tags:false, summary: false]:[summary:true],hiliteSince:params.hiliteSince]"/>
                        </tbody>
                        </table>

                            <g:if test="${total && max && total.toInteger() > max.toInteger()}">
                                <div class="info note">Showing <g:enc>${reports.size()}</g:enc> of <span class="_obs_histtotal"><g:enc>${total}</g:enc></span></div>
                                <g:if test="${params.compact}">
                                    <a href="${createLink(controller:'reports',action:params.moreLinkAction?params.moreLinkAction:'index',params:filterName?[filterName:filterName]:paginateParams?paginateParams:[:])}">More&hellip;</a>
                                </g:if>
                            </g:if>
                            <g:if test="${!params.compact}">
                                <span class="paginate"><g:paginate controller="reports" action="index" class=" pagination-sm pagination-embed"
                                                                    total="${total}" max="${max}" params="${paginateParams}"/></span>
                            </g:if>
                            <div class="pull-right">
                            <span class="obs_bulk_edit_enable " style="display: none">
                                <span class="btn btn-default act_bulk_edit_selectall  ">
                                    <g:message code="select.all"/>
                                </span>
                                <span class="btn btn-default act_bulk_edit_deselectall  ">
                                    <g:message code="select.none"/>
                                </span>

                                <span class="btn btn-xs btn-danger obs_bulk_edit_enable act_bulk_delete"
                                      data-toggle="modal"
                                      data-target="#bulkexecdelete">
                                    <g:message code="delete.selected.executions"/>
                                </span>
                                <span class="btn btn-default act_bulk_edit_disable obs_bulk_edit_enable "
                                      style="display: none">
                                    <i class="glyphicon glyphicon-remove-circle"></i>
                                    <g:message code="cancel.bulk.delete"/>
                                </span>
                            </span>
                            <g:set var="projAdminAuth" value="${auth.resourceAllowedTest(
                                    context: AuthConstants.CTX_APPLICATION, type: AuthConstants.TYPE_PROJECT, name: params.project, action: [AuthConstants.ACTION_ADMIN,AuthConstants.ACTION_APP_ADMIN])}"/>
                            <g:set var="deleteExecAuth"
                                   value="${auth.resourceAllowedTest(context: AuthConstants.CTX_APPLICATION, type: AuthConstants.TYPE_PROJECT, name:
                                           params.project, action: AuthConstants.ACTION_DELETE_EXECUTION) || projAdminAuth}"/>
                            <g:if test="${deleteExecAuth}">
                            <span class="btn btn-xs btn-warning act_bulk_edit_enable obs_bulk_edit_disable">
                                <g:message code="bulk.delete"/>
                            </span>
                                </g:if>
                            </div>

                        %{--confirm bulk delete modal--}%
                            <div class="modal" id="bulkexecdelete" tabindex="-1" role="dialog"
                                 aria-labelledby="bulkexecdeletetitle" aria-hidden="true">
                                <div class="modal-dialog">
                                    <div class="modal-content">
                                        <div class="modal-header">
                                            <button type="button" class="close" data-dismiss="modal"
                                                    aria-hidden="true">&times;</button>
                                            <h4 class="modal-title" id="bulkexecdeletetitle">Bulk Delete <g:message
                                                    code="domain.Execution.title.plural" default="Executions"/></h4>
                                        </div>

                                        <div class="modal-body">

                                            <p>Really delete all <b id="checkedIdsLength"></b> selected
                                                <g:message code="domain.Execution.title.plural" default="Executions"/>?
                                            </p>
                                        </div>

                                        <div class="modal-footer">

                                            <button type="submit" class="btn btn-default  " data-dismiss="modal">
                                                Cancel
                                            </button>
                                            <input type="submit"
                                                            class="btn  btn-danger obs_bulk_edit_enable "
                                                            value="${g.message(code: 'delete.selected.executions')}"/>
                                        </div>
                                    </div>
                                </div>
                            </div>
                        </g:form>
                    </g:if>
                </div>

                 </div>

<g:if test="${lastDate}">
<g:set var="checkUpdatedParams" value="${[since:lastDate,project:params.project]}"/>
%{
    if(filterName){
    checkUpdatedParams.filterName=filterName
    }else{
    checkUpdatedParams.putAll(paginateParams)
    }
}%
<g:set var="checkUpdatedUrl" value="${g.createLink(action:'since.json',params:checkUpdatedParams)}"/>
</g:if>
<g:set var="refreshUrl" value="${g.createLink(action:'eventsFragment',params:filterName?[filterName:filterName]:paginateParams)}"/>
<g:set var="rssUrl" value="${g.createLink(controller:'feed',action:'index',params:filterName?[filterName:filterName]:paginateParams)}"/>
<g:render template="/common/boxinfo" model="${[name:'events',model:[title:'History',total:total, max: max, offset: offset,url:refreshUrl,checkUpdatedUrl:checkUpdatedUrl,rssUrl:rssUrl,lastDate:lastDate]]}"/>

</div>
