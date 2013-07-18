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
    <g:set var="filtersOpen" value="${true}"/>
    <table cellspacing="0" cellpadding="0" class="queryTable" style="width:100%">
        <g:if test="${!params.nofilters}">
            <tr>
        <td style="text-align:left;vertical-align:top; ${wdgt.styleVisible(if:filtersOpen)}" id="${rkey}filter" >
            <g:form action="index">
                <g:if test="${params.compact}">
                    <g:hiddenField name="compact" value="${params.compact}"/>
                </g:if>
                <g:hiddenField name="formInput" value="true"/>
                <g:hiddenField name="projFilter" value="${session.project}"/>
                <g:render template="/common/queryFilterManager" model="${[rkey:rkey,filterName:filterName,filterset:filterset,update:rkey+'evtsForm',deleteActionSubmitRemote:[controller:'reports',action:'deleteFilter',params:[fragment:true]],storeActionSubmitRemote:[controller:'reports',action:'storeFilter',params:[fragment:true]]]}"/>
                <div class="presentation filter">

                    <g:hiddenField name="max" value="${max}"/>
                    <g:hiddenField name="offset" value="${offset}"/>
                        <g:render template="baseFiltersPlain" model="${[params: params, query: query]}"/>
                        <g:render template="recentDateFiltersPlain" model="${[params:params,query:query]}"/>
                        <g:render template="advDateFiltersPlain" model="${[params:params,query:query]}"/>

                    <span style="text-align:right;">
                        <g:submitButton value="Clear" name="clearFilter"/>
                        <g:submitButton value="Filter" name="filterAll"/>
                    %{--<g:submitToRemote  value="Clear" name="clearFilter" url="[controller:'reports',action:'clearFragment']" update="${rkey}evtsForm" />--}%
                        %{--<g:submitToRemote  value="Filter Events" name="filterAll" url="[controller:'reports',action:'eventsFragment']" update="${rkey}evtsForm" />--}%
                    </span>
                </div>
                </g:form>
        </td>
            </tr>
            </g:if>
        <tr>
            <td style="text-align:left;vertical-align:top;" id="${rkey}evtscontent">
                <g:if test="${!params.nofilters}">
                <div>
                    <g:if test="${displayParams}">


                        <g:if test="${!params.compact}">
                            <span class="prompt">${total} Results</span>
                            matching ${filterName?'filter':'your query'}
                        </g:if>

                        <g:if test="${ !filterName && filterset}">
                            <span class="info note">or choose a saved filter:</span>
                        </g:if>
                        <g:render template="/common/selectFilter" model="[noSelection:'-Within 1 Day-',filterset:filterset,filterName:filterName,prefName:'events']"/>

                        <div style="padding:5px 0;margin:5px 0;${!filtersOpen?'':'display:none;'} " id='${rkey}filterdispbtn' >
                            <span title="Click to modify filter" class="info textbtn query action" onclick="['${rkey}filter','${rkey}filterdispbtn'].each(Element.toggle);if(${isCompact}){$('${rkey}evtscontent').toggle();}" >
                                <g:render template="displayFilters" model="${[displayParams:displayParams]}"/>
                                <img src="${resource(dir:'images',file:'icon-tiny-disclosure.png')}" width="12px" height="12px"/>
                            </span>

                            <g:if test="${!filterName}">
                                <span class="prompt action " onclick="['${rkey}filter','${rkey}filterdispbtn','${rkey}fsave','${rkey}fsavebtn'].each(Element.toggle);if(${isCompact}){$('${rkey}evtscontent').toggle();}" id="${rkey}fsavebtn" title="Click to save this filter with a name">
                                    save this filter&hellip;
                                </span>
                            </g:if>



                        </div>
                    </g:if>
                    <g:else>
                        <span class="prompt">Events (${total})</span>
                        <span class="prompt action" onclick="['${rkey}filter','${rkey}filterdispbtn'].each(Element.toggle);if(${isCompact}){$('${rkey}evtscontent').toggle();}" id="${rkey}filterdispbtn"  style="${!filtersOpen?'':'display:none;'}">
                            Filter
                            <img src="${resource(dir:'images',file:'icon-tiny-disclosure.png')}" width="12px" height="12px"/>
                        </span>
                        <g:if test="${filterset}">
                            <span class="info note">Filter:</span>
                            <g:render template="/common/selectFilter" model="[noSelection:'-Within 1 Day-',filterset:filterset,filterName:filterName,prefName:'events']"/>
                        </g:if>
                    </g:else>
                </div>
                </g:if>

                <div class="jobsReport clear">
                    <g:if test="${reports}">
                        <g:render template="baseReport" model="['reports':reports,options:params.compact?[tags:false, summary: false]:[summary:true],hiliteSince:params.hiliteSince]"/>

                            <g:if test="${total && max && total.toInteger() > max.toInteger()}">
                                <span class="info note">Showing ${reports.size()} of ${total}</span>
                                <g:if test="${params.compact}">
                                    <a href="${createLink(controller:'reports',action:params.moreLinkAction?params.moreLinkAction:'index',params:filterName?[filterName:filterName]:paginateParams?paginateParams:[:])}">More&hellip;</a>
                                </g:if>
                            </g:if>
                            <g:if test="${!params.compact}">
                                <span class="paginate"><g:paginate controller="reports" action="index" total="${total}" max="${max}" params="${paginateParams}"/></span>
                            </g:if>
                    </g:if>
                </div>

                 </td>

                </tr>
            </table>
<g:javascript>

$$('#${rkey}evtsForm input').each(function(elem){
    if(elem.type=='text'){
        elem.observe('keypress',noenter);
    }
});
</g:javascript>
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
<g:render template="/common/boxinfo" model="${[name:'events',model:[title:'History',total:total,url:refreshUrl,checkUpdatedUrl:checkUpdatedUrl,rssUrl:rssUrl,lastDate:lastDate]]}"/>

</div>
