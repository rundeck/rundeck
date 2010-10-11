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
   _execDetailsAdhocCommand.gsp

   Author: Greg Schueler <a href="mailto:greg@dtosolutions.com">greg@dtosolutions.com</a>
   Created: Apr 20, 2010 2:44:47 PM
   $Id$
--%>

<g:set var="rkey" value="${g.rkey()}"/>
<g:if test="${execdata?.adhocRemoteString}">
    <tr>
        <td>Command:</td>
        <td><code>${execdata?.adhocRemoteString?.encodeAsHTML()}</code></td>
    </tr>

</g:if>
<g:elseif test="${execdata?.adhocLocalString}">
    <tr>
        <td>Script:</td>
        <td>
            <g:render template="/execution/scriptDetailDisplay" model="${[script:execdata?.adhocLocalString]}"/>
        </td>
    </tr>
    <g:if test="${execdata?.argString}">
    <tr>
        <td>Args:</td>
        <td><pre><code>${execdata?.argString?.encodeAsHTML()}</code></pre></td>
    </tr>
    </g:if>
</g:elseif>
<g:elseif test="${execdata?.adhocFilepath}">
    <tr>
        <td>File path:</td>
        <td><pre><code>${execdata?.adhocFilepath?.encodeAsHTML()}</code></pre></td>
    </tr>
    <tr>
        <td>Args:</td>
        <td><pre><code>${execdata?.argString?.encodeAsHTML()}</code></pre></td>
    </tr>

</g:elseif>
