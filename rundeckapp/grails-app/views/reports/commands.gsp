<html>
    <head>
        <meta http-equiv="Content-Type" content="text/html; charset=UTF-8"/>
    <meta name="layout" content="base"/>
        <meta name="tabpage" content="events"/>
        <meta name="rssfeed" content="${createLink(controller:"feed",action:"commands",params:paginateParams)}"/>
        <title><g:message code="main.app.name"/> - Commands</title>
    </head>
<body>
    <g:set var="reportOptions" value="${session.exec_reports_options?session.exec_reports_options:[author:true,stat:true,out:true,duration:true,context:true,cmdinf:true,reportId:true,node:true]}"/>

<div class="pageTop">
    <g:render template="menusPageTop" model="[menu:'events',submenu:'commands']"/>
    <g:ifServletContextAttribute attribute="RSS_ENABLED" value="true">
        <div class="floatr">
            <a title="RSS 2.0" href="${createLink(controller:"feed",action:"commands",params:paginateParams)}"><img src="${resource(dir:'images',file:'feed.png')}" width="14px" height="14px" alt=""/> RSS</a>
        </div>
    </g:ifServletContextAttribute>
</div>
<div class="pageBody">
    <g:set var="wasfiltered" value="${paginateParams}"/>
    <g:form action="commands">
    <table cellspacing="10" cellpadding="0" class="queryTable">
        <tr>
        <td style="text-align:left;vertical-align:top; display:none;" id="filter" >

                        <span class="prompt action" onclick="Element.toggle('filter');if($('filterdispbtn')){Element.toggle('filterdispbtn');}">
                            Filter
                            <img src="${resource(dir:'images',file:'icon-tiny-disclosure-open.png')}" width="12px" height="12px"/>
                        </span>
                <div class="presentation filter">
                            <g:hiddenField name="max" value="${max}"/>
                            <g:hiddenField name="offset" value="${offset}"/>
                    <table class="simpleForm">
                            <g:render template="recentDateFilters" model="${[params:params]}"/>
                            <g:render template="advDateFilters" model="${[params:params,query:query]}"/>
                            <g:render template="baseFilters" model="${[params:params,isCommandsPage:true]}"/>
                    </table>
                        <div style="text-align:right;">
                            <g:actionSubmit value="Filter Commands" action="Commands"/>
                        </div>
                        <span class="prompt">Restrict to:</span>
                        <div class="presentation">

                            <div>
                                <g:actionSubmit value="Jobs" action="Jobs"/>
                            </div>
                            <div>
                                <g:actionSubmit value="Filter All Events" action="index"/>
                            </div>

                        </div>
                        <div class="presentation">
                            <g:submitButton name="Clear" value="Reset Filter"/>
                        </div>

                </div>
            </td>
            <td style="text-align:left;vertical-align:top;">
                <div>
                    
                    <g:if test="${displayParams}">
                        <span class="prompt">${total} Completed Command<g:if test="${total!=1}">s</g:if></span>
                        matching filter

                        <div style="padding:5px 0;margin:5px 0;" id='filterdispbtn'>
                            <span title="Click to modify filter" class="info textbtn query action" onclick="Element.toggle('filter');if($('filterdispbtn')){Element.toggle('filterdispbtn');}" >
                                <g:render template="displayFilters" model="${[displayParams:displayParams]}"/>
                                <img src="${resource(dir:'images',file:'icon-tiny-disclosure.png')}" width="12px" height="12px"/>
                            </span>
                        </div>
                    </g:if>
                    <g:else>
                        <span class="prompt">Completed Commands (${total})</span>
                        <span class="prompt action" onclick="Element.toggle('filter');if($('filterdispbtn')){Element.toggle('filterdispbtn');}" id="filterdispbtn"  style="">
                            Filter
                            <img src="${resource(dir:'images',file:'icon-tiny-disclosure.png')}" width="12px" height="12px"/>
                        </span>
                    </g:else>
                </div>

                <div class="jobsReport clear">
                    <g:render template="baseReport" model="[reports:reports,options:reportOptions]"/>
                </div>

                <g:if test="${total && max && total.toInteger() > max.toInteger()}">
                    <span class="info note">Showing ${reports.size()} of ${total}</span>
                </g:if>
                <span class="paginate"><g:paginate total="${total}" max="${max}" params="${paginateParams}"/></span>

                %{--<g:ifServletContextAttribute attribute="RSS_ENABLED" value="true">--}%
                %{--<div>--}%
                    %{--<g:link controller="feed" action="reports" params="${paginateParams}" title="Completed Jobs Feed"><img src="${resource(dir:'images',file:'feed.png')}" width="14px" height="14px" alt=""/> RSS Feed</g:link>--}%
                    %{--<g:if test="${paginateParams}">--}%
                        %{--for the current filter--}%
                    %{--</g:if>--}%
                %{--</div>--}%
                %{--</g:ifServletContextAttribute>--}%
             </td>

        </tr>
    </table>
    </g:form>

</div>
</body>
</html>
