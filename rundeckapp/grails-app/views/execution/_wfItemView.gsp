<%@ page import="rundeck.PluginStep; rundeck.ScheduledExecution; rundeck.JobExec" %>
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
            <g:set var="jobitem" value="${item.instanceOf(JobExec)|| (item instanceof java.util.Map && item.jobName)}"/>
            <g:set var="pluginitem" value="${item.instanceOf(PluginStep)}"/>
            <span class="${edit?'autohilite autoedit':''} wfitem ${jobitem?'jobtype':pluginitem?'plugintype':'exectype'}" title="${edit?'Click to edit':''}">
            <g:if test="${jobitem}">

                %{--Display job icon and name--}%
                <g:set var="foundjob" value="${edit?null:ScheduledExecution.findScheduledExecution(item.jobGroup?item.jobGroup:null,item.jobName,project)}"/>
                <g:if test="${foundjob}">
                <g:link controller="scheduledExecution" action="show" id="${foundjob.extid}">
                    <g:if test="${!noimgs }">
                        <i class="glyphicon glyphicon-book"></i>
                    </g:if>
                    <g:enc>${(item.jobGroup?item.jobGroup+'/':'')+item.jobName}</g:enc></g:link>
                </g:if>
                <g:else>
                    <g:if test="${!noimgs }">
                        <i class="glyphicon glyphicon-book"></i>
                    </g:if>
                    <g:enc>${(item.jobGroup?item.jobGroup+'/':'')+item.jobName}</g:enc>
                </g:else>

                %{--display step description--}%
                <g:if test="${item.description}">
                    <div class="text-info">
                        <g:enc>${item.description}</g:enc>
                    </div>
                </g:if>

                %{--display argstring--}%
                <g:if test="${item.argString}">
                   <div class="argString" title="${enc(attr:item.argString)}">
                       <g:render template="/execution/execArgString" model="[argString: item.argString]"/>
                   </div>
                </g:if>

                %{--display if it is a node step--}%
                <g:if test="${item.nodeStep}">
                    <g:if test="${!noimgs && item.nodeStep}"><i class="rdicon node node-runnable icon-small"></i></g:if>
                    <span class="info note" title="${enc(code:'JobExec.nodeStep.true.description')}">
                        <g:message code="JobExec.nodeStep.true.label" />
                    </span>
                </g:if>
            </g:if>
            <g:elseif test="${pluginitem}">
                <g:if test="${!noimgs && item.description}">
                    <i class="rdicon icon-small plugin"></i>
                    <g:if test="${item && item.nodeStep}">
                        <i class="rdicon icon-small node"></i>
                    </g:if>
                </g:if>
                <g:if test="${item.description}">
                    <g:enc>${item.description}</g:enc>
                </g:if>
                <stepplugin:display step="${item}" prefix="" includeFormFields="false"
                                    showPluginIcon="${!noimgs && !item.description}"
                    showNodeIcon="${item && item.nodeStep && !noimgs && !item.description}"
                />
            </g:elseif>
            <g:else>
                <g:if test="${!noimgs}">
                    <g:set var="iname" value="${icon?:'icon-small'}"/>
                    <i class="rdicon ${item.adhocRemoteString?'shell':item.adhocLocalString?'script':'scriptfile'} ${enc(attr:iname)}"></i>
                </g:if>
                <g:if test="${item.adhocRemoteString}">
                    <span class="argString"><g:truncate max="150" showtitle="true"><g:enc>${item.adhocRemoteString}</g:enc></g:truncate></span>
                </g:if>
                <g:elseif test="${item.adhocLocalString}">
                    <g:render template="/execution/scriptDetailDisplay" model="${[rkey: g.rkey(),script:item.adhocLocalString,label: '',edit:edit]}"/>
                </g:elseif>
                <g:if test="${item.description}">
                    <div class="text-info">
                        <g:enc>${item.description}</g:enc>
                    </div>
                </g:if>
                <g:elseif test="${item.adhocFilepath}">
                    <g:if test="${item.adhocFilepath=~/^https?:/}">
                        <g:set var="urlString" value="${item.adhocFilepath.replaceAll('^(https?://)([^:@/]+):[^@/]*@', '$1$2:****@')}"/>
                        <span class="argString"><g:truncate max="150"
                                                            showtitle="true"><g:enc>${urlString}</g:enc></g:truncate></span>
                    </g:if>
                    <g:else>
                        <span class="argString"><g:truncate max="150"  showtitle="true"><g:enc>${item.adhocFilepath}</g:enc></g:truncate></span>
                    </g:else>
                </g:elseif>
                <g:if test="${item.scriptInterpreter}">
                    <span class="text-muted"><g:message code="executed.as" />:</span>
                    <span class="argString">
                        <g:enc>${item.scriptInterpreter}</g:enc>
                        <g:if test="${item.interpreterArgsQuoted}">
                            &quot;
                        </g:if>
                        <g:if test="${!item.scriptInterpreter.contains('${scriptfile}')}">
                        <span title="${g.message(code:'placeholder.for.the.script.file')}"><g:message code="dollar.scriptfile" /></span>
                        </g:if>
                    </span>
                </g:if>
                <g:if test="${item.argString}">
                   <span class="argString"><g:truncate max="150"  showtitle="true"><g:enc>${item.argString}</g:enc></g:truncate></span>
                </g:if>
                <g:if test="${item.interpreterArgsQuoted}">
                    <span class="argString">&quot;</span>
                </g:if>
            </g:else>
            </span>
