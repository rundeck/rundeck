<%@ page import="rundeck.User" %>
<g:set var="rkey" value="${g.rkey()}" />

<g:if test="${session.user && User.findByLogin(session.user)?.reportfilters}">
    <g:set var="filterset" value="${User.findByLogin(session.user)?.reportfilters}"/>
</g:if>
<g:set var="isCompact" value="${params.compact?true:false}"/>
    
<div id="${rkey}evtsForm">
    <g:if test="${params.createFilters}">
        <span class="note help">
            Enter filter parameters below and click "save this filter" to set a name and save it.
        </span>
    </g:if>
    <g:set var="wasfiltered" value="${paginateParams}"/>
    <div class="queryTable">
        <g:if test="${!params.nofilters}">
        <div id="${rkey}filter" >
            <g:form action="index" class="form-inline" role="form">
                <g:if test="${params.compact}">
                    <g:hiddenField name="compact" value="${params.compact}"/>
                </g:if>
                <g:hiddenField name="formInput" value="true"/>
                <g:hiddenField name="projFilter" value="${session.project}"/>

                <g:ifServletContextAttribute attribute="RSS_ENABLED" value="true">
                    <a title="RSS 2.0" class="floatr"
                       href="${createLink(controller: "feed", action: "index", params: paginateParams)}"
                        style="margin: 0 10px;"
                       id="rsslink"><img
                            src="${resource(dir: 'images', file: 'feed.png')}" width="14px" height="14px"
                            alt=""/> RSS</a>
                </g:ifServletContextAttribute>
                <g:render template="/common/queryFilterManager" model="${[rkey:rkey,filterName:filterName,filterset:filterset,update:rkey+'evtsForm',deleteActionSubmitRemote:[controller:'reports',action:'deleteFilter',params:[fragment:true]], storeActionSubmit:'storeFilter']}"/>
                <g:hiddenField name="max" value="${max}"/>
                <g:render template="baseFiltersPlain" model="${[params: params, query: query]}"/>
                <g:render template="recentDateFiltersPlain" model="${[params:params,query:query]}"/>
                <g:render template="advDateFiltersPlain" model="${[params:params,query:query]}"/>

                <g:submitButton value="Filter" name="filterAll" class="btn btn-default btn-sm"/>
            </g:form>
        </div>
        </g:if>
    </div>
            <div id="${rkey}evtscontent">
                <g:if test="${!params.nofilters}">
                <div class="queryresultsinfo">
                        <g:if test="${!params.compact}">
                            <span class="prompt"><span class="_obs_histtotal">${total}</span> Results</span>
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
                                        title="click to load new events"></span>
                                </g:link>
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
                        <table cellpadding="0" cellspacing="0" class="jobsList list history table table-hover table-condensed" style="width:100%">
                        <g:if test="${includeNowRunning}">
                            <tbody id="nowrunning"></tbody>
                        </g:if>
                        <tbody id="histcontent">
                            <g:render template="baseReport" model="['reports':reports,options:params.compact?[tags:false, summary: false]:[summary:true],hiliteSince:params.hiliteSince]"/>
                        </tbody>
                        </table>

                            <g:if test="${total && max && total.toInteger() > max.toInteger()}">
                                <span class="info note">Showing ${reports.size()} of <span class="_obs_histtotal">${total}</span></span>
                                <g:if test="${params.compact}">
                                    <a href="${createLink(controller:'reports',action:params.moreLinkAction?params.moreLinkAction:'index',params:filterName?[filterName:filterName]:paginateParams?paginateParams:[:])}">More&hellip;</a>
                                </g:if>
                            </g:if>
                            <g:if test="${!params.compact}">
                                <div class="paginate"><g:paginate controller="reports" action="index" total="${total}" max="${max}" params="${paginateParams}"/></div>
                            </g:if>
                    </g:if>
                </div>

                 </div>

<g:if test="${lastDate}">
<g:set var="checkUpdatedParams" value="${[since:lastDate]}"/>
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
