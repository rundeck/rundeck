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
        <th colspan="4">Event</th>

        <th><g:message code="jobquery.title.messageFilter"/></th>
        <g:if test="${options.tags}">
            <th><g:message code="jobquery.title.tagsFilter"/></th>
        </g:if>
        <th><g:message code="jobquery.title.userFilter"/></th>
        <th><g:message code="jobquery.title.nodeFilter"/></th>
        <th><g:message code="jobquery.title.endFilter"/></th>
    </tr>
    </thead>
    <g:each in="${reports}">
        <g:set var="rpt" value="${it}"/>
        <tr class="  ${it?.status != 'succeed' ? 'fail' : ''}  ${!it.dateCompleted ? 'nowrunning' : ''} hilite expandComponentHolder sectionhead" onclick="Expander.toggle(this,'${rkey}subsect_${it.id}');">
            <td style="width:12px;">
                <g:if test="${it.dateCompleted}">
                    <img
                        src="${resource(dir: 'images', file: 'icon-tiny-' + (it?.status == 'succeed' ? 'ok' : 'warn') + '.png')}"
                        title="${it?.status}: <g:relativeDate atDate='${it.dateCompleted}'/>"
                        alt="" width="12px" height="12px"/>
                </g:if>
            </td>
            <td style="width:12px;">
                <span class="action textbtn expandComponentControl" >
                    <img src="${resource(dir: 'images', file: 'icon-tiny-disclosure.png')}" title="Toggle extra information" alt="" width="12px" height="12px"/>
                </span>
            </td>


            <td style="overflow:hidden;white-space:nowrap;width:50px;">

                <span class="actiontitle ${it?.status != 'succeed' ? 'fail' : ''} ${it.actionType}">
                    <g:if test="${it.jcJobId || it.jcExecId}">

                        <g:if test="${it.jcJobId}">
                            ${it.title.encodeAsHTML()}
                        </g:if>
                        <g:else>
                            <span class="info note"><g:truncate max="${maxmsgsize}">${rpt.title}</g:truncate></span>
                        </g:else>
                    </g:if>
                    <g:else>
                        <span class="info note"><g:truncate max="${maxmsgsize}">${rpt.title}</g:truncate></span>
                    </g:else>
                </span>
            </td>

            <td style="width:16px;">
                <g:if test="${it.jcJobId || it.jcExecId }">
                    <img src="${resource(dir: 'images', file: 'icon-small-job.png')}"
                        title="${g.message(code:'domain.ScheduledExecution.title')} Execution"
                        alt="" width="16px" height="16px"/>
                </g:if>
                <g:else>
                    <g:img file="icon-small-shell.png" width="16px" height="16px"/>
                </g:else>
            </td>

            <td style="overflow:hidden;white-space:nowrap;">
                <g:if test="${it.message}">

                    <%
                        def msgtrunc = it.message
                        def sb = new StringBuffer()
                        if(options.msgmaxsize && it.message.size()> options.msgmaxsize){
                            msgtrunc = it.message.substring(0,options.msgmaxsize)
                        }
                        else if (it.message.size() > options.msgsplitsize) {
                            msgtrunc = it.message.replaceAll('(\\S{' + options.msgsplitsize + '})', '$1\r\n\r')
                            def argssplit = []
                            argsplit = msgtrunc.split('\\r\\n\\r')
                            argsplit.each {
                                if (sb.size() > 0) {
                                    sb << " <br>"
                                }
                                sb << it
                            }
                            msgtrunc = sb.toString()
                        }
                    %>
                    <div class="msgtext msgrow"  title="${it.message.encodeAsHTML()}" style="width:300%">
                        %{--${msgtrunc.encodeAsHTML()}--}%
                        ${it.message.encodeAsHTML()}
                    </div>
                </g:if>
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

            <td class="sepL user">
                ${it?.author.encodeAsHTML()}
            </td>

            <td>
                <g:if test="${it instanceof ExecReport}">
                    ${it?.node.encodeAsHTML()}
                </g:if>
            </td>

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