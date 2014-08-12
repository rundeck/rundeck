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
    _scriptDetailDisplay.gsp
    
    Author: Greg Schueler <a href="mailto:greg@dtosolutions.com">greg@dtosolutions.com</a>
    Created: Jun 14, 2010 10:54:02 AM
    $Id$
 --%>
<g:set var="rkey" value="${rkey?:g.rkey()}"/>
<g:set var="split" value="${script.split('(\r?\n)') as List}"/>
<g:if test="${edit}">
    <span class=""><g:enc>${label ? label : ''}</g:enc>[${split.size()} lines]</span>
</g:if>
<g:else>
    <g:expander key="${rkey}"><g:enc>${label ? label : ''}</g:enc>[${split.size()} lines]</g:expander>
    <div class="scriptContent expanded apply_ace" id="${enc(attr:rkey)}" style="display: none;"><g:enc>${script}</g:enc></div>
</g:else>
