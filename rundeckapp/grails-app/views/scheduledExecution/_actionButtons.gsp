<%@ page import="com.dtolabs.rundeck.server.authorization.AuthConstants" %>
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
    _actionButtons.gsp

    Author: Greg Schueler <a href="mailto:greg@dtosolutions.com">greg@dtosolutions.com</a>
    Created: Jun 1, 2010 3:42:29 PM
    $Id$
 --%>
<g:timerStart key="actionButtonsNew"/>
<g:set var="ukey" value="${g.rkey()}"/>
<g:set var="idKey" value="${scheduledExecution.id.toString()}"/>
<g:if test="${!jobauthorizations}">
    <%-- evaluate auth for the job on demand --%>
    <g:set var="jobauthorizations" value="${[:]}"/>
    <%
        [AuthConstants.ACTION_DELETE,AuthConstants.ACTION_RUN,AuthConstants.ACTION_READ,AuthConstants.ACTION_UPDATE].each{action->
            jobauthorizations[action]=auth.jobAllowedTest(job:scheduledExecution,action:action)?[idKey]:[]
        }
        jobauthorizations[AuthConstants.ACTION_CREATE]=auth.resourceAllowedTest(kind:'job',action: AuthConstants.ACTION_CREATE)
    %>
</g:if>
<g:set var="jobAuths" value="${ jobauthorizations }"/>
<div class="buttons">
        <span class="group floatr" id="${ukey}jobDisplayButtons${scheduledExecution.id}">
            <g:if test="${!small }">
                <g:if test="${!execPage}">
                <g:if test="${jobAuths[AuthConstants.ACTION_DELETE]?.contains(idKey) }">
                    <span class="icon button floatl" title="Delete ${g.message(code:'domain.ScheduledExecution.title')}" onclick="menus.showRelativeTo(this,'${ukey}jobDisplayDeleteConf${scheduledExecution.id}',-2,-2);return false;"><img src="${resource(dir:'images',file:'icon-small-removex.png')}" alt="delete" width="16px" height="16px"/></span>
                </g:if>
                </g:if>
                <g:if test="${jobAuths[AuthConstants.ACTION_CREATE] && jobAuths[AuthConstants.ACTION_READ]?.contains(idKey) }">
                    <g:link controller="scheduledExecution" title="Copy Job" action="copy" id="${scheduledExecution.extid}" class="icon button floatl"><img src="${resource(dir:'images',file:'icon-small-copy.png')}" alt="copy" width="16px" height="16px"/></g:link>
                </g:if>
                <g:if test="${!execPage}">
                <g:if test="${jobAuths[AuthConstants.ACTION_UPDATE]?.contains(idKey) }">
                    <g:link controller="scheduledExecution" title="Edit Job" action="edit" id="${scheduledExecution.extid}" class="icon button floatl"><img src="${resource(dir:'images',file:'icon-small-edit.png')}" alt="edit" width="16px" height="16px"/></g:link>
                </g:if>
                </g:if>
                <g:if test="${jobAuths[AuthConstants.ACTION_READ]?.contains(idKey) }">
                    <span class="icon action button textbtn floatl obs_bubblepopup" id="downloadlink" title="Download Job definition file"><img src="${resource(dir: 'images', file: 'icon-small-file.png')}" alt="download" width="13px" height="16px"/></span>
                    <div  id="downloadlink_popup" style="display:none;">
                        <span class="prompt">Select a format:</span>
                        <ul>
                            <li><g:link controller="scheduledExecution" title="Download XML" action="show" id="${scheduledExecution.extid}.xml">XML</g:link></li>
                            <li><g:link controller="scheduledExecution" title="Download YAML" action="show" id="${scheduledExecution.extid}.yaml">YAML</g:link></li>
                        </ul>
                    </div>
                </g:if>
            </g:if>
            <g:if test="${jobAuthorized || jobAuths[AuthConstants.ACTION_RUN]?.contains(idKey) }">
                <g:link controller="scheduledExecution" action="execute" id="${scheduledExecution.extid}" class="icon button floatl" onclick="if(typeof(loadExec)=='function'){loadExec(${scheduledExecution.id});return false;}"><img src="${resource(dir:'images',file:'icon-small-run.png')}" title="Run ${g.message(code:'domain.ScheduledExecution.title')}&hellip;" alt="run" width="16px" height="16px"/></g:link>
            </g:if>
            <g:elseif test="${ !jobAuthorized }">
                <span class="info note floatl" style="width:16px;padding: 2px;" title="${message(code:'unauthorized.job.run')}"><img src="${resource(dir:'images',file:'icon-small-warn.png')}" alt="" width="16px" height="16px"/></span>
            </g:elseif>
            <g:else>
                %{--<span class="floatl" style="width:16px;padding: 2px;"></span>--}%
            </g:else>
        </span>
        <div id="${ukey}jobDisplayDeleteConf${scheduledExecution.id}" class="confirmBox popout" style="display:none;">
            <g:form controller="scheduledExecution" action="delete" method="post" id="${scheduledExecution.extid}">
                <span  class="confirmMessage">Really delete this <g:message code="domain.ScheduledExecution.title"/>?</span>
                <input type="submit" value="No" onclick="Element.toggle('${ukey}jobDisplayDeleteConf${scheduledExecution.id}');return false;"/>
                <input type="submit" value="Yes"/>
            </g:form>
        </div>
        <span class="clear"></span>
</div>
<g:timerEnd key="actionButtonsNew"/>