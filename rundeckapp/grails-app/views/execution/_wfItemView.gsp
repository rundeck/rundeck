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
    _wfItemView.gsp
    
    Author: Greg Schueler <a href="mailto:greg@dtosolutions.com">greg@dtosolutions.com</a>
    Created: Jul 26, 2010 5:12:38 PM
    $Id$
 --%>
            <g:set var="jobitem" value="${item instanceof JobExec || (item instanceof java.util.Map && item.jobName)}"/>
            <span class="${edit?'autohilite autoedit':''} wfitem ${jobitem?'jobtype':'exectype'}" title="${edit?'Click to edit':''}">

            <g:if test="${jobitem}">
                <g:if test="${!noimgs}"><g:img file="icon-small-job.png" width="16px" height="16px"/></g:if>
                ${item.jobGroup?item.jobGroup.encodeAsHTML()+'/':''}${item.jobName.encodeAsHTML()}
                <g:if test="${item.argString}">
                   <span class="argString"><g:truncate max="50"  showtitle="true">${item.argString.encodeAsHTML()}</g:truncate></span>
                </g:if>
            </g:if>
            <g:else>
                <g:if test="${!noimgs}"><g:img file="icon-small-shell.png" width="16px" height="16px"/></g:if>
                <g:if test="${item.adhocRemoteString}">
                    <span class="argString"><g:truncate max="60" showtitle="true">${item.adhocRemoteString.encodeAsHTML()}</g:truncate></span>
                </g:if>
                <g:elseif test="${item.adhocLocalString}">
                    <g:render template="/execution/scriptDetailDisplay" model="${[script:item.adhocLocalString,label:'Script: ']}"/>
                </g:elseif>
                <g:elseif test="${item.adhocFilepath}">
                    <span class="argString"><g:truncate max="60"  showtitle="true">${item.adhocFilepath.encodeAsHTML()}</g:truncate></span>
                </g:elseif>
                <g:if test="${item.argString}">
                   <span class="argString"><g:truncate max="45"  showtitle="true">${item.argString.encodeAsHTML()}</g:truncate></span>
                </g:if>
            </g:else>

            </span>