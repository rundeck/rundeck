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
<g:set var="width" value="16px"/>
<g:set var="iname" value="icon-small"/>
<g:if test="${iconname}">
    <g:set var="iname" value="icon-${iconname}"/>
</g:if>
<g:if test="${iconsize}">
    <g:set var="width" value="-${iconsize}"/>
</g:if>
<g:if test="${!jobauthorizations}">
    <%-- evaluate auth for the job on demand --%>
    <g:set var="jobauthorizations" value="${[:]}"/>
    <%
        [AuthConstants.ACTION_DELETE,AuthConstants.ACTION_RUN,AuthConstants.ACTION_READ,AuthConstants.ACTION_UPDATE].each{action->
            jobauthorizations[action]=auth.jobAllowedTest(job:scheduledExecution,action:action)?[idKey]:[]
        }
        jobauthorizations[AuthConstants.ACTION_CREATE]=auth.resourceAllowedTest(kind:'job',action: AuthConstants.ACTION_CREATE,project:scheduledExecution.project)
    %>
</g:if>
<g:set var="jobAuths" value="${ jobauthorizations }"/>
        <span class="group " id="${enc(attr:ukey)}jobDisplayButtons${enc(attr:scheduledExecution.id)}">
            <g:if test="${!small }">

                <g:if test="${!execPage}">
                    <g:if test="${jobAuths[AuthConstants.ACTION_UPDATE]?.contains(idKey)}">
                        <g:link controller="scheduledExecution" title="Edit Job" action="edit"
                                id="${scheduledExecution.extid}" class="icon button "><img
                                src="${resource(dir: 'images', file: iname + '-edit.png')}" alt="edit" width="${width}"
                                height="${width}"/></g:link>
                    </g:if>
                </g:if>

                <g:if test="${jobAuths[AuthConstants.ACTION_CREATE] && jobAuths[AuthConstants.ACTION_READ]?.contains(idKey)}">
                    <g:link controller="scheduledExecution" title="Copy Job" action="copy"
                            id="${scheduledExecution.extid}" class="icon button "><img
                            src="${resource(dir: 'images', file: iname + '-copy.png')}" alt="copy" width="${width}"
                            height="${width}"/></g:link>
                </g:if>
                <g:if test="${!execPage}">
                    <auth:resourceAllowed kind="job" action="${AuthConstants.ACTION_DELETE}" project="${scheduledExecution.project}">
                        <g:if test="${jobAuths[AuthConstants.ACTION_DELETE]?.contains(idKey) }">
                            <span class="icon button " title="Delete ${g.message(code:'domain.ScheduledExecution.title')}" onclick="menus.showRelativeTo(this,'${ukey}jobDisplayDeleteConf${scheduledExecution.id}',-2,-2);return false;"><img src="${resource(dir:'images',file: iname+'-removex.png')}" alt="delete" width="${width}" height="${width}"/></span>
                        </g:if>
                    </auth:resourceAllowed>
                </g:if>
                
            </g:if>
            <g:if test="${!noRunButton && (jobAuthorized || jobAuths[AuthConstants.ACTION_RUN]?.contains(idKey)) }">
                <g:link controller="scheduledExecution" action="execute" id="${scheduledExecution.extid}" class="icon button " onclick="if(typeof(loadExec)=='function'){loadExec(${scheduledExecution.id});return false;}"><img src="${resource(dir:'images',file: iname +'-run.png')}" title="Run ${g.message(code:'domain.ScheduledExecution.title')}&hellip;" alt="run" width="${width}" height="${width}"/></g:link>
            </g:if>

        </span>
<g:timerEnd key="actionButtonsNew"/>
