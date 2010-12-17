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
<g:set var="maxtitlesize" value="${40}"/>
<table cellpadding="0" cellspacing="0" class="jobsList list" >
        <col style="width:16px;"/>
        <col style="width:16px;"/>
        <col style="width:50px;"/>
        <col style="width:18px;"/>
        <col />
        <col style="width:60px;"/>
        <col style="width:100px;"/>
        <col style="width:10ex;"/>
    <thead>

    <tr>
        <th colspan="2"></th>
        <th><g:message code="events.history.title.Name"/></th>
        <th colspan="1"><g:message code="events.history.title.Summary"/></th>

        <g:if test="${options.tags}">
            <th><g:message code="jobquery.title.tagsFilter"/></th>
        </g:if>
        <th><g:message code="jobquery.title.projFilter"/></th>
        <th><g:message code="jobquery.title.userFilter"/></th>
        <th><g:message code="events.history.title.Completed"/></th>
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
                <g:message code="${it?.status=='succeed'?'status.label.short.OK':'status.label.short.FAIL'}"/>
            </td>
            <td class="eventtitle ${rpt?.jcJobId?'job':'adhoc'}">
                <g:if test="${rpt?.reportId }">
                    <g:truncate max="${maxtitlesize}" front="true">${rpt?.reportId.encodeAsHTML()}</g:truncate>
                </g:if>
                %{--<g:elseif test="${it.jcJobId }">--}%
                    %{--<g:set var="jobname" value="${job?.generateFullName()}"/>--}%
                    %{--<g:truncate max="${maxtitlesize}" front="true">${jobname.encodeAsHTML()}</g:truncate>--}%
                %{--</g:elseif>--}%
                <g:else>
                    <g:message code="events.history.jobname.adhoc"/>
                </g:else>
            </td>

            <td style="" class="eventsummary ${rpt?.jcJobId?'job':'adhoc'}">

                <span class="actiontitle ${it?.status != 'succeed' ? '' : ''} ">
                    <g:if test="${it.jcJobId || it.jcExecId}">
                        <g:if test="${it.jcJobId}">
                            <g:truncate max="${maxmsgsize}">${rpt.title.encodeAsHTML()}</g:truncate>
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

            <td>
                <g:if test="${it instanceof ExecReport}">
                    <g:if test="${it?.node=~/^\d+\/\d+\/\d+$/}">
                        <g:set var="vals" value="${it.node.split('/')}"/>
                        <g:if test="${vals.length>2 && vals[2]!='0'}">
                            <g:set var="a" value="${Integer.parseInt(vals[0])}"/>
                            <g:set var="den" value="${Integer.parseInt(vals[2])}"/>
                            <g:set var="perc" value="${Math.floor((a/den)*100)}"/>
                        </g:if>
                        <g:else>
                            <g:set var="perc" value="${0}"/>
                        </g:else>
                        <g:render template="/common/progressBar" model="${[completePercent:(int)perc,title:'Completed nodes',className:'nodes',showpercent:true]}"/>
                        
                    </g:if>
                </g:if>
            </td>

            <td style="white-space:nowrap" class="right">
                <g:if test="${it.dateCompleted}">
                    <span title="${it?.actionType ? it.actionType : it.status}: <g:relativeDate atDate='${it?.dateCompleted}'/>">
                        <g:relativeDate elapsed="${it?.dateCompleted}" />
                    </span>
                </g:if>
            </td>
        </tr>
        <g:render template="expandedReportContent" model="[it:it,colspan:9,subkey:rkey+'subsect',index:j]"/>
        <% j++; %>
    </g:each>
</table>