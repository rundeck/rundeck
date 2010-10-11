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
    _selectFilter.gsp
    
    Author: Greg Schueler <a href="mailto:greg@dtosolutions.com">greg@dtosolutions.com</a>
    Created: Apr 16, 2010 11:17:06 AM
    $Id$
 --%>
<g:if test="${filterset}">
    <g:select name="filterName" optionKey="name" optionValue="name" from="${filterset?filterset.sort({a,b->a.name.compareTo(b.name)}):filterset}" value="${filterName}"
        noSelection="${['':noSelection?noSelection:'-select a filter-']}" onchange="setFilter('${prefName}',this.value);"/>
</g:if>