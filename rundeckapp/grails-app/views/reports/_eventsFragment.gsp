<%@ page import="com.dtolabs.rundeck.server.authorization.AuthConstants; rundeck.User" %>
<g:set var="rkey" value="${g.rkey()}" />

<g:if test="${session.user && User.findByLogin(session.user)?.reportfilters}">
    <g:set var="filterset" value="${User.findByLogin(session.user)?.reportfilters}"/>
</g:if>
<g:set var="isCompact" value="${params.compact?true:false}"/>
    
<div id="${enc(attr:rkey)}evtsForm">
    <g:if test="${params.createFilters}">
        <span class="note help">
            Enter filter parameters below and click "save this filter" to set a name and save it.
        </span>
    </g:if>
    <g:set var="wasfiltered" value="${paginateParams}"/>
    <div class="queryTable">
        <g:if test="${!params.nofilters}">
        <div id="${enc(attr:rkey)}filter" >
            <g:form action="index" class="form-inline" role="form" params="${[project: params.project ?: request.project]}" useToken="true">
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
                <g:render template="/common/queryFilterManagerModal" model="${[rkey:rkey,filterName:filterName,filterset:filterset,update:rkey+'evtsForm',deleteActionSubmit:'deleteFilter', storeActionSubmit:'storeFilter']}"/>
                <g:hiddenField name="max" value="${max}"/>
                <g:render template="baseFiltersPlain" model="${[params: params, query: query]}"/>
                <g:render template="recentDateFiltersPlain" model="${[params:params,query:query]}"/>
                <g:render template="advDateFiltersPlain" model="${[params:params,query:query]}"/>

                <g:submitButton value="Filter" name="filterAll" class="btn btn-default btn-sm"/>
                    <a class="btn btn-xs pull-right btn-success collapse ${filterName?'':'in'} obs_filter_is_deselected"
                            style="${wdgt.styleVisible(unless: params.saveFilter)}"
                            data-toggle="modal"
                            href="#saveFilterModal" title="Click to save this filter with a name">
                        <i class="glyphicon glyphicon-plus"></i> save this filter&hellip;
                    </a>
                    <span class="form-group ">
                        <div class="filterdef saved  collapse ${filterName ? 'in' : ''} obs_filter_is_selected">
                            Selected filter: <span class="prompt obs_selected_filter_name"><g:enc>${filterName}</g:enc></span>
                            <a class="btn btn-xs btn-link btn-danger pull-right " data-toggle="modal"
                                    href="#deleteFilterModal" title="Click to delete this saved filter">
                                <b class="glyphicon glyphicon-remove"></b>
                                delete filter…
                            </a>
                        </div>
                    </span>
            </g:form>
        </div>
        </g:if>
    </div>
            <div id="${enc(attr:rkey)}evtscontent">
                <g:if test="${!params.nofilters}">
                <div class="queryresultsinfo">
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
                        <g:if test="${includeAutoRefresh}">
                            <g:checkBox name="refresh" value="true" checked="${params.refresh=='true'}" class="autorefresh" id="autorefresh"/>
                            <label for="autorefresh">
                                Auto refresh
                            </label>
                        </g:if>
                </div>
                </g:if>

                <div class="jobsReport clear">
                    <g:if test="${reports}">
                        <g:form action="bulkDelete" controller="execution" method="POST" name="bulkDeleteForm" useToken="true">
                            <g:hiddenField name="project" value="${params.project}"/>
                        <table class=" table table-hover table-condensed events-table" style="width:100%">
                        <g:if test="${includeNowRunning}">
                            <tbody id="nowrunning"></tbody>
                        </g:if>
                        <tbody id="histcontent">
                            <g:render template="baseReport" model="['reports':reports,options:params.compact?[tags:false, summary: false]:[summary:true],hiliteSince:params.hiliteSince]"/>
                        </tbody>
                        </table>

                            <g:if test="${total && max && total.toInteger() > max.toInteger()}">
                                <span class="info note">Showing <g:enc>${reports.size()}</g:enc> of <span class="_obs_histtotal"><g:enc>${total}</g:enc></span></span>
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
                                <span class="textbtn textbtn-default act_bulk_edit_selectall  ">
                                    <g:message code="select.all"/>
                                </span>
                                <span class="textbtn textbtn-default act_bulk_edit_deselectall  ">
                                    <g:message code="select.none"/>
                                </span>

                                <span class="btn btn-xs btn-danger obs_bulk_edit_enable"
                                      data-toggle="modal"
                                      data-target="#bulkexecdelete">
                                    <g:message code="delete.selected.executions"/>
                                </span>
                                <span class="textbtn textbtn-default act_bulk_edit_disable obs_bulk_edit_enable "
                                      style="display: none">
                                    <i class="glyphicon glyphicon-remove-circle"></i>
                                    <g:message code="cancel.bulk.delete"/>
                                </span>
                            </span>
                            <g:set var="projAdminAuth" value="${auth.resourceAllowedTest(
                                    context: 'application', type: 'project', name: params.project, action: AuthConstants.ACTION_ADMIN)}"/>
                            <g:set var="deleteExecAuth"
                                   value="${auth.resourceAllowedTest(context: 'application', type: 'project', name:
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

                                            <p>Really delete all selected
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
