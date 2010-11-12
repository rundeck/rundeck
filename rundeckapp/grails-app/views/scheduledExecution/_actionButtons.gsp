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
<g:set var="ukey" value="${g.rkey()}"/>
<g:set var="jobAuthorized" value="${ !authMap || authMap[scheduledExecution.id.toString()] || jobAuthorized}"/>
<g:set var="jobRunAuth" value="${ auth.allowedTest(job:scheduledExecution, action:UserAuth.WF_RUN)}"/>
<g:set var="canRun" value="${ jobRunAuth }"/>
<div class="buttons">
        <span class="group floatr" id="${ukey}jobDisplayButtons${scheduledExecution.id}">
            <g:if test="${!small}">
                <g:if test="${!execPage}">
                <auth:allowed job="${scheduledExecution}" action="${UserAuth.WF_DELETE}">
                    <span class="icon button floatl" title="Delete ${g.message(code:'domain.ScheduledExecution.title')}" onclick="menus.showRelativeTo(this,'${ukey}jobDisplayDeleteConf${scheduledExecution.id}',-2,-2);return false;"><img src="${resource(dir:'images',file:'icon-small-removex.png')}" alt="delete" width="16px" height="16px"/></span>
                </auth:allowed>
                </g:if>
                <auth:allowed job="${scheduledExecution}" action="${UserAuth.WF_CREATE}">
                    <g:link controller="scheduledExecution" title="Copy Job" action="copy" id="${scheduledExecution.id}" class="icon button floatl"><img src="${resource(dir:'images',file:'icon-small-copy.png')}" alt="copy" width="16px" height="16px"/></g:link>
                </auth:allowed>
                <g:if test="${!execPage}">
                <auth:allowed job="${scheduledExecution}" name="${UserAuth.WF_UPDATE}">
                    <g:link controller="scheduledExecution" title="Edit Job" action="edit" id="${scheduledExecution.id}" class="icon button floatl"><img src="${resource(dir:'images',file:'icon-small-edit.png')}" alt="edit" width="16px" height="16px"/></g:link>
                </auth:allowed>
                </g:if>
                <auth:allowed job="${scheduledExecution}" name="${UserAuth.WF_READ}">
                    <g:link controller="scheduledExecution" title="Download XML" action="show" id="${scheduledExecution.id}.xml" class="icon button floatl"><img src="${resource(dir:'images',file:'icon-small-file-xml.png')}" alt="Download XML" width="13px" height="16px"/></g:link>
                </auth:allowed>
            </g:if>
            <g:if test="${canRun}">
                <g:link controller="scheduledExecution" action="execute" id="${scheduledExecution.id}" class="icon button floatl" onclick="loadExec(${scheduledExecution.id});return false;"><img src="${resource(dir:'images',file:'icon-small-run.png')}" title="Run ${g.message(code:'domain.ScheduledExecution.title')}&hellip;" alt="run" width="16px" height="16px"/></g:link>
            </g:if>
            <g:elseif test="${ !jobAuthorized || !jobRunAuth}">
                <span class="info note floatl" style="width:16px;padding: 2px;" title="${message(code:'unauthorized.job.run')}"><img src="${resource(dir:'images',file:'icon-small-warn.png')}" alt="" width="16px" height="16px"/></span>
            </g:elseif>
            <g:else>
                %{--<span class="floatl" style="width:16px;padding: 2px;"></span>--}%
            </g:else>
        </span>
        <div id="${ukey}jobDisplayDeleteConf${scheduledExecution.id}" class="confirmBox popout" style="display:none;">
            <g:form controller="scheduledExecution" action="delete" method="post" id="${scheduledExecution.id}">
                <span  class="confirmMessage">Really delete this <g:message code="domain.ScheduledExecution.title"/>?</span>
                <input type="submit" value="No" onclick="Element.toggle('${ukey}jobDisplayDeleteConf${scheduledExecution.id}');return false;"/>
                <input type="submit" value="Yes"/>
            </g:form>
        </div>
        <span class="clear"></span>
</div>