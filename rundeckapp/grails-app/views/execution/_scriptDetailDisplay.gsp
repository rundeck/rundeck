%{--
  - Copyright 2016 SimplifyOps, Inc. (http://simplifyops.com)
  -
  - Licensed under the Apache License, Version 2.0 (the "License");
  - you may not use this file except in compliance with the License.
  - You may obtain a copy of the License at
  -
  -     http://www.apache.org/licenses/LICENSE-2.0
  -
  - Unless required by applicable law or agreed to in writing, software
  - distributed under the License is distributed on an "AS IS" BASIS,
  - WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
  - See the License for the specific language governing permissions and
  - limitations under the License.
  --}%
<%--
    _scriptDetailDisplay.gsp
    
    Author: Greg Schueler <a href="mailto:greg@dtosolutions.com">greg@dtosolutions.com</a>
    Created: Jun 14, 2010 10:54:02 AM
    $Id$
 --%>
<g:set var="split" value="${script.split('(\r?\n)') as List}"/>
<g:if test="${edit}">
    <g:if test="${!noimgs}">
        <i class="rdicon script ${enc(attr:icon?:'icon-small')}"></i>
    </g:if>
    <span class=""><g:enc>${label ? label : ''}</g:enc>${split.size()} lines</span>
</g:if>
<g:else>
    <details class="more-info details-reset">
        <summary>
            <g:if test="${!noimgs}">
                <i class="rdicon script ${enc(attr:icon?:'icon-small')}"></i>
            </g:if>
            <g:enc>${label ? label : ''}</g:enc>${split.size()} lines

            <span class="more-indicator-verbiage more-info-icon"><g:icon name="chevron-right"/></span>
            <span class="less-indicator-verbiage more-info-icon"><g:icon name="chevron-down"/></span>
        </summary>

        <div class="scriptContent apply_ace"><g:enc>${script}</g:enc></div>
    </details>

</g:else>
