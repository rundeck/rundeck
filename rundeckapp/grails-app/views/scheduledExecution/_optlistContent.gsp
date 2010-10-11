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
    _optlistContent.gsp
    
    Author: Greg Schueler <a href="mailto:greg@dtosolutions.com">greg@dtosolutions.com</a>
    Created: Aug 2, 2010 4:15:39 PM
    $Id$
 --%>
<g:each in="${options}" var="optionsel" status="i">
    <li id="optli_${i}" class="optEntry ${highlight==optionsel?.name?'dohighlight':''} ${i%2==1?'alternate':''}">
        <g:render template="/scheduledExecution/optlistitemContent" model="${[option:optionsel,edit:edit]}"/>
    </li>
</g:each>
<g:if test="${highlight}">
    <g:javascript>
    fireWhenReady('optli_${options.size()-1}',function(){
        $$('li.optEntry.dohighlight').each(Element.highlight);
    });
    </g:javascript>
</g:if>