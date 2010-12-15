<%--
  Created by IntelliJ IDEA.
  User: greg
  Date: Aug 7, 2008
  Time: 10:32:26 AM
  To change this template use File | Settings | File Templates.
--%>
<g:set var="rkey" value="${g.rkey()}"/>
<g:if test="${!options}">
    <g:set var="options" value="[title:true,message:true,end:true,job:true,author:true,stat:true,out:true,context:true,duration:true,cmdinf:true,node:true,msgsplitsize:60]"/>
</g:if>
<%
    if(!options.msgsplitsize){
        options.msgsplitsize=60
    }
    def j = 0;
%>
<g:set var="maxmsgsize" value="${options.evtmaxsize?options.evtmaxsize:options.msgsplitsize?options.msgsplitsize:-1}"/>
<table cellpadding="0" cellspacing="0" class="jobsList list" >
        <col style="width:16px;"/>
        <col style="width:16px;"/>
        <col style="width:50px;"/>
        <col style="width:18px;"/>
        <col />
        <col style="width:60px;"/>
        <col style="width:80px;"/>
        <col style="width:10ex;"/>
    <thead>

    <tr>
        <th colspan="2"></th>
        <th>Job</th>
        <th colspan="1">Event</th>

        <g:if test="${options.tags}">
            <th><g:message code="jobquery.title.tagsFilter"/></th>
        </g:if>
        <th><g:message code="jobquery.title.projFilter"/></th>
        <th><g:message code="jobquery.title.userFilter"/></th>
        %{--<th><g:message code="jobquery.title.nodeFilter"/></th>--}%
        <th><g:message code="jobquery.title.endFilter"/></th>
    </tr>
    </thead>
    <g:each in="${reports}">
        <g:set var="rpt" value="${it}"/>
        <tr class="  ${it?.status != 'succeed' ? 'fail' : ''}  ${!it.dateCompleted ? 'nowrunning' : ''} hilite expandComponentHolder sectionhead" onclick="Expander.toggle(this,'${rkey}subsect_${it.id}');">
            <td style="width:12px;">
                <span class="action textbtn expandComponentControl" >
                    <img src="${resource(dir: 'images', file: 'icon-tiny-disclosure.png')}" title="Toggle extra information" alt="" width="12px" height="12px"/>
                </span>
            </td>

            <td style="width:16px;" class="${it?.status} statusmessage">
                ${it?.status=='succeed'?'OK':'FAIL'}
            </td>
            <td class="eventtitle">
                <g:if test="${it.jcJobId }">
                    ${ScheduledExecution.get(it.jcJobId).generateFullName()}
                </g:if>
            </td>

            <td style="" class="eventtitle ${it.jcJobId?'job':'adhoc'}">

                <span class="actiontitle ${it?.status != 'succeed' ? '' : ''} ">
                    <g:if test="${it.jcJobId || it.jcExecId}">

                        <g:if test="${it.jcJobId}">
                            ${it.title.encodeAsHTML()}
                        </g:if>
                        <g:else>
                            <g:truncate max="${maxmsgsize}">${rpt.title}</g:truncate>
                        </g:else>
                    </g:if>
                    <g:elseif test="${it instanceof ExecReport && it.adhocScript}">
                        <g:truncate max="${maxmsgsize}">${rpt.adhocScript.encodeAsHTML()}</g:truncate>
                    </g:elseif>
                    <g:else>
                        <g:truncate max="${maxmsgsize}">${rpt.title}</g:truncate>
                    </g:else>
                </span>
            </td>


           

            <g:if test="${options.tags}">
            <td>
                <g:if test="${it.tags}">
                    <g:each in="${it.tags.split(/\s*,\s*/)}" var="tag">
                        <span class="tag">${tag.encodeAsHTML()}</span>
                    </g:each>
                </g:if>
            </td>
            </g:if>

            <td class="sepL project">
                ${it?.ctxProject.encodeAsHTML()}
            </td>
            <td class=" user">
                ${it?.author.encodeAsHTML()}
            </td>

            %{--<td>--}%
                %{--<g:if test="${it instanceof ExecReport}">--}%
                    %{--${it?.node.encodeAsHTML()}--}%
                %{--</g:if>--}%
            %{--</td>--}%

            <td style="white-space:nowrap" class="right">
                <g:if test="${it.dateCompleted}">
                    <span title="${it?.actionType ? it.actionType : it.status}: <g:relativeDate atDate='${it?.dateCompleted}'/>">
                        <g:relativeDate elapsed="${it?.dateCompleted}" agoClass="timeuntil"/>
                    </span>
                </g:if>
            </td>
        </tr>
        <g:render template="expandedReportContent" model="[it:it,colspan:9,subkey:rkey+'subsect',index:j]"/>
        <% j++; %>
    </g:each>
</table>