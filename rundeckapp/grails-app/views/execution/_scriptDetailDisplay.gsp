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
<g:set var="rkey" value="${g.rkey()}"/>
<g:set var="split" value="${script.split('(\r?\n)') as List}"/>
<g:expander key="${rkey}">${label?label:''}${split.size()} lines</g:expander>
<g:set var="encoded" value="${split.collect{it.encodeAsHTML()}}"/>
<div class="scriptContent expanded" id="${rkey}" style="display:none">${ encoded.join('&nbsp;<br>') }</div>