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
<g:set var="maxtitlesize" value="${30}"/>
<table cellpadding="0" cellspacing="0" class="jobsList list history" >
    <thead>

    <tr>
        <th colspan="2"><g:message code="events.history.title.Name"/></th>
        <th colspan="1"><g:message code="events.history.title.Summary"/></th>

        <g:if test="${options.tags}">
            <th><g:message code="jobquery.title.tagsFilter"/></th>
        </g:if>
        <th colspan="2"><g:message code="events.history.title.NodeFailureCount"/></th>
        <th><g:message code="jobquery.title.userFilter"/></th>
        <th><g:message code="jobquery.title.projFilter"/></th>
        <th><g:message code="jobquery.title.endFilter"/></th>
    </tr>
    </thead>
    <g:set var="sincetime" value="${0}"/>
    <g:if test="${hiliteSince}">
        <g:set var="sincetime" value="${hiliteSince instanceof String? Long.parseLong(hiliteSince) : hiliteSince}"/>
    </g:if>
    <g:each in="${reports}">
        <g:set var="rpt" value="${it}"/>
        <tr class="  ${it?.status != 'succeed' ? 'fail' : ''}  ${!it.dateCompleted ? 'nowrunning' : ''} ${sincetime && it.dateCompleted.time>sincetime?'newitem':''} hilite expandComponentHolder sectionhead" onclick="Expander.toggle(this,'${rkey}subsect_${it.id}');">
            <td style="width:12px;">
                <span class="action textbtn expandComponentControl" >
                    <img src="${resource(dir: 'images', file: 'icon-tiny-disclosure.png')}" title="Toggle extra information" alt="" width="12px" height="12px"/>
                </span>
            </td>

            <g:set var="vals" value="${['?','?','?']}"/>
            <g:if test="${it instanceof ExecReport}">
                <g:if test="${it?.node=~/^\d+\/\d+\/\d+$/}">
                    <g:set var="vals" value="${it.node.split('/') as List}"/>
                </g:if>
                <g:else>
                    <g:set var="vals" value="${[it?.status=='succeed'?'1':'0',it?.status=='succeed'?'0':'1','1']}"/>
                </g:else>
            </g:if>


            <td class="eventtitle ${rpt?.jcJobId?'job':'adhoc'}">
            <span>
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
                </span>
            </td>

            <td style="" class="eventsummary ${rpt?.jcJobId?'job':'adhoc'}">

                <span class="actiontitle ${it?.status != 'succeed' ? '' : ''} ">
                    <g:if test="${it.jcJobId || it.jcExecId}">
                        <g:truncate max="${maxmsgsize}">${rpt.title.encodeAsHTML()}</g:truncate>
                    </g:if>
                    <g:elseif test="${it instanceof ExecReport && it.adhocScript}">
                        <g:truncate max="${maxmsgsize}">${rpt.adhocScript.encodeAsHTML()}</g:truncate>
                    </g:elseif>
                    <g:else>
                        <g:truncate max="${maxmsgsize}">${rpt.title.encodeAsHTML()}</g:truncate>
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

            <td style="white-space:nowrap;text-align:right;" class="${vals[1]!='0'?'fail':'ok'}  nodecount sepL">
                <g:if test="${vals[1]!='0'}">
                    ${vals[1]} failed
                </g:if>
                <g:else>
                    ${vals[0]} ok
                </g:else>
            </td>

            <td >
                <g:if test="${it instanceof ExecReport && vals}">
                    <g:set var="summary" value=""/>
                    <g:if test="${vals.size()>2 && vals[2]!='0'}">
                        <g:set var="a" value="${Integer.parseInt(vals[0])}"/>
                        <g:set var="den" value="${Integer.parseInt(vals[2])}"/>
                        <g:set var="fai" value="${Integer.parseInt(vals[1])}"/>
                        <g:set var="sucperc" value="${(int)Math.floor((a/den)*100)}"/>
                        <g:set var="perc" value="${(int)Math.floor((fai/den)*100)}"/>
                        <g:if test="${vals[0] && vals[2]}">
                        <g:set var="sucsummary" value="${vals[0]+' of '+vals[2]}"/>
                        <g:set var="summary" value="${vals[1]+' of '+vals[2]}"/>
                        </g:if>
                    </g:if>
                    <g:else>
                        <g:set var="perc" value="${0}"/>
                    </g:else>
                    <g:if test="${perc>0}">
                    <g:render template="/common/progressBar" model="${[completePercent:(int)perc,title:'Completed nodes',className:'nodes failure',showpercent:false,innerContent:summary]}"/>
                    </g:if>
                </g:if>
            </td>

            <td class=" sepL user">
                ${it?.author.encodeAsHTML()}
            </td>

            <td class="project">
                ${it?.ctxProject.encodeAsHTML()}
            </td>

            <td style="white-space:nowrap" class="right sepL">
                <g:if test="${it.dateCompleted}">
                    <span title="<g:relativeDate atDate='${it?.dateStarted}'/> to <g:relativeDate atDate='${it?.dateCompleted}'/> ">
                        <g:relativeDate elapsed="${it?.dateCompleted}" />
                        (<g:relativeDate end="${it?.dateCompleted}" start="${it?.dateStarted}"/>)
                    </span>
                </g:if>
            </td>
        </tr>
        <g:render template="expandedReportContent" model="[it:it,colspan:9,subkey:rkey+'subsect',index:j]"/>
        <% j++; %>
    </g:each>
</table>