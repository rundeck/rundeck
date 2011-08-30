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
    projectSelect.gsp
    
    Author: Greg Schueler <a href="mailto:greg@dtosolutions.com">greg@dtosolutions.com</a>
    Created: Aug 24, 2010 2:02:35 PM
    $Id$
 --%>
<g:set var="projectSet" value="${[]}"/>
<g:each in="${projects*.name.sort()}" var="proj">
    %{
      projectSet<<['key':proj,'value':proj]
    }%
</g:each>
<auth:resourceAllowed action="create" kind="project" context="application">
    <g:if test="${!params.nocreate}">
    %{
      projectSet<<[value:"Create new Project...",key:'-new-']
    }%
    </g:if>
</auth:resourceAllowed>
<g:select from="${projectSet}" optionKey='key' optionValue='value' name="${params.key?params.key:'projectSelect'}" onchange="${params.callback?params.callback:'selectProject'}(this.value);" value="${params.selected?params.selected:project}" />
<g:if test="${error}">
    <span class="error message">${error}</span>
</g:if>